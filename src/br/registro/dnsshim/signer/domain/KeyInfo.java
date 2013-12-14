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

import java.security.PrivateKey;
import java.security.PublicKey;

import br.registro.dnsshim.domain.DnskeyAlgorithm;
import br.registro.dnsshim.domain.DnskeyType;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.DnskeyFlags;
import br.registro.dnsshim.domain.logic.Cacheable;

public class KeyInfo implements Cacheable<String> {
	private String name;
	private PrivateKey privateKey;
	private PublicKey publicKey;
	private ResourceRecord resourceRecord; // dnskey
	private DnskeyAlgorithm algorithm;
	private DnskeyFlags dnskeyFlags;
	private DnskeyType type;
	
	private short keyTag;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(PrivateKey privateKey) {
		this.privateKey = privateKey;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}

	public ResourceRecord getResourceRecord() {
		return resourceRecord;
	}

	public void setResourceRecord(ResourceRecord resourceRecord) {
		this.resourceRecord = resourceRecord;
	}

	public DnskeyAlgorithm getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(DnskeyAlgorithm algorithm) {
		this.algorithm = algorithm;
	}

	public DnskeyFlags getDnskeyFlags() {
		return dnskeyFlags;
	}

	public void setDnskeyFlags(DnskeyFlags dnskeyFlags) {
		this.dnskeyFlags = dnskeyFlags;
	}

	public short getKeyTag() {
		return keyTag;
	}

	public void setKeyTag(short keyTag) {
		this.keyTag = keyTag;
	}

	public DnskeyType getType() {
		return type;
	}
	public void setType(DnskeyType type) {
		this.type = type;
	}

	@Override
	public String getCacheKey() {
		return name;
	}

}
