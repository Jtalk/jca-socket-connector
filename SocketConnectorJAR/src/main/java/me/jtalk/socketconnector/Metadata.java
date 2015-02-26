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

import javax.resource.cci.ResourceAdapterMetaData;

public class Metadata implements ResourceAdapterMetaData {

	static final String NAME = "Socket adapter";
	static final String DESCRIPTION = "Resource adapter for TCP/IP sockets manupulations";
	static final String VENDOR = "Roman Nazarenko";
	static final String EIS_TYPE = "Socket";
	static final String VERSION = "1.0";
	static final String JCA_VERSION = "1.6";

	@Override
	public String getAdapterVersion() {
		return VERSION;
	}

	@Override
	public String getAdapterVendorName() {
		return VENDOR;
	}

	@Override
	public String getAdapterName() {
		return NAME;
	}

	@Override
	public String getAdapterShortDescription() {
		return DESCRIPTION;
	}

	@Override
	public String getSpecVersion() {
		return JCA_VERSION;
	}

	@Override
	public String[] getInteractionSpecsSupported() {
		return new String[0];
	}

	@Override
	public boolean supportsExecuteWithInputAndOutputRecord() {
		return true;
	}

	@Override
	public boolean supportsExecuteWithInputRecordOnly() {
		return true;
	}

	@Override
	public boolean supportsLocalTransactionDemarcation() {
		return false;
	}

}
