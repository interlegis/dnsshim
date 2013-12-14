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
import java.util.EnumSet;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import br.registro.dnsshim.util.Base32;
import br.registro.dnsshim.util.ByteUtil;

public class Nsec3 extends ResourceRecord {
	private final DsDigestType hashAlgorithm;
	private final byte flags;
	private final short iterations;
	private final byte[] salt;
	private final String nextHashedOwnerName;
	private final Set<RrType> types;

	public Nsec3(String ownername, DnsClass dnsClass, int ttl,
			DsDigestType hashAlgorithm, byte flags, short iterations,
			byte[] salt, String nextHashedOwnerName,
			Set<RrType> types) {
		super(ownername, RrType.NSEC3, dnsClass, ttl);
		this.hashAlgorithm = hashAlgorithm;
		this.flags = flags;
		this.iterations = iterations;
		this.salt = salt;
		this.nextHashedOwnerName = nextHashedOwnerName.toLowerCase();
		this.types = types;
		this.rdata = RdataNsec3Builder.get(hashAlgorithm, flags, iterations,
				salt, this.nextHashedOwnerName, types);
	}

	public Nsec3(String ownername, DnsClass dnsClass, int ttl, String rdata) {
		super(ownername, RrType.NSEC3, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");

		/*
		 * Example NSEC3:
		 * b4um86eghhds6nea196smvmlo4ors995.example. NSEC3 1 1 12 aabbccdd ( gjeqe526plbf1g8mklp59enfd789njgi MX RRSIG )
		 */
		types = EnumSet.noneOf(RrType.class);

		byte hashAlgorithmByte = Byte.parseByte(scanner.next());
		this.hashAlgorithm = DsDigestType.fromValue(hashAlgorithmByte);

		this.flags = Byte.parseByte(scanner.next());
		this.iterations = Short.parseShort(scanner.next());
		this.salt = scanner.next().getBytes();
		String next = scanner.next();
		if (next.equals("(")) {
			this.nextHashedOwnerName = scanner.next().toLowerCase();
		} else {
			this.nextHashedOwnerName = next.toLowerCase();
		}
		while (scanner.hasNext()) {
			String typeStr = scanner.next();
			types.add(RrType.valueOf(typeStr));
		}
		this.rdata = RdataNsec3Builder.get(hashAlgorithm, flags, iterations, salt, nextHashedOwnerName, types);
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

	public String getNextHashedOwnerName() {
		return nextHashedOwnerName;
	}

	public Set<RrType> getTypes() {
		return types;
	}

	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		rdata.append(getHashAlgorithm()).append(SEPARATOR);
		rdata.append(getFlags()).append(SEPARATOR);
		rdata.append(getIterations()).append(SEPARATOR);
		rdata.append(Arrays.toString(getSalt())).append(SEPARATOR);
		rdata.append(getNextHashedOwnerName()).append(SEPARATOR);
		for (RrType type : this.types) {
			rdata.append(type).append(SEPARATOR);
		}

		return rdata.toString();
	}

	public static Nsec3 parseNsec3(String ownername, DnsClass dnsClass, int ttl,
			ByteBuffer buffer) {
		int rdlength = ByteUtil.toUnsigned(buffer.getShort());
		int beginPos = buffer.position();
		byte hashAlgorithmByte = buffer.get();
		DsDigestType hashAlgorithm = DsDigestType.fromValue(hashAlgorithmByte);
		byte flags = buffer.get();
		short iterations = buffer.getShort();
		byte saltLength = buffer.get();
		byte[] salt = new byte[saltLength];
		buffer.get(salt);
		byte hashLength = buffer.get();
		byte[] nextHashedOwnername = new byte[hashLength];
		buffer.get(nextHashedOwnername);
		String nextOwnername = Base32.base32HexEncode(nextHashedOwnername);
		Set<RrType> types = new TreeSet<RrType>();
		while (buffer.position() - beginPos < rdlength) {
			short windowBlock = ByteUtil.toUnsigned(buffer.get());
			short bitmapLen = ByteUtil.toUnsigned(buffer.get());
			byte[] bitmap = new byte[bitmapLen];
			buffer.get(bitmap);
			for (int i = 0; i < bitmapLen; i++) {
				for (int j = 0; j < 8; j++) {
					if (ByteUtil.getBit(bitmap[i], j + 1)) {
						int typeValue = j + i*8 + windowBlock*256;
						types.add(RrType.fromValue((short) typeValue));
					}
				}
			}
		}

		return new Nsec3(ownername, dnsClass, ttl, hashAlgorithm, flags, iterations,
				salt, nextOwnername, types);
	}
}
