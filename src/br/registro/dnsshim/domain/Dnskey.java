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

import java.nio.ByteBuffer;
import java.util.Scanner;

import org.apache.commons.codec.binary.Base64;

import br.registro.dnsshim.util.ByteUtil;

public class Dnskey extends ResourceRecord {
	final private DnskeyFlags flags;
	final private DnskeyProtocol protocol;
	final private DnskeyAlgorithm algorithm;
	final private byte[] publicKey;

	public Dnskey(String ownername, DnsClass dnsClass, int ttl,
			DnskeyFlags flags,
			DnskeyProtocol protocol,
			DnskeyAlgorithm algorithm,
			byte[] publicKey) {
		super(ownername, RrType.DNSKEY, dnsClass, ttl);
		this.flags = flags;
		this.protocol = protocol;
		this.algorithm = algorithm;
		this.publicKey = publicKey;
		this.rdata = RdataDnskeyBuilder.get(algorithm, flags, publicKey, false);
	}
	
	public Dnskey(String ownername, DnsClass dnsClass, int ttl, String rdata) {
		super(ownername, RrType.DNSKEY, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");
		
		int flagsInt = Integer.parseInt(scanner.next());
		this.flags = DnskeyFlags.fromValue((short) flagsInt);
		
		byte protocolByte = Byte.parseByte(scanner.next());
		this.protocol = DnskeyProtocol.fromValue(protocolByte);
	
		short algorithmShort = Short.parseShort(scanner.next());
		this.algorithm = DnskeyAlgorithm.fromValue((byte) algorithmShort);
		
		String keyDigestStr = scanner.next();
		Base64 b64 = new Base64();
		this.publicKey = b64.decode(keyDigestStr.getBytes());
		
		this.rdata = RdataDnskeyBuilder.get(algorithm, flags, publicKey, false);
		scanner.close();
	}
	
	public DnskeyFlags getFlags() {
		return flags;
	}

	public DnskeyProtocol getProtocol() {
		return protocol;
	}

	public DnskeyAlgorithm getAlgorithm() {
		return algorithm;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}
	
	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();

		Base64 b64 = new Base64();
		byte[] keyDigest = b64.encode(publicKey);
		rdata.append(flags.getFlags()).append(SEPARATOR);
		rdata.append(protocol.getValue()).append(SEPARATOR);
		rdata.append(algorithm.getValue()).append(SEPARATOR);
		rdata.append(new String(keyDigest)).append(SEPARATOR);
		return rdata.toString();
	}
	
	public static Dnskey parseDnskey(String ownername,
			DnsClass dnsClass, int ttl, ByteBuffer buffer) {
		int rdlength = ByteUtil.toUnsigned(buffer.getShort());
		short flagsShort = buffer.getShort();
		DnskeyFlags flags = DnskeyFlags.fromValue(flagsShort);
		DnskeyProtocol protocol = DnskeyProtocol.fromValue(buffer.get());
		byte algorithmByte = buffer.get();
		DnskeyAlgorithm algorithm = DnskeyAlgorithm.fromValue(algorithmByte);
		int publicKeyLen = rdlength - 4;
		byte[] publicKey = new byte[publicKeyLen];
		buffer.get(publicKey);
		return new Dnskey(ownername, dnsClass, ttl, flags, protocol, algorithm, publicKey);
	}
}