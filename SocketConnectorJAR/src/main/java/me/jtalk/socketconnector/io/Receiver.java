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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

import java.util.logging.Logger;

class Receiver extends ChannelInboundHandlerAdapter {

	public static final String ID_ATTRIBUTE_NAME = "SocketRATCPID";
	public static final AttributeKey<Long> KEY = AttributeKey.valueOf(ID_ATTRIBUTE_NAME);

	private static final Logger log = Logger.getLogger(Receiver.class.getName());

	private long id;
	private final TCPManager manager;
	private Throwable cause;

	public Receiver(TCPManager manager) {
		this.manager = manager;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf buffer = (ByteBuf)msg;
		byte[] data = new byte[buffer.readableBytes()];
		buffer.readBytes(data);
		buffer.release();
		this.manager.dataReceived(this.id, data);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		this.manager.connectionShutdown(this.id, this.cause);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.cause = null;
		this.id = this.manager.connectionEstablished(ctx);
		ctx.attr(KEY).set(this.id);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		this.addCause(cause);
		ctx.disconnect();
	}

	private void addCause(Throwable cause) {
		if (this.cause == null) {
			this.cause = cause;
		} else {
			this.cause.addSuppressed(cause);
		}
	}
}
