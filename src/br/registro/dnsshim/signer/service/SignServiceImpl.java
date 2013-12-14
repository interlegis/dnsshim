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
package br.registro.dnsshim.signer.service;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.DnskeyAlgorithm;
import br.registro.dnsshim.domain.Rdata;
import br.registro.dnsshim.domain.RdataRrsigBuilder;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.RrType;
import br.registro.dnsshim.domain.Rrset;
import br.registro.dnsshim.domain.Rrsig;
import br.registro.dnsshim.signer.domain.KeyInfo;
import br.registro.dnsshim.signer.domain.SignData;
import br.registro.dnsshim.signer.domain.logic.KeyInfoCache;
import br.registro.dnsshim.signer.domain.logic.RrsigEnvelopeFactory;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DomainNameUtil;

public class SignServiceImpl implements SignService {
	private static final Logger logger = Logger.getLogger(SignServiceImpl.class);

	@Override
	public List<ResourceRecord> sign(SignData signData,
			List<String> keyIdentifiers, Rrset rrset)
		throws DnsshimProtocolException {

		String zone = signData.getZonename();
		String ownername = rrset.getOwnername();
		if (logger.isDebugEnabled()) {
			logger.debug("Signing rrset " + rrset);
		}
		
		List<ResourceRecord> rrsigList = new ArrayList<ResourceRecord>();
		int originalTtl = 0;

		RrType type = rrset.getType();
		switch (type) {
		case DNSKEY:
			originalTtl = signData.getTtlDnskey();
			break;
		case NSEC:
			originalTtl = signData.getSoaMinimum();
			break;
		default:
			originalTtl = signData.getTtlZone();
			break;
		}
		
		Set<ResourceRecord> records = rrset.getRecords();
		if (records.isEmpty()) {
			throw new DnsshimProtocolException(ProtocolStatusCode.EMPTY_RRSET);
		}

		byte[] envelope = RrsigEnvelopeFactory.generate(zone, ownername, type,
				DnsClass.IN, originalTtl);

		KeyInfoCache keyInfoCache = KeyInfoCache.getInstance();
		
		try {
			for (String key : keyIdentifiers) {
				KeyInfo signerKey = keyInfoCache.get(key);
				if (signerKey == null) {
					throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_KEY);
				}

				byte labelCount =	DomainNameUtil.countLabels(ownername,	zone);


				Rdata rrsigRdata =
					RdataRrsigBuilder.get(null, type,
																labelCount,
																originalTtl,
																signData.getExpiration(),
																signData.getInception(),
																signerKey.getKeyTag(),
																zone,
																signerKey.getAlgorithm());

				Signature signature =
					Signature.getInstance(signerKey.getAlgorithm().getDigestAlgorithm());

				signature.initSign(signerKey.getPrivateKey());

				signature.update(rrsigRdata.getData());

				for (ResourceRecord rr : records) {
					Rdata rdata = rr.getRdata();
					int rdlen = ByteUtil.toUnsigned(rdata.getRdlen());
					byte[] dataUnit = new byte[envelope.length + 2 + rdlen];

					System.arraycopy(envelope, 0, dataUnit, 0, envelope.length);
					ByteUtil.copyBytes(dataUnit, envelope.length, rdata.getRdlen());
					System.arraycopy(rdata.getData(), 0, dataUnit,
							envelope.length + 2, rdlen);
					signature.update(dataUnit);
				}

				byte[] sig = signature.sign();
				
				DnskeyAlgorithm algorithm = signerKey.getAlgorithm();
				int expiration = signData.getExpiration();
				int inception = signData.getInception();
				short keyTag = signerKey.getKeyTag();
				String signerName = zone;
				
				Rrsig rrsig = new Rrsig(ownername,
																rrset.getDnsClass(),
																originalTtl,
																type,
																algorithm,
																labelCount,
																originalTtl,
																expiration,
																inception,
																keyTag,
																signerName,
																sig);
				rrsigList.add(rrsig);
			}

		} catch (NoSuchAlgorithmException nsae) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_ALGORITHM);
		} catch (InvalidKeyException ike) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_KEY);
		} catch (SignatureException se) {
			throw new DnsshimProtocolException(ProtocolStatusCode.SIGNATURE_ERROR);
		}

		return rrsigList;
	}

}
