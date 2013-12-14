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
package br.registro.dnsshim.xfrd.dns.protocol;

/**
 * RFC 1035 - 4.1.1. Header section format,
 * RFC 2136 - 2.2. Message Header,
 * RFC 2845 - 1.7. New Assigned Numbers
 * 
 * RCODE (Response code) - this four bit field is undefined in requests and 
 * set in responses.
 * 
 */
public enum ResponseCode {
	NOERROR(0), 
	FORMERR(1), 
	SERVFAIL(2), 
	NXDOMAIN(3), 
	NOTIMP(4), 
	REFUSED(5), 
	YXDOMAIN(6), 
	YXRRSET(7), 
	NXRRSET(8), 
	NOTAUTH(9), 
	NOTZONE(10), 
	BADSIG(16), 
	BADKEY(17), 
	BADTIME(18);

	private byte value; // only necessary 4 bits
	
	private ResponseCode(int value) {
		this.value = (byte) value;
	}

	public byte getValue() {
		return value;
	}

	public static ResponseCode fromValue(byte value) throws IllegalArgumentException {
		ResponseCode[] values = ResponseCode.values();
		for (ResponseCode type : values) {
			if (type.getValue() == value) {
				return type;
			}
		}

		throw new IllegalArgumentException();
	}
}
