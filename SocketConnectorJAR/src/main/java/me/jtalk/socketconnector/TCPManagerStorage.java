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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import me.jtalk.socketconnector.io.TCPManager;

public class TCPManagerStorage {

	private TCPManager manager;
	private TCPActivationSpec spec;
	private final Set<MessageEndpointFactory> factories = Collections.newSetFromMap(new ConcurrentHashMap<MessageEndpointFactory, Boolean>());

	public TCPManager getManager() {
		return manager;
	}

	public void setManager(TCPManager manager) {
		this.manager = manager;
	}

	public TCPActivationSpec getSpec() {
		return spec;
	}

	public void setSpec(TCPActivationSpec spec) {
		this.spec = spec;
	}

	public void addEndpoint(MessageEndpointFactory factory) {
		this.factories.add(factory);
	}

	public void removeEndpoint(MessageEndpointFactory factory) {
		this.factories.remove(factory);
	}

	public boolean isEmpty() {
		return this.factories.isEmpty();
	}

	public Iterable<MessageEndpointFactory> endpoints() {
		return this.factories;
	}
}
