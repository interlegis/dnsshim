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

import java.security.InvalidAlgorithmParameterException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.Dnskey;
import br.registro.dnsshim.domain.DnskeyAlgorithm;
import br.registro.dnsshim.domain.DnskeyFlags;
import br.registro.dnsshim.domain.DnskeyProtocol;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.signer.domain.KeyInfo;

public class KeyInfoFactory {

	public KeyInfo generate(String keyInfoName, PrivateKey privateKey,
			PublicKey publicKey, DnskeyFlags dnskeyFlags)
			throws InvalidAlgorithmParameterException {

		KeyInfo keyInfo = new KeyInfo();
		keyInfo.setName(keyInfoName);
		keyInfo.setPrivateKey(privateKey);
		keyInfo.setPublicKey(publicKey);
		keyInfo.setDnskeyFlags(dnskeyFlags);

		// TODO: so far only RSASHA1 supported
		if ((!privateKey.getAlgorithm().equals(
				DnskeyAlgorithm.RSASHA1.getAlgorithmName()) && !privateKey
				.getAlgorithm().equals(
						DnskeyAlgorithm.RSASHA1_NSEC3.getAlgorithmName()))) {
			throw new InvalidAlgorithmParameterException();
		}

		DnskeyAlgorithm algorithm = DnskeyAlgorithm.RSASHA1;
		byte[] keyDigest = DnskeyCalc.calcRsaBuffer((RSAPublicKey) publicKey);

		// all DNSKEYs must appear in the apex of the zone		
		Dnskey dnskey = new Dnskey(ResourceRecord.APEX_OWNERNAME, DnsClass.IN, 0,
				dnskeyFlags, DnskeyProtocol.PROTOCOL, algorithm, keyDigest);

		keyInfo.setResourceRecord(dnskey);
		keyInfo.setAlgorithm(algorithm);
		keyInfo.setKeyTag((DnskeyCalc.calcKeyTag(keyInfo.getResourceRecord()
				.getRdata())));

		return keyInfo;
	}
}
