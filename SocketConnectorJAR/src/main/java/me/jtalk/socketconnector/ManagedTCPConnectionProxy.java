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

import me.jtalk.socketconnector.utils.ValidationUtils;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
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
import lombok.extern.slf4j.Slf4j;
import me.jtalk.socketconnector.api.TCPConnection;
import me.jtalk.socketconnector.utils.ConnectionRequestInfoUtils;
import me.jtalk.socketconnector.utils.ConnectorLogger;
import me.jtalk.socketconnector.utils.NamedIdObject;

public class ManagedTCPConnectionProxy implements ManagedConnection {

	private static final Logger LOG = Logger.getLogger(ManagedTCPConnectionProxy.class.getName());

	private long ID = 0;
	private long clientID = 0;
	private boolean listening = false;

	private final SocketResourceAdapter adapter;

	private final ConnectorLogger logWriter = new ConnectorLogger();
	private final AtomicReference<TCPConnectionImpl> connection = new AtomicReference<>(null);
	private final EventListeners eventListeners = new EventListeners();

	private final AtomicBoolean isRunning = new AtomicBoolean(false);

	public ManagedTCPConnectionProxy(SocketResourceAdapter adapter, NewTCPConnectionRequest info) throws ResourceException {
		this.adapter = adapter;
		reset(info);
	}

	public ManagedTCPConnectionProxy(SocketResourceAdapter adapter, ExistingTCPConnectionRequest info) throws ResourceException {
		this.adapter = adapter;
		reset(info);
	}

	public long getId() {
		return this.ID;
	}

	public void disconnect() throws ResourceException {

		LOG.finer("Connection disconnect requested");

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
	public TCPConnection getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {

		if (!this.isRunning.get()) {
			throw new ResourceException("Socket connection requested from disconnected managed connection");
		}
		ConnectionRequestInfoUtils.checkInfo(clientID, ID, cxRequestInfo);
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
		logWriter.setLogWriter(out);
	}

	@Override
	public PrintWriter getLogWriter() throws ResourceException {
		return logWriter.getLogWriter();
	}

	public void requestCleanup() throws ResourceException {
		TCPConnectionImpl conn = connection.get();
		if (conn == null) {
			LOG.warning("Cleanup requested without connection associated");
			return;
		}
		final ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
		event.setConnectionHandle(conn);
		eventListeners.notifyEvent(event, ConnectionEventListener::connectionClosed);
	}

	public void reset(NewTCPConnectionRequest request) throws ResourceException {

		LOG.finer("Resetting managed connection proxy for new connection");

		ValidationUtils.validateInfo(adapter.getValidator(), logWriter::printLog, request);

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

	public void reset(ExistingTCPConnectionRequest request) throws ResourceException {

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

	protected void replaceActiveConnection(TCPConnectionImpl newConnection) {

		LOG.fine(String.format("Connection replacing request"));

		TCPConnectionImpl old = this.connection.getAndSet(newConnection);
		if (old != null) {
			old.invalidate();
		}
	}
}
