/*
 * Copyright (C) 2016 Roman Nazarenko <me@jtalk.me>
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
import java.util.function.BiConsumer;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventListeners {

	private final Set<ConnectionEventListener> eventListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public void notifyEvent(ConnectionEvent event, final BiConsumer<ConnectionEventListener, ConnectionEvent> method) {
		if (this.eventListeners.isEmpty()) {
			log.error("Sending event ''{}'' to empty listeners set", event.getId());
			return;
		}
		this.eventListeners.forEach(l -> {
			log.trace("Sending event ''{}'' to listener ''{}''", event.getId(), l);
			method.accept(l, event);
		});
	}

	public boolean add(ConnectionEventListener listener) {
		return eventListeners.add(listener);
	}

	public boolean remove(ConnectionEventListener listener) {
		return eventListeners.remove(listener);
	}
}
