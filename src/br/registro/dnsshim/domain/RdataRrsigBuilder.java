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

public abstract class RdataRrsigBuilder implements RdataBuilder {
	// RRSIG FIELDS
	private static final int RRSIG_TYPE_COVERED_LEN = 2;
	private static final int RRSIG_LABELS_LEN = 1;
	private static final int RRSIG_ORIGINAL_TTL_LEN = 4;
	private static final int RRSIG_EXPIRATION_LEN = 4;
	private static final int RRSIG_INCEPTION_LEN = 4;

	public static Rdata get(byte[] signature, RrType typeCovered, byte labels,
			int originalTtl, int expiration, int inception, short keyTag,
			String signerName, DnskeyAlgorithm algorithm) {

		byte[] signer = DomainNameUtil.toWireFormat(signerName);

		if (signature == null) {
			signature = new byte[0];
		}

		byte[] data = new byte[RRSIG_TYPE_COVERED_LEN + DNSSEC_ALGORITHM_LEN
				+ RRSIG_LABELS_LEN + RRSIG_ORIGINAL_TTL_LEN + RRSIG_EXPIRATION_LEN
				+ RRSIG_INCEPTION_LEN + DNSSEC_KEYTAG_LEN + signer.length
				+ signature.length];
		int pos = 0;
		// type covered
		ByteUtil.copyBytes(data, pos, typeCovered.getValue());
		pos += RRSIG_TYPE_COVERED_LEN;

		// algorithm
		ByteUtil.copyBytes(data, pos, algorithm.getValue());
		pos += DNSSEC_ALGORITHM_LEN;

		// labels
		ByteUtil.copyBytes(data, pos, labels);
		pos += RRSIG_LABELS_LEN;

		// orignal ttl
		ByteUtil.copyBytes(data, pos, originalTtl);
		pos += RRSIG_ORIGINAL_TTL_LEN;

		// signature expiration
		ByteUtil.copyBytes(data, pos, expiration);
		pos += RRSIG_EXPIRATION_LEN;

		// signature inception
		ByteUtil.copyBytes(data, pos, inception);
		pos += RRSIG_INCEPTION_LEN;

		// key tag
		ByteUtil.copyBytes(data, pos, keyTag);
		pos += DNSSEC_KEYTAG_LEN;

		// signer's name
		System.arraycopy(signer, 0, data, pos, signer.length);
		pos += signer.length;

		System.arraycopy(signature, 0, data, pos, signature.length);

		return new Rdata(data);

	}

	public static Rdata get(Rdata rdataRrsigTemplate, byte[] sig) {
		if (rdataRrsigTemplate == null) {
			throw new IllegalArgumentException();
		}

		int rdlen = rdataRrsigTemplate.getRdlen();
		byte[] data = new byte[rdlen + sig.length];
		System.arraycopy(rdataRrsigTemplate.getData(), 0, data, 0, rdlen);
		System.arraycopy(sig, 0, data, rdlen, sig.length);

		return new Rdata(data);
	}
}
