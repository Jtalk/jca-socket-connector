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
import java.util.Objects;
import javax.resource.spi.ConnectionRequestInfo;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import me.jtalk.socketconnector.validation.NetAddress;

class NewTCPConnectionRequest implements ConnectionRequestInfo {

	private final long uid;

	@NotNull
	@NetAddress
	private final String address;

	@Min(1)
	@Max(65535)
	private final int port;

	private final boolean listening;

	public NewTCPConnectionRequest(long uid, InetSocketAddress address, boolean listening) {
		this.uid = uid;
		this.address = address.getHostString();
		this.port = address.getPort();
		this.listening = listening;
	}

	public long getUid() {
		return this.uid;
	}

	public String getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

	public InetSocketAddress createInetAddress() {
		return new InetSocketAddress(this.address, this.port);
	}

	public boolean isListening() {
		return this.listening;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final NewTCPConnectionRequest other = (NewTCPConnectionRequest) obj;
		if (!Objects.equals(this.address, other.address)) {
			return false;
		}
		if (this.port != other.port) {
			return false;
		}
		if (this.listening != other.listening) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 97 * hash + Objects.hashCode(this.address);
		hash = 97 * hash + this.port;
		hash = 97 * hash + (this.listening ? 1 : 0);
		return hash;
	}
}
