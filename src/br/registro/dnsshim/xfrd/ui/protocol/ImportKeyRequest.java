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
import br.registro.dnsshim.domain.DnskeyType;
import br.registro.dnsshim.xfrd.domain.DnskeyStatus;

public class ImportKeyRequest extends ZoneOperation {
	private DnskeyAlgorithm algorithm;
	private DnskeyFlags flags;
	private DnskeyType keyType;
	private DnskeyStatus keyStatus;
	private String modulus;
	private String publicExponent;
	private String privateExponent;
	private String prime1;
	private String prime2;
	private String exponent1;
	private String exponent2;
	private String coefficient;

	public ImportKeyRequest() {
		modulus = "";
		publicExponent = "";
		privateExponent = "";
		prime1 = "";
		prime2 = "";
		exponent1 = "";
		exponent2 = "";
		coefficient = "";
	}

	public DnskeyAlgorithm getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(DnskeyAlgorithm algorithm) {
		this.algorithm = algorithm;
	}

	public DnskeyFlags getFlags() {
		return flags;
	}

	public void setFlags(DnskeyFlags flags) {
		this.flags = flags;
	}

	public DnskeyType getKeyType() {
		return keyType;
	}

	public void setKeyType(DnskeyType keyType) {
		this.keyType = keyType;
	}

	public DnskeyStatus getKeyStatus() {
		return keyStatus;
	}

	public void setKeyStatus(DnskeyStatus keyStatus) {
		this.keyStatus = keyStatus;
	}

	public String getModulus() {
		return modulus;
	}

	public void setModulus(String modulus) {
		this.modulus = modulus;
	}

	public String getPublicExponent() {
		return publicExponent;
	}

	public void setPublicExponent(String publicExponent) {
		this.publicExponent = publicExponent;
	}

	public String getPrivateExponent() {
		return privateExponent;
	}

	public void setPrivateExponent(String privateExponent) {
		this.privateExponent = privateExponent;
	}

	public String getPrime1() {
		return prime1;
	}

	public void setPrime1(String prime1) {
		this.prime1 = prime1;
	}

	public String getPrime2() {
		return prime2;
	}

	public void setPrime2(String prime2) {
		this.prime2 = prime2;
	}

	public String getExponent1() {
		return exponent1;
	}

	public void setExponent1(String exponent1) {
		this.exponent1 = exponent1;
	}

	public String getExponent2() {
		return exponent2;
	}

	public void setExponent2(String exponent2) {
		this.exponent2 = exponent2;
	}

	public String getCoefficient() {
		return coefficient;
	}

	public void setCoefficient(String coefficient) {
		this.coefficient = coefficient;
	}
}
