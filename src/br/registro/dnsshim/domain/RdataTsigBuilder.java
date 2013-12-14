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
import br.registro.dnsshim.xfrd.dns.protocol.ResponseCode;

public class RdataTsigBuilder implements RdataBuilder {

	public static Rdata get(String algorithmName, long timeSigned, short fudge,
			byte[] mac, short originalId, ResponseCode error, byte[] otherData) {
		
		byte[] aName = DomainNameUtil.toWireFormat(algorithmName);
		
		// algorithmName.length + timeSigned(6) + fudge(2) + macSize(2) + mac +
		// originalId(2) + error(2) + otherDataLen(2) + otherData
		byte[] rdata = new byte[aName.length + 6 + 2 + 2 + mac.length + 2 + 2 + 2 + otherData.length];
		int pos = 0;
		
		// ALGORITHM NAME
		System.arraycopy(aName, 0, rdata, pos, aName.length);
		pos += aName.length;

		// TIME SIGNED: discard the first two bytes
		byte[] b = ByteUtil.toByteArray(timeSigned);
		System.arraycopy(b, 2, rdata, pos, b.length - 2);
		pos += b.length - 2;
		
		// FUDGE
		b = ByteUtil.toByteArray(fudge);
		System.arraycopy(b, 0, rdata, pos, 2);
		pos += 2;
		
		// MAC LEN
		b = ByteUtil.toByteArray((short) mac.length);
		System.arraycopy(b, 0, rdata, pos, 2);
		pos += 2;
		
		// MAC
		System.arraycopy(mac, 0, rdata, pos, mac.length);
		pos += mac.length;
		
		// ORIGINAL ID
		b = ByteUtil.toByteArray(originalId);
		System.arraycopy(b, 0, rdata, pos, 2);
		pos += 2;
		
		// DNS RESPONSE CODE
		b = ByteUtil.toByteArray((short) error.getValue());
		System.arraycopy(b, 0, rdata, pos, 2);
		pos += 2;
		
		// OTHER DATA LENGTH
		b = ByteUtil.toByteArray((short) otherData.length);
		System.arraycopy(b, 0, rdata, pos, 2);
		pos += 2;

		// OTHER DATA
		System.arraycopy(otherData, 0, rdata, pos, otherData.length);
		
		return new Rdata(rdata);
	}
}
