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
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import br.registro.dnsshim.common.server.AbstractProtocolDecoder;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.IoSession;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.DnskeyAlgorithm;
import br.registro.dnsshim.domain.DnskeyFlags;
import br.registro.dnsshim.domain.DnskeyProtocol;
import br.registro.dnsshim.domain.DnskeyType;
import br.registro.dnsshim.domain.GenericResourceRecord;
import br.registro.dnsshim.domain.Rdata;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.RrType;
import br.registro.dnsshim.domain.Rrset;
import br.registro.dnsshim.signer.domain.SignData;
import br.registro.dnsshim.util.ByteUtil;

public class Decoder extends AbstractProtocolDecoder {
	@Override
	public void decode(IoSession session) throws IOException, DnsshimProtocolException {
		byte operation = (byte) session.read();

		OperationType operationType = OperationType.fromValue(operation);
		
		if (operationType == OperationType.GENERATE_KEY) {
			ByteBuffer buffer = ByteBuffer.allocate(2);
			readSession(session, buffer);
			short keyTypeLen = buffer.getShort();
			byte[] keyTypeBytes = new byte[keyTypeLen];
			
			buffer = ByteBuffer.allocate(keyTypeLen);
			readSession(session, buffer);
			buffer.get(keyTypeBytes);
			String keyType = new String(keyTypeBytes);

			buffer = ByteBuffer.allocate(8);
			readSession(session, buffer);
			short flags = buffer.getShort();
			byte algorithm = buffer.get();
			byte protocol = buffer.get();
			int keySize = buffer.getInt();
			
			GenerateKeyRequest request = new GenerateKeyRequest();
			request.setKeyType(DnskeyType.valueOf(keyType));
			request.setFlags(DnskeyFlags.fromValue(flags));
			request.setAlgorithm(DnskeyAlgorithm.fromValue(algorithm));
			request.setDnskeyProtocol(DnskeyProtocol.fromValue(protocol));
			request.setKeySize(keySize);
			getOutput().write(request, session);
			
		} else if (operationType == OperationType.IMPORT_KEY) {
			ByteBuffer buffer = ByteBuffer.allocate(2);
			readSession(session, buffer);
			short keyTypeLen = buffer.getShort();
			byte[] keyTypeBytes = new byte[keyTypeLen];
			
			buffer = ByteBuffer.allocate(keyTypeLen);
			readSession(session, buffer);
			buffer.get(keyTypeBytes);
			String keyType = new String(keyTypeBytes);

			buffer = ByteBuffer.allocate(1);
			readSession(session, buffer);
			byte algorithm = buffer.get();
			
			buffer = ByteBuffer.allocate(2);
			readSession(session, buffer);
			short flags = buffer.getShort();
			
			buffer = ByteBuffer.allocate(4);
			readSession(session, buffer);
			int modulusLen = buffer.getInt();
			buffer = ByteBuffer.allocate(modulusLen);
			readSession(session, buffer);
			byte[] modulus = new byte[modulusLen];
			buffer.get(modulus);
			
			buffer = ByteBuffer.allocate(4);
			readSession(session, buffer);
			int publicExpLen = buffer.getInt();
			buffer = ByteBuffer.allocate(publicExpLen);
			readSession(session, buffer);
			byte[] publicExponent = new byte[publicExpLen];
			buffer.get(publicExponent);
			
			buffer = ByteBuffer.allocate(4);
			readSession(session, buffer);
			int privateExpLen = buffer.getInt();			
			buffer = ByteBuffer.allocate(privateExpLen);
			readSession(session, buffer);
			byte[] privateExponent = new byte[privateExpLen];
			buffer.get(privateExponent);
		
			buffer = ByteBuffer.allocate(4);
			readSession(session, buffer);
			int prime1Len = buffer.getInt();			
			buffer = ByteBuffer.allocate(prime1Len);
			readSession(session, buffer);
			byte[] prime1 = new byte[prime1Len];
			buffer.get(prime1);
		
			buffer = ByteBuffer.allocate(4);
			readSession(session, buffer);
			int prime2Len = buffer.getInt();
			buffer = ByteBuffer.allocate(prime2Len);
			readSession(session, buffer);
			byte[] prime2 = new byte[prime2Len];
			buffer.get(prime2);
			
			buffer = ByteBuffer.allocate(4);
			readSession(session, buffer);
			int exponent1Len = buffer.getInt();
			buffer = ByteBuffer.allocate(exponent1Len);
			readSession(session, buffer);
			byte[] exponent1 = new byte[exponent1Len];
			buffer.get(exponent1);
			
			buffer = ByteBuffer.allocate(4);
			readSession(session, buffer);
			int exponent2Len = buffer.getInt();
			buffer = ByteBuffer.allocate(exponent2Len);
			readSession(session, buffer);
			byte[] exponent2 = new byte[exponent2Len];
			buffer.get(exponent2);
			
			buffer = ByteBuffer.allocate(4);
			readSession(session, buffer);
			int coefficientLen = buffer.getInt();
			buffer = ByteBuffer.allocate(coefficientLen);
			readSession(session, buffer);
			byte[] coefficient = new byte[coefficientLen];
			buffer.get(coefficient);
			
			ImportKeyRequest request = new ImportKeyRequest();
			request.setKeyType(DnskeyType.valueOf(keyType));
			request.setAlgorithm(DnskeyAlgorithm.fromValue(algorithm));
			request.setFlags(DnskeyFlags.fromValue(flags));
			request.setModulus(new BigInteger(modulus));
			request.setPublicExponent(new BigInteger(publicExponent));
			request.setPrivateExponent(new BigInteger(privateExponent));
			request.setPrime1(new BigInteger(prime1));
			request.setPrime2(new BigInteger(prime2));
			request.setExponent1(new BigInteger(exponent1));
			request.setExponent2(new BigInteger(exponent2));
			request.setCoefficient(new BigInteger(coefficient));
			getOutput().write(request, session);
		
		} else if (operationType == OperationType.REMOVE_KEY) {
			ByteBuffer buffer = ByteBuffer.allocate(2);
			readSession(session, buffer);
			short keyNameLen = buffer.getShort();
			buffer = ByteBuffer.allocate(keyNameLen);
			readSession(session, buffer);
			String keyName = new String(buffer.array());
			RemoveKeyRequest request = new RemoveKeyRequest();
			request.setKeyName(keyName);
			getOutput().write(request, session);

		} else if (operationType == OperationType.SIGNING) {
			ByteBuffer buffer = ByteBuffer.allocate(4);
			readSession(session, buffer);
			int zoneNameLength = buffer.getInt();
			buffer = ByteBuffer.allocate(zoneNameLength + 4 + 4 + 4 + 4 + 4);
			readSession(session, buffer);

			byte[] zoneName = new byte[zoneNameLength];
			buffer.get(zoneName);

			int ttlZone = buffer.getInt();
			int ttlDnskey = buffer.getInt();
			int soaMinimum = buffer.getInt();
			int inception = buffer.getInt();
			int expiration = buffer.getInt();

			buffer = ByteBuffer.allocate(4);
			readSession(session, buffer);

			int dnskeyCount = buffer.getInt();
			List<String> keys = new ArrayList<String>();
			for (int i = 0; i < dnskeyCount; i++) {
				buffer = ByteBuffer.allocate(2);
				readSession(session, buffer);
				short keyNameLen = buffer.getShort();
				buffer = ByteBuffer.allocate(keyNameLen);
				readSession(session, buffer);

				byte[] keyNameBytes = new byte[keyNameLen];
				buffer.get(keyNameBytes);
				String keyName = new String(keyNameBytes);
				keys.add(keyName);
			}

			buffer = ByteBuffer.allocate(4);
			readSession(session, buffer);
			int rrsetCount = buffer.getInt();

			SignRequest request = new SignRequest();
			Set<Rrset> rrsets = new LinkedHashSet<Rrset>();
			for (int i = 0; i < rrsetCount; i++) {
				buffer = ByteBuffer.allocate(4);
				readSession(session, buffer);
				int rrsetSize = buffer.getInt();
				buffer = ByteBuffer.allocate(rrsetSize);
				readSession(session, buffer);
				int ownernameLength = buffer.getInt();
				byte[] ownernameBytes = new byte[ownernameLength];
				buffer.get(ownernameBytes);
				String ownername = new String(ownernameBytes);
				
				RrType rrType = RrType.fromValue(buffer.getShort());
				int rdataCount = buffer.getInt();
				DnsClass dnsClass = DnsClass.fromValue(buffer.getShort());
				int rrsetTtl = buffer.getInt();
				Rrset rrset = new Rrset(ownername, rrType, dnsClass);
				
				for (int x = 0; x < rdataCount; x++) {
					int rdataLength = ByteUtil.toUnsigned(buffer.getShort());
					byte[] data = new byte[rdataLength];
					buffer.get(data);

					Rdata rdata = new Rdata(data);
					
					// Here in the signer, we don't care about the type of the RR
					ResourceRecord record =	new GenericResourceRecord(ownername,
							rrType, dnsClass, rrsetTtl, rdata);
					rrset.add(record);
				}
				
				SignRequestHeader header = new SignRequestHeader();
				
				SignData signData = new SignData();
				signData.setZonename(new String(zoneName));
				signData.setTtlZone(ttlZone);
				signData.setTtlDnskey(ttlDnskey);
				signData.setSoaMinimum(soaMinimum);
				signData.setInception(inception);
				signData.setExpiration(expiration);
				header.setSignData(signData);
				
				header.setKeys(keys);
				
				request.setSignRequestHeader(header);
				rrsets.add(rrset);
			}
			request.setRrsets(rrsets);
			getOutput().write(request, session);
		} else {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_SIGNER_OPERATION);
		}
	}
}