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
package br.registro.dnsshim.signer.service;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.Dnskey;
import br.registro.dnsshim.domain.DnskeyAlgorithm;
import br.registro.dnsshim.domain.DnskeyFlags;
import br.registro.dnsshim.domain.DnskeyProtocol;
import br.registro.dnsshim.domain.DnskeyType;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.signer.dao.filesystem.KeyInfoDao;
import br.registro.dnsshim.signer.domain.KeyInfo;
import br.registro.dnsshim.signer.domain.logic.DnskeyCalc;
import br.registro.dnsshim.signer.domain.logic.KeyInfoCache;
import br.registro.dnsshim.signer.protocol.ImportKeyRequest;
import br.registro.dnsshim.signer.protocol.RemoveKeyRequest;
import br.registro.dnsshim.util.ByteUtil;

public class KeyServiceImpl implements KeyService {
	private static final Logger logger = Logger.getLogger(KeyServiceImpl.class);

	@Override
	public KeyInfo generate(DnskeyAlgorithm algorithm,
													int keySize,
													DnskeyFlags flags,
													DnskeyType keyType) throws DnsshimProtocolException {
		KeyInfo keyInfo = new KeyInfo();
		byte[] keyDigest;

		try {
			if ("RSA".equals(algorithm.getAlgorithmName())) {
				KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");            
				RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(keySize, RSAKeyGenParameterSpec.F4);
				generator.initialize(spec);
				KeyPair keyPair = generator.generateKeyPair();
				RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
				keyInfo.setPublicKey(publicKey);
				keyInfo.setPrivateKey(keyPair.getPrivate());

				// Get the RSA key digest
				keyDigest = DnskeyCalc.calcRsaBuffer(publicKey);

			} else {
				// TODO: DSA not yet supported
				throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_ALGORITHM);
			}

			Dnskey dnskey = new Dnskey(ResourceRecord.APEX_OWNERNAME, DnsClass.IN, 0,
					flags, DnskeyProtocol.PROTOCOL, algorithm, keyDigest);
			
			// Calculate Key Tag
			short keyTag = DnskeyCalc.calcKeyTag(dnskey.getRdata());

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			NumberFormat numberFormat = new DecimalFormat("00000");

			StringBuffer keyName = new StringBuffer();
			keyName.append(dateFormat.format(new Date()));
			keyName.append('-');
			keyName.append(keyType);
			keyName.append('-');
			keyName.append(flags.getName());
			keyName.append('-');
			keyName.append(numberFormat.format(ByteUtil.toUnsigned(keyTag)));
			if (logger.isInfoEnabled()) {
				logger.info(keyName.toString() + " created");
			}
			keyInfo.setName(keyName.toString());
			keyInfo.setAlgorithm(algorithm);
			keyInfo.setKeyTag(keyTag);
			keyInfo.setResourceRecord(dnskey);
			keyInfo.setDnskeyFlags(flags);
			keyInfo.setType(keyType);

			return keyInfo;
		} catch (NoSuchAlgorithmException nsae) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_ALGORITHM);
		} catch (InvalidAlgorithmParameterException e) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_ALGORITHM);
		}
	}

	@Override
	public KeyInfo importKey(ImportKeyRequest request) throws DnsshimProtocolException {
		KeyInfo keyInfo = new KeyInfo();
		byte[] keyDigest;
		DnskeyAlgorithm algorithm = request.getAlgorithm();
		
		try {
			if (algorithm == DnskeyAlgorithm.RSASHA1 || 
					algorithm == DnskeyAlgorithm.RSASHA1_NSEC3) {
				BigInteger modulus = request.getModulus();
				BigInteger privateExponent = request.getPrivateExponent();
				BigInteger publicExponent = request.getPublicExponent();
				BigInteger prime1 = request.getPrime1();
				BigInteger prime2 = request.getPrime2();
				BigInteger exponent1 = request.getExponent1();
				BigInteger exponent2 = request.getExponent2();
				BigInteger coefficient = request.getCoefficient();
				RSAPrivateCrtKeySpec privateKeySpec =
					new RSAPrivateCrtKeySpec(modulus,
							publicExponent, privateExponent,
							prime1, prime2,
							exponent1, exponent2,
							coefficient);
				RSAPublicKeySpec publicKeySpec =
					new RSAPublicKeySpec(modulus, publicExponent);
				KeyFactory keyFactory = KeyFactory.getInstance("RSA");
				RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyFactory.generatePrivate(privateKeySpec);
				RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

				keyInfo.setPrivateKey(privateKey);
				keyInfo.setPublicKey(publicKey);
				keyDigest = DnskeyCalc.calcRsaBuffer(publicKey);

			} else {
				return null;
			}

			DnskeyFlags flags = request.getFlags();
			DnskeyType keyType = request.getKeyType();
			
			Dnskey dnskey =	new Dnskey(ResourceRecord.APEX_OWNERNAME, DnsClass.IN, 0,
					flags, DnskeyProtocol.PROTOCOL, algorithm, keyDigest);

			// Calculate Key Tag
			short keyTag = DnskeyCalc.calcKeyTag(dnskey.getRdata());

			StringBuffer keyName = new StringBuffer();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			NumberFormat numberFormat = new DecimalFormat("00000");
			
			keyName.append(dateFormat.format(new Date()));
			keyName.append('-');
			keyName.append(keyType);
			keyName.append('-');
			keyName.append(flags.getName());
			keyName.append('-');
			keyName.append(numberFormat.format(ByteUtil.toUnsigned(keyTag)));
			if (logger.isInfoEnabled()) {
				logger.info(keyName.toString() + " imported");
			}
			keyInfo.setName(keyName.toString());
			keyInfo.setAlgorithm(algorithm);
			keyInfo.setKeyTag(keyTag);
			keyInfo.setResourceRecord(dnskey);
			keyInfo.setDnskeyFlags(flags);
			keyInfo.setType(keyType);
			
			return keyInfo;
		} catch (NoSuchAlgorithmException nsae) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_ALGORITHM);
		} catch (InvalidKeySpecException ikse) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_KEY);
		}
	}

	@Override
	public void removeKey(RemoveKeyRequest request)
			throws DnsshimProtocolException, IOException {		
		String keyName = request.getKeyName();
		KeyInfoCache keyInfoCache = KeyInfoCache.getInstance();
		keyInfoCache.remove(keyName);

		KeyInfoDao dao = new KeyInfoDao();
		dao.delete(keyName);
		if (logger.isInfoEnabled()) {
			logger.info(keyName + " removed");
		}

	}
}