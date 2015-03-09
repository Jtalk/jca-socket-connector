/*
 * Copyright (C) 2015 Jtalk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.jtalk.socketconnector;

import me.jtalk.socketconnector.api.TCPMessageImpl;
import me.jtalk.socketconnector.api.TCPDisconnectionNotificationImpl;
import me.jtalk.socketconnector.api.TCPMessage;
import me.jtalk.socketconnector.api.ConnectionClosedException;
import me.jtalk.socketconnector.api.TCPMessageListener;
import me.jtalk.socketconnector.api.TCPDisconnectionNotification;
import me.jtalk.socketconnector.io.TCPManager;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.TransactionSupport;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;
import javax.validation.Validation;
import javax.validation.Validator;

@Connector(
	displayName = Metadata.NAME,
	description = Metadata.DESCRIPTION,
	vendorName = Metadata.VENDOR,
	eisType = Metadata.EIS_TYPE,
	version = Metadata.VERSION,
	transactionSupport = TransactionSupport.TransactionSupportLevel.NoTransaction
)
public class SocketResourceAdapter implements ResourceAdapter {

	private static final Logger log = Logger.getLogger(SocketResourceAdapter.class.getName());
	private static final Method TCP_MESSAGE_INIT_METHOD;
	private static final Method TCP_MESSAGE_DATA_METHOD;
	private static final Method TCP_MESSAGE_DISCONNECT_METHOD;

	private WorkManager workManager;

	private final AtomicBoolean running = new AtomicBoolean(false);
	private final ConcurrentHashMap<Long, TCPManagerStorage> tcpManagers = new ConcurrentHashMap<>();

	private final Validator validator;

	static {
		try {
			TCP_MESSAGE_INIT_METHOD = TCPMessageListener.class.getMethod("initialized");
			TCP_MESSAGE_DATA_METHOD = TCPMessageListener.class.getMethod("onMessage", TCPMessage.class);
			TCP_MESSAGE_DISCONNECT_METHOD = TCPMessageListener.class.getMethod("disconnected", TCPDisconnectionNotification.class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Methods initialized, onMessage or disconnected not found in TCPMessageListener, poor refactoring?", e);
		}
	}

	public SocketResourceAdapter() throws IOException {
		this.validator = Validation.buildDefaultValidatorFactory().getValidator();
		log.info(String.format("Soccket resource adapter is instantiated: validator is %s", this.validator == null ? "null" : "instantiated"));
	}

	@Override
	public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
		log.info("Starting Socket resource adapter");
		this.workManager = ctx.getWorkManager();
		this.running.set(true);
		log.info("Socket resource adapter is started");
	}

	@Override
	public void stop() {
		log.info("Stopping Socket resource adapter");
		this.running.set(false);
		this.stopTCP();
		this.workManager = null;
		log.info("Socket resource adapter is stopped");
	}

	@Override
	public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {
		log.log(Level.INFO, "Endpoint activation request received for class {0}", endpointFactory.getEndpointClass().getCanonicalName());
		if (!this.running.get()) {
			throw new ResourceException("This resource adapter is stopped");
		}
		if (!(spec instanceof TCPActivationSpec)) {
			throw new NotSupportedException("Activation spec supplied has unsupported type " + spec.getClass().getCanonicalName());
		}
		else
		{
			this.activateTCP(endpointFactory, (TCPActivationSpec)spec);
			log.log(Level.INFO, "Endpoint activated for class {0}", endpointFactory.getEndpointClass().getCanonicalName());
		}
	}

	@Override
	public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
		log.log(Level.INFO, "Endpoint deactivation request received for class {0}", endpointFactory.getEndpointClass().getCanonicalName());
		if (!this.running.get()) {
			log.warning("Endpoint deactivation called on disabled resource adapter");
			return;
		}
		if (!(spec instanceof TCPActivationSpec)) {
			log.warning("Endpoint deactivation called with invalid ActivationSpec type");
		}
		else
		{
			log.log(Level.INFO, "Endpoint deactivation for class {0}", endpointFactory.getEndpointClass().getCanonicalName());
			this.deactivateTCP(endpointFactory, (TCPActivationSpec)spec);
			log.log(Level.INFO, "Endpoint deactivated for class {0}", endpointFactory.getEndpointClass().getCanonicalName());
		}
	}

	@Override
	public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
		return null;
	}

	public void notifyReceived(long clientId, long id, byte[] data, SocketAddress local, SocketAddress remote) {
		TCPMessage message = new TCPMessageImpl(id, remote, local, data);
		this.sendEndpoints(clientId, TCP_MESSAGE_DATA_METHOD, message);
	}

	public void notifyShutdown(long clientId, long id, SocketAddress local, SocketAddress remote, Throwable cause) {
		TCPDisconnectionNotification notification = new TCPDisconnectionNotificationImpl(id, remote, local, cause);
		this.sendEndpoints(clientId, TCP_MESSAGE_DISCONNECT_METHOD, notification);
	}

	Validator getValidator() {
		return this.validator;
	}

	long createTCPConnection(long clientId, InetSocketAddress target) throws ResourceException {
		TCPManager manager = this.getTCPManager(clientId);
		if (manager == null) {
			throw new ConnectionClosedException("Connection is already closed");
		}
		return manager.connect(target);
	}

	void sendTCP(long clientId, long id, ByteBuffer data) throws ResourceException {
		TCPManager manager = this.getTCPManager(clientId);
		if (manager == null) {
			throw new ConnectionClosedException("Connection is already closed");
		}
		manager.send(id, data);
	}

	void closeTCPConnection(long clientId, long id) throws ResourceException {
		TCPManager manager = this.getTCPManager(clientId);
		if (manager == null) {
			throw new ConnectionClosedException("Connection is already closed");
		}
		manager.close(id);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	private void activateTCP(final MessageEndpointFactory factory, TCPActivationSpec spec) throws ResourceException {
		synchronized(this.tcpManagers) {
			long id = spec.getClientId();
			TCPManagerStorage newStorage = new TCPManagerStorage();
			TCPManagerStorage storage = this.tcpManagers.putIfAbsent(id, newStorage);
			if (storage == null) {
				TCPManager manager = new TCPManager(this, id, spec);
				newStorage.setManager(manager);
				newStorage.setSpec(spec);
				newStorage.addEndpoint(factory);
			} else {
				storage.addEndpoint(factory);
			}
			this.workManager.scheduleWork(new SimpleWork(() -> this.sendEndpoint(factory, TCP_MESSAGE_INIT_METHOD, null)));
		}
	}

	private void deactivateTCP(MessageEndpointFactory factory, TCPActivationSpec spec) {
		synchronized(this.tcpManagers) {
			long id = spec.getClientId();
			TCPManagerStorage storage = this.tcpManagers.get(id);
			storage.removeEndpoint(factory);
			if (storage.isEmpty()) {
				this.tcpManagers.remove(id);
				TCPManager manager = storage.getManager();
				if (manager != null) {
					manager.close();
				}
			}
		}
	}

	private void stopTCP() {
		synchronized(this.tcpManagers) {
			Iterator<Map.Entry<Long, TCPManagerStorage>> iter = this.tcpManagers.entrySet().iterator();
			while (iter.hasNext()) {
				TCPManagerStorage s = iter.next().getValue();
				TCPManager manager = s.getManager();
				if (manager != null) {
					manager.close();
				}
				iter.remove();
			}
		}
	}

	private TCPManager getTCPManager(long clientId) {
		TCPManagerStorage s = this.tcpManagers.get(clientId);
		if (s == null) {
			return null;
		} else {
			return s.getManager();
		}
	}

	private <T> void sendEndpoints(long clientId, Method target, T message) {
		TCPManagerStorage storage = this.tcpManagers.get(clientId);
		if (storage == null) {
			log.finer("Message sending requested for deactivated client");
			return;
		}

		Iterable<MessageEndpointFactory> endpoints = storage.endpoints();
		for (MessageEndpointFactory factory : endpoints) {
			this.sendEndpoint(factory, target, message);
		}
	}

	private <T> void sendEndpoint(MessageEndpointFactory factory, Method target, T message) {
		try {
			MessageEndpoint endpoint = factory.createEndpoint(null);
			endpoint.beforeDelivery(target);
			try {
				if (message == null) {
					target.invoke(endpoint);
				} else {
					target.invoke(endpoint, message);
				}
			} catch (IllegalAccessException | InvocationTargetException e) {
				log.log(Level.SEVERE, "Exception on message endpoint invocation", e);
			}
			endpoint.afterDelivery();
			endpoint.release();
		} catch (UnavailableException e) {
			log.log(Level.SEVERE, "Message endpoint is unavailable", e);
		} catch (ResourceException | NoSuchMethodException e) {
			log.log(Level.SEVERE, "Exception on message endpoint processing", e);
		}
	}
}
