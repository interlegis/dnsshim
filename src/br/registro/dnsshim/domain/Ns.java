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
package br.registro.dnsshim.domain;

import java.nio.ByteBuffer;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.util.DomainNameUtil;

public class Ns extends ResourceRecord {
	private final String nameserver;

	public Ns(String ownername, DnsClass dnsClass, int ttl, String nameserver) throws DnsshimProtocolException {
		super(ownername, RrType.NS, dnsClass, ttl);
		StringBuilder ns = new StringBuilder(nameserver);
		if (!DomainNameUtil.validate(nameserver)) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Invalid domain name:"+nameserver);
		}
		if (ns.charAt(ns.length() - 1) != '.') {
			ns.append('.');
		}
		this.nameserver = ns.toString().toLowerCase();
		this.rdata = RdataNsBuilder.get(this.nameserver);
	}

	public String getNameserver() {
		return nameserver;
	}

	@Override
	public String rdataPresentation() {
		return nameserver;
	}
	
	public static Ns parseNs(String ownername, DnsClass dnsClass, int ttl,
			ByteBuffer buffer) throws DnsshimProtocolException {
		buffer.getShort(); // rdlength
		String nameserver = DomainNameUtil.toPresentationFormat(buffer);
		return new Ns(ownername, dnsClass, ttl, nameserver);
	}
}
