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

import me.jtalk.socketconnector.io.TCPManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Resource;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.TransactionSupport;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;
import javax.validation.Validator;
import sun.misc.CEFormatException;

@Connector(
	displayName = Metadata.NAME,
	description = Metadata.DESCRIPTION,
	vendorName = Metadata.VENDOR,
	eisType = Metadata.EIS_TYPE,
	version = Metadata.VERSION,
	transactionSupport = TransactionSupport.TransactionSupportLevel.NoTransaction
)
public class SocketResourceAdapter implements ResourceAdapter {

	private WorkManager workManager;

	private boolean running;
	private final ConcurrentHashMap<Long, TCPManagerStorage> tcpManagers = new ConcurrentHashMap<>();

	@Resource
	private Validator validator;

	public SocketResourceAdapter() throws IOException {
	}

	@Override
	public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
		this.workManager = ctx.getWorkManager();
		this.running = true;
	}

	@Override
	public void stop() {
		this.running = false;
		this.stopTCP();
		this.workManager = null;
	}

	@Override
	public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {
		if (!this.running) {
			throw new ResourceException("This resource adapter is stopped");
		}
		if (!(spec instanceof TCPActivationSpec)) {
			throw new NotSupportedException("Activation spec supplied has unsupported type " + spec.getClass().getCanonicalName());
		}
		else
		{
			this.activateTCP(endpointFactory, (TCPActivationSpec)spec);
		}
	}

	@Override
	public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
		if (!this.running) {
			return;
		}
		if (!(spec instanceof TCPActivationSpec)) {
			return;
		}
		else
		{
			this.deactivateTCP(endpointFactory, (TCPActivationSpec)spec);
		}

	}

	@Override
	public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
		return null;
	}

	Validator getValidator() {
		return this.validator;
	}

	long createTCPConnection(long clientId, InetSocketAddress target) throws ResourceException {
		TCPManager manager = this.getTCPManager(clientId);
		if (manager == null) {
			throw new ConnectionClosedException("Connection is already clised");
		}
		return manager.connect(target);
	}

	void sendTCP(long clientId, long id, ByteBuffer data) throws ResourceException {
		TCPManager manager = this.getTCPManager(clientId);
		if (manager == null) {
			throw new ConnectionClosedException("Connection is already clised");
		}
		manager.send(id, data);
	}

	void closeTCPConnection(long clientId, long id) throws ResourceException {
		TCPManager manager = this.getTCPManager(clientId);
		if (manager == null) {
			throw new ConnectionClosedException("Connection is already clised");
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

	private void activateTCP(MessageEndpointFactory factory, TCPActivationSpec spec) throws ResourceException {
		synchronized(this.tcpManagers) {
			long id = spec.getClientId();
			TCPManagerStorage newStorage = new TCPManagerStorage();
			TCPManagerStorage storage = this.tcpManagers.putIfAbsent(id, newStorage);
			if (storage == newStorage) {
				TCPManager manager = new TCPManager(this, id, spec);
				storage.setManager(manager);
				storage.setSpec(spec);
				storage.addEndpoint(factory);
			}
		}
	}

	private void deactivateTCP(MessageEndpointFactory factory, TCPActivationSpec spec) {
		synchronized(this.tcpManagers) {
			long id = spec.getClientId();
			TCPManagerStorage storage = this.tcpManagers.get(id);
			storage.removeEndpoint(factory);
			if (storage.isEmpty()) {
				this.tcpManagers.remove(id);
				storage.getManager().close();
			}
		}
	}

	private void stopTCP() {
		synchronized(this.tcpManagers) {
			Iterator<Map.Entry<Long, TCPManagerStorage>> iter = this.tcpManagers.entrySet().iterator();
			while (iter.hasNext()) {
				TCPManagerStorage s = iter.next().getValue();
				s.getManager().close();
				iter.remove();
			}
		}
	}

	private TCPManager getTCPManager(long clientId) {
		TCPManagerStorage s = this.tcpManagers.get(clientId);
		return s.getManager();
	}
}
