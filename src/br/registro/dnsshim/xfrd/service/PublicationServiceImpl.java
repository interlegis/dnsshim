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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.Dnskey;
import br.registro.dnsshim.domain.Nsec;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.RrType;
import br.registro.dnsshim.domain.Rrset;
import br.registro.dnsshim.domain.Rrsig;
import br.registro.dnsshim.domain.Soa;
import br.registro.dnsshim.domain.Zone;
import br.registro.dnsshim.domain.ZoneIncrement;
import br.registro.dnsshim.signer.domain.SignData;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DateUtil;
import br.registro.dnsshim.util.DomainNameUtil;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneIncrementDao;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneInfoDao;
import br.registro.dnsshim.xfrd.dns.notifier.domain.ZoneNotify;
import br.registro.dnsshim.xfrd.dns.notifier.logic.NotifierManager;
import br.registro.dnsshim.xfrd.domain.DnskeyInfo;
import br.registro.dnsshim.xfrd.domain.DnskeyStatus;
import br.registro.dnsshim.xfrd.domain.Slave;
import br.registro.dnsshim.xfrd.domain.SlaveGroup;
import br.registro.dnsshim.xfrd.domain.XfrdConfig;
import br.registro.dnsshim.xfrd.domain.ZoneData;
import br.registro.dnsshim.xfrd.domain.ZoneDataPrePublish;
import br.registro.dnsshim.xfrd.domain.ZoneInfo;
import br.registro.dnsshim.xfrd.domain.logic.XfrdConfigManager;
import br.registro.dnsshim.xfrd.domain.logic.ZoneDataCache;
import br.registro.dnsshim.xfrd.domain.logic.ZoneDataPrePublishCache;
import br.registro.dnsshim.xfrd.ui.protocol.PubZoneResponse;
import br.registro.dnsshim.xfrd.util.ServerContext;

public class PublicationServiceImpl implements PublicationService {
	private static final Logger logger = Logger.getLogger(PublicationServiceImpl.class);

	private ZoneDataPrePublishCache zonePrePubCache = ZoneDataPrePublishCache.getInstance();
	private ZoneDataCache zoneDataCache = ZoneDataCache.getInstance();
	private ZoneInfoDao zoneInfoDao;

	public PublicationServiceImpl() {
		zoneInfoDao = new ZoneInfoDao(ServerContext.getEntityManager());
	}
	
	@Override
	public PubZoneResponse full(String zonename, long newSoaVersion)
			throws DnsshimProtocolException {
		PubZoneResponse response = new PubZoneResponse();
		
		ZoneDataPrePublish zonePrePublish = zonePrePubCache.get(zonename);
		if (zonePrePublish == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found " + zonename);
		}

		ZoneDataPrePublish backupPrePublish = new ZoneDataPrePublish(zonePrePublish);
		
		try {
			synchronized (zonePrePublish) {
				newSoaVersion = publication(zonename, newSoaVersion, false);				
			}
			
			response.setZone(zonename);
			response.setSerial(newSoaVersion);
			if (logger.isInfoEnabled()) {
				logger.info("Full publication done for zone "+zonename+"(serial: " + newSoaVersion + ")");
			}	
			return response;
			
		} catch (Exception e) {
			// Rollback
			zonePrePubCache.put(backupPrePublish);		
			throw new DnsshimProtocolException(ProtocolStatusCode.PUBLICATION_ERROR,
					"Error during publication for zone " + zonename + ": " + e.getMessage(), e);
		}
	}

	@Override
	public PubZoneResponse incremental(String zonename, long newSoaVersion)
			throws DnsshimProtocolException {
		PubZoneResponse response = new PubZoneResponse();
		ZoneDataPrePublish zonePrePublish = zonePrePubCache.get(zonename);
		if (zonePrePublish == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found " + zonename);
		}
		
		ZoneDataPrePublish backupPrePublish = new ZoneDataPrePublish(zonePrePublish);
		
		try {			
			synchronized (zonePrePublish) {
				newSoaVersion = publication(zonename, newSoaVersion, true);
			}
			response.setZone(zonename);
			response.setSerial(newSoaVersion);
			
			if (logger.isInfoEnabled()) {
				logger.info("Incremental publication done " +
					"(serial: " + newSoaVersion + ")");

			}
			return response;
			
		} catch (IOException ioe) {
			// Rollback
			zonePrePubCache.put(backupPrePublish);
			
			logger.info("IOException during publication: " + ioe.getMessage(), ioe);
			throw new DnsshimProtocolException(ProtocolStatusCode.PUBLICATION_ERROR, "Error during publication");
		}
	}

	private long publication(String zonename, long newSoaVersion,	boolean incremental)
			throws DnsshimProtocolException, IOException {
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zonename);
		if (zoneInfo == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.PUBLICATION_ERROR, "Error during publication");
		}

		ZoneDataPrePublish zonePrePublish = zonePrePubCache.get(zonename);

		// Update the zone's apex
		newSoaVersion = updateSoa(zonePrePublish, newSoaVersion);

		Zone zone = zonePrePublish.getAxfr();
		if (zoneInfo.isSigned()) {
			// Update DNSKEYs
			updateDnskeys(zonePrePublish);

			Set<Rrset> rrsetsToSign = new LinkedHashSet<Rrset>();
			if (!incremental) {
				zone.removeDnssecRecords();
				updateNsec(zonePrePublish);

				Set<Rrset> rrs = zone.getRrsets();

				Iterator<Rrset> it = rrs.iterator();
				while (it.hasNext()) {
					Rrset rrset = it.next();
					String ownername = rrset.getOwnername();
					RrType type = rrset.getType();
					if (type == RrType.RRSIG ||
							(type == RrType.NS && zone.isDelegation(ownername)) ||
							zone.isGlue(ownername)) {
						continue;
					}

					rrsetsToSign.add(rrset);
				}

			} else {
				updateNsec(zonePrePublish);
				rrsetsToSign = zone.getRrsetsToSign();
			}

			// Get the keys that will be signing
			List<String> zskList = new ArrayList<String>();
			for (DnskeyInfo key : zoneInfo.getKeys()) {
				if (key.getName().contains("ZSK")) {
					zskList.add(key.getName());
				}
			}

			// Do the signing
			SignServiceImpl signServiceImpl = new SignServiceImpl();
			SignData signData = getSigningData(zoneInfo);
			List<ResourceRecord> records = signServiceImpl.sign(signData,	zskList, rrsetsToSign);
			zonePrePublish.addAll(records);
		}

		// Clear the list of RRSETs to sign
		zone.clearRrsetToSign();

		// Save the zone
		ZoneData zoneData = zoneDataCache.get(zonename);
		if (zoneData == null) {
			zoneData = new ZoneData(zonename, zone.isSigned());
		}

		zoneData.setAxfr(zone);

		if (incremental) {
			ZoneIncrement ixfr = zonePrePublish.getIxfr();
			zoneData.addIxfr(ixfr);
		} else {
			// After a full publication, all the IXFR increments are discarded
			zoneData.clearIncrements();

			ZoneIncrementDao zoneIncRepository = new ZoneIncrementDao();
			zoneIncRepository.removeAll(zonename);
		}

		zoneDataCache.put(zoneData);

		// Save the ZoneDataPrePublish to disk and put it into cache
		ZoneDataPrePublish newZonePrePublish = new ZoneDataPrePublish(new Zone(zone));
		zonePrePubCache.put(newZonePrePublish);

		// Send notifies
		sendNotifies(zoneInfo);

		// Schedule resigning of the zone
		resignScheduler(zoneInfo);

		return newSoaVersion;
	}
	
	private void sendNotifies(ZoneInfo zoneInfo) {
		NotifierManager.clear(zoneInfo.getZonename());

		for (SlaveGroup slaveGroup: zoneInfo.getSlaveGroups()) {
			List<Slave> slaves = slaveGroup.getSlaves();
			for (Slave slave : slaves) {
				ZoneNotify notify = new ZoneNotify();
				notify.setZonename(zoneInfo.getZonename());
				notify.setSlaveAddress(slave.getAddress());
				notify.setSlavePort(slave.getPort());
				Random random = new Random();
				int id = random.nextInt(65534) + 1;
				notify.setQueryId((short) id);
					
				NotifierManager.addZoneNotify(notify);
			}	
		}
	}
	
	private void resignScheduler(ZoneInfo zoneInfo) {
		ZoneData zoneData = zoneDataCache.get(zoneInfo.getZonename());
		Zone zone = zoneData.getAxfr();
		Iterator<String> ownernames = zone.ownernameIterator();

		Date firstExpire = null;
		while (ownernames.hasNext()) {
			String ownername = ownernames.next();
			Rrset rrset = zone.getRrset(ownername, RrType.RRSIG, DnsClass.IN);
			Set<ResourceRecord> records = rrset.getRecords();
			
			// Get the lowest zone expiration RRSIG
			for (ResourceRecord record : records) {
				Rrsig rrsig = (Rrsig) record;
				if (firstExpire == null || rrsig.getExpiration().getTime() < firstExpire.getTime()) {
					firstExpire = rrsig.getExpiration();
				}
			}
		}
		
		if (firstExpire == null) {
			firstExpire = Calendar.getInstance().getTime();
		}
		
		zoneInfo.setExpirationDate(firstExpire);
		zoneInfo.scheduleResignDate();
		zoneInfoDao.save(zoneInfo);
	}

	private long updateSoa(ZoneDataPrePublish zoneData, long newSoaVersion)
			throws DnsshimProtocolException, IOException {
		Zone zone = zoneData.getAxfr();
		if (newSoaVersion == 0) {
			newSoaVersion = Soa.getNextSerial(zone.getSerial());
		}

		String zonename = zoneData.getZonename();
		Soa oldSoa = zone.getSoa();
		if (oldSoa == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.NO_SOA, "SOA not found in zone: " + zonename);
		}

		if (newSoaVersion <= ByteUtil.toUnsigned(oldSoa.getSerial())) {
			throw new DnsshimProtocolException(
					ProtocolStatusCode.INVALID_SOA_VERSION,	"Invalid SOA version! (new soa version: " 
							+ newSoaVersion + ", current soa version: "
							+ oldSoa.getSerial() + ")");
		}

		Soa newSoa = new Soa(oldSoa.getOwnername(), oldSoa.getDnsClass(),
				oldSoa.getTtl(), oldSoa.getMname(), oldSoa.getRname(),
				(int) newSoaVersion, oldSoa.getRefresh(),
				oldSoa.getRetry(), oldSoa.getExpire(), oldSoa.getMinimum());

		// Replace the SOA RR in the zone
		if (zoneData.remove(oldSoa) == false || zoneData.add(newSoa) == false) {
			throw new DnsshimProtocolException(
					ProtocolStatusCode.INVALID_SOA_VERSION,
					"Error updating SOA for zone:" + zonename);
		}
		return newSoaVersion;
	}

	private void updateDnskeys(ZoneDataPrePublish zoneData)
			throws IOException,	DnsshimProtocolException {
		// Remove all the DNSKEYs
		zoneData.removeRrset(ResourceRecord.APEX_OWNERNAME,
				RrType.DNSKEY, DnsClass.IN);
		
		// Add the new DNSKEYs (Sign and Publish)
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zoneData.getZonename());
		List<String> kskList = new ArrayList<String>();
		int ttl = zoneInfo.getTtlZone();
		for (DnskeyInfo key : zoneInfo.getKeys()) {
			if (key.getStatus() == DnskeyStatus.NONE) {
				continue;
			}
			
			Dnskey dnskey = new Dnskey(ResourceRecord.APEX_OWNERNAME, DnsClass.IN, ttl, key.getRdata());
			zoneData.add(dnskey);
			
			if (key.getStatus() == DnskeyStatus.SIGN && key.getName().contains("KSK")) {
				// Sign the DNSKEY RRSETs
				kskList.add(key.getName());				
			}
		}

		if (kskList.isEmpty()) {
			// No KSKs at zone
			return;
		}

		Zone zone = zoneData.getAxfr();
		Rrset dnskeyRrset =	zone.getRrset(ResourceRecord.APEX_OWNERNAME,
				RrType.DNSKEY, DnsClass.IN);
		SignServiceImpl signServiceImpl = new SignServiceImpl();
		SignData signData = getSigningData(zoneInfo);
		List<ResourceRecord> rrsigs = signServiceImpl.sign(signData, kskList,
				dnskeyRrset);

		zoneData.addAll(rrsigs);
	}

	private void updateNsec(ZoneDataPrePublish zoneData) throws DnsshimProtocolException {
		if (zoneData.isSigned() == false) {
			return;
		}

		Zone zone = zoneData.getAxfr();		
		Iterator<String> it = zone.ownernameIterator();
		String first = it.next();
		String current = first;
		boolean done = false;
		while (!done) {
			String next;
			if (it.hasNext()) {
				next = it.next();
				while (zone.isGlue(next)) {
					if (it.hasNext()) {
						next = it.next();
					} else {
						next = first;
						done = true;
					}
				}
			} else {
				next = first;
				done = true;
			}
			
			/*
			 * RFC 2845 - 2. The NSEC Resource Record The NSEC RR SHOULD have the
			 * same TTL value as the SOA minimum TTL field.
			 */
			String zonename = zoneData.getZonename();
			ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zonename);
			int ttl = zoneInfo.getSoaMinimum();

			Set<RrType> types = zone.getTypes(current);
			types.add(RrType.RRSIG);
			types.add(RrType.NSEC);
			String fqdnNext = DomainNameUtil.toFullName(next, zonename);
			Nsec nsec = new Nsec(current, DnsClass.IN, ttl, fqdnNext, types);
			
			Rrset rrset = zone.getRrset(current, RrType.NSEC, DnsClass.IN);
			boolean nsecUpdate = true;
			for (ResourceRecord rr : rrset.getRecords()) {
				if (rr.equals(nsec)) {
					nsecUpdate = false;
					break;
				}
			}
			
			if (nsecUpdate) {
				zoneData.removeRrset(current, RrType.NSEC, DnsClass.IN);
				if (zoneData.add(nsec) == false) {
					throw new DnsshimProtocolException(ProtocolStatusCode.PUBLICATION_ERROR, "Error updating NSEC chain: " + zonename);					
				}
			}
			current = next;
		}
	}
	
	private SignData getSigningData(ZoneInfo zoneInfo) {
		SignData signData = new SignData();
		signData.setZonename(zoneInfo.getZonename());
		signData.setTtlZone(zoneInfo.getTtlZone());
		signData.setTtlDnskey(zoneInfo.getTtlDnskey());
		signData.setSoaMinimum(zoneInfo.getSoaMinimum());
		Date now = new Date();
		long epoch = DateUtil.removeMillisFromEpoch(now.getTime());
		
		int expiration = zoneInfo.getValidity();
		int expHours = (expiration / 60) / 60;
		XfrdConfig config = XfrdConfigManager.getInstance();
		int lowLimit = config.getSchedulerLowLimit();
		if (lowLimit >= expHours) {
			logger.warn("Zone "+zoneInfo.getZonename()+" expiration must be higher (in hours) than scheduler low priority limit:"+lowLimit+">="+expHours);
			logger.warn("Zone "+zoneInfo.getZonename()+" expiration has been modified to "+ (lowLimit + 1) + " hours");
			int lowPriorityInSeconds = (config.getSchedulerLowLimit() + 1) * 60 * 60;
			signData.setInception((int) epoch);
			signData.setExpiration((int) (epoch + lowPriorityInSeconds));
		} else {
			signData.setInception((int) epoch);
			signData.setExpiration((int) (epoch + expiration));
		}	

		return signData;
	}
}
