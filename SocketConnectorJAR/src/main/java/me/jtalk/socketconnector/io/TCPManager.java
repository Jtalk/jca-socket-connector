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

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.ResourceException;
import javax.resource.spi.EISSystemException;
import javax.resource.spi.ResourceAdapterInternalException;
import me.jtalk.socketconnector.api.ConnectionClosedException;
import me.jtalk.socketconnector.SocketResourceAdapter;
import me.jtalk.socketconnector.TCPActivationSpec;

public class TCPManager implements Closeable {

	private static final Logger log = Logger.getLogger(TCPManager.class.getName());
	private static final long SHUTDOWN_TIMEOUT_SEC = 60;

	private SocketResourceAdapter parent;
	private final long id;

	private final EventLoopGroup listeners;
	private final EventLoopGroup workers;
	private final ServerBootstrap server;
	private final Bootstrap client;

	private final ConcurrentHashMap<Long, ConnectionContext> connections = new ConcurrentHashMap<>();
	private final ConcurrentLinkedQueue<ConnectionContext> contextPool = new ConcurrentLinkedQueue<>();
	private final AtomicLong ids = new AtomicLong(0);

	public TCPManager(SocketResourceAdapter parent, long id, TCPActivationSpec spec) throws ResourceException {

		this.parent = parent;
		this.id = id;

		ThreadFactory factory = new DaemonThreadFactory();
		this.listeners = new NioEventLoopGroup(spec.getListnerThreadsCount(), factory);
		this.workers = new NioEventLoopGroup(spec.getReceiverThreadsCount(), factory);

		this.server = this.instantiateServer(spec);
		this.client = this.instantiateClient();
	}

	public long connect(InetSocketAddress target) throws ResourceException {
		try {
			ChannelFuture completed = this.client.connect(target).sync();
			if (!completed.isSuccess()) {
				throw new EISSystemException("Connection failed", completed.cause());
			}
			Receiver handler = completed.channel().pipeline().get(Receiver.class);
			long id = handler.getId();
			return id;

		} catch (InterruptedException e) {
			throw new EISSystemException("Execution interrupted during connecting to remote client", e);
		}
	}

	public void send(long id, ByteBuffer data) throws ResourceException {
		ConnectionContext ctx = this.connections.get(id);
		ChannelHandlerContext output = ctx.context;
		if (output == null) {
			throw new ConnectionClosedException("Connection is already closed");
		}
		output.writeAndFlush(data);
	}

	public boolean close(long id) {
		final ConnectionContext ctx = this.connections.get(id);
		if (ctx == null) {
			return false;
		}
		ctx.context.disconnect().addListener(f -> {
			if (!f.isSuccess()) {
				TCPManager.log.log(Level.SEVERE, "Disconnection failed due to error", f.cause());
			}
		});
		return true;
	}

	@Override
	public void close() {
		this.listeners.shutdownGracefully(0, SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS);
		this.workers.shutdownGracefully(0, SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS);
		this.connections.clear();
		this.contextPool.clear();
		this.parent = null;
	}

	// Receiver callbacks
	void connectionEstablished(long id, ChannelHandlerContext ctx) {

		ConnectionContext context = this.contextPool.poll();
		if (context == null) {
			context = new ConnectionContext();
		}

		context.context = ctx;
		context.local = ctx.channel().localAddress();
		context.remote = ctx.channel().remoteAddress();

		this.connections.put(id, context);
	}

	void connectionShutdown(long id, Throwable cause) {

		ConnectionContext ctx = this.connections.remove(id);

		SocketAddress local = null;
		SocketAddress remote = null;
		if (ctx != null) {
			local = ctx.local;
			remote = ctx.remote;
			ctx.clear();
			this.contextPool.add(ctx);
		}
		this.parent.notifyShutdown(this.id, id, local, remote, cause);
	}

	void dataReceived(long id, byte[] data) {
		ConnectionContext ctx = this.connections.get(id);
		if (ctx == null) {
			// Drop data from closed connection
			return;
		}
		this.parent.notifyReceived(this.id, id, data, ctx.local, ctx.remote);
	}

	private ServerBootstrap instantiateServer(TCPActivationSpec spec) throws ResourceException {

		ServerBootstrap newServer = new ServerBootstrap();
		newServer.group(this.listeners, this.workers);
		newServer.channel(NioServerSocketChannel.class);
		newServer.childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel c) throws Exception {
				c.pipeline().addLast(new Receiver(TCPManager.this, TCPManager.this.ids.incrementAndGet()));
			}
		});
		newServer.option(ChannelOption.SO_KEEPALIVE, true);
		newServer.option(ChannelOption.SO_BACKLOG, spec.getBacklog());
		newServer.option(ChannelOption.TCP_NODELAY, true);

		final InetSocketAddress address = new InetSocketAddress(spec.getLocalAddress(), spec.getLocalPort());
		newServer.bind(address).addListener(f -> {
			if (!f.isSuccess()) {
				TCPManager.log.log(Level.SEVERE, "Binding of TCP Manager failed: " + address.toString(), f.cause());
			}
		});
		return newServer;
	}

	private Bootstrap instantiateClient() {
		Bootstrap newClient = new Bootstrap();
		newClient.group(this.workers);
		newClient.channel(NioSocketChannel.class);
		newClient.handler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new Receiver(TCPManager.this, TCPManager.this.ids.incrementAndGet()));
			}
		});
		newClient.option(ChannelOption.SO_KEEPALIVE, true);

		return newClient;
	}
}
