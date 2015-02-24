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
import org.apache.commons.validator.routines.InetAddressValidator;

@Activation(messageListeners = SocketMessageListener.class)
public class SocketAdapterActivationSpec implements ActivationSpec {

	private static final Logger log = Logger.getLogger(SocketAdapterActivationSpec.class.getName());

	private ResourceAdapter adapter;

	@ConfigProperty(
		description = "TCP keepalive enablement",
		defaultValue = "true"
	)
	public boolean hasKeepalive;

	@ConfigProperty(
		description = "IP address to bind to",
		defaultValue = "0.0.0.0"
	)
	public String localAddress;

	@ConfigProperty(
		description = "Local port to bind to",
		defaultValue = "0"
	)
	public int localPort;

	@ConfigProperty(
		description = "Free port searching",
		defaultValue = "false"
	)
	public boolean searchFreeLocalPort;

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
		log.log(Level.FINE, String.format("%s is assigned to %s", ra.getClass().getName(), SocketAdapterActivationSpec.class.getName()));
		if (this.adapter != null) {
			throw new javax.resource.spi.IllegalStateException("Resource adapter is associated with ActivationSpec again");
		}
		this.adapter = ra;
	}

}
