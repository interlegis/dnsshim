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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

import br.registro.dnsshim.util.ByteUtil;

public class RdataSshfpBuilder implements RdataBuilder {
	private static final Hex hex = new Hex();

	public static Rdata get(FingerprintAlgorithm algorithm,
			FingerprintType type, byte[] fingerprint) throws NoSuchAlgorithmException {
		byte[] digest = RdataSshfpBuilder.digest(type, fingerprint);
		byte[] rdata = new byte[1 + 1 + digest.length];
		int pos = 0;
		ByteUtil.copyBytes(rdata, pos, algorithm.getValue());
		pos += 1;
		ByteUtil.copyBytes(rdata, pos, type.getValue());
		pos += 1;
		System.arraycopy(digest, 0, rdata, pos, digest.length);
		pos += digest.length;
		return new Rdata(rdata);
	}

	private static byte[] digest(FingerprintType algorithm, byte[] data) throws NoSuchAlgorithmException {
		MessageDigest md = null;
		md = MessageDigest.getInstance(algorithm.getName());
		md.update(data);
		byte[] digest = md.digest();
		return hex.encode(digest);
	}
}
