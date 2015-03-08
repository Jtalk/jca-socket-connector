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

import me.jtalk.socketconnector.api.TCPConnection;
import java.nio.ByteBuffer;
import javax.resource.ResourceException;

public class TCPConnectionImpl implements TCPConnection {

	private ManagedTCPConnectionProxy owner;

	public TCPConnectionImpl(ManagedTCPConnectionProxy owner) {
		this.owner = owner;
	}

	@Override
	public long getId() throws ResourceException {
		ManagedTCPConnectionProxy local = this.owner;
		if (local == null) {
			throw new ResourceException("Connection is detached");
		}
		return local.getId();
	}

	@Override
	public void send(ByteBuffer message) throws ResourceException {
		ManagedTCPConnectionProxy local = this.owner;
		if (local == null) {
			throw new ResourceException("Connection is detached");
		}
		local.send(message);
	}

	@Override
	public void disconnect() throws ResourceException {
		ManagedTCPConnectionProxy local = this.owner;
		if (local != null) {
			local.disconnect();
		}
	}

	void reassign(ManagedTCPConnectionProxy newOwner) {
		this.owner = newOwner;
	}

	void invalidate() {
		this.reassign(null);
	}
}
