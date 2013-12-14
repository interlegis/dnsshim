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
package br.registro.dnsshim.signer.protocol;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.AbstractDecoderOutput;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.IoSession;
import br.registro.dnsshim.domain.Dnskey;
import br.registro.dnsshim.domain.DnskeyAlgorithm;
import br.registro.dnsshim.domain.DnskeyFlags;
import br.registro.dnsshim.domain.DnskeyType;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.Rrset;
import br.registro.dnsshim.signer.domain.KeyInfo;
import br.registro.dnsshim.signer.domain.logic.KeyInfoCache;
import br.registro.dnsshim.signer.service.KeyService;
import br.registro.dnsshim.signer.service.KeyServiceImpl;
import br.registro.dnsshim.signer.service.SignService;
import br.registro.dnsshim.signer.service.SignServiceImpl;
import br.registro.dnsshim.util.ByteUtil;

public class DecoderOutput extends AbstractDecoderOutput {
	private static final Logger logger = Logger.getLogger(DecoderOutput.class);

	@Override
	public void doOutput(Object message, IoSession session) throws DnsshimProtocolException, IOException{
		if (message instanceof GenerateKeyRequest) {
			GenerateKeyRequest generateKeyRequest = (GenerateKeyRequest) message;

			KeyService generateKeyService = new KeyServiceImpl();
			DnskeyAlgorithm algorithm = generateKeyRequest.getAlgorithm();
			DnskeyType keyType = generateKeyRequest.getKeyType();
			int keySize = generateKeyRequest.getKeySize();
			DnskeyFlags flags = generateKeyRequest.getFlags();
			KeyInfo keyInfo = generateKeyService.generate(algorithm, keySize, flags, keyType);

			KeyInfoCache keyInfoCache = KeyInfoCache.getInstance();
			keyInfoCache.put(keyInfo);

			KeyResponse response = new KeyResponse();
			response.setOperation(OperationType.GENERATE_KEY);
			response.setKeyName(keyInfo.getName());
			Dnskey dnskey = (Dnskey) keyInfo.getResourceRecord();
			response.setResourceRecord(dnskey);
			response.setKeyLength(ByteUtil.toUnsigned(dnskey.getRdata().getRdlen()));
			response.setResponseStatus(ResponseStatus.OK);
			getEncoder().encode(response, session);

		} else if (message instanceof ImportKeyRequest) {
			ImportKeyRequest importKeyRequest = (ImportKeyRequest) message;
			
			KeyService service = new KeyServiceImpl();
			KeyInfo keyInfo = service.importKey(importKeyRequest);
			
			KeyInfoCache keyInfoCache = KeyInfoCache.getInstance();
			keyInfoCache.put(keyInfo);

			KeyResponse response = new KeyResponse();
			response.setOperation(OperationType.IMPORT_KEY);
			response.setKeyName(keyInfo.getName());
			response.setResourceRecord(keyInfo.getResourceRecord());
			response.setKeyLength(ByteUtil.toUnsigned(keyInfo.getResourceRecord()
					.getRdata().getRdlen()));
			response.setResponseStatus(ResponseStatus.OK);
			getEncoder().encode(response, session);
			
		} else if (message instanceof RemoveKeyRequest) {
			RemoveKeyRequest request = (RemoveKeyRequest) message;
			KeyService service = new KeyServiceImpl();
			service.removeKey(request);
			
			RemoveKeyResponse response = new RemoveKeyResponse();
			response.setOperation(OperationType.REMOVE_KEY);
			response.setResponseStatus(ResponseStatus.OK);
			getEncoder().encode(response, session);
			
		} else if (message instanceof SignRequest) {
			SignRequest signRequest = (SignRequest) message;
			SignRequestHeader header = signRequest.getSignRequestHeader();

			SignService signService = new SignServiceImpl();

			SignResponse signResponse = new SignResponse();
			for (Rrset rrset : signRequest.getRrsets()) {
				List<ResourceRecord> rrsigs = signService.sign(header.getSignData(), 
						header.getKeys(), rrset);
				signResponse.addAllRecords(rrsigs);
			}

			signResponse.setResponseStatus(ResponseStatus.OK);
			if (logger.isInfoEnabled()) {
				logger.info("Zone " + header.getSignData().getZonename() + " signed.");
			}
			getEncoder().encode(signResponse, session);
		}

	}
}