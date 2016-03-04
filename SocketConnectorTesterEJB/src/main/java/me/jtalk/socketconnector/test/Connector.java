/*
 * Copyright (C) 2015 Liza Lukicheva
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package me.jtalk.socketconnector.test;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.resource.ResourceException;
import me.jtalk.socketconnector.api.TCPConnection;
import me.jtalk.socketconnector.api.TCPConnectionFactory;
import me.jtalk.socketconnector.api.TCPMessage;

@Singleton
public class Connector {
	private static final Logger log = Logger.getLogger(Connector.class.getName());

	public static final String UUID_STRING = "43760934769043760";
	public static final long UUID = Long.parseLong(UUID_STRING);

	private final AtomicBoolean sent = new AtomicBoolean(false);
	private volatile long clientId = -1;
	private volatile long listeningId = -1;

	@Resource(lookup = "java:/socket/TCP")
	TCPConnectionFactory factory;

	public void sendRecord() {

		if (!this.sent.compareAndSet(false, true)) {
			return;
		}

		log.info("Test pilot is ready");

		int port = 23553;

		try {
			InetSocketAddress local = new InetSocketAddress("0.0.0.0", port);
			InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", port);
			try (TCPConnection listening = factory.listen(UUID, local)) {
				this.listeningId = listening.getId();
				try (TCPConnection connection1 = factory.createConnection(UUID, socketAddress)) {
					this.clientId = connection1.getId();
					try (TCPConnection connection2 = factory.getConnection(UUID, connection1.getId())) {
						log.info("Connections established");
						connection2.send(ByteBuffer.wrap("InitData".getBytes()));
					}
				}
			}

		} catch (ResourceException e) {
			throw new EJBException("Exception on connection creation", e);
		}
	}

	public void received(TCPMessage msg) throws ResourceException {
		if (msg.getConnectionId() == this.clientId) {
			log.info("Received " + new String(msg.getData()));
		} else {
			log.info("Received init: " + new String(msg.getData()));
			try (TCPConnection connection = factory.getConnection(UUID, msg.getConnectionId())) {
				connection.send(ByteBuffer.wrap("Hello!".getBytes()));
				log.info("Sending test data");
			}
		}
	}
}
