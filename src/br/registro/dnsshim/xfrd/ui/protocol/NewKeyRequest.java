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
package br.registro.dnsshim.xfrd.ui.protocol;

import br.registro.dnsshim.domain.DnskeyAlgorithm;
import br.registro.dnsshim.domain.DnskeyFlags;
import br.registro.dnsshim.domain.DnskeyProtocol;
import br.registro.dnsshim.domain.DnskeyType;
import br.registro.dnsshim.xfrd.domain.DnskeyStatus;

public class NewKeyRequest extends ZoneOperation {
	private int keySize;
	private DnskeyAlgorithm algorithm;
	private DnskeyType keyType;
	private DnskeyFlags flags;
	private DnskeyStatus keyStatus;
	private DnskeyProtocol protocol;

	public int getKeySize() {
		return keySize;
	}

	public void setKeySize(int keySize) {
		this.keySize = keySize;
	}

	public DnskeyAlgorithm getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(DnskeyAlgorithm algorithm) {
		this.algorithm = algorithm;
	}

	public DnskeyType getKeyType() {
		return keyType;
	}

	public void setKeyType(DnskeyType keyType) {
		this.keyType = keyType;
	}

	public DnskeyFlags getFlags() {
		return flags;
	}

	public void setFlags(DnskeyFlags flags) {
		this.flags = flags;
	}

	public DnskeyStatus getKeyStatus() {
		return keyStatus;
	}

	public void setKeyStatus(DnskeyStatus keyStatus) {
		this.keyStatus = keyStatus;
	}

	public DnskeyProtocol getProtocol() {
		return protocol;
	}

	public void setProtocol(DnskeyProtocol protocol) {
		this.protocol = protocol;
	}
}
