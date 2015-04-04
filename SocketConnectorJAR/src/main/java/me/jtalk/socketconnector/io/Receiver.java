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

import java.util.logging.Logger;

class Receiver extends ChannelInboundHandlerAdapter {

	private static final Logger log = Logger.getLogger(Receiver.class.getName());

	private final long id;
	private final TCPManager manager;
	private Throwable cause;

	public Receiver(TCPManager manager, long id) {
		this.id = id;
		this.manager = manager;
	}

	public long getId() {
		return this.id;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

		log.finest(String.format("Channel data available for id %d for %s -> %s",
			this.id, ctx.channel().localAddress(), ctx.channel().remoteAddress()));

		ByteBuf buffer = (ByteBuf)msg;
		byte[] data = new byte[buffer.readableBytes()];
		buffer.readBytes(data);
		buffer.release();
		this.manager.dataReceived(this.id, data);

		log.finer(String.format("Channel data (%d bytes) received for id %d for %s -> %s",
			data.length, this.id, ctx.channel().localAddress(), ctx.channel().remoteAddress()));
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.finer(String.format("Channel unregistering requested for id %d for %s -> %s",
			this.id, ctx.channel().localAddress(), ctx.channel().remoteAddress()));

		this.manager.connectionShutdown(this.id, this.cause);

		log.finer(String.format("Channel unregistered for id %d for %s -> %s",
			this.id, ctx.channel().localAddress(), ctx.channel().remoteAddress()));
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.finer(String.format("Channel registering requested for id %d for %s -> %s",
			this.id, ctx.channel().localAddress(), ctx.channel().remoteAddress()));

		this.cause = null;
		this.manager.connectionEstablished(this.id, ctx);

		log.finer(String.format("Channel registered for id %d for %s -> %s",
			this.id, ctx.channel().localAddress(), ctx.channel().remoteAddress()));
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
