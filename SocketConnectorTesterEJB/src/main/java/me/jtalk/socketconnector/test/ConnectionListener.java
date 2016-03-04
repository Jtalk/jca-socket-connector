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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.resource.ResourceException;
import me.jtalk.socketconnector.api.TCPDisconnectionNotification;
import me.jtalk.socketconnector.api.TCPMessage;
import me.jtalk.socketconnector.api.TCPMessageListener;

@MessageDriven(
	activationConfig = {
		@ActivationConfigProperty(propertyName = "clientId", propertyValue = Connector.UUID_STRING),
	},
	name = "TCPListener"
)
public class ConnectionListener implements TCPMessageListener {
	private static final Logger log = Logger.getLogger(ConnectionListener.class.getName());

	@EJB
	Connector connector;

	public ConnectionListener() {
	}

	@Override
	public void initialized() {
		log.info("Connection initialized, sending data");
		this.connector.sendRecord();
	}

	@Override
	public void onMessage(TCPMessage message) {
		try {
			this.connector.received(message);
		} catch (ResourceException e) {
			log.log(Level.SEVERE, "Exception on message receival id " + message.getConnectionId(), e);
		}
	}

	@Override
	public void disconnected(TCPDisconnectionNotification notification) {
		if (notification.isError()) {
			log.log(Level.WARNING, "TCP disconnection caused by error", notification.getCause());
		} else {
			log.info(String.format("TCP disconnect id %s, from %s to %s", notification.getId(), notification.getLocal(), notification.getRemote()));
		}
	}
}
