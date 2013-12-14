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
package br.registro.dnsshim.signer.protocol;

import java.math.BigInteger;

import br.registro.dnsshim.domain.DnskeyAlgorithm;
import br.registro.dnsshim.domain.DnskeyFlags;
import br.registro.dnsshim.domain.DnskeyType;

public class ImportKeyRequest {
	private DnskeyAlgorithm algorithm;
	private DnskeyFlags flags;
	private DnskeyType keyType;
	private BigInteger modulus;
	private BigInteger publicExponent;
	private BigInteger privateExponent;
	private BigInteger prime1;
	private BigInteger prime2;
	private BigInteger exponent1;
	private BigInteger exponent2;
	private BigInteger coefficient;
	
	public ImportKeyRequest() {
		modulus = new BigInteger("0");
		privateExponent = new BigInteger("0");
		prime1 = new BigInteger("0");
		prime2 = new BigInteger("0");
		exponent1 = new BigInteger("0");
		exponent2 = new BigInteger("0");
		coefficient = new BigInteger("0");
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

	public BigInteger getModulus() {
		return modulus;
	}
	public void setModulus(BigInteger modulus) {
		this.modulus = modulus;
	}

	public BigInteger getPublicExponent() {
		return publicExponent;
	}
	public void setPublicExponent(BigInteger publicExponent) {
		this.publicExponent = publicExponent;
	}

	public BigInteger getPrivateExponent() {
		return privateExponent;
	}
	public void setPrivateExponent(BigInteger privateExponent) {
		this.privateExponent = privateExponent;
	}

	public BigInteger getPrime1() {
		return prime1;
	}

	public void setPrime1(BigInteger prime1) {
		this.prime1 = prime1;
	}

	public BigInteger getPrime2() {
		return prime2;
	}

	public void setPrime2(BigInteger prime2) {
		this.prime2 = prime2;
	}

	public BigInteger getExponent1() {
		return exponent1;
	}

	public void setExponent1(BigInteger exponent1) {
		this.exponent1 = exponent1;
	}

	public BigInteger getExponent2() {
		return exponent2;
	}

	public void setExponent2(BigInteger exponent2) {
		this.exponent2 = exponent2;
	}

	public BigInteger getCoefficient() {
		return coefficient;
	}

	public void setCoefficient(BigInteger coefficient) {
		this.coefficient = coefficient;
	}
}
