package br.registro.dnsshim.domain;

public enum TlsaUsage {
	CA_CONSTRAINT((byte) 0),
	SERVICE_CERT_CONSTRAINT((byte) 1),
	TRUST_ANCHOR_ASSERTION((byte) 2),
	DOMAIN_ISSUED_CERT((byte) 3);
	
	private byte value;

	private TlsaUsage(byte value) {
		this.value = value;
	}
	
	public byte getValue() {
		return value;
	}

	@Override
	public String toString() {
		return name();
	}
	
	public static TlsaUsage fromValue(byte value) throws IllegalArgumentException {
		TlsaUsage[] values = TlsaUsage.values();
		for (TlsaUsage type : values) {
			if (type.getValue() == value) {
				return type;
			}
		}

		throw new IllegalArgumentException("Invalido "+value);

	}
	
}
