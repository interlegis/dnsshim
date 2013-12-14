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

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.util.ByteUtil;

public class Ds extends ResourceRecord {
	private final short keyTag;
	private final DnskeyAlgorithm algorithm;
	private final DsDigestType digestType;
	private final byte[] digest;
	private final String digestHex;

	public Ds(String ownername, DnsClass dnsClass, int ttl,
			short keyTag, DnskeyAlgorithm algorithm,
			DsDigestType digestType, byte[] digest) {
		super(ownername, RrType.DS, dnsClass, ttl);
		this.keyTag = keyTag;
		this.algorithm = algorithm;
		this.digestType = digestType;
		this.digest = digest;
		this.digestHex = new String(Hex.encodeHex(this.digest));
		this.rdata = RdataDsBuilder.get(keyTag, algorithm, digestType, digest);
	}
	
	public Ds(String ownername, DnsClass dnsClass, int ttl,
			short keyTag, DnskeyAlgorithm algorithm,
			DsDigestType digestType, String digestHex) 
		throws DnsshimProtocolException {
		super(ownername, RrType.DS, dnsClass, ttl);
		this.keyTag = keyTag;
		this.algorithm = algorithm;
		this.digestType = digestType;
		this.digestHex = digestHex;
		try {
			this.digest = Hex.decodeHex(this.digestHex.toCharArray());
		} catch (DecoderException de) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Error parsing DS record");
		}
		this.rdata = RdataDsBuilder.get(keyTag, algorithm, digestType, digest);
	}

	public Ds(String ownername, DnsClass dnsClass, int ttl, String rdata)
		throws DnsshimProtocolException {
		super(ownername, RrType.DS, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");
		
		this.keyTag = (short) Integer.parseInt(scanner.next());
		short algorithmShort = Short.parseShort(scanner.next());
		this.algorithm = DnskeyAlgorithm.fromValue((byte) algorithmShort);
		short digestTypeShort = Short.parseShort(scanner.next());
		this.digestType = DsDigestType.fromValue((byte) digestTypeShort);
		this.digestHex = scanner.next();
		try {
			this.digest = Hex.decodeHex(this.digestHex.toCharArray());
		} catch (DecoderException de) {
			scanner.close();
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Error parsing DS record");
		}
		this.rdata = RdataDsBuilder.get(keyTag, algorithm, digestType, digest);
		
		scanner.close();
	}

	public short getKeyTag() {
		return keyTag;
	}

	public DnskeyAlgorithm getAlgorithm() {
		return algorithm;
	}

	public DsDigestType getDigestType() {
		return digestType;
	}

	public byte[] getDigest() {
		return digest;
	}
	
	public String getDigestHex() {
		return digestHex;
	}

	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		rdata.append(ByteUtil.toUnsigned(keyTag)).append(SEPARATOR);
		rdata.append(algorithm.getValue()).append(SEPARATOR);
		rdata.append(digestType.getValue()).append(SEPARATOR);
		rdata.append(digestHex.toUpperCase());
	
		return rdata.toString();
	}

	public static Ds parseDs(String ownername, DnsClass dnsClass, int ttl,
			ByteBuffer buffer) {
		int rdlength = ByteUtil.toUnsigned(buffer.getShort());
		short keyTag = buffer.getShort();
		DnskeyAlgorithm algorithm = DnskeyAlgorithm.fromValue(buffer.get());
		DsDigestType digestType = DsDigestType.fromValue(buffer.get());
		int digestLen = rdlength - 4;
		byte[] digest = new byte[digestLen];
		buffer.get(digest);
		return new Ds(ownername, dnsClass, ttl, keyTag, algorithm, digestType,
				digest);
	}
}
