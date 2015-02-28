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
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
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

public class ManagedSocketConnectionProxy implements ManagedConnection {

	private final long ID;
	private final SocketResourceAdapter adapter;
	private final SocketConnectionRequestInfo info;

	private final AtomicReference<PrintWriter> logWriter = new AtomicReference<>(null);

	private final AtomicReference<SocketConnection> connection = new AtomicReference<>(null);
	private final Set<ConnectionEventListener> eventListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private final AtomicBoolean isRunning = new AtomicBoolean(false);

	ManagedSocketConnectionProxy(long id, SocketResourceAdapter adapter, SocketConnectionRequestInfo info) throws ResourceException {

		this.ID = id;
		this.adapter = adapter;
		this.info = info;
		this.isRunning.set(true);

		this.validateInfo(info);
	}

	@Override
	public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {

		if (!this.isRunning.get()) {
			throw new ResourceException("Socket connection requested from disconnected managed connection");
		}

		if (!this.info.equals(cxRequestInfo)) {
			throw new ResourceException("Connection request info supplied does not match this managed connection");
		}

		SocketConnection conn = new SocketConnection(this);
		this.replaceActiveConnection(conn);

		return conn;
	}

	@Override
	public void destroy() throws ResourceException {

		this.cleanup();
		this.isRunning.set(false);
		this.adapter.connectionClose(this.ID, this.info);
		this.notifyEvent(new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED), ConnectionEventListener::connectionClosed);
	}

	@Override
	public void cleanup() throws ResourceException {
		this.replaceActiveConnection(null);
	}

	@Override
	public void associateConnection(Object connection) throws ResourceException {
		if (!(connection instanceof SocketConnection)) {
			throw new ResourceException("Connection supplied is not a SocketConnection");
		}

		SocketConnection newConnection = (SocketConnection)connection;
		newConnection.reassign(this);
		this.replaceActiveConnection(newConnection);
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

	public void log(String message) {
		PrintWriter writer = this.logWriter.get();
		if (writer != null) {
			writer.print(message);
		}
	}

	private void replaceActiveConnection(SocketConnection newConnection) {

		SocketConnection old = this.connection.getAndSet(newConnection);
		if (old != null) {
			old.invalidate();
		}
	}

	private void notifyEvent(ConnectionEvent event, BiConsumer<ConnectionEventListener, ConnectionEvent> method) {
		for (ConnectionEventListener listener : this.eventListeners) {
			method.accept(listener, event);
		}
	}

	private void validateInfo(SocketConnectionRequestInfo info) throws ResourceException {
		Set<ConstraintViolation<SocketConnectionRequestInfo>> violations = this.adapter.getValidator().validate(info, Default.class);
		if (violations.isEmpty()) {
			return;
		}

		for (ConstraintViolation<SocketConnectionRequestInfo> violation : violations) {
			String message = violation.getMessage();
			this.log(message);
		}

		throw new ResourceException("Socket connection request info constraints violation failed");
	}
}
