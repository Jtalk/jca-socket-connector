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

import java.io.Serializable;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.spi.ConnectionManager;

public abstract class BaseConnectionFactory implements Referenceable, Serializable {

	private static final long serialVersionUID = 1L;

	protected ConnectionManager manager;
	protected Reference jndiReference;

	public BaseConnectionFactory(ConnectionManager manager) {
		this.manager = manager;
	}

	@Override
	public void setReference(Reference reference) {
		this.jndiReference = reference;
	}

	@Override
	public Reference getReference() throws NamingException {
		return this.jndiReference;
	}
}
