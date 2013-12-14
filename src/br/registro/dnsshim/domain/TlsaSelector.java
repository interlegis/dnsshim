package br.registro.dnsshim.domain;

public enum TlsaSelector {
	FULL_CERT((byte) 0),
	SUBJECT_PUB_KEY_INFO((byte) 1);
	
	private byte value;
	
	private TlsaSelector(byte value) {
		this.value = value;
	}
	
	public byte getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return name();
	}
	
	public static TlsaSelector fromValue(byte value) throws IllegalArgumentException {
		TlsaSelector[] values = TlsaSelector.values();
		for (TlsaSelector type : values) {
			if (type.getValue() == value) {
				return type;
			}
		}

		throw new IllegalArgumentException("Invalido "+value);
	}

}
