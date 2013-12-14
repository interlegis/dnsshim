package br.registro.dnsshim.domain;

public enum TlsaMatchingType {
	NO_HASH((byte) 0),
	SHA_256((byte) 1),
	SHA_512((byte) 2);
	
	private byte value;
	private TlsaMatchingType(byte value) {
		this.value = value;
	}
	
	public byte getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return name();
	}
	
	public static TlsaMatchingType fromValue(short value) throws IllegalArgumentException {
		TlsaMatchingType[] values = TlsaMatchingType.values();
		for (TlsaMatchingType type : values) {
			if (type.getValue() == value) {
				return type;
			}
		}

		throw new IllegalArgumentException("Invalido "+value);

	}

}
