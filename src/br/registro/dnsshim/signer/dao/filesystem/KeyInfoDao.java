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
package br.registro.dnsshim.signer.dao.filesystem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import br.registro.dnsshim.common.repository.FileSystemUtil;
import br.registro.dnsshim.domain.DnskeyFlags;
import br.registro.dnsshim.signer.domain.KeyInfo;
import br.registro.dnsshim.signer.domain.logic.KeyInfoFactory;
import br.registro.dnsshim.signer.repository.KeyInfoRepository;
import br.registro.dnsshim.util.DomainNameUtil;

public class KeyInfoDao implements KeyInfoRepository {
	private final String BASE_DIR;
	private final KeyInfoFactory keyInfoFactory = new KeyInfoFactory();
	private static final Logger logger = Logger.getLogger(KeyInfoDao.class);
	private static final String EXTENSION = ".private";

	public KeyInfoDao() {
		StringBuilder root = new StringBuilder();
		try {
			// {DNSSHIM_HOME|user.home}/dnsshim/keys/SHA1#1/SHA1#2/SHA1#3/<keys>.key
			if (System.getenv("DNSSHIM_HOME") == null) {
				root.append(System.getProperty("user.home"));
				root.append(File.separatorChar);
				root.append("dnsshim");
			} else {
				root.append(System.getenv("DNSSHIM_HOME"));
			}
			root.append(File.separatorChar);
			root.append("signer");
			root.append(File.separatorChar);
			root.append("keys");
			root.append(File.separatorChar);

			File directory = new File(root.toString());
			FileSystemUtil.makePath(directory);
			if (logger.isDebugEnabled()) {
				logger.debug("KeyInfo base dir:" + root);
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		BASE_DIR = root.toString();
	}

	@Override
	public void save(KeyInfo keyInfo) throws IOException {
		String ki = DomainNameUtil.trimDot(keyInfo.getName());
		StringBuilder path = new StringBuilder(BASE_DIR);
		path.append(FileSystemUtil.hash(ki));
		
		File dir = new File(path.toString());
		FileSystemUtil.makePath(dir);
		
		path.append(ki).append(EXTENSION);
		
		if (logger.isDebugEnabled()) {
			logger.debug("Saving key pair:" + ki);
		}

		KeyPair pair = new KeyPair(keyInfo.getPublicKey(),
															keyInfo.getPrivateKey());
		RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) pair.getPrivate();		
		
		StringBuilder sb = new StringBuilder();
		sb.append("Private-key-format: v1.2");
		sb.append(System.getProperty("line.separator"));
		sb.append("Algorithm: 5 (RSASHA1)");
		sb.append(System.getProperty("line.separator"));
		
		Base64 base64 = new Base64();
		BigInteger modulus = privateKey.getModulus();
		byte[] bytesTmp = modulus.toByteArray();
		byte[] bytes = null;
		if (bytesTmp[0] == 0) {
			bytes = Arrays.copyOfRange(bytesTmp, 1, bytesTmp.length);
		} else {
			bytes = Arrays.copyOfRange(bytesTmp, 0, bytesTmp.length);
		}
		String modStr = new String(base64.encode(bytes));
		sb.append("Modulus: ").append(modStr).append(System.getProperty("line.separator"));
		
		BigInteger pubExponent = privateKey.getPublicExponent();
		bytesTmp = pubExponent.toByteArray();
		if (bytesTmp[0] == 0) {
			bytes = Arrays.copyOfRange(bytesTmp, 1, bytesTmp.length);
		} else {
			bytes = Arrays.copyOfRange(bytesTmp, 0, bytesTmp.length);
		}
		String pubExponentStr =	new String(base64.encode(bytes));
		sb.append("PublicExponent: ").append(pubExponentStr).append(System.getProperty("line.separator"));
		
		BigInteger pvtExponent = privateKey.getPrivateExponent();
		bytesTmp = pvtExponent.toByteArray();
		if (bytesTmp[0] == 0) {
			bytes = Arrays.copyOfRange(bytesTmp, 1, bytesTmp.length);
		} else {
			bytes = Arrays.copyOfRange(bytesTmp, 0, bytesTmp.length);
		}
		String pvtExponentStr = new String(base64.encode(bytes));
		sb.append("PrivateExponent: ").append(pvtExponentStr).append(System.getProperty("line.separator"));
		
		BigInteger prime1 = privateKey.getPrimeP();
		bytesTmp = prime1.toByteArray();
		if (bytesTmp[0] == 0) {
			bytes = Arrays.copyOfRange(bytesTmp, 1, bytesTmp.length);
		} else {
			bytes = Arrays.copyOfRange(bytesTmp, 0, bytesTmp.length);
		}
		String prime1Str = new String(base64.encode(bytes));
		sb.append("Prime1: ").append(prime1Str).append(System.getProperty("line.separator"));
		
		BigInteger prime2 = privateKey.getPrimeQ();
		bytesTmp = prime2.toByteArray();
		if (bytesTmp[0] == 0) {
			bytes = Arrays.copyOfRange(bytesTmp, 1, bytesTmp.length);
		} else {
			bytes = Arrays.copyOfRange(bytesTmp, 0, bytesTmp.length);
		}
		String prime2Str = new String(base64.encode(bytes));
		sb.append("Prime2: ").append(prime2Str).append(System.getProperty("line.separator"));
		
		BigInteger exponent1 = privateKey.getPrimeExponentP();
		bytesTmp = exponent1.toByteArray();
		if (bytesTmp[0] == 0) {
			bytes = Arrays.copyOfRange(bytesTmp, 1, bytesTmp.length);
		} else {
			bytes = Arrays.copyOfRange(bytesTmp, 0, bytesTmp.length);
		}
		String exponent1Str = new String(base64.encode(bytes));
		sb.append("Exponent1: ").append(exponent1Str).append(System.getProperty("line.separator"));
		
		BigInteger exponent2 = privateKey.getPrimeExponentQ();
		bytesTmp = exponent2.toByteArray();
		if (bytesTmp[0] == 0) {
			bytes = Arrays.copyOfRange(bytesTmp, 1, bytesTmp.length);
		} else {
			bytes = Arrays.copyOfRange(bytesTmp, 0, bytesTmp.length);
		}
		String exponent2Str = new String(base64.encode(bytes));
		sb.append("Exponent2: ").append(exponent2Str).append(System.getProperty("line.separator"));
		
		BigInteger crt = privateKey.getCrtCoefficient();
		bytesTmp = crt.toByteArray();
		if (bytesTmp[0] == 0) {
			bytes = Arrays.copyOfRange(bytesTmp, 1, bytesTmp.length);
		} else {
			bytes = Arrays.copyOfRange(bytesTmp, 0, bytesTmp.length);
		}
		String crtStr = new String(base64.encode(bytes));
		sb.append("Coefficient: ").append(crtStr).append(System.getProperty("line.separator"));
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(path.toString()));
		bw.write(sb.toString());
		bw.close();
	}
	
	public void delete(String keyName) throws IOException {
		StringBuilder path = new StringBuilder(BASE_DIR);
		path.append(FileSystemUtil.hash(keyName));
		File dir = new File(path.toString());
		path.append(keyName + EXTENSION);
		File file = new File(path.toString());
		file.delete();
		
		while (dir.list().length == 0) {
			File parent = dir.getParentFile();
			dir.delete();
			dir = parent;
		}
	}

	@Override
	public Map<String, KeyInfo> findAll() throws IOException,
			InvalidAlgorithmParameterException {
		if (logger.isDebugEnabled()) {
			logger.debug("Loading all keys...");
		}
		Map<String, KeyInfo> keys = new LinkedHashMap<String, KeyInfo>();
		File keysRootDirectory = new File(BASE_DIR);
		loadKeysRecursively(keysRootDirectory, keys);
		return keys;
	}

	private void loadKeysRecursively(File rootDirectory,
			Map<String, KeyInfo> keys)
			throws InvalidAlgorithmParameterException, IOException {
		File[] files = rootDirectory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				loadKeysRecursively(file, keys);
			} else {
				if (file.exists() && (file.getName().endsWith(EXTENSION))) {
					
					String identifier = 
						file.getName().substring(0, file.getName().lastIndexOf('.'));
					
					KeyPair keyPair = readKeyPair(file);
					PrivateKey privateKey = keyPair.getPrivate();
					PublicKey publicKey = keyPair.getPublic();
					
					if (file.getName().contains("257")) {
						keys.put(identifier, keyInfoFactory.generate(identifier,
								privateKey, publicKey, DnskeyFlags.SEP_ON));
						if (logger.isDebugEnabled()) {
							logger.debug("Keyfile " + file + " loaded");
						}
					} else if (file.getName().contains("256")) {
						keys.put(identifier, keyInfoFactory.generate(identifier,
								privateKey, publicKey, DnskeyFlags.SEP_OFF));
						if (logger.isDebugEnabled()) {
							logger.debug("Keyfile " + file + " loaded");
						}
					} else {
						throw new IOException("Cannot define DNSKEYFLAG (SEP_ON or SEP_OFF)");
					}
				}
			}
		}
	}
	
	private void loadKeyNamesRecursively(File rootDirectory,List<String> keyNames) {
		File[] files = rootDirectory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				loadKeyNamesRecursively(rootDirectory, keyNames);
			} else {
				if (file.exists() && (file.getName().endsWith(EXTENSION))) {
					String identifier = 
						file.getName().substring(0, file.getName().lastIndexOf('.'));
					keyNames.add(identifier);
				}
			}
		}	
	}
	
	@Override
	public List<String> findAllKeyNames() {
		List<String> keyNames = new ArrayList<String>();
		File keysRootDirectory = new File(BASE_DIR);
		loadKeyNamesRecursively(keysRootDirectory, keyNames);
		return keyNames;
	}

	private KeyPair readKeyPair(File keyFile) throws IOException {
		try {
			BufferedReader br = new BufferedReader(new FileReader(keyFile));
			String line = br.readLine();

			BigInteger modulus = null;
			BigInteger pubExponent = null;
			BigInteger pvtExponent = null;
			BigInteger prime1 = null;
			BigInteger prime2 = null;
			BigInteger exponent1 = null;
			BigInteger exponent2 = null;
			BigInteger coefficient = null;
			Base64 b64 = new Base64();
			while (line != null) {
				String[] fields = line.split(" ");
				if (fields.length < 2) {
					logger.error("Error parsing key file " + keyFile.toString() + ".");
					continue;
				}
				
				String key = fields[0];
				String value = fields[1];
				if (key.equals("Algorithm:")) {
					
				} else if (key.equals("Modulus:")) {
					modulus = new BigInteger(1, b64.decode(value.getBytes()));
				} else if (key.equals("PublicExponent:")) {
					pubExponent = new BigInteger(1, b64.decode(value.getBytes()));
				} else if (key.equals("PrivateExponent:")) {
					pvtExponent = new BigInteger(1, b64.decode(value.getBytes()));
				} else if (key.equals("Prime1:")) {
					prime1 = new BigInteger(1, b64.decode(value.getBytes()));
				} else if (key.equals("Prime2:")) {
					prime2 = new BigInteger(1, b64.decode(value.getBytes()));
				} else if (key.equals("Exponent1:")) {
					exponent1 = new BigInteger(1, b64.decode(value.getBytes()));
				} else if (key.equals("Exponent2:")) {
					exponent2 = new BigInteger(1, b64.decode(value.getBytes()));
				} else if (key.equals("Coefficient:")) {
					coefficient = new BigInteger(1, b64.decode(value.getBytes()));
				}
				line = br.readLine();
			}
			
			if (modulus == null ||
					pubExponent == null || pvtExponent == null ||
					prime1 == null || prime2 == null ||
					exponent1 == null || exponent2 == null ||
					coefficient == null) {
				br.close();
				throw new IOException("Invalid Key File");
			}
			
			RSAPrivateCrtKeySpec privateKeySpec =
				new RSAPrivateCrtKeySpec(modulus, pubExponent, pvtExponent,
																 prime1, prime2,
																 exponent1, exponent2,
																 coefficient);
			RSAPublicKeySpec publicKeySpec =
				new RSAPublicKeySpec(modulus, pubExponent);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			RSAPrivateCrtKey privateKey = 
				(RSAPrivateCrtKey) keyFactory.generatePrivate(privateKeySpec);
			RSAPublicKey publicKey =
				(RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
			
			KeyPair keyPair = new KeyPair(publicKey, privateKey);
			br.close();
			return keyPair;
		} catch (NoSuchAlgorithmException nsae) {
			logger.error("cannot read key pair");
			throw new IOException("cannot read key pair");
		} catch (InvalidKeySpecException ike) {
			logger.error("cannot read key pair");
			throw new IOException("cannot read key pair");
		}
	}
	
	@Override
	public KeyInfo findByName(String name) throws IOException,
			InvalidAlgorithmParameterException {
		if (logger.isDebugEnabled()) {
			logger.debug("Finding key" + name + "...");
		}
		String kn = DomainNameUtil.trimDot(name);
		StringBuilder path = new StringBuilder(BASE_DIR);
		path.append(FileSystemUtil.hash(kn));
		
		File dir = new File(path.toString());
		FileSystemUtil.makePath(dir);
		path.append(kn).append(EXTENSION);	
		
		String keyname = path.toString();

		File file = new File(path.toString());
		KeyPair keyPair = readKeyPair(file);
		KeyInfo keyInfo = null;

		PrivateKey privateKey = keyPair.getPrivate();
		PublicKey publicKey = keyPair.getPublic();

		String identifier = file.getName().substring(0, file.getName().lastIndexOf('.'));
		if (keyname.contains("257")) {
			keyInfo = keyInfoFactory.generate(identifier, privateKey,
					publicKey, DnskeyFlags.SEP_ON);
			if (logger.isDebugEnabled()) {
				logger.debug("Key " + keyname + " loaded");
			}
		} else if (keyname.contains("256")) {
			keyInfo = keyInfoFactory.generate(identifier, privateKey,
					publicKey, DnskeyFlags.SEP_OFF);
			if (logger.isDebugEnabled()) {
				logger.debug("Key " + keyname + " loaded");
			}
		} else {
			throw new IOException("Cannot define DNSKEYFLAG (SEP_ON or SEP_OFF)");
		}

		return keyInfo;
	}

}