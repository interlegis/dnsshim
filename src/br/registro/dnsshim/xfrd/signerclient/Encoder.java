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

import br.registro.dnsshim.common.server.AbstractProtocolEncoder;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ErrorMessage;
import br.registro.dnsshim.common.server.IoSession;
import br.registro.dnsshim.domain.Rdata;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.Rrset;
import br.registro.dnsshim.signer.domain.SignData;
import br.registro.dnsshim.signer.protocol.GenerateKeyRequest;
import br.registro.dnsshim.signer.protocol.ImportKeyRequest;
import br.registro.dnsshim.signer.protocol.OperationType;
import br.registro.dnsshim.signer.protocol.RemoveKeyRequest;
import br.registro.dnsshim.signer.protocol.SignRequest;
import br.registro.dnsshim.signer.protocol.SignRequestHeader;
import br.registro.dnsshim.util.ByteUtil;

public class Encoder extends AbstractProtocolEncoder {

	@Override
	public void encode(Object message, IoSession session) throws IOException, DnsshimProtocolException {
		if (message instanceof GenerateKeyRequest) {
			GenerateKeyRequest request = (GenerateKeyRequest) message;
			String keyType = request.getKeyType().toString();
			byte[] keyTypeBytes = keyType.getBytes();
			ByteBuffer buffer = ByteBuffer.allocate(1 + 2 + keyTypeBytes.length + 2 + 1 + 1 + 4);
			
			buffer.put(OperationType.GENERATE_KEY.getValue());
			
			buffer.putShort((short) keyTypeBytes.length);
			buffer.put(keyTypeBytes);
			buffer.putShort(request.getFlags().getFlags());
			buffer.put(request.getAlgorithm().getValue());
			buffer.put(request.getDnskeyProtocol().getValue());
			buffer.putInt(request.getKeySize());
			buffer.flip();
			getOutput().write(buffer, session);
			
		} else if (message instanceof ImportKeyRequest) {
			ImportKeyRequest request = (ImportKeyRequest) message;
			
			byte[] modulus = request.getModulus().toByteArray();
			int modLen = modulus.length;
			byte[] publicExponent = request.getPublicExponent().toByteArray();
			int publicExpLen = publicExponent.length;
			byte[] privateExponent = request.getPrivateExponent().toByteArray();
			int privateExpLen = privateExponent.length;
			byte[] prime1 = request.getPrime1().toByteArray();
			int prime1Len = prime1.length;
			byte[] prime2 = request.getPrime2().toByteArray();
			int prime2Len = prime2.length;
			byte[] exponent1 = request.getExponent1().toByteArray();
			int exponent1Len = exponent1.length;
			byte[] exponent2 = request.getExponent2().toByteArray();
			int exponent2Len = exponent2.length;
			byte[] coefficient = request.getCoefficient().toByteArray();
			int coefficientLen = coefficient.length;
			
			String keyType = request.getKeyType().toString();
			byte[] keyTypeBytes = keyType.getBytes();
			ByteBuffer buffer =
				ByteBuffer.allocate(1 + 1 + 2 + keyTypeBytes.length +
														2 + 4 + modLen +
														4 +	publicExpLen + 4 + privateExpLen +
														4 + prime1Len + 4 + prime2Len +
														4 + exponent1Len + 4 + exponent2Len +
														4 + coefficientLen);
			
			buffer.put(OperationType.IMPORT_KEY.getValue());
			
			buffer.putShort((short) keyTypeBytes.length);
			buffer.put(keyTypeBytes);
			buffer.put(request.getAlgorithm().getValue());
			buffer.putShort(request.getFlags().getFlags());
			buffer.putInt(modLen);
			buffer.put(modulus);
			buffer.putInt(publicExpLen);
			buffer.put(publicExponent);
			buffer.putInt(privateExpLen);
			buffer.put(privateExponent);
			buffer.putInt(prime1Len);
			buffer.put(prime1);
			buffer.putInt(prime2Len);
			buffer.put(prime2);
			buffer.putInt(exponent1Len);
			buffer.put(exponent1);
			buffer.putInt(exponent2Len);
			buffer.put(exponent2);
			buffer.putInt(coefficientLen);
			buffer.put(coefficient);
			
			buffer.flip();
			getOutput().write(buffer, session);
			
		} else if (message instanceof RemoveKeyRequest) {
			RemoveKeyRequest request = (RemoveKeyRequest) message;
			String keyName = request.getKeyName();
			byte[] keyNameBytes = keyName.getBytes();
			ByteBuffer buffer = ByteBuffer.allocate(1 + 2 + keyNameBytes.length);
			buffer.put(OperationType.REMOVE_KEY.getValue());
			buffer.putShort((short) keyNameBytes.length);
			buffer.put(keyNameBytes);
			buffer.flip();
			getOutput().write(buffer, session);

		} else if (message instanceof SignRequest) {
			SignRequest request = (SignRequest) message;
			SignRequestHeader header = request.getSignRequestHeader();
			SignData signData = header.getSignData();
			String zonename = signData.getZonename();
			List<String> keys = header.getKeys();
			
			int requestLen = 1 + 4 + zonename.length() + 4 + 4 + 4 + 4 + 4 + 4;
			for (String key : keys) {
				requestLen += 2 + key.getBytes().length;
			}
			requestLen += 4;
			ByteBuffer headerBuffer = ByteBuffer.allocate(requestLen);
			
			headerBuffer.put(OperationType.SIGNING.getValue());
			headerBuffer.putInt(zonename.length());
			headerBuffer.put(zonename.getBytes());
			headerBuffer.putInt(signData.getTtlZone());
			headerBuffer.putInt(signData.getTtlDnskey());
			headerBuffer.putInt(signData.getSoaMinimum());
			headerBuffer.putInt(signData.getInception());
			headerBuffer.putInt(signData.getExpiration());
			headerBuffer.putInt(keys.size());

			for (String key : keys) {
				byte[] keyBytes = key.getBytes();
				headerBuffer.putShort((short) keyBytes.length);
				headerBuffer.put(keyBytes);
			}
			
			headerBuffer.putInt(request.getRrsets().size());
			
			List<ByteBuffer> rrsets = new ArrayList<ByteBuffer>();
			
			int totalLen = 0;
			for (Rrset rrset : request.getRrsets()) {
				List<ByteBuffer> rdatas = new ArrayList<ByteBuffer>();
				int rdataLen = 0;
				for (ResourceRecord record : rrset.getRecords()) {
					Rdata rdata = record.getRdata();
				
					int rdlen = ByteUtil.toUnsigned(rdata.getRdlen());
					ByteBuffer bufferRdata = ByteBuffer.allocate(2 + rdlen);
					bufferRdata.putShort(rdata.getRdlen());
					bufferRdata.put(rdata.getData());
					
					rdatas.add(bufferRdata);
					rdataLen += bufferRdata.limit();
				}
				
				int ownernameLen = rrset.getOwnername().length();
				int rrsetLen = 4 + ownernameLen + 2 + 2 + 4 + 4 + rdataLen;
				ByteBuffer rrsetBuffer = ByteBuffer.allocate(4 + rrsetLen);
				totalLen += 4 + rrsetLen;
				rrsetBuffer.putInt(rrsetLen);
				rrsetBuffer.putInt(rrset.getOwnername().length());
				rrsetBuffer.put(rrset.getOwnername().getBytes());
				rrsetBuffer.putShort(rrset.getType().getValue());
				rrsetBuffer.putInt(rrset.getRecords().size());
				rrsetBuffer.putShort(rrset.getDnsClass().getValue());
				rrsetBuffer.putInt(rrset.getTtl());

				for (ByteBuffer buffer : rdatas) {
					rrsetBuffer.put(buffer.array());
				}
				rdatas.clear();

				rrsets.add(rrsetBuffer);
			}
			
			ByteBuffer messageBuffer =
				ByteBuffer.allocate(headerBuffer.limit() + totalLen);
			messageBuffer.put(headerBuffer.array());
			for (ByteBuffer buffer : rrsets) {
				messageBuffer.put(buffer.array());
			}
			
			messageBuffer.flip();
			getOutput().write(messageBuffer, session);
		}
	}

	@Override
	public void encodeErrorMessage(ErrorMessage message, IoSession session)
			throws IOException {
		// o cliente nao precisa tratar nesse pto
	}	
}
