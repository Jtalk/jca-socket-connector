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
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import javax.resource.NotSupportedException;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.ValidatingManagedConnectionFactory;
import javax.security.auth.Subject;

@ConnectionDefinition(
	connection = Connection.class,
	connectionImpl = SocketConnection.class,
	connectionFactory = ConnectionFactory.class,
	connectionFactoryImpl = SocketConnectionFactory.class
)
public class ManagedSocketConnectionFactory implements ManagedConnectionFactory, ValidatingManagedConnectionFactory, ResourceAdapterAssociation {

	static final Metadata METADATA = new Metadata();

	private final ConcurrentLinkedDeque<SocketConnectionFactory> factories = new ConcurrentLinkedDeque<>();
	private final SocketRecordFactory recordFactory = new SocketRecordFactory();

	private PrintWriter logWriter;

	@Override
	public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
		SocketConnectionFactory factory = new SocketConnectionFactory(this, cxManager);
		this.factories.add(factory);
		return factory;
	}

	@Override
	public Object createConnectionFactory() throws ResourceException {
		throw new NotSupportedException("SocketConnector is not intended for unmamaged environment-based usage");
	}

	@Override
	public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
		if (!(cxRequestInfo instanceof SocketConnectionRequestInfo)) {
			throw new ResourceException("Request info provided is not a SocketConnectionRequestInfo");
		}
		SocketConnectionRequestInfo info = (SocketConnectionRequestInfo)cxRequestInfo;
		return new ManagedSocketConnection(info);
	}

	@Override
	public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
		if (!(cxRequestInfo instanceof SocketConnectionRequestInfo)) {
			throw new ResourceException("Request info provided is not a SocketConnectionRequestInfo");
		}
		SocketConnectionRequestInfo info = (SocketConnectionRequestInfo)cxRequestInfo;
		for (Object obj : connectionSet) {
			if (!(obj instanceof ManagedSocketConnection)) {
				continue;
			}
			ManagedSocketConnection conn = (ManagedSocketConnection)obj;
			if (conn.applies(info)) {
				return conn;
			}
		}
		return null;
	}

	@Override
	public void setLogWriter(PrintWriter out) throws ResourceException {
		this.logWriter = out;
	}

	@Override
	public PrintWriter getLogWriter() throws ResourceException {
		return this.logWriter;
	}

	@Override
	public Set getInvalidConnections(Set connectionSet) throws ResourceException {
		return (Set)connectionSet.stream()
			.filter(obj -> {
				if (!(obj instanceof ManagedSocketConnection)) {
					return true;
				}
				ManagedSocketConnection conn = (ManagedSocketConnection)obj;
				return !conn.isActive();
			})
			.collect(Collectors.toSet());
	}

	SocketRecordFactory getRecordFactory() {
		return this.recordFactory;
	}

	private void log(String message) {
		PrintWriter writer = this.logWriter;
		if (writer != null) {
			writer.println(message);
		}
	}

	@Override
	public ResourceAdapter getResourceAdapter() {
		return this.adapter.get();
	}

	@Override
	public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
		if (!(ra instanceof SocketResourceAdapter)) {
			throw new ResourceException("Resource adapter supplied to ManagedSocketConnectionFactory is not a SocketResourceAdapter");
		}
		SocketResourceAdapter newAdapter = (SocketResourceAdapter)ra;
		if (!this.adapter.compareAndSet(null, newAdapter)) {
			throw new javax.resource.spi.IllegalStateException("Resource adapter is applied more than once");
		}
		newAdapter.registerConnectionFactory(this);
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public int hashCode() {
		return super.hashCode(); //To change body of generated methods, choose Tools | Templates.
	}
}
