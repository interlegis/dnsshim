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

import java.util.List;

public class RdataTxtBuilder implements RdataBuilder {
	public static Rdata get(String text) {
		short length = 255;
		if (text.length() <= 255) {
			length = (short) text.length();
		} 
		
		byte[] data = new byte[length + 1];
		data[0] = (byte) length;
		System.arraycopy(text.getBytes(), 0, data, 1, length);
		return new Rdata(data);
	}
	
	public static Rdata get(List<String> texts) {
		int totalLength = 0;
		for (String text : texts) {
			short length = 255;
			if (text.length() <= 255) {
				length = (short) text.length();
			}
			totalLength += length + 1;
		}
		
		byte[] data = new byte[totalLength];
		int pos = 0;
		for (String text : texts) {
			Rdata dummy = get(text);
			System.arraycopy(dummy.getData(), 0, data, pos, dummy.getRdlen());
			pos += dummy.getRdlen();
		}
		
		return new Rdata(data);
	}
}
