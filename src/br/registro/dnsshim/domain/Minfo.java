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

public class Minfo extends ResourceRecord {
	private final String rmailbx;
	private final String emailbx;

	public Minfo(String ownername, DnsClass dnsClass, int ttl,
			String rmailbx, String emailbx) {
		super(ownername, RrType.MINFO, dnsClass, ttl);
		this.rmailbx = rmailbx.toLowerCase();
		this.emailbx = emailbx.toLowerCase();
		this.rdata = RdataMinfoBuilder.get(this.rmailbx, this.emailbx);
	}

	public Minfo(String ownername, DnsClass dnsClass, int ttl, String rdata) {
		super(ownername, RrType.MINFO, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");
		this.rmailbx = scanner.next().toLowerCase();
		this.emailbx = scanner.next().toLowerCase();
		this.rdata = RdataMinfoBuilder.get(rmailbx, emailbx);
		scanner.close();
	}

	public String getRmailbx() {
		return rmailbx;
	}

	public String getEmailbx() {
		return emailbx;
	}

	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		rdata.append(rmailbx).append(SEPARATOR);
		rdata.append(emailbx).append(SEPARATOR);
		return rdata.toString();
	}

	public static Minfo parseMinfo(String ownername,
			DnsClass dnsClass, int ttl, ByteBuffer buffer) {
		buffer.getShort(); // rdlen
		String rmailbx = DomainNameUtil.toPresentationFormat(buffer);
		String emailbx = DomainNameUtil.toPresentationFormat(buffer);
		return new Minfo(ownername, dnsClass, ttl, rmailbx, emailbx);
	}

}
