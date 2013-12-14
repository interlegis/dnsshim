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

public abstract class RdataSoaBuilder implements RdataBuilder {

	public static Rdata get(String soaMname, String soaRname, int serial,
			int refresh, int retry, int expire, int minimum) {

		byte[] mname = DomainNameUtil.toWireFormat(soaMname);
		byte[] rname = DomainNameUtil.toWireFormat(soaRname);

		int len = mname.length + rname.length + (5 * 4);
		byte data[] = new byte[len];

		int pos = 0;

		// mname
		System.arraycopy(mname, 0, data, pos, mname.length);
		pos += mname.length;

		// rname
		System.arraycopy(rname, 0, data, pos, rname.length);
		pos += rname.length;

		// serial
		byte[] serialArray = ByteUtil.toByteArray(serial);
		System.arraycopy(serialArray, 0, data, pos, serialArray.length);
		pos += serialArray.length;

		// refresh
		byte[] refreshArray = ByteUtil.toByteArray(refresh);
		System.arraycopy(refreshArray, 0, data, pos, refreshArray.length);
		pos += refreshArray.length;

		// retry
		byte[] retryArray = ByteUtil.toByteArray(retry);
		System.arraycopy(retryArray, 0, data, pos, retryArray.length);
		pos += retryArray.length;

		// expire
		byte[] expireArray = ByteUtil.toByteArray(expire);
		System.arraycopy(expireArray, 0, data, pos, expireArray.length);
		pos += expireArray.length;

		// minimum
		byte[] minimumArray = ByteUtil.toByteArray(minimum);
		System.arraycopy(minimumArray, 0, data, pos, minimumArray.length);
		pos += minimumArray.length;

		return new Rdata(data);
	}
}
