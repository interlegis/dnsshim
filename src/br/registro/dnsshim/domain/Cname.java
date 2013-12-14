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

public class Cname extends ResourceRecord {
	private final String canonicalName;

	public Cname(String ownername, DnsClass dnsClass, int ttl, String cname) throws DnsshimProtocolException {
		super(ownername, RrType.CNAME, dnsClass, ttl);
		this.canonicalName = cname.toLowerCase();
		if (!DomainNameUtil.validate(canonicalName)) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Invalid domain name:"+canonicalName);
		}
		this.rdata = RdataCnameBuilder.get(this.canonicalName);
	}
	
	public String getCanonicalName() {
		return canonicalName;
	}
	
	public String rdataPresentation() {
		return canonicalName;
	}

	public static Cname parseCname(String ownername, DnsClass dnsClass, int ttl,
			ByteBuffer buffer) throws DnsshimProtocolException {
		buffer.getShort(); // rdlength
		String canonicalName = DomainNameUtil.toPresentationFormat(buffer);
		return new Cname(ownername, dnsClass, ttl, canonicalName);
	}
}
