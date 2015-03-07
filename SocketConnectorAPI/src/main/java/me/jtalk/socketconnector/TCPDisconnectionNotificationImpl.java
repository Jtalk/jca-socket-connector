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

import java.net.SocketAddress;

class TCPDisconnectionNotificationImpl implements TCPDisconnectionNotification {

	private final long connectionId;
	private final SocketAddress remote;
	private final SocketAddress local;
	private final Throwable cause;

	TCPDisconnectionNotificationImpl(long connectionId, SocketAddress remote, SocketAddress local, Throwable cause) {
		this.connectionId = connectionId;
		this.remote = remote;
		this.local = local;
		this.cause = cause;
	}

	@Override
	public long getId() {
		return this.connectionId;
	}

	@Override
	public SocketAddress getRemote() {
		return this.remote;
	}

	@Override
	public SocketAddress getLocal() {
		return this.local;
	}

	@Override
	public boolean isError() {
		return this.cause != null;
	}

	@Override
	public Throwable getCause() {
		return this.cause;
	}

}
