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

import javax.resource.spi.ConnectionRequestInfo;

public class ExistingTCPConnectionRequest implements ConnectionRequestInfo {

	private final long uid;
	private final long id;

	public ExistingTCPConnectionRequest(long uid, long id) {
		this.uid = uid;
		this.id = id;
	}

	public long getUid() {
		return this.uid;
	}

	public long getId() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ExistingTCPConnectionRequest other = (ExistingTCPConnectionRequest) obj;
		if (this.id != other.id) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.id);
	}
}
