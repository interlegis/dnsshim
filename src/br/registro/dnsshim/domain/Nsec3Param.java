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
import java.util.Arrays;
import java.util.Scanner;


public class Nsec3Param extends ResourceRecord {
	private final DsDigestType hashAlgorithm;
	private final byte flags;
	private final short iterations;
	private final byte[] salt;

	public Nsec3Param(String ownername, DnsClass dnsClass, int ttl,
			DsDigestType hashAlgorithm, byte flags, short iterations,
			byte[] salt) {
		super(ownername, RrType.NSEC3PARAM, dnsClass, ttl);
		this.hashAlgorithm = hashAlgorithm;
		this.flags = flags;
		this.iterations = iterations;
		this.salt = salt;
		this.rdata = RdataNsec3ParamBuilder.get(hashAlgorithm, flags, iterations,
				salt);
	}

	public Nsec3Param(String ownername, DnsClass dnsClass, int ttl, String rdata) {
		super(ownername, RrType.NSEC3PARAM, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");

		byte hashAlgorithmByte = Byte.parseByte(scanner.next());
		this.hashAlgorithm = DsDigestType.fromValue(hashAlgorithmByte);

		this.flags = Byte.parseByte(scanner.next());
		this.iterations = Short.parseShort(scanner.next());
		this.salt = scanner.next().getBytes();
		this.rdata = RdataNsec3ParamBuilder.get(hashAlgorithm, flags, iterations, salt);
		scanner.close();
	}

	public DsDigestType getHashAlgorithm() {
		return hashAlgorithm;
	}

	public byte getFlags() {
		return flags;
	}

	public short getIterations() {
		return iterations;
	}

	public byte[] getSalt() {
		return salt;
	}

	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		rdata.append(getHashAlgorithm()).append(SEPARATOR);
		rdata.append(getFlags()).append(SEPARATOR);
		rdata.append(getIterations()).append(SEPARATOR);
		rdata.append(Arrays.toString(getSalt()));

		return rdata.toString();
	}

	public static Nsec3Param parseNsec3Param(String ownername, DnsClass dnsClass, int ttl,
			ByteBuffer buffer) {
		buffer.getShort();
		byte hashAlgorithmByte = buffer.get();
		DsDigestType hashAlgorithm = DsDigestType.fromValue(hashAlgorithmByte);
		byte flags = buffer.get();
		short iterations = buffer.getShort();
		byte saltLength = buffer.get();
		byte[] salt = new byte[saltLength];
		buffer.get(salt);

		return new Nsec3Param(ownername, dnsClass, ttl, hashAlgorithm, flags,
				iterations,	salt);
	}
}
