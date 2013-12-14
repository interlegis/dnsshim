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
package br.registro.dnsshim.xfrd.signerclient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.AbstractProtocolDecoder;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.IoSession;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.Dnskey;
import br.registro.dnsshim.domain.DnskeyAlgorithm;
import br.registro.dnsshim.domain.DnskeyFlags;
import br.registro.dnsshim.domain.DnskeyProtocol;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.Rrsig;
import br.registro.dnsshim.signer.protocol.KeyResponse;
import br.registro.dnsshim.signer.protocol.OperationType;
import br.registro.dnsshim.signer.protocol.RemoveKeyResponse;
import br.registro.dnsshim.signer.protocol.ResponseStatus;
import br.registro.dnsshim.signer.protocol.SignResponse;
import br.registro.dnsshim.util.ByteUtil;

public class Decoder extends AbstractProtocolDecoder {
	private static final Logger logger = Logger.getLogger(Decoder.class);

	@Override
	public void decode(IoSession session) throws IOException, DnsshimProtocolException {

		ByteBuffer buffer = ByteBuffer.allocate(1);
		readSession(session, buffer);
		ResponseStatus responseStatus = ResponseStatus.fromValue(buffer.get());
		
		if (responseStatus == ResponseStatus.OK) {
			buffer.rewind();
			readSession(session, buffer);
			OperationType operation = OperationType.fromValue(buffer.get());
			
			if (operation == OperationType.GENERATE_KEY ||
					operation == OperationType.IMPORT_KEY) {
				KeyResponse response = new KeyResponse();
				response.setResponseStatus(responseStatus);

				buffer = ByteBuffer.allocate(2);
				readSession(session, buffer);
				short keyNameLen = buffer.getShort();
				buffer = ByteBuffer.allocate(keyNameLen);
				readSession(session, buffer);
				response.setKeyName(new String(buffer.array()));

				buffer = ByteBuffer.allocate(4);
				readSession(session, buffer);

				response.setKeyLength(buffer.getInt());

				int keyLength = response.getKeyLength();
				buffer = ByteBuffer.allocate(keyLength);
				readSession(session, buffer);
				DnskeyFlags flags = DnskeyFlags.fromValue(buffer.getShort());
				DnskeyProtocol protocol = DnskeyProtocol.fromValue(buffer.get());
				DnskeyAlgorithm algorithm = DnskeyAlgorithm.fromValue(buffer.get());
				byte[] keyDigest = new byte[keyLength - 4];
				buffer.get(keyDigest);
				
				Dnskey dnskey =	new Dnskey(ResourceRecord.APEX_OWNERNAME, DnsClass.IN, 0,
						flags, protocol, algorithm, keyDigest);
				response.setResourceRecord(dnskey);
				getOutput().write(response, session);
				
			} else if (operation == OperationType.REMOVE_KEY) {
				RemoveKeyResponse response = new RemoveKeyResponse();
				response.setOperation(OperationType.REMOVE_KEY);
				response.setResponseStatus(responseStatus);
				getOutput().write(response, session);
				
			} else if (operation == OperationType.SIGNING) {
				
				// STATUS(1) + OP(1) + RRSIG_COUNT(4) + *[ (OWNERNAME_LEN(4) + OWNERNAME + CLASS(2) + TTL(4) + RDLEN(4) + RDATA]

				List<SignResponse> responses = new ArrayList<SignResponse>();
				
				SignResponse response = new SignResponse();
				response.setResponseStatus(responseStatus);

				buffer = ByteBuffer.allocate(4);
				readSession(session, buffer);
				int rrsigCount = buffer.getInt();
				
				List<ResourceRecord> rrsigs = new ArrayList<ResourceRecord>();
				
				for (int x = 0; x < rrsigCount; x++) {
					buffer = ByteBuffer.allocate(4);
					readSession(session, buffer);
					int ownernameLen = buffer.getInt();
					
					buffer = ByteBuffer.allocate(ownernameLen);
					readSession(session, buffer);
					String ownername = new String(buffer.array());

					buffer = ByteBuffer.allocate(2);
					readSession(session, buffer);
					DnsClass dnsClass = DnsClass.fromValue(buffer.getShort());

					buffer = ByteBuffer.allocate(4);
					readSession(session, buffer);
					int ttl = buffer.getInt();
					
					buffer = ByteBuffer.allocate(2);
					readSession(session, buffer);
					short rdataLen = buffer.getShort();

					buffer = ByteBuffer.allocate(ByteUtil.toUnsigned(rdataLen));

					readSession(session, buffer);
					byte[] rdata = buffer.array();
					buffer = ByteBuffer.allocate(rdata.length + 2);
					buffer.putShort(rdataLen);
					buffer.put(rdata);
					buffer.flip();
					Rrsig rrsig = Rrsig.parseRrsig(ownername, dnsClass, ttl, buffer);
					rrsigs.add(rrsig);
				}
				
				response.setRecords(rrsigs);
				
				responses.add(response);
							
				getOutput().write(responses, session);
			}
		} else if (responseStatus == ResponseStatus.ERROR){
			buffer = ByteBuffer.allocate(4);
			readSession(session, buffer);
			int messageLen = buffer.getInt();
			logger.error(messageLen);
			buffer = ByteBuffer.allocate(messageLen);
			readSession(session, buffer);
			String message = new String(buffer.array());
			logger.error(message);
			throw new DnsshimProtocolException(ProtocolStatusCode.SIGNER_SERVER_FAILURE, message);
		}
	}
}