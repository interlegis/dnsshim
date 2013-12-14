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

import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

import br.registro.dnsshim.util.ByteUtil;

public class Rdata implements Comparable<Rdata> {
	private byte[] data;

	public Rdata(byte[] data) {
		this.data = data;
	}

	public Rdata(Rdata rdata) {
		this.data = rdata.data.clone();
	}
	
	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public short getRdlen() {
		return (short) this.data.length;
	}

	@Override
	public String toString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(System.getProperty("line.separator"));
		for (int i = 0; i < data.length; i++) {
			stringBuffer.append(data[i] + ", ");
		}
		stringBuffer.append(System.getProperty("line.separator"));
		return stringBuffer.toString();
	}

	public String toStringHex() {
		Base64 base64 = new Base64();
		return new String(base64.encode(data));
	}
	
	@Override
	public int compareTo(Rdata o) {
		int minSize = (this.data.length < o.data.length) ? this.data.length : o.data.length;

		for (int i = 0; i < minSize; i++) {
			short unsignedThisData = ByteUtil.toUnsigned(this.data[i]);
			short unsignedOtherData = ByteUtil.toUnsigned(o.data[i]);
			if (unsignedThisData != unsignedOtherData) {
				if (unsignedThisData < unsignedOtherData) {
					return -1;
				} else {
					return 1;
				}
			}
		}

		if (this.data.length < o.data.length) {
			return -1;
		} else if (this.data.length > o.data.length) {
			return 1;
		}
	
		return 0;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Rdata) {
			Rdata other = (Rdata) obj;
			if (Arrays.equals(this.data, other.data)) {
				return true;
			}
		}
		return false;
	}
	@Override
	public int hashCode() {
		return Arrays.hashCode(data);
	}
}
