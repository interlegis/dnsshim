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
import java.util.EnumSet;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DomainNameUtil;

public class Nsec extends ResourceRecord {

	private final String nextOwnername;
	private final Set<RrType> types;

	public Nsec(String ownername, DnsClass dnsClass, int ttl,
			String nextOwnername, Set<RrType> types) {
		super(ownername, RrType.NSEC, dnsClass, ttl);
		this.nextOwnername = nextOwnername.toLowerCase();
		this.types = types;
		this.rdata = RdataNsecBuilder.get(this.nextOwnername, types);
	}
	
	public Nsec(String ownername, DnsClass dnsClass, int ttl, String rdata) {
		super(ownername, RrType.NSEC, dnsClass, ttl);
		
		types = EnumSet.noneOf(RrType.class);
		
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");
		this.nextOwnername = scanner.next().toLowerCase();
		 
		while (scanner.hasNext()) {
			String typeStr = scanner.next();
			types.add(RrType.valueOf(typeStr));
		}
		this.rdata = RdataNsecBuilder.get(nextOwnername, types);
		scanner.close();
	}
	
	public String getNextOwnername() {
		return nextOwnername;
	}

	public Set<RrType> getTypes() {
		return types;
	}
	
	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		rdata.append(nextOwnername).append(SEPARATOR);

		for (RrType type : this.types) {
			rdata.append(type).append(SEPARATOR);
		}

		return rdata.toString();
	}
	
	public static Nsec parseNsec(String ownername, DnsClass dnsClass, int ttl,
			ByteBuffer buffer) {
		int rdlength = ByteUtil.toUnsigned(buffer.getShort());
		int beginPos = buffer.position();
		String nextOwnername = DomainNameUtil.toPresentationFormat(buffer);
		Set<RrType> types = new TreeSet<RrType>();
		while (buffer.position() - beginPos < rdlength) {
			short windowBlock = ByteUtil.toUnsigned(buffer.get());
			short bitmapLen = ByteUtil.toUnsigned(buffer.get());
			byte[] bitmap = new byte[bitmapLen];
			buffer.get(bitmap);
			for (int i = 0; i < bitmapLen; i++) {
				for (int j = 0; j < 8; j++) {
					if (ByteUtil.getBit(bitmap[i], j + 1)) {
						int typeValue = j + i*8 + windowBlock*256;
						types.add(RrType.fromValue((short) typeValue));
					}
				}
			}
		}
		
		return new Nsec(ownername, dnsClass, ttl, nextOwnername, types);
	}
}
