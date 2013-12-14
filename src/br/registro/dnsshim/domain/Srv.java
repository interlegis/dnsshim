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

import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DomainNameUtil;

public class Srv extends ResourceRecord {
	private final short priority;
	private final short weight;
	private final short port;
	private final String target;
	
	public Srv(String ownername, DnsClass dnsClass, int ttl, String rdata) {
		super(ownername, RrType.SRV, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");
		this.priority = Short.parseShort(scanner.next());
		this.weight = Short.parseShort(scanner.next());
		this.port = Short.parseShort(scanner.next());
		this.target = scanner.next().toLowerCase();
		this.rdata = RdataSrvBuilder.get(priority, weight, port, target);
		scanner.close();
	}
	
	public Srv(String ownername, DnsClass dnsClass, int ttl, short priority, short weight, short port, String target) {
		super(ownername, RrType.SRV, dnsClass, ttl);
		this.priority = priority;
		this.weight = weight;
		this.port = port;
		this.target = target.toLowerCase();
		this.rdata = RdataSrvBuilder.get(priority, weight, port, this.target);
	}
	
	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		rdata.append(ByteUtil.toUnsigned(priority)).append(SEPARATOR)
			.append(ByteUtil.toUnsigned(weight)).append(SEPARATOR)
			.append(ByteUtil.toUnsigned(port)).append(SEPARATOR)
			.append(target);
		return rdata.toString();
	}
	
	
	public static Srv parseSrv(String ownername, DnsClass dnsClass, int ttl, ByteBuffer buffer) {
		buffer.getShort(); // rdlength
		short priority = buffer.getShort();
		short weight = buffer.getShort();
		short port = buffer.getShort();
		String target = DomainNameUtil.toPresentationFormat(buffer);
		return new Srv(ownername, dnsClass, ttl, priority, weight, port, target);
	}

	public short getPriority() {
		return priority;
	}

	public short getWeight() {
		return weight;
	}

	public short getPort() {
		return port;
	}

	public String getTarget() {
		return target;
	}
}