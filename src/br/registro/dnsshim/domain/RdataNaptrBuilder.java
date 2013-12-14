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

import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DomainNameUtil;

public class RdataNaptrBuilder implements RdataBuilder {
	public static Rdata get(short order, short preference, String flags,
			String services, String regexp,	String replacement) {
		byte[] flagsBytes = flags.getBytes();
		int flagsLen = flagsBytes.length;
		byte[] servicesBytes = services.getBytes();
		int servicesLen = services.length();
		byte[] regexpBytes = regexp.getBytes();
		int regexpLen = regexpBytes.length;
		byte[] replacementBytes = DomainNameUtil.toWireFormat(replacement);
		int replacementLen = replacementBytes.length;

		int len = 2 + 2 + flagsLen + servicesLen + regexpLen + replacementLen;

		byte[] naptrBytes = new byte[len];
		int pos = 0;

		ByteUtil.copyBytes(naptrBytes, pos, order);
		pos += 2;

		ByteUtil.copyBytes(naptrBytes, pos, preference);
		pos += 2;

		System.arraycopy(flagsBytes, 0, naptrBytes, pos, flagsLen);
		pos += flagsLen;

		System.arraycopy(servicesBytes, 0, naptrBytes, pos, servicesLen);
		pos += servicesLen;

		System.arraycopy(regexpBytes, 0, naptrBytes, pos, regexpLen);
		pos += regexpLen;

		System.arraycopy(replacementBytes, 0, naptrBytes, pos, replacementLen);

		return new Rdata(naptrBytes);

	}

}
