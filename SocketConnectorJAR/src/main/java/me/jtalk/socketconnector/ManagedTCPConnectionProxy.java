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

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import javax.validation.ConstraintViolation;
import javax.validation.groups.Default;

public class ManagedTCPConnectionProxy implements ManagedConnection {

	private static final Logger LOG = Logger.getLogger(ManagedTCPConnectionProxy.class.getName());

	private long ID = 0;
	private long clientID = 0;
	private boolean listening = false;

	private final SocketResourceAdapter adapter;

	private final AtomicReference<PrintWriter> logWriter = new AtomicReference<>(null);
	private final AtomicReference<TCPConnectionImpl> connection = new AtomicReference<>(null);
	private final Set<ConnectionEventListener> eventListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private final AtomicBoolean isRunning = new AtomicBoolean(false);

	ManagedTCPConnectionProxy(SocketResourceAdapter adapter, NewTCPConnectionRequest info) throws ResourceException {

		this.adapter = adapter;
		this.reset(info);
	}

	ManagedTCPConnectionProxy(SocketResourceAdapter adapter, ExistingTCPConnectionRequest info) throws ResourceException {

		this.adapter = adapter;
		this.reset(info);
	}

	public long getId() {
		return this.ID;
	}

	public void disconnect() throws ResourceException {

		LOG.finer("Connection disconnect requested");

		this.cleanup();
		this.adapter.closeTCPConnection(this.clientID, this.ID);
	}

	public void send(ByteBuffer data) throws ResourceException {
		if (this.listening) {
			throw new NotSupportedException("Sending data through listening socket");
		} else {
			this.adapter.sendTCP(this.clientID, this.ID, data);
		}
	}

	@Override
	public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {

		if (!this.isRunning.get()) {
			throw new ResourceException("Socket connection requested from disconnected managed connection");
		}

		this.checkInfo(cxRequestInfo);

		TCPConnectionImpl conn = new TCPConnectionImpl(this);
		this.replaceActiveConnection(conn);

		return conn;
	}

	@Override
	public void destroy() throws ResourceException {
		LOG.log(Level.FINEST, "Connection destroyal requested");
		this.disconnect();
	}

	@Override
	public void cleanup() throws ResourceException {
		LOG.log(Level.FINEST, "Connection cleanup requested");
		this.replaceActiveConnection(null);
		this.isRunning.set(false);
	}

	@Override
	public void associateConnection(Object connection) throws ResourceException {
		LOG.finer("Connection association replacement requested");
		if (!(connection instanceof TCPConnectionImpl)) {
			throw new ResourceException("Connection supplied is not a TCPConnectionImpl");
		}

		TCPConnectionImpl newConnection = (TCPConnectionImpl)connection;
		newConnection.reassign(this);
		this.replaceActiveConnection(newConnection);
		LOG.finer("Connection association replacement completed");
	}

	@Override
	public void addConnectionEventListener(ConnectionEventListener listener) {
		this.eventListeners.add(listener);
	}

	@Override
	public void removeConnectionEventListener(ConnectionEventListener listener) {
		this.eventListeners.remove(listener);
	}

	@Override
	public XAResource getXAResource() throws ResourceException {
		throw new NotSupportedException("Transactions are not supported");
	}

	@Override
	public LocalTransaction getLocalTransaction() throws ResourceException {
		throw new NotSupportedException("Transactions are not supported");
	}

	@Override
	public ManagedConnectionMetaData getMetaData() throws ResourceException {
		throw new NotSupportedException("Connection metadata is not supported");
	}

	@Override
	public void setLogWriter(PrintWriter out) throws ResourceException {
		if (!this.logWriter.compareAndSet(null, out)) {
			throw new javax.resource.spi.IllegalStateException("LogWriter is set more than once");
		}
	}

	@Override
	public PrintWriter getLogWriter() throws ResourceException {
		return this.logWriter.get();
	}

	public void printLog(String message) {
		PrintWriter writer = this.logWriter.get();
		if (writer != null) {
			writer.print(message);
		}
	}

	void requestCleanup() throws ResourceException {
		TCPConnectionImpl conn = this.connection.get();
		if (conn == null) {
			LOG.warning("Cleanup requested without connection associated");
			return;
		}
		final ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
		event.setConnectionHandle(conn);
		this.notifyEvent(event, ConnectionEventListener::connectionClosed);
	}

	final void reset(NewTCPConnectionRequest request) throws ResourceException {

		LOG.finer("Resetting managed connection proxy for new connection");

		this.validateInfo(request);

		this.clientID = request.getUid();
		this.listening = request.isListening();
		if (request.isListening()) {
			this.ID = this.adapter.listenTCP(this.clientID, request.createInetAddress());
		} else {
			this.ID = this.adapter.createTCPConnection(this.clientID, request.createInetAddress());
		}
		if (this.isRunning.getAndSet(true)) {
			LOG.severe("Managed connection reset while being running");
		}
	}

	final void reset(ExistingTCPConnectionRequest request) throws ResourceException {

		LOG.finer("Resetting managed connection proxy for existing connection");

		this.clientID = request.getUid();
		this.ID = request.getId();
		this.listening = false;
		try {
			this.adapter.isTCPListener(request.getUid(), request.getId());
		} catch (ResourceException e) {
			// Suppressed
		}
		if (this.isRunning.getAndSet(true)) {
			LOG.severe("Managed connection reset while being running");
		}
	}

	private void replaceActiveConnection(TCPConnectionImpl newConnection) {

		LOG.fine(String.format("Connection replacing request"));

		TCPConnectionImpl old = this.connection.getAndSet(newConnection);
		if (old != null) {
			old.invalidate();
		}
	}

	private void notifyEvent(ConnectionEvent event, final BiConsumer<ConnectionEventListener, ConnectionEvent> method) {
		if (this.eventListeners.isEmpty()) {
			LOG.log(Level.SEVERE, "Sending event {0} to empty listeners set", event.getId());
			return;
		}

		this.eventListeners.forEach(l -> {
			LOG.log(Level.FINEST, "Sending event {0} to listener", event.getId());
			method.accept(l, event);
		});
	}

	private void validateInfo(NewTCPConnectionRequest info) throws ResourceException {
		Set<ConstraintViolation<NewTCPConnectionRequest>> violations = this.adapter.getValidator().validate(info, Default.class);
		if (violations.isEmpty()) {
			return;
		}

		for (ConstraintViolation<NewTCPConnectionRequest> violation : violations) {
			String message = violation.getMessage();
			this.printLog(message);
		}

		throw new ResourceException("Socket connection request info constraints violation failed");
	}

	private void checkInfo(ConnectionRequestInfo rawInfo) throws ResourceException {
		if (rawInfo instanceof ExistingTCPConnectionRequest) {
			ExistingTCPConnectionRequest request = (ExistingTCPConnectionRequest)rawInfo;
			if (request.getUid() != this.clientID || request.getId() != this.ID) {
				throw new ResourceException("Incompatible UID and ConnectionID supplied to managed connection");
			}
		} else if (rawInfo instanceof NewTCPConnectionRequest) {
			NewTCPConnectionRequest request = (NewTCPConnectionRequest)rawInfo;
			if (request.getUid() != this.clientID) {
				throw new ResourceException("Incompatible UID supplied to managed connection");
			}
		} else {
			throw new ResourceException(String.format("Incompatible connection request info type %s supplied to managed connection",
				rawInfo.getClass().getCanonicalName()));
		}
	}
}
