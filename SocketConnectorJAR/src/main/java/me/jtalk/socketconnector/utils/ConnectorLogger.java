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

package me.jtalk.socketconnector.utils;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicReference;
import javax.resource.ResourceException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectorLogger {

	private final AtomicReference<PrintWriter> logWriter = new AtomicReference<>(null);

	public PrintWriter getLogWriter() throws ResourceException {
		return logWriter.get();
	}

	public void setLogWriter(PrintWriter out) throws ResourceException {
		if (!logWriter.compareAndSet(null, out)) {
			throw new javax.resource.spi.IllegalStateException("LogWriter is set more than once");
		}
	}

	public void printLog(String message) {
		PrintWriter writer = logWriter.get();
		if (writer != null) {
			writer.print(message);
			log.trace("Logging message applied to the log writer: ''{}''", message);
		} else {
			log.error("Logging message applied before log writer is initialized: ''{}''", message);
		}
	}
}
