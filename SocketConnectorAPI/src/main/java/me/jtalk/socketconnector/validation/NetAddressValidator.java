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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;

public class NetAddressValidator implements ConstraintValidator<NetAddress, String> {

	private boolean hasDomain = true;
	private boolean hasIPv4 = true;
	private boolean hasIPv6 = true;

	@Override
	public void initialize(NetAddress constraintAnnotation) {
		this.hasDomain = constraintAnnotation.allowDomains();
		this.hasIPv4 = constraintAnnotation.allowIPv4();
		this.hasIPv6 = constraintAnnotation.allowIPv6();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		if (!this.hasDomain && !this.hasIPv4 && !this.hasIPv6) {
			return false;
		}

		if (this.hasDomain && DomainValidator.getInstance(true).isValid(value)) {
			return true;
		}

		InetAddressValidator validator = InetAddressValidator.getInstance();

		if (this.hasIPv4 && this.hasIPv6) {
			return validator.isValid(value);
		}

		if (this.hasIPv4 && validator.isValidInet4Address(value)) {
			return true;
		}

		if (this.hasIPv6 && validator.isValidInet6Address(value)) {
			return true;
		}

		return false;
	}
}
