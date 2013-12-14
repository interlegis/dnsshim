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

public class Cert extends ResourceRecord {
	private final CertType certType;
	private final short keyTag;
	private final DnskeyAlgorithm algorithm;
	private final byte[] certificate;

	public Cert(String ownername, DnsClass dnsClass, int ttl,
			CertType certType, short keyTag, DnskeyAlgorithm algorithm, byte[] certificate) {
		super(ownername, RrType.CERT, dnsClass, ttl);
		this.certType = certType;
		this.keyTag = keyTag;
		this.algorithm = algorithm;
		this.certificate = certificate;
		this.rdata = RdataCertBuilder.get(certType, keyTag, algorithm, certificate);

	}

	public Cert(String ownername, DnsClass dnsClass, int ttl, String rdata) {
		super(ownername, RrType.CERT, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");
		
		String certTypeStr = scanner.next();
		CertType certType;
		try {
			int certTypeShort = Integer.parseInt(certTypeStr);
			certType = CertType.fromValue((short) certTypeShort);
		} catch (NumberFormatException nfe) {
			certType = CertType.valueOf(certTypeStr);
		}
		this.certType = certType;
		
		this.keyTag = Short.parseShort(scanner.next());
		
		String algorithmStr = scanner.next();
		DnskeyAlgorithm algorithm;
		try {
			short algorithmShort = Short.parseShort(algorithmStr);
			algorithm = DnskeyAlgorithm.fromValue((byte) algorithmShort);
		} catch (NumberFormatException nfe) {
			algorithm = DnskeyAlgorithm.valueOf(algorithmStr);
		}
		this.algorithm = algorithm;

		scanner.useDelimiter(System.getProperty("line.separator"));
		Base64 base64 = new Base64();
		String certificateStr = scanner.next();
		this.certificate = base64.decode(certificateStr.getBytes());
		this.rdata = RdataCertBuilder.get(certType, keyTag, algorithm, certificate);
		scanner.close();
	}

	public CertType getCertType() {
		return certType;
	}

	public short getKeyTag() {
		return keyTag;
	}

	public DnskeyAlgorithm getAlgorithm() {
		return algorithm;
	}

	public byte[] getCertificate() {
		return certificate;
	}
	
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		rdata.append(certType).append(SEPARATOR);
		rdata.append(keyTag).append(SEPARATOR);
		rdata.append(algorithm.getValue()).append(SEPARATOR);
		Base64 b64 = new Base64();
		rdata.append(new String(b64.encode(this.certificate)));
		return rdata.toString();
	}
	
	public static Cert parseCert(String ownername,
			DnsClass dnsClass, int ttl, ByteBuffer buffer) {
		int rdlength = ByteUtil.toUnsigned(buffer.getShort());
		short certTypeShort = buffer.getShort();
		CertType certType = CertType.fromValue(certTypeShort);
		short keyTag = buffer.getShort();
		byte algorithmByte = buffer.get();
		DnskeyAlgorithm algorithm = DnskeyAlgorithm.fromValue(algorithmByte);
		int certificateLen = rdlength - 5;
		byte[] certificate = new byte[certificateLen];
		buffer.get(certificate);
		return new Cert(ownername, dnsClass, ttl, certType, keyTag, algorithm, certificate);
	}
}
