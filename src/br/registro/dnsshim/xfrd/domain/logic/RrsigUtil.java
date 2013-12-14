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
package br.registro.dnsshim.xfrd.domain.logic;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;

import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.Dnskey;
import br.registro.dnsshim.domain.Rdata;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.RrType;
import br.registro.dnsshim.domain.Rrset;
import br.registro.dnsshim.domain.Rrsig;
import br.registro.dnsshim.signer.domain.logic.DnskeyCalc;
import br.registro.dnsshim.signer.domain.logic.RrsigEnvelopeFactory;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DomainNameUtil;
import br.registro.dnsshim.xfrd.domain.DnskeyInfo;
import br.registro.dnsshim.xfrd.domain.ZoneInfo;

public class RrsigUtil {

	private ZoneInfo zoneInfo;

	public RrsigUtil(ZoneInfo zoneinfo) throws IOException {
		this.zoneInfo = zoneinfo;
	}

	public boolean verifyRrsig(Set<Rrset> rrsetsToSign, ResourceRecord record)
			throws Exception {
		Rrsig rrsig = (Rrsig) record;
		Signature signature;

		signature =	Signature.getInstance(rrsig.getDnskeyAlgorithmType().getDigestAlgorithm());
		DnskeyInfo dnskeyInfo = zoneInfo.getKey(rrsig.getKeyTag());
		if (dnskeyInfo == null) {
			String msg = "Key not found: " + ByteUtil.toUnsigned(rrsig.getKeyTag());
			throw new KeyStoreException(msg);
		}

		Dnskey dnskey = new Dnskey(ResourceRecord.APEX_OWNERNAME, DnsClass.IN, zoneInfo.getTtlZone(), dnskeyInfo.getRdata());
		
		/* Initializing the object with the public key */
		RSAPublicKey pub = 
			DnskeyCalc.getRsaPublicKey(dnskey.getPublicKey());
		signature.initVerify(pub);

		Rrset rrset = null;
		RrType type = rrsig.getTypeCovered();
		for (Rrset set : rrsetsToSign) {
			if (set.getType() == type && 
					set.getOwnername().equals(rrsig.getOwnername())) {
				rrset = set;
				break;
			}
		}

		if (rrset == null) {
			String msg = 
				"RRSET not found: " + rrsig.getOwnername()	+ " - " + type;
			throw new Exception(msg);
		}

		byte[] envelope = RrsigEnvelopeFactory.generate(DomainNameUtil
				.trimDot(rrsig.getSignerName()), rrsig.getOwnername(), rrsig
				.getTypeCovered(), DnsClass.IN, rrsig.getOriginalTtl());

		for (ResourceRecord rr : rrset.getRecords()) {
			Rdata rd = rr.getRdata();
			byte[] dataUnit = new byte[envelope.length + 2 + rd.getRdlen()];

			System.arraycopy(envelope, 0, dataUnit, 0, envelope.length);
			ByteUtil.copyBytes(dataUnit, envelope.length, rd.getRdlen());
			System.arraycopy(rd.getData(), 0, dataUnit, envelope.length + 2, rd
					.getRdlen());

			signature.update(dataUnit);
		}

		return signature.verify(rrsig.getSignature());
	}
}
