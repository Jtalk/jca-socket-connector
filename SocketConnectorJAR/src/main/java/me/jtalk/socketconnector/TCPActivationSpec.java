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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.ResourceException;
import javax.resource.spi.Activation;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import me.jtalk.socketconnector.validation.NetAddress;
import org.apache.commons.validator.routines.InetAddressValidator;

@Activation(messageListeners = TCPMessageListener.class)
public class TCPActivationSpec implements ActivationSpec {

	private static final Logger log = Logger.getLogger(TCPActivationSpec.class.getName());

	private ResourceAdapter adapter;

	@ConfigProperty(
		description = "Unique connection pool identifier. This value is used "
			+ "to distinguish different AS applications using same Resource Adapter. "
			+ "Application must use unique identifier for all created Connections "
			+ "and all MessageListeners. Once last MessageListener with this identifier "
			+ "is undeployed, connection pool will be destroyed as well."
	)
	private long clientId;

	@ConfigProperty(
		description = "TCP keepalive enablement",
		defaultValue = "true"
	)
	private boolean keepalive;

	@ConfigProperty(
		description = "IP address to bind to",
		defaultValue = "0.0.0.0"
	)
	@NetAddress
	private String localAddress;

	@ConfigProperty(
		description = "Local port to bind to",
		defaultValue = "0"
	)
	@Min(0)
	@Max(65535)
	private int localPort;

	@ConfigProperty(
		description = "Socket listener threads count",
		defaultValue = "2"
	)
	private int listnerThreadsCount;

	@ConfigProperty(
		description = "Socket receiver threads count",
		defaultValue = "4"
	)
	private int receiverThreadsCount;

	@ConfigProperty(
		description = "TCP listening backlog size",
		defaultValue = "50"
	)
	private int backlog;

	@Override
	public void validate() throws InvalidPropertyException {
		if (this.localPort >= 65535) {
			throw new InvalidPropertyException(String.format("Invalid local port value: %s is greater than 65535", this.localPort));
		}
		if (!InetAddressValidator.getInstance().isValid(this.localAddress)) {
			throw new InvalidPropertyException(String.format("Invalid local IP address %s", this.localAddress));
		}
	}

	@Override
	public ResourceAdapter getResourceAdapter() {
		return this.adapter;
	}

	@Override
	public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
		log.log(Level.FINE, String.format("%s is assigned to %s", ra.getClass().getName(), TCPActivationSpec.class.getName()));
		if (this.adapter != null) {
			throw new javax.resource.spi.IllegalStateException("Resource adapter is associated with ActivationSpec again");
		}
		this.adapter = ra;
	}

	public long getClientId() {
		return clientId;
	}

	public void setClientId(long clientId) {
		this.clientId = clientId;
	}

	public boolean isKeepalive() {
		return keepalive;
	}

	public void setKeepalive(boolean keepalive) {
		this.keepalive = keepalive;
	}

	public String getLocalAddress() {
		return localAddress;
	}

	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}

	public int getLocalPort() {
		return localPort;
	}

	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	public ResourceAdapter getAdapter() {
		return adapter;
	}

	public void setAdapter(ResourceAdapter adapter) {
		this.adapter = adapter;
	}

	public int getListnerThreadsCount() {
		return listnerThreadsCount;
	}

	public void setListnerThreadsCount(int listnerThreadsCount) {
		this.listnerThreadsCount = listnerThreadsCount;
	}

	public int getReceiverThreadsCount() {
		return receiverThreadsCount;
	}

	public void setReceiverThreadsCount(int receiverThreadsCount) {
		this.receiverThreadsCount = receiverThreadsCount;
	}

	public int getBacklog() {
		return backlog;
	}

	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}
}
