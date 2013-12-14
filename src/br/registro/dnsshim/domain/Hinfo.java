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

public class Hinfo extends ResourceRecord {
	private final String cpu;
	private final String os;

	public Hinfo(String ownername, DnsClass dnsClass, int ttl,
			String cpu, String os) {
		super(ownername, RrType.HINFO, dnsClass, ttl);
		this.cpu = cpu.toLowerCase();
		this.os = os.toLowerCase();
		this.rdata = RdataHinfoBuilder.get(this.cpu, this.os);
	}

	public Hinfo(String ownername, DnsClass dnsClass, int ttl, String rdata) {
		super(ownername, RrType.HINFO, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");
		this.cpu = scanner.next().toLowerCase();
		this.os = scanner.next().toLowerCase();
		this.rdata = RdataHinfoBuilder.get(cpu, os);
		scanner.close();
	}

	public String getCpu() {
		return cpu;
	}

	public String getOs() {
		return os;
	}

	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		rdata.append(cpu).append(SEPARATOR);
		rdata.append(os).append(SEPARATOR);
		return rdata.toString();
	}

	public static Hinfo parseHinfo(String ownername,
			DnsClass dnsClass, int ttl, ByteBuffer buffer) {
		buffer.getShort(); // rdlength
		short cpuLength = ByteUtil.toUnsigned(buffer.get());
		byte[] cpuBytes = new byte[cpuLength];
		buffer.get(cpuBytes);
		String cpu = new String(cpuBytes);
		short osLength = ByteUtil.toUnsigned(buffer.get());
		byte[] osBytes = new byte[osLength];
		buffer.get(osBytes);
		String os = new String(osBytes);
		return new Hinfo(ownername, dnsClass, ttl, cpu, os);
	}
}
