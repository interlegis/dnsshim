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

import org.apache.commons.codec.binary.Base64;

import br.registro.dnsshim.util.ByteUtil;

public abstract class RdataDnskeyBuilder implements RdataBuilder {
	// DNSKEY FIELDS
	private static final byte DNSKEY_PROTO = 3;
	private static final int DNSKEY_PROTO_LEN = 1;
	private static final int DNSKEY_FLAGS_LEN = 2;

	public static Rdata get(DnskeyAlgorithm algorithm,
			DnskeyFlags dnskeyFlags, byte[] publicKey, boolean decodePublicKey) {
		byte[] binaryPublicKey = null;

		if (decodePublicKey) {
			Base64 base64 = new Base64();
			binaryPublicKey = base64.decode(publicKey);
		} else {
			binaryPublicKey = publicKey;
		}

		int len = DNSKEY_FLAGS_LEN + DNSKEY_PROTO_LEN + DNSSEC_ALGORITHM_LEN
				+ binaryPublicKey.length;
		byte[] data = new byte[len];
		int curr = 0;
		
		// flags
		ByteUtil.copyBytes(data, curr, dnskeyFlags.getFlags());
		curr += DNSKEY_FLAGS_LEN;

		// protocol
		data[curr] = DNSKEY_PROTO;
		curr += DNSKEY_PROTO_LEN;

		// algorithm
		data[curr] = algorithm.getValue();
		curr += DNSSEC_ALGORITHM_LEN;

		// public key
		System.arraycopy(binaryPublicKey, 0, data, curr, binaryPublicKey.length);

		return new Rdata(data);
	}
}
