/* Copyright (C) 2009 Registro.br. All rights reserved. 
* 
* Redistribution and use in source and binary forms, with or without 
* modification, are permitted provided that the following conditions are 
* met:
* 1. Redistribution of source code must retain the above copyright 
*    notice, this list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in the
*    documentation and/or other materials provided with the distribution.
* 
* THIS SOFTWARE IS PROVIDED BY REGISTRO.BR ``AS IS'' AND ANY EXPRESS OR
* IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIE OF FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
* EVENT SHALL REGISTRO.BR BE LIABLE FOR ANY DIRECT, INDIRECT,
* INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
* BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
* OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
* TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
* USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
* DAMAGE.
 */
package br.registro.dnsshim.xfrd.dns.notifier.domain;

import java.util.concurrent.atomic.AtomicInteger;

import br.registro.dnsshim.domain.Soa;

public class ZoneNotify {
	private String zonename;
	private String slaveAddress;
	private short queryId;
	private int slavePort;
	private Soa soa;
	private AtomicInteger notifySent = new AtomicInteger(0);

	public String getZonename() {
		return zonename;
	}

	public void setZonename(String zonename) {
		this.zonename = zonename;
	}

	public String getSlaveAddress() {
		return slaveAddress;
	}

	public void setSlaveAddress(String slaveAddress) {
		this.slaveAddress = slaveAddress;
	}
	
	public short getQueryId() {
		return queryId;
	}

	public void setQueryId(short queryId) {
		this.queryId = queryId;
	}

	public AtomicInteger getNotifySent() {
		return notifySent;
	}

	public void setNotifySent(AtomicInteger notifySent) {
		this.notifySent = notifySent;
	}

	public int getSlavePort() {
		return slavePort;
	}

	public void setSlavePort(int slavePort) {
		this.slavePort = slavePort;
	}
	
	public Soa getSoa() {
		return soa;
	}

	public void setSoa(Soa soa) {
		this.soa = soa;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ZoneNotify) {
			ZoneNotify notify = (ZoneNotify) obj;
			return this.zonename.equals(notify.getZonename())
					&& this.getSlaveAddress().equals(notify.getSlaveAddress())
					&& this.getQueryId() == notify.getQueryId();
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return zonename.hashCode() * slaveAddress.hashCode();
	}
	
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.
			append(zonename).
			append(' ').
			append(slaveAddress).
			append(' ').
			append(slavePort);
		return buffer.toString();
	}

}
