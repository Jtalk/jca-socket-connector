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

package me.jtalk.socketconnector.api;

import java.io.Serializable;
import java.net.InetSocketAddress;
import javax.resource.Referenceable;
import javax.resource.ResourceException;

public interface TCPConnectionFactory extends Serializable, Referenceable {

	TCPConnection getConnection(long uid, long connectionId) throws ResourceException;
	TCPConnection createConnection(long uid, InetSocketAddress target) throws ResourceException;
}
