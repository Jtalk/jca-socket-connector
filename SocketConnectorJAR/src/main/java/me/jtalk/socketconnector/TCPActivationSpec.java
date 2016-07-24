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

import me.jtalk.socketconnector.api.TCPMessageListener;
import java.util.logging.Logger;
import javax.resource.spi.Activation;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Activation(messageListeners = {TCPMessageListener.class})
public class TCPActivationSpec implements ActivationSpec {

	private static final Logger log = Logger.getLogger(TCPActivationSpec.class.getName());

	private volatile ResourceAdapter resourceAdapter;

	@ConfigProperty(
		description = "Unique connection pool identifier. This value is used "
			+ "to distinguish different AS applications using same Resource Adapter. "
			+ "Application must use unique identifier for all created Connections "
			+ "and all MessageListeners. Once last MessageListener with this identifier "
			+ "is undeployed, connection pool will be destroyed as well."
	)
	private Long clientId;

	@ConfigProperty(
		description = "TCP keepalive enablement",
		defaultValue = "true"
	)
	private Boolean keepalive;

	@ConfigProperty(
		description = "Socket listener threads count",
		defaultValue = "2"
	)
	private Integer listnerThreadsCount;

	@ConfigProperty(
		description = "Socket receiver threads count",
		defaultValue = "4"
	)
	private Integer receiverThreadsCount;

	@ConfigProperty(
		description = "TCP listening backlog size",
		defaultValue = "50"
	)
	private Integer backlog;

	@Override
	public void validate() throws InvalidPropertyException {
		// Nothing to validate here
	}
}
