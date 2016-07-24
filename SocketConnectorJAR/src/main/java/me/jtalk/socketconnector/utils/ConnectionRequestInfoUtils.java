/*
 * Copyright (C) 2016 Roman Nazarenko <me@jtalk.me>
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

package me.jtalk.socketconnector.utils;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import me.jtalk.socketconnector.ExistingTCPConnectionRequest;
import me.jtalk.socketconnector.NewTCPConnectionRequest;

public class ConnectionRequestInfoUtils {

	public static void checkInfo(long clientId, long id, ConnectionRequestInfo rawInfo) throws ResourceException {
		if (rawInfo instanceof ExistingTCPConnectionRequest) {
			ExistingTCPConnectionRequest request = (ExistingTCPConnectionRequest) rawInfo;
			if (request.getUid() != clientId || request.getId() != id) {
				throw new ResourceException("Incompatible UID and ConnectionID supplied to managed connection");
			}
		} else if (rawInfo instanceof NewTCPConnectionRequest) {
			NewTCPConnectionRequest request = (NewTCPConnectionRequest) rawInfo;
			if (request.getUid() != clientId) {
				throw new ResourceException("Incompatible UID supplied to managed connection");
			}
		} else {
			throw new ResourceException(String.format("Incompatible connection request info type %s supplied to managed connection", rawInfo.getClass().getCanonicalName()));
		}
	}
}
