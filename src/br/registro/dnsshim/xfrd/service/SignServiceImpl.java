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
package br.registro.dnsshim.xfrd.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import br.registro.dnsshim.common.server.ClientConfig;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.Rrset;
import br.registro.dnsshim.domain.Rrsig;
import br.registro.dnsshim.signer.domain.SignData;
import br.registro.dnsshim.signer.protocol.ResponseStatus;
import br.registro.dnsshim.signer.protocol.SignRequest;
import br.registro.dnsshim.signer.protocol.SignRequestHeader;
import br.registro.dnsshim.signer.protocol.SignResponse;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneInfoDao;
import br.registro.dnsshim.xfrd.domain.XfrdConfig;
import br.registro.dnsshim.xfrd.domain.ZoneInfo;
import br.registro.dnsshim.xfrd.domain.logic.RrsigUtil;
import br.registro.dnsshim.xfrd.domain.logic.XfrdConfigManager;
import br.registro.dnsshim.xfrd.signerclient.SignerClient;
import br.registro.dnsshim.xfrd.util.ServerContext;

public class SignServiceImpl implements SignService {

	@Override
	public List<ResourceRecord> sign(SignData signData,
			List<String> keyIdentifiers, Rrset rrsetToSign) throws IOException, DnsshimProtocolException {

		Set<Rrset> rrsets = new LinkedHashSet<Rrset>();
		rrsets.add(rrsetToSign);
		return sign(signData, keyIdentifiers, rrsets);
	}

	@Override
	public List<ResourceRecord> sign(SignData signData,
			List<String> keyIdentifiers, Set<Rrset> rrsetsToSign)
		throws IOException, DnsshimProtocolException {

		List<ResourceRecord> records = new ArrayList<ResourceRecord>();

		if (rrsetsToSign.isEmpty()) {
			return records;
		} else if (keyIdentifiers.isEmpty()) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_KEY, "Empty list of key identifiers");
		}

		XfrdConfig config = XfrdConfigManager.getInstance();
		ClientConfig signerConfig = config.getSignerClientConfig();
		SignerClient client = new SignerClient(signerConfig.getHost(), signerConfig.getPort(),
				signerConfig.getCaPath(), signerConfig.getCaPasswd());

		SignRequestHeader header = new SignRequestHeader();
		header.setSignData(signData);
		header.setKeys(keyIdentifiers);

		SignRequest request = new SignRequest();

		request.setRrsets(rrsetsToSign);
		request.setSignRequestHeader(header);

		List<SignResponse> responses = client.sign(request);
		
		ZoneInfoDao zoneInfoDao = new ZoneInfoDao(ServerContext.getEntityManager());
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(signData.getZonename());

		if (zoneInfo == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found: "+signData.getZonename());
		}
		
		RrsigUtil rrsigUtil = new RrsigUtil(zoneInfo);

		for (SignResponse resp : responses) {
			if (resp.getResponseStatus() != ResponseStatus.OK) {
				throw new DnsshimProtocolException(ProtocolStatusCode.SIGNER_SERVER_FAILURE, "ERROR: Response status != OK");
			}

			for (ResourceRecord record : resp.getRecords()) {
				try {
					if (!zoneInfo.isVerifySignature() ||
							rrsigUtil.verifyRrsig(rrsetsToSign, record)) {
						Rrsig rrsig = (Rrsig) record;
						records.add(rrsig);				
					} else {
						throw new DnsshimProtocolException(ProtocolStatusCode.BAD_TSIG, "Error: Invalid RRSIG");
					}
				} catch (Exception e) {
					throw new DnsshimProtocolException(ProtocolStatusCode.SIGNATURE_ERROR, "Error verifying signatures");
				}
			}

		}

		return records;
	}
}