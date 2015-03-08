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

import me.jtalk.socketconnector.api.TCPConnectionFactory;
import me.jtalk.socketconnector.api.TCPConnection;
import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;

@ConnectionDefinition(
	connection = TCPConnection.class,
	connectionImpl = TCPConnectionImpl.class,
	connectionFactory = TCPConnectionFactory.class,
	connectionFactoryImpl = TCPConnectionFactoryImpl.class
)
public class ManagedTCPConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation {

	private static final Logger log = Logger.getLogger(ManagedTCPConnectionFactory.class.getName());
	static final Metadata METADATA = new Metadata();

	private AtomicReference<SocketResourceAdapter> adapter = new AtomicReference<>();
	private AtomicReference<PrintWriter> logWriter = new AtomicReference<>();

	public ManagedTCPConnectionFactory() {
		log.info("Managed TCP connection factory instantiated");
	}

	@Override
	public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
		log.info("Managed TCP connection factory received connection factory request");
		TCPConnectionFactoryImpl factory = new TCPConnectionFactoryImpl(this, cxManager);
		return factory;
	}

	@Override
	public Object createConnectionFactory() throws ResourceException {
		throw new NotSupportedException("SocketConnector is not intended for unmamaged environment-based usage");
	}

	@Override
	public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
		log.info("Managed TCP connection factory received managed connection request");
		if (!(cxRequestInfo instanceof NewTCPConnectionRequest)) {
			throw new ResourceException("Request info provided is not a SocketConnectionRequestInfo");
		}
		NewTCPConnectionRequest info = (NewTCPConnectionRequest)cxRequestInfo;
		ManagedTCPConnectionProxy newConnection = new ManagedTCPConnectionProxy(this.adapter.get(), info);
		return newConnection;
	}

	@Override
	public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
		throw new NotSupportedException("Managed connections pooling is not supported");
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

	@Override
	public ResourceAdapter getResourceAdapter() {
		return this.adapter.get();
	}

	@Override
	public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
		log.info("Managed TCP connection factory received adapter assignment request");
		if (!(ra instanceof SocketResourceAdapter)) {
			throw new ResourceException("Resource adapter supplied to ManagedSocketConnectionFactory is not a SocketResourceAdapter");
		}
		SocketResourceAdapter newAdapter = (SocketResourceAdapter)ra;
		if (!this.adapter.compareAndSet(null, newAdapter)) {
			throw new javax.resource.spi.IllegalStateException("Resource adapter is applied more than once");
		}
	}

	private void log(String message) {
		PrintWriter writer = this.logWriter.get();
		if (writer != null) {
			writer.println(message);
		}
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
