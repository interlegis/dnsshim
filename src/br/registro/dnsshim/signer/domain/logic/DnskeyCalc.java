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
package br.registro.dnsshim.signer.domain.logic;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import org.apache.commons.codec.binary.Base64;

import br.registro.dnsshim.domain.Rdata;
import br.registro.dnsshim.util.ByteUtil;

public class DnskeyCalc {
	/** Calculates the RSA buffer to be used in the DNSKEY Rdata
	* (See RFC 3110, Section 2, Page 2)
	*/
	public static byte[] calcRsaBuffer(RSAPublicKey publicKey) {
		byte[] exponent = publicKey.getPublicExponent().toByteArray();
		byte[] modulus = publicKey.getModulus().toByteArray();
		int expLen = exponent.length;
		int modLen = modulus.length;
		int modulusFirst = 0;
		if (modulus[0] == 0) {
			modLen--;
			modulusFirst = 1;
		}

		int totalLen = 3 + expLen + modLen;

		byte[] rsaBuffer = null;

		if (expLen < 255) {
			totalLen -= 2;
			rsaBuffer = new byte[totalLen];
			byte len = (byte) (expLen & 0xFF);
			rsaBuffer[0] = len;
			System.arraycopy(exponent, 0, rsaBuffer, 1, expLen);
			System.arraycopy(modulus, modulusFirst, rsaBuffer, expLen + 1, modLen);
		} else {
			rsaBuffer = new byte[totalLen];
			short len = (short) (expLen & 0xFFFF);
			ByteUtil.copyBytes(rsaBuffer, 0, 0);
			ByteUtil.copyBytes(rsaBuffer, 1, len);
			System.arraycopy(exponent, 0, rsaBuffer, 3, expLen);
			System.arraycopy(modulus, modulusFirst, rsaBuffer, expLen + 3, modLen);
		}
		
		return rsaBuffer;
	}

	public static short calcKeyTag(Rdata rdata) {
		byte[] data = rdata.getData();

		int rdlength = rdata.getRdlen();
		int i;
		int ac = 0;

		for (i = 0; i < rdlength; ++i) {
			ac += (i & 1) > 0 ? ByteUtil.toInt(data[i]) : ByteUtil.toInt(data[i]) << 8;
		}
		ac += ac >> 16 & 0XFFFF;
		return (short) (ac & 0XFFFF);
	}

	public static RSAPublicKey getRsaPublicKey(byte[] rsaBuffer) throws NoSuchAlgorithmException, InvalidKeySpecException {
		Base64 base64 = new Base64();
		byte[] rsaBufferDecoded =  base64.decode(rsaBuffer);

		byte[] expBytes = null;
		byte[] modBytes = null;
		int pos = 0;
		byte size = rsaBufferDecoded[pos];
		if (size > 0) {
			pos++;
			expBytes = new byte[size];
			System.arraycopy(rsaBufferDecoded, 1, expBytes, 0, size);

			pos += size;
			int modLen = rsaBufferDecoded.length - pos;
			modBytes = new byte[modLen];
			System.arraycopy(rsaBufferDecoded, pos, modBytes, 0, modLen);
		} else {
			byte[] expLenBytes = { rsaBufferDecoded[1], rsaBufferDecoded[2] };
			pos = 3;
			short expLen = ByteUtil.toShort(expLenBytes);
			expBytes = new byte[expLen];
			System.arraycopy(rsaBufferDecoded, 3, expBytes, 0, expLen);
			pos += expLen;
			int modLen = rsaBufferDecoded.length - pos;
			modBytes = new byte[modLen];
			System.arraycopy(rsaBufferDecoded, pos, modBytes, 0, modLen);
		}
		BigInteger exponent = new BigInteger(1, expBytes);
		BigInteger modulus  = new BigInteger(1, modBytes);

		RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(spec);
		return publicKey;
	}

}
