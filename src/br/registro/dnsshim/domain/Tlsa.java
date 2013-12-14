package br.registro.dnsshim.domain;

import java.nio.ByteBuffer;
import java.util.Scanner;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.util.ByteUtil;

public class Tlsa extends ResourceRecord {
	private final TlsaUsage usage;
	private final TlsaSelector selector;
	private final TlsaMatchingType matchingType;
	private final byte[] certificate;

	public Tlsa(String ownername,
			DnsClass dnsClass, int ttl, TlsaUsage usage, TlsaSelector selector, TlsaMatchingType matchingType, byte[] certificate) {
		super(ownername, RrType.TLSA, dnsClass, ttl);
		this.usage = usage;
		this.selector = selector;
		this.matchingType = matchingType;
		this.certificate = certificate;
		this.rdata = RdataTlsaBuilder.get(usage, selector, matchingType, certificate);
	}
	
	public Tlsa(String ownername, DnsClass dnsClass, int ttl, String rdata) throws DnsshimProtocolException {
		super(ownername, RrType.TLSA, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");
		
		String usage = scanner.next();
		byte s = Byte.parseByte(usage);
		this.usage = TlsaUsage.fromValue(s);
		
		String selector = scanner.next();
		s = Byte.parseByte(selector);
		this.selector = TlsaSelector.fromValue(s);
		
		String matching = scanner.next();
		s = Byte.parseByte(matching);
		this.matchingType = TlsaMatchingType.fromValue(s);

		//	The certificate association data field MUST be represented as a
		//	string of hexadecimal characters.  Whitespace is allowed within
		//	the string of hexadecimal characters, as described in [RFC1035].
		scanner.useDelimiter(System.getProperty("line.separator"));
		String cert = scanner.next();
		
		try {
			this.certificate = Hex.decodeHex(cert.trim().toCharArray());
		} catch (DecoderException e) {
			scanner.close();
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Error parsing TLSA record:"+cert);
		}
		
		this.rdata = RdataTlsaBuilder.get(this.usage, this.selector, this.matchingType, this.certificate);

		scanner.close();
	}

	public TlsaUsage getUsage() {
		return usage;
	}

	public TlsaSelector getSelector() {
		return selector;
	}

	public TlsaMatchingType getMatchingType() {
		return matchingType;
	}

	public byte[] getCertificate() {
		return certificate;
	}
	
	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		rdata.append(usage.getValue()).append(SEPARATOR);
		rdata.append(selector.getValue()).append(SEPARATOR);
		rdata.append(matchingType.getValue()).append(SEPARATOR);
		String certHex = new String(Hex.encodeHex(certificate));
		rdata.append(certHex).append(SEPARATOR);
	
		return rdata.toString();
	}


	public static Tlsa parseTlsa(String ownername, DnsClass dnsClass,
			int ttl, ByteBuffer buffer) {
		
		int rdlength = ByteUtil.toUnsigned(buffer.getShort());
		byte u = buffer.get();
		TlsaUsage usage = TlsaUsage.fromValue(u);
		
		byte s = buffer.get();
		TlsaSelector selector = TlsaSelector.fromValue(s);
		
		byte m = buffer.get();
		TlsaMatchingType matchingType = TlsaMatchingType.fromValue(m);
		
		int certificateLen = rdlength - 3;
		byte[] certificate = new byte[certificateLen];
		buffer.get(certificate);
		return new Tlsa(ownername, dnsClass, ttl, usage, selector, matchingType, certificate);
	}
}
