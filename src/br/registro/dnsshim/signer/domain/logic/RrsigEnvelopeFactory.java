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
package br.registro.dnsshim.signer.domain.logic;

import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.RrType;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DomainNameUtil;

public class RrsigEnvelopeFactory {
	public static byte[] generate(String zoneName, String ownername, RrType type,
			DnsClass dnsClass, int ttl) {

		byte[] ownernameWf = DomainNameUtil.toWireFormat(ownername, zoneName);

		// Ownername + type (2 bytes) + DNS class (2 bytes) + TTL (4 bytes)
		byte[] envelope = new byte[ownernameWf.length + (2 * 2) + 4];
		
		// Copy the ownername already in wire format
		System.arraycopy(ownernameWf, 0, envelope, 0, ownernameWf.length);
		
		// Copy the type
		ByteUtil.copyBytes(envelope, ownernameWf.length, type.getValue());
		
		// Copy the DNS class
		ByteUtil.copyBytes(envelope, ownernameWf.length + 2, dnsClass.getValue());
		
		// Copy the TTL
		ByteUtil.copyBytes(envelope, ownernameWf.length + 2 + 2, ttl);

		return envelope;
	}
}
