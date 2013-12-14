package br.registro.dnsshim.domain;

public class RdataTlsaBuilder implements RdataBuilder {

	public static Rdata get(TlsaUsage usage, TlsaSelector selector, TlsaMatchingType matchingType, byte[] cert) {
		int rdLength = 1 + 1 + 1 + cert.length; // usage + selector + matchingType + cert
		byte[] data = new byte[rdLength];
		int pos = 0;
		
		// usage
		data[pos] = usage.getValue();
		pos += 1;

		// selector
		data[pos] = selector.getValue();
		pos += 1;

		// matchingType
		data[pos] = matchingType.getValue();
		pos += 1;

		// cert
		System.arraycopy(cert, 0, data, pos, cert.length);

		return new Rdata(data);
	}
}
