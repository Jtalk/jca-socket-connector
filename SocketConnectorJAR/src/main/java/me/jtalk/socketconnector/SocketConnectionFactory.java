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

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionManager;

public class SocketConnectionFactory implements ConnectionFactory {

	private static final long serialVersionUID = 1L;

	private ManagedSocketConnectionFactory parent;
	private ConnectionManager manager;
	private Reference jndiReference;

	public SocketConnectionFactory(ManagedSocketConnectionFactory parent, ConnectionManager manager) {
		this.parent = parent;
		this.manager = manager;
	}

	@Override
	public Connection getConnection() throws ResourceException {
		throw new ResourceException("Socket Connector connection requested without destination specification");
	}

	@Override
	public Connection getConnection(ConnectionSpec properties) throws ResourceException {
		if (!(properties instanceof SocketConnectionSpec)) {
			throw new ResourceException("Socket Connector connection request with invalid properties");
		}
		SocketConnectionSpec spec = (SocketConnectionSpec)properties;
		SocketConnectionRequestInfo info = createRequestInfo(spec);
		return (Connection)this.manager.allocateConnection(this.parent, info);
	}

	@Override
	public RecordFactory getRecordFactory() throws ResourceException {
		return this.parent.getRecordFactory();
	}

	@Override
	public ResourceAdapterMetaData getMetaData() throws ResourceException {
		return ManagedSocketConnectionFactory.METADATA;
	}

	@Override
	public void setReference(Reference reference) {
		this.jndiReference = reference;
	}

	@Override
	public Reference getReference() throws NamingException {
		return this.jndiReference;
	}
}
