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

public enum RrType {

	A			(1),
	NS			(2),
	CNAME		(5),
	SOA			(6),
	PTR			(12),
	HINFO		(13),
	MINFO		(14),
	MX			(15),
	TXT			(16),
	AAAA		(28),
	LOC			(29),
	SRV			(33),
	NAPTR		(35),
	CERT		(37),
	DNAME		(39),
	OPT			(41),
	DS			(43),
	SSHFP		(44),
	IPSECKEY	(45),
	RRSIG		(46),
	NSEC		(47),
	DNSKEY		(48),
	NSEC3		(50),
	NSEC3PARAM	(51),
	TLSA        (52),
	TSIG		(250);

	@Override
	public String toString() {
		return name();
	}

	private RrType(int value) {
		this.value = (short) value;
	}

	private short value;

	public short getValue() {
		return value;
	}

	public boolean isDnssec() {
		if (this == RrType.DNSKEY ||
				this == RrType.NSEC ||
				this == RrType.RRSIG ||
				this == RrType.NSEC3 ||
				this == RrType.NSEC3PARAM) {
			return true;
		}
		
		return false;
	}
	
	public static RrType fromValue(short value)
		throws IllegalArgumentException {
		RrType[] values = RrType.values();
		for (RrType type : values) {
			if (type.getValue() == value) {
				return type;
			}
		}

		throw new IllegalArgumentException("Invalid RRTYPE: " + value);
	}

}
