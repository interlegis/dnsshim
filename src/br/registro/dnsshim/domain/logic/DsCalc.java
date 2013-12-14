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
package br.registro.dnsshim.domain.logic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

import br.registro.dnsshim.domain.Dnskey;
import br.registro.dnsshim.domain.DsDigestType;
import br.registro.dnsshim.util.DomainNameUtil;

public class DsCalc {
	private static final Hex hex = new Hex();
	private static final Logger logger = Logger.getLogger(DsCalc.class);
	/** <b>RFC 4034 - 5.1.4. The Digest Field</b> <br>
	 * The digest is calculated by concatenating the canonical form of the
   	 * fully qualified owner name of the DNSKEY RR with the DNSKEY RDATA,
     * and then applying the digest algorithm.
	 */
	public static String calcDigest(Dnskey dnskey,
			DsDigestType algorithm, String zonename) throws NoSuchAlgorithmException {
		if (logger.isDebugEnabled()) {
			logger.debug("Calculating Digest...");
		}

		byte[] canonicalOwn = DomainNameUtil.toWireFormat(dnskey.getOwnername(), zonename);

		int dsInputLen = canonicalOwn.length + dnskey.getRdata().getRdlen();
		byte[] dsInput = new byte[dsInputLen];

		int pos = 0;

		System.arraycopy(canonicalOwn, 0, dsInput, pos, canonicalOwn.length);

		pos += canonicalOwn.length;

		System.arraycopy(dnskey.getRdata().getData(), 0, dsInput, pos, dnskey.getRdata().getRdlen());

		return new String(DsCalc.digest(algorithm, dsInput));
	}

	private static byte[] digest(DsDigestType algorithm,
			byte[] data) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(algorithm.getAlgorithmName());
		md.update(data);
		byte[] digest = md.digest();
		return hex.encode(digest);
	}
}