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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.TimeZone;

import org.apache.commons.codec.binary.Base64;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DateUtil;
import br.registro.dnsshim.util.DomainNameUtil;

public class Rrsig extends ResourceRecord {

	
	final private RrType typeCovered;
	final private DnskeyAlgorithm algorithm;
	final private byte labels;
	final private int originalTtl;
	final private Date expiration;
	final private Date inception;
	final private short keyTag;
	final private String signerName;
	final private byte[] signature;

	public Rrsig(String ownername, DnsClass dnsClass, int ttl,
			RrType typeCovered, DnskeyAlgorithm algorithm, byte labels,
			int originalTtl, int expiration, int inception, short keyTag,
			String signerName, byte[] signature) {
		super(ownername, RrType.RRSIG, dnsClass, ttl);
		this.typeCovered = typeCovered;
		this.algorithm = algorithm;
		this.labels = labels;
		this.originalTtl = originalTtl;
		this.expiration = new Date(DateUtil.putMillisFromEpoch(expiration));
		this.inception = new Date(DateUtil.putMillisFromEpoch(inception));
		this.keyTag = keyTag;
		this.signerName = signerName.toLowerCase();
		this.signature = signature;
		this.rdata = RdataRrsigBuilder.get(signature, typeCovered,
				labels, originalTtl, expiration, inception,
				keyTag, signerName, algorithm);
	}

	public Rrsig(String ownername, DnsClass dnsClass, int ttl, String rdata)
		throws DnsshimProtocolException {
		super(ownername, RrType.RRSIG, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");
		
		String typeCoveredStr = scanner.next();
		this.typeCovered = RrType.valueOf(typeCoveredStr);
		short algorithmShort = Short.parseShort(scanner.next());
		this.algorithm = DnskeyAlgorithm.fromValue((byte) algorithmShort);
		this.labels = (byte) Short.parseShort(scanner.next());
		this.originalTtl = (int)Long.parseLong(scanner.next());
		String expirationStr = scanner.next();
		String inceptionStr = scanner.next();
		this.keyTag = (short) Integer.parseInt(scanner.next());
		this.signerName = scanner.next().toLowerCase();
		
		String signatureStr = scanner.next();		
		Base64 base64 = new Base64();
		this.signature = base64.decode(signatureStr.getBytes());
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));		
		try {
			this.inception  = dateFormat.parse(inceptionStr);
			this.expiration = dateFormat.parse(expirationStr);
		} catch (ParseException pe) {
			scanner.close();
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Error loading RRSIG");
		}
		
		int inceptionInt =
			(int) DateUtil.removeMillisFromEpoch(inception.getTime());
		int expirationInt =
			(int) DateUtil.removeMillisFromEpoch(expiration.getTime());
		this.rdata = RdataRrsigBuilder.get(signature, typeCovered,
				labels, originalTtl, expirationInt, inceptionInt,
				keyTag, signerName, algorithm);
		scanner.close();
	}

	public RrType getTypeCovered() {
		return typeCovered;
	}

	public DnskeyAlgorithm getDnskeyAlgorithmType() {
		return algorithm;
	}

	public byte getLabels() {
		return labels;
	}

	public int getOriginalTtl() {
		return originalTtl;
	}

	public Date getExpiration() {
		return expiration;
	}

	public Date getInception() {
		return inception;
	}

	public short getKeyTag() {
		return keyTag;
	}

	public String getSignerName() {
		return signerName;
	}

	public byte[] getSignature() {
		return signature;
	}

	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		rdata.append(getTypeCovered()).append(SEPARATOR);
		rdata.append(algorithm.getValue()).append(SEPARATOR);
		rdata.append(labels).append(SEPARATOR);
		rdata.append(originalTtl).append(SEPARATOR);

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		rdata.append(dateFormat.format(expiration));
		rdata.append(SEPARATOR);

		rdata.append(dateFormat.format(inception));
		rdata.append(SEPARATOR);

		rdata.append(ByteUtil.toUnsigned(keyTag)).append(SEPARATOR);
		rdata.append(signerName).append(SEPARATOR);
		Base64 base64 = new Base64();
		rdata.append(new String(base64.encode(signature)));

		return rdata.toString();
	}

	public static Rrsig parseRrsig(String ownername, DnsClass dnsClass, int ttl,
			ByteBuffer buffer) {
		int rdlength = ByteUtil.toUnsigned(buffer.getShort());
		short typeCoveredShort = buffer.getShort();
		RrType typeCovered = RrType.fromValue(typeCoveredShort);
		DnskeyAlgorithm algorithm = DnskeyAlgorithm.fromValue(buffer.get());
		byte labels = buffer.get();
		int originalTtl = buffer.getInt();
		int expiration = buffer.getInt();
		int inception = buffer.getInt();
		short keyTag = buffer.getShort();
		int pos1 = buffer.position();
		String signerName = DomainNameUtil.toPresentationFormat(buffer);
		int pos2 = buffer.position();
		int signerNameLen = pos2 - pos1;
		int signatureLen = rdlength - 18 - signerNameLen;
		byte[] signature = new byte[signatureLen];
		buffer.get(signature);
		return new Rrsig(ownername, dnsClass, ttl, typeCovered, algorithm,
				labels, originalTtl, expiration, inception, keyTag, signerName,
				signature);
	}
}
