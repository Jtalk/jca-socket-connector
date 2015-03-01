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

import java.net.InetSocketAddress;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ResourceAdapterInternalException;

public class TCPConnectionFactoryImpl extends BaseConnectionFactory implements TCPConnectionFactory {

	private static final long serialVersionUID = 1L;

	protected ManagedTCPConnectionFactory parent;

	public TCPConnectionFactoryImpl(ManagedTCPConnectionFactory parent, ConnectionManager manager) {
		super(manager);
		this.parent = parent;
	}

	@Override
	public TCPConnection getConnection(long connectionId) throws ResourceException {
		Object connObject = this.manager.allocateConnection(this.parent, new ExistingTCPConnectionRequest(connectionId));
		return check(connObject);
	}

	@Override
	public TCPConnection getConnection(InetSocketAddress target) throws ResourceException {
		Object connObject = this.manager.allocateConnection(this.parent, new NewTCPConnectionRequest(target));
		return check(connObject);
	}

	private static TCPConnection check(Object obj) throws ResourceException {
		if (!(obj instanceof TCPConnection)) {
			throw new ResourceAdapterInternalException("Object allocated from ConnectionManager is not TCP Connection");
		}
		return (TCPConnection)obj;
	}
}
