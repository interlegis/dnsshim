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
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.util.ByteUtil;

public class Sshfp extends ResourceRecord {
	private final FingerprintAlgorithm algorithm;
	private final FingerprintType fpType;
	private final byte[] fingerprint;

	public Sshfp(String ownername, DnsClass dnsClass, int ttl, 
			FingerprintAlgorithm algorithm, FingerprintType fpType,
			byte[] fingerprint) throws NoSuchAlgorithmException {
		super(ownername, RrType.SSHFP, dnsClass, ttl);
		this.algorithm = algorithm;
		this.fpType = fpType;
		this.fingerprint = fingerprint;
		this.rdata = RdataSshfpBuilder.get(algorithm, fpType, fingerprint);
	}

	public Sshfp(String ownername, DnsClass dnsClass, int ttl, String rdata) throws DnsshimProtocolException {
		super(ownername, RrType.SSHFP, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");
		short algorithmShort = Short.parseShort(scanner.next());
		this.algorithm = FingerprintAlgorithm.fromValue((byte) algorithmShort);
		short fpTypeShort = Short.parseShort(scanner.next());
		this.fpType = FingerprintType.fromValue((byte) fpTypeShort);
		String fingerprintStr = scanner.next();
		this.fingerprint = fingerprintStr.getBytes();
		try {
			this.rdata = RdataSshfpBuilder.get(algorithm, fpType, fingerprint);
		} catch (NoSuchAlgorithmException e) {
			scanner.close();
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, e.getMessage());
		}
		scanner.close();
	}

	public FingerprintAlgorithm getAlgorithm() {
		return algorithm;
	}

	public FingerprintType getFpType() {
		return fpType;
	}

	public byte[] getFingerprint() {
		return fingerprint;
	}

	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		rdata.append(this.algorithm).append(SEPARATOR);
		rdata.append(this.fpType).append(SEPARATOR);
		rdata.append(new String(this.fingerprint));

		return rdata.toString();
	}

	public static Sshfp parseSshfp(String ownername,
			DnsClass dnsClass, int ttl, ByteBuffer buffer) throws DnsshimProtocolException {
		int rdlength = ByteUtil.toUnsigned(buffer.getShort());
		byte algorithmByte = buffer.get();
		FingerprintAlgorithm algorithm = FingerprintAlgorithm.fromValue(algorithmByte);
		byte fpTypeByte = buffer.get();
		FingerprintType fpType = FingerprintType.fromValue(fpTypeByte);
		int fingerprintLen = rdlength - 2;
		byte[] fingerprint = new byte[fingerprintLen];
		buffer.get(fingerprint);
		
		try {
			return new Sshfp(ownername, dnsClass, ttl, algorithm, fpType, fingerprint);
		} catch (NoSuchAlgorithmException e) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, e.getMessage());
		}
	}

}
