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

import java.nio.ByteBuffer;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;

public interface TCPConnection extends AutoCloseable {

	/**
	 * Underlying connection ID. This ID is unique per socket and represents
	 * a physical connection.
	 * @return Connection ID value.
	 * @throws ResourceException if TCPConnection is detached from ManagedConnection.
	 */
	long getId() throws ResourceException;

	/**
	 * Sends message to underlying connection.
	 * @param message binary data to send to socket.
	 * @throws ConnectionClosedException if socket with ID associated with this
	 * connection object is already closed.
	 * @throws NotSupportedException if called on listening connection
	 * @throws ResourceException in case of generic error.
	 */
	void send(ByteBuffer message) throws NotSupportedException, ResourceException;

	/**
	 * Performs disconnection of the socket with ID associated with this connection
	 * object. This connection will be detached from it's parent ManagedConnection
	 * as if close() were called as well.
	 * @throws ConnectionClosedException if socket with ID associated with this
	 * connection object is already closed.
	 * @throws ResourceException in case of generic error.
	 */
	void disconnect() throws ResourceException;

	/**
	 * Detaches this connection object from its underlying ManagedConnection. The
	 * actual physical connection associated with this ID is not closed (!)
	 * @throws ResourceException in case of generic error.
	 */
	@Override
	void close() throws ResourceException;
}
