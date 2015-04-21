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

package me.jtalk.socketconnector.io;

import io.netty.channel.Channel;
import java.net.SocketAddress;

class ConnectionContext {

	public final Channel channel;
	public final SocketAddress local;
	public final SocketAddress remote;
	public final boolean listening;

	public ConnectionContext(Channel channel, SocketAddress local, SocketAddress remote, boolean listening) {
		this.channel = channel;
		this.local = local;
		this.remote = remote;
		this.listening = listening;
	}
}
