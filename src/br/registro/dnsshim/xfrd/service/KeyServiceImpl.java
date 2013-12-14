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
package br.registro.dnsshim.xfrd.service;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.ClientConfig;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.domain.Dnskey;
import br.registro.dnsshim.domain.DnskeyProtocol;
import br.registro.dnsshim.domain.DsDigestType;
import br.registro.dnsshim.domain.DsInfo;
import br.registro.dnsshim.domain.Zone;
import br.registro.dnsshim.domain.logic.DsCalc;
import br.registro.dnsshim.signer.domain.logic.DnskeyCalc;
import br.registro.dnsshim.signer.protocol.GenerateKeyRequest;
import br.registro.dnsshim.signer.protocol.KeyResponse;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneDataPrePublishDao;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneInfoDao;
import br.registro.dnsshim.xfrd.domain.DnskeyInfo;
import br.registro.dnsshim.xfrd.domain.DnskeyStatus;
import br.registro.dnsshim.xfrd.domain.XfrdConfig;
import br.registro.dnsshim.xfrd.domain.ZoneDataPrePublish;
import br.registro.dnsshim.xfrd.domain.ZoneInfo;
import br.registro.dnsshim.xfrd.domain.logic.XfrdConfigManager;
import br.registro.dnsshim.xfrd.domain.logic.ZoneDataPrePublishCache;
import br.registro.dnsshim.xfrd.signerclient.SignerClient;
import br.registro.dnsshim.xfrd.ui.protocol.ImportKeyRequest;
import br.registro.dnsshim.xfrd.ui.protocol.ImportKeyResponse;
import br.registro.dnsshim.xfrd.ui.protocol.ListKeysResponse;
import br.registro.dnsshim.xfrd.ui.protocol.NewKeyRequest;
import br.registro.dnsshim.xfrd.ui.protocol.NewKeyResponse;
import br.registro.dnsshim.xfrd.util.ServerContext;

public class KeyServiceImpl implements KeyService {
	private static final Logger logger = Logger.getLogger(KeyServiceImpl.class);

	private EntityManager entityManager;
	
	public KeyServiceImpl() {
		entityManager = ServerContext.getEntityManager();
	}
	
	@Override
	public NewKeyResponse newKey(NewKeyRequest request)
		throws DnsshimProtocolException, IOException {
		
		String zonename = request.getZone();
		ZoneInfoDao zoneInfoDao = new ZoneInfoDao(entityManager);
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zonename);
		if (zoneInfo == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found: " + zonename);
		}

		XfrdConfig config = XfrdConfigManager.getInstance();
		GenerateKeyRequest signerRequest = new GenerateKeyRequest();
		signerRequest.setAlgorithm(request.getAlgorithm());
		signerRequest.setDnskeyProtocol(DnskeyProtocol.PROTOCOL);
		signerRequest.setKeySize(request.getKeySize());
		signerRequest.setKeyType(request.getKeyType());
		signerRequest.setFlags(request.getFlags());
		
		ClientConfig signerConfig = config.getSignerClientConfig(); 
		SignerClient client = new SignerClient(signerConfig.getHost(),
				signerConfig.getPort(), signerConfig.getCaPath(), signerConfig.getCaPasswd());
	
		KeyResponse signerResponse = client.generateKey(signerRequest);
		Dnskey dnskey = (Dnskey) signerResponse.getResourceRecord();
		String keyName = signerResponse.getKeyName();
		
		DnskeyInfo dnskeyInfo = new DnskeyInfo();
		dnskeyInfo.setRdata(dnskey.rdataPresentation());
		dnskeyInfo.setName(keyName);
		dnskeyInfo.setStatus(request.getKeyStatus());
		dnskeyInfo.setZone(zoneInfo);
		
		zoneInfo.addKey(dnskeyInfo);
		
		// Mark the zone as Signed, if key status is SIGN
		if (request.getKeyStatus() == DnskeyStatus.SIGN) {
			ZoneDataPrePublishCache zoneDataPrePubCache = ZoneDataPrePublishCache.getInstance();
			ZoneDataPrePublish zoneData = zoneDataPrePubCache.get(zonename);
			dnskey.setTtl(zoneInfo.getTtlDnskey());
			zoneData.add(dnskey);
			Zone zone = zoneData.getAxfr();
			zone.setSigned(true);
			zoneInfo.setSigned(true);
			zoneDataPrePubCache.put(zoneData);
		}
		
		zoneInfoDao.save(zoneInfo);

		NewKeyResponse response = new NewKeyResponse();
		response.setZone(zonename);
		response.setKeyName(keyName);
		String dsDigest;
		try {
			dsDigest = DsCalc.calcDigest(dnskey, DsDigestType.SHA1, zonename);
		} catch (NoSuchAlgorithmException nsae) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_ALGORITHM, nsae.getMessage());
		}
		
		if (logger.isInfoEnabled()) {
			logger.info("New key " + keyName + " created for zone " + zonename);
		}
		short keytag = DnskeyCalc.calcKeyTag(dnskey.getRdata());
		DsInfo dsInfo = new DsInfo();
		dsInfo.setKeytag(ByteUtil.toUnsigned(keytag));
		dsInfo.setDigestType(DsDigestType.SHA1);
		dsInfo.setDigest(dsDigest);
		response.setDsInfo(dsInfo);
		return response;
	}

	@Override
	public ImportKeyResponse importKey(ImportKeyRequest request)
			throws IOException, DnsshimProtocolException {
		String zonename = request.getZone();
		
		ZoneInfoDao zoneInfoDao = new ZoneInfoDao(entityManager);
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zonename);

		if (zoneInfo == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found: " + zonename);
		}

		br.registro.dnsshim.signer.protocol.ImportKeyRequest signerReq =
			new br.registro.dnsshim.signer.protocol.ImportKeyRequest();
		signerReq.setAlgorithm(request.getAlgorithm());
		signerReq.setFlags(request.getFlags());
		signerReq.setKeyType(request.getKeyType());
					
		Base64 b64 = new Base64();
		String modulus = request.getModulus();
		signerReq.setModulus(new BigInteger(1, b64.decode(modulus.getBytes())));
		
		String publicExponent = request.getPublicExponent();
		BigInteger publicExp = 
			new BigInteger(1, b64.decode(publicExponent.getBytes()));
		signerReq.setPublicExponent(publicExp);
		
		String privateExponent = request.getPrivateExponent();
		BigInteger privateExp =
			new BigInteger(1, b64.decode(privateExponent.getBytes()));
		signerReq.setPrivateExponent(privateExp);
		
		String prime1 = request.getPrime1();
		signerReq.setPrime1(new BigInteger(1, b64.decode(prime1.getBytes())));
		
		String prime2 = request.getPrime2();
		signerReq.setPrime2(new BigInteger(1, b64.decode(prime2.getBytes())));
		
		String exp1 = request.getExponent1();
		signerReq.setExponent1(new BigInteger(1, b64.decode(exp1.getBytes())));
		
		String exp2 = request.getExponent2();
		signerReq.setExponent2(new BigInteger(1, b64.decode(exp2.getBytes())));
		
		String coef = request.getCoefficient();
		signerReq.setCoefficient(new BigInteger(1, b64.decode(coef.getBytes())));
		
		XfrdConfig config = XfrdConfigManager.getInstance();
		ClientConfig signerConfig = config.getSignerClientConfig(); 

		SignerClient client =	new SignerClient(signerConfig.getHost(), signerConfig.getPort(),
				signerConfig.getCaPath(), signerConfig.getCaPasswd());			
		KeyResponse signerResponse = client.importKey(signerReq);
		Dnskey dnskey = (Dnskey) signerResponse.getResourceRecord();
		String keyName = signerResponse.getKeyName();
		
		DnskeyInfo dnskeyInfo = new DnskeyInfo();
		dnskeyInfo.setRdata(dnskey.rdataPresentation());
		dnskeyInfo.setName(keyName);
		dnskeyInfo.setStatus(request.getKeyStatus());
		dnskeyInfo.setZone(zoneInfo);
		
		zoneInfo.addKey(dnskeyInfo);
		zoneInfoDao.save(zoneInfo);

		// Mark the zone as Signed, if key status is SIGN
		if (request.getKeyStatus() == DnskeyStatus.SIGN) {
			ZoneDataPrePublishCache zoneDataPrePubCache = ZoneDataPrePublishCache.getInstance();
			ZoneDataPrePublish zoneData = zoneDataPrePubCache.get(zonename);
			Zone zone = zoneData.getAxfr();
			zone.setSigned(true);
			zoneInfo.setSigned(true);
			ZoneDataPrePublishDao dao = new ZoneDataPrePublishDao();
			dao.save(zoneData);
		}
		
		ImportKeyResponse response = new ImportKeyResponse();
		response.setZone(zonename);
		response.setKeyName(signerResponse.getKeyName());
		String dsDigest;
		try {
			dsDigest = DsCalc.calcDigest(dnskey, DsDigestType.SHA1, zonename);
		} catch (NoSuchAlgorithmException nsae) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_ALGORITHM, nsae.getMessage());
		}
		
		if (logger.isInfoEnabled()) {
			logger.info("Key " + keyName + " imported for zone " + zonename);
		}
		short keytag = DnskeyCalc.calcKeyTag(dnskey.getRdata());
		DsInfo dsInfo = new DsInfo();
		dsInfo.setKeytag(ByteUtil.toUnsigned(keytag));
		dsInfo.setDigestType(DsDigestType.SHA1);
		dsInfo.setDigest(dsDigest);
		response.setDsInfo(dsInfo);
		return response;
	}

	@Override
	public ListKeysResponse listKeys(String zone)
		throws IOException, DnsshimProtocolException {
		ListKeysResponse response = new ListKeysResponse();
		response.setZone(zone);
		
		ZoneInfoDao zoneInfoDao = new ZoneInfoDao(entityManager);
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zone);

		if (zoneInfo == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found: " + zone);
		}

		List<String> noneKeys = new ArrayList<String>();
		List<String> publishKeys = new ArrayList<String>();
		List<String> signKeys = new ArrayList<String>();
		List<DnskeyInfo> keys = zoneInfo.getKeys();
		for (DnskeyInfo key : keys) {
			switch (key.getStatus()) {
			case NONE:
				noneKeys.add(key.getName());
				break;
			case PUBLISH:
				publishKeys.add(key.getName());
				break;
			case SIGN:
				signKeys.add(key.getName());
				break;
			}
		}
		response.setNoneKeys(noneKeys);
		response.setPublishKeys(publishKeys);
		response.setSignKeys(signKeys);

		return response;
	}
}
