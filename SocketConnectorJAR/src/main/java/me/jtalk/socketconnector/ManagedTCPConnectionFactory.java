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
import java.util.Iterator;
import java.util.Set;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.jtalk.socketconnector.utils.ConnectorLogger;

@Slf4j
@ConnectionDefinition(
	connection = TCPConnection.class,
	connectionImpl = TCPConnectionImpl.class,
	connectionFactory = TCPConnectionFactory.class,
	connectionFactoryImpl = TCPConnectionFactoryImpl.class
)
public class ManagedTCPConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation {

	public static final Metadata METADATA = new Metadata();

	@Getter
	private volatile SocketResourceAdapter resourceAdapter;

	private final ConnectorLogger logWriter = new ConnectorLogger();

	public ManagedTCPConnectionFactory() {
		log.trace("Managed TCP connection factory instantiated");
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
		if (cxRequestInfo instanceof NewTCPConnectionRequest) {
			ManagedTCPConnectionProxy newConnection = new ManagedTCPConnectionProxy(resourceAdapter, (NewTCPConnectionRequest)cxRequestInfo);
			return newConnection;
		} else if (cxRequestInfo instanceof ExistingTCPConnectionRequest) {
			ManagedTCPConnectionProxy newConnection = new ManagedTCPConnectionProxy(resourceAdapter, (ExistingTCPConnectionRequest)cxRequestInfo);
			return newConnection;
		} else {
			throw new ResourceException("Info provided is not supported");
		}
	}

	@Override
	public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
		Iterator<Object> iter = connectionSet.iterator();
		if (!iter.hasNext()) {
			return null;
		}
		ManagedTCPConnectionProxy result = (ManagedTCPConnectionProxy)iter.next();
		if (cxRequestInfo instanceof NewTCPConnectionRequest) {
			result.reset((NewTCPConnectionRequest)cxRequestInfo);
			return result;
		} else if (cxRequestInfo instanceof ExistingTCPConnectionRequest) {
			result.reset((ExistingTCPConnectionRequest)cxRequestInfo);
			return result;
		} else {
			return null;
		}
	}

	@Override
	public void setLogWriter(PrintWriter out) throws ResourceException {
		logWriter.setLogWriter(out);
	}

	@Override
	public PrintWriter getLogWriter() throws ResourceException {
		return logWriter.getLogWriter();
	}

	@Override
	public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
		resourceAdapter = (SocketResourceAdapter) ra;
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
