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

import io.netty.channel.ChannelHandlerContext;
import java.net.SocketAddress;

class ConnectionContext {

	public final ChannelHandlerContext context;
	public final SocketAddress local;
	public final SocketAddress remote;

	public ConnectionContext(ChannelHandlerContext ctx, SocketAddress local, SocketAddress remote) {
		this.context = ctx;
		this.local = local;
		this.remote = remote;
	}
}
