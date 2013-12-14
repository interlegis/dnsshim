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
import java.util.Iterator;
import java.util.Set;

import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.Ns;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.RrType;
import br.registro.dnsshim.domain.Rrset;
import br.registro.dnsshim.domain.Rrsig;
import br.registro.dnsshim.domain.Soa;
import br.registro.dnsshim.domain.Zone;
import br.registro.dnsshim.domain.ZoneIncrement;
import br.registro.dnsshim.domain.logic.Cacheable;
import br.registro.dnsshim.xfrd.domain.logic.XfrdConfigManager;

public class ZoneDataPrePublish implements Cacheable<String> {
	private String zonename;
	
	private Zone axfr;
	private ZoneIncrement ixfr;
	private XfrdConfig xfrdConfig = XfrdConfigManager.getInstance();
	
	public ZoneDataPrePublish(Zone axfr) {
		if (axfr == null) {
			throw new IllegalArgumentException("Empty (NULL pointer) AXFR");
		}
		
		this.zonename = axfr.getName();
		this.axfr = axfr;
		
		this.ixfr = new ZoneIncrement(zonename);
	}
	
	public ZoneDataPrePublish(Zone axfr, ZoneIncrement ixfr) {
		if (axfr == null || ixfr == null) {
			throw new IllegalArgumentException("Empty (NULL pointer) AXFR or IXFR");
		}
		
		if (axfr.getName().equals(ixfr.getName()) == false) {
			throw new IllegalArgumentException("AXFR and IXFR don't match");
		}
		
		this.zonename = axfr.getName();
		this.axfr = axfr;
		this.ixfr = ixfr;
	}
	
	public ZoneDataPrePublish(ZoneDataPrePublish zonePrePublish) {
		if (zonePrePublish == null) {
			throw new IllegalArgumentException();
		}
		
		this.zonename = zonePrePublish.getAxfr().getName();
		this.axfr = new Zone(zonePrePublish.getAxfr());
		
		this.ixfr = new ZoneIncrement(zonePrePublish.getIxfr());
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
	
	public ZoneIncrement getIxfr() {
		return ixfr;
	}
	
	public void addAll(Collection<ResourceRecord> records) {
		for (ResourceRecord record: records) {
			add(record);
		}
	}
	
	public boolean add(ResourceRecord rr) {
		boolean firstNs = !axfr.getTypes(rr.getOwnername()).contains(RrType.NS);
		if (!axfr.add(rr)) {
			return false;
		}
		
		RrType type = rr.getType();
		if (rr.getType() == RrType.SOA) {
			ixfr.setToSoa((Soa)rr);
		} else {
			ixfr.add(rr);
		}
		
		if (rr.getType() == RrType.NS && xfrdConfig.getOverwriteMname()) {
			Ns ns = ((Ns) rr);
			if (firstNs) {
				axfr.getSoa().setMname(ns.getNameserver());	
			}
		}
		
		if (!isSigned() || type == RrType.RRSIG) {
			return true;
		}
		
		String ownername = rr.getOwnername();
		if (axfr.rrsetSize(ownername, type, rr.getDnsClass()) > 1) {
			// Added a new RR to an existing RRSET:
			// Need to remove the current RRSIG
			Rrset rrsigRrset = axfr.getRrset(ownername, RrType.RRSIG,
					DnsClass.IN);
			ResourceRecord[] rrsigs =
				rrsigRrset.getRecords().toArray(new ResourceRecord[0]);
			for (ResourceRecord record : rrsigs) {
				Rrsig rrsig = (Rrsig) record;
				if (rrsig.getTypeCovered() == type) {
					remove(rrsig);
				}
			}
		}
		
		return true;
	}

	public void removeAll(Collection<ResourceRecord> records) {
		ResourceRecord[] rrs = records.toArray(new ResourceRecord[0]);
		for (ResourceRecord record : rrs) {
			remove(record);
		}
	}
	
	public void removeRrset(String ownername, RrType type, DnsClass dnsClass) {
		Rrset rrset = axfr.getRrset(ownername, type, dnsClass);
		ResourceRecord[] rrs = rrset.getRecords().toArray(new ResourceRecord[0]);
		for (ResourceRecord rr : rrs) {
			remove(rr);
		}
	}
	
	public boolean remove(ResourceRecord rr) {
		String ownername = rr.getOwnername();
		RrType type = rr.getType();
		Rrset nsecRrset = axfr.getRrset(ownername, RrType.NSEC,
				DnsClass.IN);
		Rrset rrsigRrset = axfr.getRrset(ownername, RrType.RRSIG,
				DnsClass.IN);
		if(type == RrType.SOA) {
			ixfr.setFromSoa((Soa)rr);
		} else {
			ixfr.remove(rr);
		}
		
		if (!axfr.remove(rr) &&	type != RrType.RRSIG &&	type != RrType.NSEC) {
			return false;
		}
				
		if (!isSigned() || type == RrType.RRSIG) {
			return true;
		}
		
		// Name no longer in the zone: remove the NSEC
		boolean nsecRemoved = false;
		if (type != RrType.NSEC && !axfr.hasOwnername(ownername)) {
			Set<ResourceRecord> nsecs = nsecRrset.getRecords();
			Iterator<ResourceRecord> it = nsecs.iterator();
			if (it.hasNext()) {
				ResourceRecord record = it.next();
				remove(record);
				nsecRemoved = true;
			}
		}

		// Remove the RRSIG
		ResourceRecord[] rrsigs =
			rrsigRrset.getRecords().toArray(new ResourceRecord[0]);
		for (ResourceRecord record : rrsigs) {
			Rrsig rrsig = (Rrsig) record;
			if (rrsig.getTypeCovered() == type) {
				remove(rrsig);
			}

			if (nsecRemoved && rrsig.getTypeCovered() == RrType.NSEC) {
				remove(rrsig);
			}
		}

		return true;
	}

	@Override
	public String getCacheKey() {
		return this.zonename;
	}
}
