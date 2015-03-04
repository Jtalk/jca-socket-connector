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

import javax.resource.spi.EISSystemException;

public class ConnectionClosedException extends EISSystemException {

	public ConnectionClosedException() {
	}

	public ConnectionClosedException(String message) {
		super(message);
	}

	public ConnectionClosedException(Throwable cause) {
		super(cause);
	}

	public ConnectionClosedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConnectionClosedException(String message, String errorCode) {
		super(message, errorCode);
	}
}
