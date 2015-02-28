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
package me.jtalk.socketconnector.validation;

import java.lang.annotation.*;
import javax.validation.Constraint;
import javax.validation.Payload;

@Constraint(validatedBy = NetAddressValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NetAddress {
	boolean allowDomains() default true;
	boolean allowIPv4() default true;
	boolean allowIPv6() default true;

	String message() default "Not an address";

	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}
