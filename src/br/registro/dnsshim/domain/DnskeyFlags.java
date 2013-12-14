/*  Copyright (C) 2009 Registro.br. All rights reserved. 
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

 *  $Id: $ 
 */
package br.registro.dnsshim.domain;

public enum DnskeyFlags {
	SEP_OFF((byte) 0x00, (short) 256, "256"), 
	SEP_ON((byte) 0x01, (short) 257, "257");

	private DnskeyFlags(byte type, short flag, String name) {
		this.type = type;
		this.flags = flag;
		this.name = name;
	}

	private byte type;
	private short flags;
	private String name;

	public byte getType() {
		return type;
	}

	public void setType(byte type) {
		this.type = type;
	}

	public short getFlags() {
		return flags;
	}

	public void setFlags(short flags) {
		this.flags = flags;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static DnskeyFlags fromValue(short value)
			throws IllegalArgumentException {
		DnskeyFlags[] values = DnskeyFlags.values();
		for (DnskeyFlags type : values) {
			if (type.getFlags() == value) {
				return type;
			}
		}
		throw new IllegalArgumentException();
	}
	
	public static DnskeyFlags fromValue(String value)
		throws IllegalArgumentException {
		DnskeyFlags[] values = DnskeyFlags.values();
		for (DnskeyFlags type : values) {
			if (type.getName().equalsIgnoreCase(value)) {
				return type;
			}
		}
		throw new IllegalArgumentException();
	}

}
