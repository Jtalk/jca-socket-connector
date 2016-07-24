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

import java.util.Set;
import java.util.function.Consumer;
import javax.resource.ResourceException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.groups.Default;
import me.jtalk.socketconnector.NewTCPConnectionRequest;

public class ValidationUtils {

	public static void validateInfo(Validator validator, Consumer<String> logger, NewTCPConnectionRequest info) throws ResourceException {
		Set<ConstraintViolation<NewTCPConnectionRequest>> violations = validator.validate(info, Default.class);
		if (violations.isEmpty()) {
			return;
		}
		for (ConstraintViolation<NewTCPConnectionRequest> violation : violations) {
			String message = violation.getMessage();
			logger.accept(message);
		}
		throw new ResourceException("Socket connection request info constraints violation failed");
	}

}
