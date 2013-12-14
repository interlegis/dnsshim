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

import org.apache.commons.codec.binary.Base64;

import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DomainNameUtil;
import br.registro.dnsshim.xfrd.dns.protocol.ResponseCode;

/**
 * 
 * <pre>
 * TYPE TSIG (250: Transaction SIGnature)
 * CLASS ANY
 * TTL  0
 * RdLen (variable)
 * RDATA 
 * 	Field Name       Data Type      Notes
 * 	--------------------------------------------------------------
 * 	Algorithm Name   domain-name    Name of the algorithm
 * 	                                in domain name syntax.
 * 	Time Signed      u_int48_t      seconds since 1-Jan-70 UTC.
 * 	Fudge            u_int16_t      seconds of error permitted
 * 	                                in Time Signed.
 * 	MAC Size         u_int16_t      number of octets in MAC.
 * 	MAC              octet stream   defined by Algorithm Name.
 * 	Original ID      u_int16_t      original message IDamanha
 * 	Error            u_int16_t      expanded RCODE covering
 * 	                                TSIG processing.
 * 	Other Len        u_int16_t      length, in octets, of
 * 	                                Other Data.
 * 	Other Data       octet stream   empty unless Error == BADTIME
 * </pre>
 */
public class Tsig extends ResourceRecord {
	private final String algorithmName;
	private final long timeSigned;
	private final short fudge;

	private final byte[] mac;
	private final short originalId;

	private final ResponseCode error;
	private final byte[] otherData;

	private static final int TSIG_TTL = 0;

	public Tsig(String ownername, Rdata rdata) {
		super(ownername, RrType.TSIG, DnsClass.ANY, TSIG_TTL, rdata);

		ByteBuffer buffer = ByteBuffer.wrap(rdata.getData());
		algorithmName = DomainNameUtil.toPresentationFormat(buffer);
		
		byte[] timeSig = new byte[6];
		buffer.get(timeSig);
		byte[] aux = new byte[8];
		System.arraycopy(timeSig, 0, aux, 2, 6);

		timeSigned = ByteUtil.toLong(aux);
		fudge = buffer.getShort();
		
		mac = new byte[ByteUtil.toUnsigned(buffer.getShort())];
		buffer.get(mac);
		originalId = buffer.getShort();
		
		error = ResponseCode.fromValue((byte) buffer.getShort());
		
		int usignedOtherLen = ByteUtil.toUnsigned(buffer.getShort());
		otherData = new byte[usignedOtherLen];
		if (usignedOtherLen > 0) {
			buffer.get(otherData);
		}

		this.rdata = new Rdata(rdata);
	}

	public Tsig(String ownername, DnsClass dnsClass, int ttl, String rdataStr) {
		super(ownername, RrType.TSIG, DnsClass.ANY, TSIG_TTL);

		if (dnsClass != DnsClass.ANY || ttl != TSIG_TTL) {
			throw new IllegalArgumentException();
		}

		Scanner scanner = new Scanner(rdataStr);
		scanner.useDelimiter("\\s+");
		
		algorithmName = scanner.next();
		timeSigned = Integer.parseInt(scanner.next());
		fudge = (short) Integer.parseInt(scanner.next());
		
		Base64 base64 = new Base64();
		mac = base64.decode(scanner.next().getBytes());
		
		originalId = (short) Integer.parseInt(scanner.next());
		error = ResponseCode.valueOf(scanner.next());
		
		if (scanner.hasNext()) {
			otherData = base64.decode(scanner.next().getBytes());
		} else {
			otherData = new byte[0];
		}
		
		this.rdata = RdataTsigBuilder.get(algorithmName, timeSigned, fudge, mac, originalId, error, otherData);
		scanner.close();
	}

	public Tsig(String ownername, String algorithmName, long timeSigned,
			short fudge, byte[] mac, short originalId, ResponseCode error,
			byte[] otherData) {
		super(ownername, RrType.TSIG, DnsClass.ANY, TSIG_TTL);
		
		if (mac.length <= 0) {
			throw new IllegalArgumentException();
		}

		this.algorithmName = algorithmName;
		this.timeSigned = timeSigned;
		this.fudge = fudge;

		this.mac = new byte[mac.length];
		System.arraycopy(mac, 0, this.mac, 0, mac.length);
		this.originalId = originalId;

		this.error = error;
		
		this.otherData = new byte[otherData.length];
		System.arraycopy(otherData, 0, this.otherData, 0, otherData.length);

		this.rdata = RdataTsigBuilder.get(algorithmName, timeSigned, fudge, mac,
				originalId, error, otherData);
	}

	public Tsig(String ownername, ByteBuffer buffer) {
		super(ownername, RrType.TSIG, DnsClass.ANY, TSIG_TTL);
		
		buffer.getShort(); // rdLen
		algorithmName = DomainNameUtil.toPresentationFormat(buffer);
		
		byte[] timeSig = new byte[6];
		buffer.get(timeSig);
		byte[] aux = new byte[8];
		System.arraycopy(timeSig, 0, aux, 2, 6);

		timeSigned = ByteUtil.toLong(aux);
		fudge = buffer.getShort();
		 
		int macSize = ByteUtil.toUnsigned(buffer.getShort());
		mac = new byte[macSize];
		buffer.get(mac);
		originalId = buffer.getShort();
		
		error = ResponseCode.fromValue((byte) buffer.getShort());
		
		int otherLen = ByteUtil.toUnsigned(buffer.getShort());
		otherData = new byte[otherLen];
		if (otherLen > 0) {
			buffer.get(otherData);
		}

		this.rdata = RdataTsigBuilder.get(algorithmName, timeSigned, fudge, mac,
				originalId, error, otherData);
	}

	public static Tsig parseTsig(String ownername, DnsClass dnsClass, int ttl,
			ByteBuffer buffer) {
		if (dnsClass != DnsClass.ANY || ttl != TSIG_TTL) {
			throw new IllegalArgumentException();
		}

		return new Tsig(ownername, buffer);
	}

	public String getAlgorithmName() {
		return algorithmName;
	}

	public long getTimeSigned() {
		return timeSigned;
	}
	
	public short getFudge() {
		return fudge;
	}

	public short getMacSize() {
		return (short) this.mac.length;
	}

	public byte[] getMac() {
		return mac;
	}

	public short getOriginalId() {
		return originalId;
	}

	public ResponseCode getError() {
		return error;
	}

	public short getOtherLen() {
		return (short) this.otherData.length;
	}

	public byte[] getOtherData() {
		return otherData;
	}

	@Override
	public String rdataPresentation() {
		StringBuffer rdata = new StringBuffer();
		rdata.append(algorithmName).append(SEPARATOR);
		rdata.append(timeSigned).append(SEPARATOR);
		rdata.append(ByteUtil.toUnsigned(fudge)).append(SEPARATOR);
		
		Base64 base64 = new Base64();
		rdata.append(new String(base64.encode(mac))).append(SEPARATOR);		
		rdata.append(ByteUtil.toUnsigned(originalId)).append(SEPARATOR);		
		rdata.append(error).append(SEPARATOR);
		
		if (otherData.length > 0) {
			rdata.append(new String(base64.encode(otherData))).append(SEPARATOR);
		}

		return rdata.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Tsig) {
			Tsig t = (Tsig) obj;
			return Arrays.equals(mac, t.mac);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(this.mac);
	}

}
