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

public class RdataSrvBuilder implements RdataBuilder {
	public static Rdata get(short priority, short weight, short port, String target) {
		byte[] p = ByteUtil.toByteArray(priority);
		byte[] w = ByteUtil.toByteArray(weight);
		byte[] po = ByteUtil.toByteArray(port);
		byte[] name = DomainNameUtil.toWireFormat(target);
		
		byte[] data = new byte[p.length + w.length + po.length + name.length];
		
		int pos = 0;
		System.arraycopy(p, 0, data, pos, p.length);
		pos += p.length;
		
		System.arraycopy(w, 0, data, pos, w.length);
		pos += w.length;
		
		System.arraycopy(po, 0, data, pos, po.length);
		pos += po.length;
		
		System.arraycopy(name, 0, data, pos, name.length);
		pos += name.length;
		
		return new Rdata(data);
	}
}
