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

import br.registro.dnsshim.util.DomainNameUtil;

public class Naptr extends ResourceRecord {
	private final short order;
	private final short preference;
	private final String flags;
	private final String services;
	private final String regexp;
	private final String replacement;

	public Naptr(String ownername, DnsClass dnsClass, int ttl, short order,
			short preference, String flags, String services, String regexp,
			String replacement) {
		super(ownername, RrType.NAPTR, dnsClass, ttl);
		this.order = order;
		this.preference = preference;
		this.flags = flags;
		this.services = services;
		this.regexp = regexp;
		this.replacement = replacement.toLowerCase();
		this.rdata = RdataNaptrBuilder.get(order, preference, flags, services, regexp, this.replacement);
	}

	public Naptr(String ownername, DnsClass dnsClass, int ttl, String rdata) {
		super(ownername, RrType.NAPTR, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");
		this.order = scanner.nextShort();
		this.preference = scanner.nextShort();
		this.flags = scanner.next();
		this.services = scanner.next();
		this.regexp = scanner.next();
		this.replacement = scanner.next().toLowerCase();
		this.rdata = RdataNaptrBuilder.get(order, preference, flags, services, regexp, replacement);
		scanner.close();
	}

	public short getOrder() {
		return order;
	}

	public short getPreference() {
		return preference;
	}

	public String getFlags() {
		return flags;
	}

	public String getServices() {
		return services;
	}

	public String getRegexp() {
		return regexp;
	}

	public String getReplacement() {
		return replacement;
	}

	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		rdata.append(order).append(SEPARATOR);
		rdata.append(preference).append(SEPARATOR);
		rdata.append(flags).append(SEPARATOR);
		rdata.append(services).append(SEPARATOR);
		rdata.append(regexp).append(SEPARATOR);
		rdata.append(replacement);

		return rdata.toString();
	}

	public static Naptr parseNaptr(String ownername, DnsClass dnsClass, int ttl,
			ByteBuffer buffer) {
		buffer.getShort(); // rdlength
		short order = buffer.getShort();
		short preference = buffer.getShort();
		byte flagsLen = buffer.get();
		byte[] flagsBytes = new byte[flagsLen];
		buffer.get(flagsBytes);
		String flags = new String(flagsBytes);
		byte servicesLen = buffer.get();
		byte[] servicesBytes = new byte[servicesLen];
		buffer.get(servicesBytes);
		String services = new String(servicesBytes);
		byte regexpLen = buffer.get();
		byte[] regexpBytes = new byte[regexpLen];
		buffer.get(regexpBytes);
		String regexp = new String(regexpBytes);
		String replacement = DomainNameUtil.toPresentationFormat(buffer);
		return new Naptr(ownername, dnsClass, ttl, order, preference, flags, services, regexp, replacement);
	}
}
