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

import java.net.InetSocketAddress;
import javax.annotation.Resource;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.TransactionSupport;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;
import javax.validation.Validator;

@Connector(
	displayName = Metadata.NAME,
	description = Metadata.DESCRIPTION,
	vendorName = Metadata.VENDOR,
	eisType = Metadata.EIS_TYPE,
	version = Metadata.VERSION,
	transactionSupport = TransactionSupport.TransactionSupportLevel.NoTransaction
)
public class SocketResourceAdapter implements ResourceAdapter {

	private WorkManager workManager;

	@Resource
	Validator validator;

	@Override
	public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
		this.workManager = ctx.getWorkManager();
	}

	@Override
	public void stop() {
		this.workManager = null;
	}

	@Override
	public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {

	}

	@Override
	public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {

	}

	@Override
	public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
		return null;
	}

	Validator getValidator() {
		return this.validator;
	}

	long createTCPConnection(InetSocketAddress target) {
		return this.tcpManager.connect(target);
	}

	void sendTCP(long id, byte[] data) {
		this.tcpManager.send(id, data);
	}

	void closeTCPConnection(long id) {
		this.tcpManager.close(id);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
}
