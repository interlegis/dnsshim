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

public class RdataNsec3ParamBuilder implements RdataBuilder {
	private static int NSEC3_HASH_ALGORITHM_LEN = 1;
	private static int NSEC3_FLAGS_LEN = 1;
	private static int NSEC3_ITERATIONS_LEN = 2;

	public static Rdata get(DsDigestType hashAlgorithm, byte flags,
			short iterations, byte[] salt) {
		byte[] rdata = new byte[NSEC3_HASH_ALGORITHM_LEN + NSEC3_FLAGS_LEN +
		                        NSEC3_ITERATIONS_LEN + 1 + salt.length];
		int pos = 0;
		ByteUtil.copyBytes(rdata, pos, hashAlgorithm.getValue());
		pos += 1;
		ByteUtil.copyBytes(rdata, pos, flags);
		pos += 1;
		ByteUtil.copyBytes(rdata, pos, iterations);
		pos += 2;
		ByteUtil.copyBytes(rdata, pos, (byte) salt.length);
		pos += 1;
		System.arraycopy(salt, 0, rdata, pos, salt.length);
		pos += salt.length;
		return new Rdata(rdata);
	}
}
