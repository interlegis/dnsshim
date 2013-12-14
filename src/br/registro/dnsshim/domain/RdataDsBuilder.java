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

public class RdataDsBuilder implements RdataBuilder {

	// DS FIELD
	private static final byte DS_DIGEST_TYPE_LEN = 1;

	/**
	 * RFC 4034 - SEC. 5.1
	 */
	public static Rdata get(short keyTag, DnskeyAlgorithm algorithm, DsDigestType digestType, byte[] digest) {
		int rdLength = DNSSEC_KEYTAG_LEN + DNSSEC_ALGORITHM_LEN +
			DS_DIGEST_TYPE_LEN + digest.length;
		byte[] data = new byte[rdLength];
		int pos = 0;
		// keytag
		ByteUtil.copyBytes(data, pos, keyTag);
		pos += DNSSEC_KEYTAG_LEN;

		// algorithm
		ByteUtil.copyBytes(data, pos, algorithm.getValue());
		pos += DNSSEC_ALGORITHM_LEN;

		// digest type
		ByteUtil.copyBytes(data, pos, digestType.getValue());
		pos += DS_DIGEST_TYPE_LEN;

		// digest
		System.arraycopy(digest, 0, data, pos, digest.length);

		return new Rdata(data);
	}
}
