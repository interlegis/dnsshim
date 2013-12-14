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
import java.nio.ByteBuffer;
import java.util.List;

import br.registro.dnsshim.common.server.AbstractProtocolEncoder;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ErrorMessage;
import br.registro.dnsshim.common.server.IoSession;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.domain.Dnskey;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.util.ByteUtil;

public class Encoder extends AbstractProtocolEncoder {
	
	@Override
	public void encode(Object message, IoSession session) throws DnsshimProtocolException, IOException {
		OperationType operation;
		if (message instanceof KeyResponse) {
			KeyResponse response = (KeyResponse) message;
			operation = response.getOperation(); 

			Dnskey dnskey = (Dnskey) response.getResourceRecord();
			String keyName = response.getKeyName();
			byte[] keyNameBytes = keyName.getBytes();
			int len =	ByteUtil.toUnsigned(dnskey.getRdata().getRdlen());
			ByteBuffer buffer = ByteBuffer.allocate(1 + 1 + 2 + keyNameBytes.length + 4 + len); 

			buffer.put(response.getResponseStatus().getValue());
			buffer.put(operation.getValue());
			buffer.putShort((short) keyNameBytes.length);
			buffer.put(keyNameBytes);
			buffer.putInt(len);
			buffer.put(dnskey.getRdata().getData());
			buffer.flip();

			getOutput().write(buffer, session);
			
		} else if (message instanceof RemoveKeyResponse) {
			RemoveKeyResponse response = (RemoveKeyResponse) message;
			operation = response.getOperation();
			ByteBuffer buffer = ByteBuffer.allocate(1 + 1);
			buffer.put(response.getResponseStatus().getValue());
			buffer.put(operation.getValue());
			buffer.flip();
			getOutput().write(buffer, session);
			
		} else if (message instanceof SignResponse) {
			SignResponse responses = (SignResponse) message;
			int packLen = 0; 
			List<ResourceRecord> rrsigs = responses.getRecords();
			if (rrsigs.isEmpty()) {
				throw new DnsshimProtocolException(ProtocolStatusCode.EMPTY_RRSET);
			}

			for (ResourceRecord record : rrsigs) {
				packLen += ByteUtil.toUnsigned(record.getRdata().getRdlen());
				packLen += record.getOwnername().length();
			}
			
			// *[RDATA + OWNERNAME] + STATUS(1) + OP(1) + RRSIG_COUNT(4) + *[ (OWNERNAME_LEN(4) + CLASS(2) + TTL(4) + RDLEN(4) ]
			
			packLen +=  1 + 1 + 4 + (rrsigs.size() * (4 + 2 + 4 + 4));

			ByteBuffer buffer = ByteBuffer.allocate(packLen);
			operation = OperationType.SIGNING;
			
			buffer.put(responses.getResponseStatus().getValue());
			buffer.put(operation.getValue());
			buffer.putInt(rrsigs.size());
			
			for (ResourceRecord rrsig : rrsigs) {
				String ownername = rrsig.getOwnername();
				buffer.putInt(ownername.length());
				buffer.put(ownername.getBytes());
				buffer.putShort(rrsig.getDnsClass().getValue());
				buffer.putInt(rrsig.getTtl());
				buffer.putShort(rrsig.getRdata().getRdlen());
				buffer.put(rrsig.getRdata().getData());
			}

			buffer.flip();

			getOutput().write(buffer, session);	
		}
		
	}

	@Override
	public void encodeErrorMessage(ErrorMessage message, IoSession session)
			throws DnsshimProtocolException,IOException {
		ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + message.getMessage().length());
		buffer.put(ResponseStatus.ERROR.getValue());
		buffer.putInt(message.getMessage().length());
		buffer.put(message.getMessage().getBytes());
		buffer.flip();
		getOutput().write(buffer, session);		
	}
}
