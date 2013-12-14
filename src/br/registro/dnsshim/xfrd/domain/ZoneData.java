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
package br.registro.dnsshim.xfrd.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import br.registro.dnsshim.domain.Zone;
import br.registro.dnsshim.domain.ZoneIncrement;
import br.registro.dnsshim.domain.logic.Cacheable;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneInfoDao;
import br.registro.dnsshim.xfrd.util.ServerContext;

public class ZoneData implements Cacheable<String>{
	private String zonename;
	
	private Zone axfr;
	private Map<Long, ZoneIncrement> ixfrManager = new TreeMap<Long, ZoneIncrement>();

	public ZoneData(Zone axfr) {
		if (axfr == null) {
			throw new IllegalArgumentException();
		}
		
		this.zonename = axfr.getName();
		this.axfr = axfr;
	}
	
	public ZoneData(String zonename, boolean signed) {
		if (zonename == null) {
			throw new IllegalArgumentException();
		}

		this.zonename = zonename;
		this.axfr = new Zone(zonename, signed);
	}

	public String getZonename() {
		return zonename;
	}

	public boolean isSigned() {
		return axfr.isSigned();
	}

	public Zone getAxfr() {
		return axfr;
	}
	
	public boolean setAxfr(Zone axfr) {
		if (axfr == null || !zonename.equals(axfr.getName())) {			
			return false;
		}

		this.axfr = axfr;
		return true;
	}

	public boolean setIxfrs(Map<Long, ZoneIncrement> ixfrs) {
		if (ixfrs == null) {
			throw new IllegalArgumentException();
		}
		
		for (ZoneIncrement ixfr: ixfrs.values()) {
			if (ixfr == null || !zonename.equals(ixfr.getName())) {
				return false;
			}
		}
		
		for (ZoneIncrement ixfr: ixfrs.values()) {
			addIxfr(ixfr);
		}
		
		return true;
	}
	
	public boolean addIxfr(ZoneIncrement zoneIncrement) {
		if (zoneIncrement == null || !zonename.equals(zoneIncrement.getName())) {
			return false;
		}
		
		EntityManager entityManager = ServerContext.getEntityManager();
		ZoneInfoDao zoneInfoDao = new ZoneInfoDao(entityManager);
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zonename);
		
		int maxIncrements = (zoneInfo != null) ? zoneInfo.getMaxIncrements() : ZoneInfo.DEFAULT_MAX_INCREMENTS; 
		
		if (ixfrManager.size() < maxIncrements) {
			ixfrManager.put(ByteUtil.toUnsigned(zoneIncrement.getFromSoa().getSerial()), zoneIncrement);
		} else {
			Long serial = Collections.min(ixfrManager.keySet());
			ixfrManager.remove(serial);
			ixfrManager.put(ByteUtil.toUnsigned(zoneIncrement.getFromSoa().getSerial()), zoneIncrement);
		}
		
		return true;
	}
	
	public ZoneIncrement getIxfr(long fromSerial, long toSerial) {		
		ZoneIncrement firstIxfr = ixfrManager.get(fromSerial);
		if (ByteUtil.toUnsigned(firstIxfr.getToSoa().getSerial()) == toSerial) {
			return firstIxfr;
		} 
		
		ZoneIncrement condensedIxfr = new ZoneIncrement(firstIxfr);
		
		condensedIxfr.setFromSoa(firstIxfr.getFromSoa());
		long serial = ByteUtil.toUnsigned(condensedIxfr.getToSoa().getSerial());
		
		do {
			ZoneIncrement ixfrAux = ixfrManager.get(serial);
			
			condensedIxfr.removeAll(ixfrAux.getRemovedRecords());
			condensedIxfr.addAll(ixfrAux.getAddedRecords());
			
			serial = ByteUtil.toUnsigned(ixfrAux.getToSoa().getSerial());
			condensedIxfr.setToSoa(ixfrAux.getToSoa());
		} while (serial !=  toSerial);
		
		return condensedIxfr;
	}
	
	public Collection<ZoneIncrement> getIncrements() {
		return Collections.unmodifiableCollection(ixfrManager.values());
	}
	
	public boolean removeIxfr(ZoneIncrement zoneIncrement) {
		return ixfrManager.remove(ByteUtil.toUnsigned(zoneIncrement.getToSoa().getSerial())) != null;
	}

	public void clearIncrements() {
		ixfrManager.clear();
	}

	public boolean containsIxfrSerial(int fromSerial) {
		return ixfrManager.containsKey(ByteUtil.toUnsigned(fromSerial));
	}
	
	@Override
	public String getCacheKey() {
		return this.zonename;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		
		result.append("Zonename: ").append(zonename).append(System.getProperty("line.separator"));
		result.append("AXFR    : ").append(System.getProperty("line.separator")).append(axfr).append(System.getProperty("line.separator"));
		result.append("IXFRs   : ").append(System.getProperty("line.separator"));
		
		for (Map.Entry<Long, ZoneIncrement> pair: ixfrManager.entrySet()) {
			result.append("\t[serial = ").append(pair.getKey()).append("]").append(System.getProperty("line.separator"));
			result.append(pair.getValue()).append(System.getProperty("line.separator"));
		}
		
		return result.toString();
	}
}