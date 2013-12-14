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
import java.util.Scanner;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DomainNameUtil;

public class Mx extends ResourceRecord {
	private final short preference;
	private final String exchange;
	
	public Mx(String ownername,
			DnsClass dnsClass, int ttl, short preference, String exchange) throws DnsshimProtocolException {
		super(ownername, RrType.MX, dnsClass, ttl);
		
		this.preference = preference;
		this.exchange = exchange.toLowerCase();
		if (!DomainNameUtil.validate(exchange)) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Invalid domain name:"+exchange);
		}
		this.rdata = RdataMxBuilder.get(preference, this.exchange);
	}
	
	public Mx(String ownername, DnsClass dnsClass, int ttl, String rdata) throws DnsshimProtocolException {
		super(ownername, RrType.MX, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");
		this.preference = Short.parseShort(scanner.next());
		this.exchange = scanner.next().toLowerCase();
		if (!DomainNameUtil.validate(exchange)) {
			scanner.close();
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Invalid domain name:"+exchange);
		}
		this.rdata = RdataMxBuilder.get(preference, exchange);
		scanner.close();
	}
	
	public short getPreference() {
		return preference;
	}

	public String getExchange() {
		return exchange;
	}

	public static Mx parseMx(String ownername, DnsClass dnsClass, int ttl,
			ByteBuffer buffer) throws DnsshimProtocolException {
		buffer.getShort(); // rdlength
		short preference = buffer.getShort();
		String exchange = DomainNameUtil.toPresentationFormat(buffer);
		return new Mx(ownername, dnsClass, ttl, preference, exchange);
	}
	
	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		rdata.append(ByteUtil.toUnsigned(preference)).append(SEPARATOR);
		rdata.append(exchange);

		return rdata.toString();
	}
}
