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

import java.util.Arrays;
import java.util.function.Supplier;
import org.slf4j.Logger;

/**
 * An utility class for lazy logger arguments evaluation.
 *
 * It's API tries to mimic SLF4J's one, still allowing caller to avoid
 * time-consuming computations of it's arguments if a logging level specified is
 * disabled.
 *
 * @author Roman Nazarenko <me@jtalk.me>
 */
public class LazyLoggingUtils {

	public static void lazyTrace(Logger log, String format, Supplier<Object>... data) {
		if (log.isTraceEnabled()) {
			Object[] args = Arrays.stream(data)
					.map(Supplier::get)
					.toArray();
			log.trace(format, args);
		}
	}

	public static void lazyTrace(Logger log, String format, Supplier<Object> arg) {
		if (log.isTraceEnabled()) {
			log.trace(format, arg.get());
		}
	}

	public static void lazyTrace(Logger log, String format, Supplier<Object> arg1, Supplier<Object> arg2) {
		if (log.isTraceEnabled()) {
			log.trace(format, arg1.get(), arg2.get());
		}
	}
}
