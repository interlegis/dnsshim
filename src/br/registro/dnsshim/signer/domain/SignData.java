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
package br.registro.dnsshim.signer.domain;

public class SignData {
	private String zonename;
	private int ttlZone;
	private int ttlDnskey;
	private int soaMinimum;
	private int inception;
	private int expiration;

	public String getZonename() {
		return zonename;
	}

	public void setZonename(String zonename) {
		this.zonename = zonename;
	}

	public int getTtlZone() {
		return ttlZone;
	}

	public void setTtlZone(int ttlZone) {
		this.ttlZone = ttlZone;
	}

	public int getTtlDnskey() {
		return ttlDnskey;
	}

	public void setTtlDnskey(int ttlDnskey) {
		this.ttlDnskey = ttlDnskey;
	}

	public int getSoaMinimum() {
		return soaMinimum;
	}

	public void setSoaMinimum(int soaMinimum) {
		this.soaMinimum = soaMinimum;
	}

	public int getInception() {
		return inception;
	}

	public void setInception(int inception) {
		this.inception = inception;
	}

	public int getExpiration() {
		return expiration;
	}

	public void setExpiration(int expiration) {
		this.expiration = expiration;
	}
	
	@Override
	public String toString() {
		char sep = ' ';
		StringBuffer result = new StringBuffer();
		
		result.append(zonename).append(sep);
		result.append(ttlZone).append(sep);
		result.append(ttlDnskey).append(sep);
		result.append(soaMinimum).append(sep);
		result.append(inception).append(sep);
		result.append(expiration).append(sep);
		
		return result.toString();
	}

}
