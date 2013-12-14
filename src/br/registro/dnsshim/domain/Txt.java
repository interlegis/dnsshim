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

import br.registro.dnsshim.util.ByteUtil;

public class Txt extends ResourceRecord {
	private final String text;
		
	public Txt(String ownername, DnsClass dnsClass, int ttl, String rdata) {
		super(ownername, RrType.TXT, dnsClass, ttl);
		
		StringBuilder rd = new StringBuilder(rdata);
		if (rd.charAt(0) == '"') {
			rd.deleteCharAt(0);
		}
		
		if (rd.charAt(rd.length() - 1) == '"') {
			rd.deleteCharAt(rd.length() - 1);
		}
		
		text = rd.toString();
		this.rdata = RdataTxtBuilder.get(text);
	}

	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		
		rdata.append('"');
		rdata.append(text);
		rdata.append('"');
		
		return rdata.toString();
	}

	public static Txt parseTxt(String ownername, DnsClass dnsClass, int ttl,
			ByteBuffer buffer) {
		int rdlength = ByteUtil.toUnsigned(buffer.getShort());
		int beginPos = buffer.position();
		
		String text = new String();
		if (buffer.position() - beginPos < rdlength) {
			short length = ByteUtil.toUnsigned(buffer.get());
			byte[] txtBytes = new byte[length];
			buffer.get(txtBytes);
			text = new String(txtBytes);
		}
		
		return new Txt(ownername, dnsClass, ttl, text);
	}
}
