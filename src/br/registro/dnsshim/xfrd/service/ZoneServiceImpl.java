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
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.ClientConfig;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.Dnskey;
import br.registro.dnsshim.domain.DnskeyFlags;
import br.registro.dnsshim.domain.DnskeyProtocol;
import br.registro.dnsshim.domain.DnskeyType;
import br.registro.dnsshim.domain.DsDigestType;
import br.registro.dnsshim.domain.DsInfo;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.Soa;
import br.registro.dnsshim.domain.Zone;
import br.registro.dnsshim.domain.logic.DsCalc;
import br.registro.dnsshim.signer.domain.logic.DnskeyCalc;
import br.registro.dnsshim.signer.protocol.RemoveKeyRequest;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.xfrd.dao.filesystem.SlaveGroupDao;
import br.registro.dnsshim.xfrd.dao.filesystem.UserDao;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneDataDao;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneDataPrePublishDao;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneInfoDao;
import br.registro.dnsshim.xfrd.dns.axfr.AxfrClient;
import br.registro.dnsshim.xfrd.dns.notifier.domain.ZoneNotify;
import br.registro.dnsshim.xfrd.dns.notifier.logic.NotifierManager;
import br.registro.dnsshim.xfrd.domain.DnskeyInfo;
import br.registro.dnsshim.xfrd.domain.DnskeyStatus;
import br.registro.dnsshim.xfrd.domain.Slave;
import br.registro.dnsshim.xfrd.domain.SlaveGroup;
import br.registro.dnsshim.xfrd.domain.User;
import br.registro.dnsshim.xfrd.domain.XfrdConfig;
import br.registro.dnsshim.xfrd.domain.ZoneData;
import br.registro.dnsshim.xfrd.domain.ZoneDataPrePublish;
import br.registro.dnsshim.xfrd.domain.ZoneInfo;
import br.registro.dnsshim.xfrd.domain.logic.DnsshimSession;
import br.registro.dnsshim.xfrd.domain.logic.DnsshimSessionCache;
import br.registro.dnsshim.xfrd.domain.logic.XfrdConfigManager;
import br.registro.dnsshim.xfrd.domain.logic.ZoneDataCache;
import br.registro.dnsshim.xfrd.domain.logic.ZoneDataPrePublishCache;
import br.registro.dnsshim.xfrd.signerclient.SignerClient;
import br.registro.dnsshim.xfrd.ui.protocol.ChangeKeyStatusResponse;
import br.registro.dnsshim.xfrd.ui.protocol.ImportZoneRequest;
import br.registro.dnsshim.xfrd.ui.protocol.ImportZoneResponse;
import br.registro.dnsshim.xfrd.ui.protocol.ListSlavesResponse;
import br.registro.dnsshim.xfrd.ui.protocol.ListZonesBySlaveGroupResponse;
import br.registro.dnsshim.xfrd.ui.protocol.ListZonesRequest;
import br.registro.dnsshim.xfrd.ui.protocol.ListZonesResponse;
import br.registro.dnsshim.xfrd.ui.protocol.NewKeyRequest;
import br.registro.dnsshim.xfrd.ui.protocol.NewKeyResponse;
import br.registro.dnsshim.xfrd.ui.protocol.NewZoneRequest;
import br.registro.dnsshim.xfrd.ui.protocol.NewZoneResponse;
import br.registro.dnsshim.xfrd.ui.protocol.PrintZoneResponse;
import br.registro.dnsshim.xfrd.ui.protocol.RemoveKeyResponse;
import br.registro.dnsshim.xfrd.ui.protocol.RemoveZoneRequest;
import br.registro.dnsshim.xfrd.ui.protocol.RemoveZoneResponse;
import br.registro.dnsshim.xfrd.ui.protocol.Response;
import br.registro.dnsshim.xfrd.ui.protocol.RrResponse;
import br.registro.dnsshim.xfrd.ui.protocol.SetExpirationPeriodRequest;
import br.registro.dnsshim.xfrd.ui.protocol.SetExpirationPeriodResponse;
import br.registro.dnsshim.xfrd.ui.protocol.SlaveGroupRequest;
import br.registro.dnsshim.xfrd.ui.protocol.SlaveOperation;
import br.registro.dnsshim.xfrd.ui.protocol.SlaveResponse;
import br.registro.dnsshim.xfrd.ui.protocol.ZoneExistsRequest;
import br.registro.dnsshim.xfrd.ui.protocol.ZoneUserRequest;
import br.registro.dnsshim.xfrd.ui.protocol.ZoneUserResponse;
import br.registro.dnsshim.xfrd.ui.protocol.ZoneVersionResponse;
import br.registro.dnsshim.xfrd.util.ServerContext;

public class ZoneServiceImpl implements ZoneService {
	private static final Logger logger = Logger.getLogger(ZoneServiceImpl.class);

	private ZoneDataCache zoneDataCache = ZoneDataCache.getInstance();
	private ZoneDataPrePublishCache zoneDataPrePubCache = ZoneDataPrePublishCache.getInstance();
	private EntityManager entityManager;
	private ZoneInfoDao zoneInfoDao;
	
	public ZoneServiceImpl() {
		entityManager = ServerContext.getEntityManager();
		zoneInfoDao = new ZoneInfoDao(entityManager);
	}
	
	@Override
	public NewZoneResponse newZone(NewZoneRequest request)
		throws DnsshimProtocolException, IOException {
		// Check if the zone already exists
		String zonename = request.getZone();
	
		if (zoneInfoDao.findByZonename(zonename) != null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_ALREADY_EXISTS, "Zone already exists " + zonename);
		}
		
		Soa soa = request.getSoa();
		ZoneInfo zoneInfo = new ZoneInfo();
		zoneInfo.setZonename(zonename);
		zoneInfo.setSoaMinimum(soa.getMinimum());
		zoneInfo.setTtlZone(soa.getTtl());
		zoneInfo.setTtlDnskey(soa.getTtl());
		zoneInfo.setValidity(request.getExpirationPeriod());
		zoneInfo.setSigned(request.isSigned());
		
		DnsshimSessionCache dnsshimSessionCache = DnsshimSessionCache.getInstance();
		DnsshimSession session = dnsshimSessionCache.get(request.getSessionId());
		User user = (User) session.getAttribute("user");
		if (user != null) {
			zoneInfo.addUser(user);
		}
		
		zoneInfoDao.save(zoneInfo);

		Zone zone = new Zone(zonename, zoneInfo.isSigned());
		if (zone.add(soa) == false) {
			throw new DnsshimProtocolException(ProtocolStatusCode.NO_SOA, "Error creating zone " + zonename);			
		}
		
		ZoneDataPrePublish zoneDataPre = new ZoneDataPrePublish(zone);
		zoneDataPrePubCache.put(zoneDataPre);

		NewZoneResponse response = new NewZoneResponse();
		response.setZone(zonename);
		if (request.isSigned()) {
			NewKeyRequest keyRequest = new NewKeyRequest();
			keyRequest.setZone(zonename);
			keyRequest.setAlgorithm(request.getAlgorithm());
			keyRequest.setKeySize(request.getKeySize());
			keyRequest.setKeyStatus(DnskeyStatus.SIGN);
			
			// The key being created with the zone must be a ZSK
			keyRequest.setKeyType(DnskeyType.ZSK);
			keyRequest.setFlags(DnskeyFlags.SEP_ON);
			keyRequest.setProtocol(DnskeyProtocol.PROTOCOL);
			KeyServiceImpl keyService = new KeyServiceImpl();
			NewKeyResponse keyResponse = keyService.newKey(keyRequest);
			
			response.setSigned(true);
			response.setKeyName(keyResponse.getKeyName());
			response.setDsInfo(keyResponse.getDsInfo());
		}
		
		if (logger.isInfoEnabled()) {
			logger.info("Zone " + zonename + " created.");
		}
		
		if (request.getSlaveGroup() != null) {
			if (request.isAutoBalance()) {
				throw new DnsshimProtocolException(ProtocolStatusCode.BAD_UICLIENT_REQUEST, "auto-balance cannot be used with slavegroup");
			}
			
			SlaveGroupRequest assignRequest = new SlaveGroupRequest();
			assignRequest.setGroupName(request.getSlaveGroup());
			assignRequest.setSessionId(request.getSessionId());
			assignRequest.setZone(zonename);
			assignRequest.setOperation(SlaveOperation.ASSIGN);
			assignSlaveGroup(assignRequest);
			
			SlaveGroupDao slaveGroupDao = new SlaveGroupDao(ServerContext.getEntityManager());
			SlaveGroup slaveGroup = slaveGroupDao.findByName(request.getSlaveGroup());		
			if (slaveGroup == null) {
				throw new DnsshimProtocolException(ProtocolStatusCode.SLAVE_GROUP_NOT_FOUND, "Slave group not found");
			}

			response.setSlaves(slaveGroup.getSlaves());
		}
		
		if (request.isAutoBalance()) {
			SlaveGroupDao dao = new SlaveGroupDao(ServerContext.getEntityManager());
			SlaveGroup slaveGroup = dao.findMostIdleGroup();
			
			if (slaveGroup == null) {
				throw new DnsshimProtocolException(ProtocolStatusCode.SLAVE_GROUP_NOT_FOUND, "Cannot find the most idle slave group");
			}
			
			SlaveGroupRequest assignRequest = new SlaveGroupRequest();
			assignRequest.setGroupName(slaveGroup.getName());
			assignRequest.setSessionId(request.getSessionId());
			assignRequest.setZone(zonename);
			assignRequest.setOperation(SlaveOperation.ASSIGN);
			assignSlaveGroup(assignRequest);

			response.setSlaves(slaveGroup.getSlaves());
		}
		
		return response;
	}

	@Override
	public ImportZoneResponse importZone(ImportZoneRequest request)
			throws DnsshimProtocolException, IOException {
		// Check if the zone already exists
		String zonename = request.getZone().toLowerCase();

		if (zoneInfoDao.findByZonename(zonename) == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_ALREADY_EXISTS, "Zone already exists " + zonename);
		}

		String server = request.getServer();
		int port = request.getPort();
		AxfrClient axfrClient = new AxfrClient(server, port);
		axfrClient.requestAxfr(zonename, request.getSessionId());
		
		ImportZoneResponse response = new ImportZoneResponse();
		response.setZone(zonename);
		response.setMsg("Zone imported successfully");

		if (logger.isInfoEnabled()) {
			logger.info("Zone " + zonename + " imported from " + server);
		}
		return response;
	}

	@Override
	public RrResponse addRecord(String zonename, ResourceRecord rr)
		throws DnsshimProtocolException, IOException {
		RrResponse response = new RrResponse();

		ZoneDataPrePublish zoneData = zoneDataPrePubCache.get(zonename);
		if (!zoneData.add(rr)) {
			throw new DnsshimProtocolException(ProtocolStatusCode.RESOURCE_RECORD_ADD_ERROR, "RR add error");
		}

		zoneDataPrePubCache.put(zoneData);

		if (logger.isInfoEnabled()) {
			logger.info("Record " + rr.toString() + " added to zone " + zonename);
		}
		return response;
	}
	
	@Override
	public RrResponse removeRecord(String zonename, ResourceRecord rr)
		throws IOException, DnsshimProtocolException {
		RrResponse response = new RrResponse();
		ZoneDataPrePublish zoneData = zoneDataPrePubCache.get(zonename);
		if (zoneData == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found " + zonename);
		}
		
		if (zoneData.remove(rr) == false) {
			throw new DnsshimProtocolException(ProtocolStatusCode.RESOURCE_RECORD_NOT_FOUND, "Record not found in zone " + zonename);
		}
		
		zoneDataPrePubCache.put(zoneData);
		
		if (logger.isInfoEnabled()) {
			logger.info("Record " + rr.toString() + " removed from zone " + zonename);
		}
		return response;
	}
	
	public ListZonesResponse listZones(ListZonesRequest request) throws IOException, DnsshimProtocolException {
		ListZonesResponse response = new ListZonesResponse();
		DnsshimSessionCache dnsshimSessionCache = DnsshimSessionCache.getInstance();
		DnsshimSession session = dnsshimSessionCache.get(request.getSessionId());
		
		User user = (User) session.getAttribute("user");
		String username = user.getUsername();
		
		List<ZoneInfo> zones = zoneInfoDao.findByUser(username);
		for (ZoneInfo zone : zones) {
			response.addZonename(zone.getZonename());
		}

		return response;
	}

	public ListZonesBySlaveGroupResponse listZonesBySlaveGroup(String slaveGroupName) throws DnsshimProtocolException, IOException {
		ListZonesBySlaveGroupResponse response = new ListZonesBySlaveGroupResponse();
		
		SlaveGroupDao slaveGroupDao = new SlaveGroupDao(entityManager);
		SlaveGroup slaveGroup = slaveGroupDao.findByName(slaveGroupName);
		if (slaveGroup == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.SLAVE_GROUP_NOT_FOUND, "Slave group not found");
		}
		
		for (ZoneInfo zone : zoneInfoDao.findBySlavegroup(slaveGroupName)) {
			response.addZonename(zone.getZonename());
		}
		response.setSlaveGroup(slaveGroupName);
		
		return response;
	}
	
	public PrintZoneResponse printZone(String zonename)
	throws DnsshimProtocolException, IOException {
		PrintZoneResponse response = new PrintZoneResponse();
		response.setZone(zonename);
		ZoneDataPrePublish zoneData = zoneDataPrePubCache.get(zonename);
		if (zoneData == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found " + zonename);
		}

		Iterator<ResourceRecord> it = zoneData.getAxfr().iterator();
		while (it.hasNext()) {
			ResourceRecord rr = it.next();
			if (rr.getType().isDnssec()) {
				continue;
			}
			response.addRr(rr);
		}
		
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zonename);
		if (zoneInfo == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found " + zonename);
		}
		
		List<Dnskey> dnskeys = new ArrayList<Dnskey>();
		for (DnskeyInfo key : zoneInfo.getKeys()) {
			if (key.getStatus() == DnskeyStatus.NONE) {
				continue;
			}
			
			Dnskey dnskey = new Dnskey(ResourceRecord.APEX_OWNERNAME, DnsClass.IN, 0, key.getRdata());
			dnskeys.add(dnskey);
		}
		
		for (Dnskey dnskey : dnskeys) {
			String digest = "";
			try {
				digest = DsCalc.calcDigest(dnskey, DsDigestType.SHA1, zonename);
			} catch (NoSuchAlgorithmException nsae) {
				
			}
			short keytag = DnskeyCalc.calcKeyTag(dnskey.getRdata());
			DsInfo dsInfo = new DsInfo();
			dsInfo.setKeytag(ByteUtil.toUnsigned(keytag));
			dsInfo.setDigestType(DsDigestType.SHA1);
			dsInfo.setDigest(digest);
			response.addDs(dsInfo);
		}
		return response;
	}
	
	public RemoveZoneResponse removeZone(RemoveZoneRequest request) throws DnsshimProtocolException, IOException {
		RemoveZoneResponse response = new RemoveZoneResponse();
		String zone = request.getZone();
		response.setZone(zone);
		
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zone);
		ZoneDataPrePublish zonePrePublish = zoneDataPrePubCache.get(zone);
		
		if (zoneInfo == null || zonePrePublish == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found " + zone);
		}
		
		for (DnskeyInfo key : zoneInfo.getKeys()) {
			removeKey(zone, key.getName());
		}

		List<SlaveGroup> slaveGroups = new ArrayList<SlaveGroup>(zoneInfo.getSlaveGroups());
		for (SlaveGroup slaveGroup : slaveGroups) {
			SlaveGroupRequest unassignRequest = new SlaveGroupRequest();
			unassignRequest.setGroupName(slaveGroup.getName());
			unassignRequest.setSessionId(request.getSessionId());
			unassignRequest.setZone(zone);
			unassignRequest.setOperation(SlaveOperation.UNASSIGN);
			unassignSlaveGroup(unassignRequest);
		}
		
		zoneInfoDao.delete(zoneInfo);	
		zoneDataPrePubCache.remove(zone);
		
		ZoneData zoneData = zoneDataCache.get(zone);
		if (zoneData != null) {
			zoneDataCache.remove(zone);
		}

		ZoneDataDao zoneDataDao = new ZoneDataDao();
		zoneDataDao.delete(zone);
		ZoneDataPrePublishDao zonePrePublishDao = new ZoneDataPrePublishDao();
		zonePrePublishDao.delete(zone);
		
		if (logger.isInfoEnabled()) {
			logger.info("Zone " + zone + " removed.");
		}
		return response;
	}
	
	public ZoneVersionResponse zoneVersion(String zonename)
		throws DnsshimProtocolException, IOException {
		ZoneDataPrePublish zoneData = zoneDataPrePubCache.get(zonename);
		if (zoneData == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found " +zonename);
		}
		
		ZoneVersionResponse response = new ZoneVersionResponse();
		Zone zone = zoneData.getAxfr();
		response.setZone(zonename);
		response.setSerial(zone.getSerial());
		return response;
	}

	public SlaveResponse assignSlaveGroup(SlaveGroupRequest request) 
			throws DnsshimProtocolException, IOException {
		SlaveGroupDao slaveGroupDao = new SlaveGroupDao(entityManager);
		String groupName = request.getGroupName();		

		SlaveGroup slaveGroup = slaveGroupDao.findByName(groupName);		
		if (slaveGroup == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.SLAVE_GROUP_NOT_FOUND, "Slave group not found");
		}		

		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(request.getZone());
		List<SlaveGroup> slaves = zoneInfo.getSlaveGroups();
		if (slaves.contains(slaveGroup)) {
			throw new DnsshimProtocolException(ProtocolStatusCode.SLAVE_ALREADY_EXISTS, "Slave already in zone " + request.getZone());
		}

		zoneInfo.assignSlaveGroup(slaveGroup);
		zoneInfoDao.save(zoneInfo);

		slaveGroup.addZone(zoneInfo.getZonename());
		slaveGroupDao.save(slaveGroup);

		// If the zone is published, notify the new slave
		ZoneData zoneData = zoneDataCache.get(request.getZone());

		if (zoneData != null) {
			NotifierManager.clear(request.getZone());
			for (Slave slave : slaveGroup.getSlaves()) {
				ZoneNotify notify = new ZoneNotify();
				notify.setZonename(request.getZone());
				notify.setSlaveAddress(slave.getAddress());
				notify.setSlavePort(slave.getPort());
				Random random = new Random();
				int id = random.nextInt(65534) + 1;
				notify.setQueryId((short) id);
				NotifierManager.addZoneNotify(notify);	
			}
		}

		if (logger.isInfoEnabled()) {
			logger.info("Slavegroup " + groupName + " assigned to zone " + request.getZone());
		}
		return new SlaveResponse();
	}

	@Override
	public SlaveResponse unassignSlaveGroup(SlaveGroupRequest request)
			throws DnsshimProtocolException, IOException {
		String groupName = request.getGroupName();
		String zonename = request.getZone();
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zonename);
		if (zoneInfo == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found " + zonename);
		}
		
		SlaveGroupDao dao = new SlaveGroupDao(entityManager);
		SlaveGroup slaveGroup = dao.findByName(groupName);
		if (slaveGroup == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.SLAVE_NOT_FOUND, "Slavegroup not found");			
		}
		
		if (zoneInfo.unassignSlaveGroup(slaveGroup) == false) {
			throw new DnsshimProtocolException(ProtocolStatusCode.SLAVE_NOT_FOUND, "Slave not in zone " + zonename);
		}
		
		slaveGroup.removeZone(zoneInfo.getZonename());
		zoneInfoDao.save(zoneInfo);
		
		if (logger.isInfoEnabled()) {
			logger.info("Slavegroup " + groupName + " unassigned from zone " + zonename);
		}
		
		return new SlaveResponse();
	}

	public ListSlavesResponse listSlaves(String zonename)
			throws DnsshimProtocolException, IOException {
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zonename);
		if (zoneInfo == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found " + zonename);
		}

		List<SlaveGroup> slaveGroups = zoneInfo.getSlaveGroups();
		ListSlavesResponse response = new ListSlavesResponse();
		response.setZone(zonename);
		response.setSlaveGroups(slaveGroups);
		return response;
	}
	
	@Override
	public RemoveKeyResponse removeKey(String zonename, String keyName) throws IOException, DnsshimProtocolException {
		RemoveKeyResponse response = new RemoveKeyResponse();

		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zonename);
		
		if (zoneInfo == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found " + zonename);
		}
		
		DnskeyInfo key = zoneInfo.getKey(keyName);
		if (key == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_KEY, "Key not found");
		}
		
		// Remove the key from the zone
		zoneInfo.removeKey(key);
		
		// Remove the key files
		XfrdConfig config = XfrdConfigManager.getInstance();
		ClientConfig signerConfig = config.getSignerClientConfig();
		SignerClient client = new SignerClient(signerConfig.getHost(),
				signerConfig.getPort(), signerConfig.getCaPath(), signerConfig.getCaPasswd());
		RemoveKeyRequest signerRequest = new RemoveKeyRequest();
		signerRequest.setKeyName(keyName);
		client.removeKey(signerRequest);

		// Check if the zone has any signing keys
		if (zoneInfo.hasSigningKey() == false) {
			ZoneDataPrePublish zoneData = zoneDataPrePubCache.get(zonename);
			Zone zone = zoneData.getAxfr();
			zone.setSigned(false);
			zoneInfo.setSigned(false);

			zoneDataPrePubCache.put(zoneData);
		}
		
		zoneInfoDao.save(zoneInfo);

		if (logger.isInfoEnabled()) {
			logger.info("Key " + keyName + " removed from zone " + zonename);
		}
		return response;
	}
	
	@Override
	public ChangeKeyStatusResponse changeKey(String zonename, String keyName, DnskeyStatus oldStatus, DnskeyStatus newStatus) throws DnsshimProtocolException, IOException {
		ChangeKeyStatusResponse response = new ChangeKeyStatusResponse();

		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zonename);
		if (zoneInfo == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found " + zonename);
		}
		
		DnskeyInfo key = zoneInfo.getKey(keyName);
		if (key == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_KEY, "Key not found");
		}
		
		if (key.getStatus() != oldStatus) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_KEY_STATUS, "Incompatible key status");
		}
		
		key.setStatus(newStatus);

		try {
			Dnskey dnskey = new Dnskey(ResourceRecord.APEX_OWNERNAME, DnsClass.IN, zoneInfo.getTtlZone(), key.getRdata());
			
			String digest = DsCalc.calcDigest(dnskey, DsDigestType.SHA1, zonename);
			short keytag = DnskeyCalc.calcKeyTag(dnskey.getRdata());
			DsInfo dsInfo = new DsInfo();
			dsInfo.setKeytag(ByteUtil.toUnsigned(keytag));
			dsInfo.setDigestType(DsDigestType.SHA1);
			dsInfo.setDigest(digest);
			response.setDsInfo(dsInfo);
			
			// Check if the zone has any signing keys
			ZoneDataPrePublish zoneData = zoneDataPrePubCache.get(zonename);
			Zone zone = zoneData.getAxfr();
			zone.setSigned(zoneInfo.hasSigningKey());
			zoneInfo.setSigned(zoneInfo.hasSigningKey());
			zoneDataPrePubCache.put(zoneData);
			zoneInfoDao.save(zoneInfo);
		} catch (NoSuchAlgorithmException nsae) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_ALGORITHM, nsae.getMessage());
		}
		
		if (logger.isInfoEnabled()) {
			logger.info("Key status changed to " + newStatus.toString() +
					" on key " + keyName + "(" + zonename + ")");
		}
		return response;
	}

	@Override
	public SetExpirationPeriodResponse setExpirationPeriod(SetExpirationPeriodRequest request)
			throws DnsshimProtocolException {
		String zonename = request.getZone();
		int expiration = request.getExpirationPeriod();
		
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zonename);
		if (zoneInfo == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found " + zonename);
		}
		
		zoneInfo.setValidity(expiration);
		
		zoneInfoDao.save(zoneInfo);
		
		if (logger.isInfoEnabled()) {
			logger.info("Expiration period set to " + String.valueOf(zoneInfo.getValidity()) +
					" on zone " + zonename);
		}
		SetExpirationPeriodResponse response = new SetExpirationPeriodResponse();
		response.setZone(zonename);
		response.setExpirationPeriod(zoneInfo.getValidity());
		return response;
	}

	@Override
	public ZoneUserResponse addUser(ZoneUserRequest request)
			throws DnsshimProtocolException {
		String zonename = request.getZone();
		String username = request.getUsername();

		UserDao userDao = new UserDao(entityManager);
		User user = userDao.findByUsername(username);
		 
		if (user == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.USER_NOT_FOUND, "User not found " + username);
		}
		
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zonename);
		if (zoneInfo == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found "  + zonename);
		}
				
		if (!zoneInfo.hasUser(user)) {
			zoneInfo.addUser(user);
			zoneInfoDao.save(zoneInfo);
		} else {
			throw new DnsshimProtocolException(ProtocolStatusCode.REFUSED_OPERATION, "User already owns this zone");
		}
		
		if (logger.isInfoEnabled()) {
			logger.info("User " + username + " added to zone " + zonename);
		}
		return new ZoneUserResponse();
	}

	@Override
	public ZoneUserResponse removeUser(ZoneUserRequest request)
			throws DnsshimProtocolException {
		
		String zonename = request.getZone();
		String username = request.getUsername();
		
		UserDao userDao = new UserDao(entityManager);
		User user = userDao.findByUsername(username);
		 
		if (user == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.USER_NOT_FOUND, "User not found " + username);
		}
		
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zonename);
		if (zoneInfo == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found " + zonename);
		}
		
		if (zoneInfo.hasUser(user)) {
			if (zoneInfo.getUsers().size() == 1) {
				throw new DnsshimProtocolException(ProtocolStatusCode.REFUSED_OPERATION, "Zone must have at least one user");
			} else {
				zoneInfo.removeUser(user);
				zoneInfoDao.save(zoneInfo);
			}
		} else {
			throw new DnsshimProtocolException(ProtocolStatusCode.REFUSED_OPERATION, "User not owns this zone");
		}

		if (logger.isInfoEnabled()) {
			logger.info("User " + username + " removed from zone " + zonename);
		}
		return new ZoneUserResponse();
	}

	@Override
	public Response zoneExists(ZoneExistsRequest request) throws DnsshimProtocolException {
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(request.getZone());
		Response response = new Response();
		if (zoneInfo == null) {
			response.setStatus(ProtocolStatusCode.ZONE_NOT_FOUND);
			response.setMsg("Zone not found " + request.getZone());
		}
		
		return response;
	}
}