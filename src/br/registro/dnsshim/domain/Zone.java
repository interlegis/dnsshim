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
package br.registro.dnsshim.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DomainNameUtil;

public class Zone implements Iterable<ResourceRecord> {
	private boolean signed;
	private String name;
	private int size;
	private TreeMap<String, RrList> tree;
	private Map<String, Set<String>> delegations;
	private Set<String> glues;
	private LinkedHashSet<Rrset> rrsetToSign;

	public Zone(String name, boolean signed) {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		
		this.name = DomainNameUtil.trimDot(name);
		this.tree = new TreeMap<String, RrList>(new OwnernameComparator());
		this.delegations = new TreeMap<String, Set<String>>();
		this.glues = new HashSet<String>();
		this.rrsetToSign = new LinkedHashSet<Rrset>();
		this.signed = signed;
		this.size = 0;
	}

	public Zone(String name) {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		
		this.name = DomainNameUtil.trimDot(name);
		this.tree = new TreeMap<String, RrList>(new OwnernameComparator());
		this.delegations = new TreeMap<String, Set<String>>();
		this.glues = new HashSet<String>();
		this.rrsetToSign = new LinkedHashSet<Rrset>();
		this.signed = false;
		this.size = 0;
	}

	public Zone(Zone zone) {
		if (zone == null) {
			throw new IllegalArgumentException();
		}
		
		this.name = zone.name;
		this.tree = new TreeMap<String, RrList>(new OwnernameComparator());
		this.delegations = new TreeMap<String, Set<String>>();
		this.glues = new HashSet<String>(zone.getGlues());
		this.rrsetToSign = new LinkedHashSet<Rrset>();
		this.signed = zone.signed;
		this.size = 0;		
		
		Iterator<ResourceRecord> records = zone.iterator();
		while (records.hasNext()) {
			ResourceRecord rr = records.next();
			this.add(rr);
		}
		
		rrsetToSign.clear();
		this.rrsetToSign.addAll(zone.rrsetToSign);
	}

	public Set<String> getGlues() {
		return Collections.unmodifiableSet(glues);
	}

	public void setGlues(Set<String> glues) {
		this.glues = glues;
	}

	public boolean add(ResourceRecord record) {
		if (record == null || contains(record)) {
			return false;
		}
		
		String ownername = record.getOwnername();
		ownername = DomainNameUtil.removeZoneFromName(ownername, name);
		RrType type = record.getType();
		
		RrList rrList = tree.get(ownername);
		if (rrList == null) {
			rrList = new RrList();
			rrList.insert(record);
			tree.put(ownername, rrList);
		} else {
			// CNAME record handling
			if (type == RrType.CNAME) {
				for (RrType t: rrList.getTypes()) {
					if (!t.equals(RrType.RRSIG) && !t.equals(RrType.NSEC)) {
						return false;
					}
				}
			} else {
				if (rrList.getTypes().contains(RrType.CNAME)) {
					if (!type.equals(RrType.RRSIG) && !type.equals(RrType.NSEC)) {
						return false;
					}
				}
			}
			
			if (!rrList.insert(record)) {
				return false;
			}
		}

		size++;
		
		if (type == RrType.NS &&
				!ownername.equals(ResourceRecord.APEX_OWNERNAME)) {
			Ns ns = (Ns) record;
			String nameserver = ns.getNameserver();
			Set<String> nameservers = this.delegations.get(ownername);
			if (nameservers == null) {
				nameservers = new HashSet<String>();
				this.delegations.put(ownername, nameservers);
			}
			
			nameservers.add(nameserver);
		}
		
		if (type == RrType.A || type == RrType.AAAA) {
			for (Set<String> nameservers : this.delegations.values()) {
				String fqdn = DomainNameUtil.toFullName(ownername, name);
				if (nameservers.contains(fqdn)) {
					glues.add(ownername);
					break;
				}
			}
		}
		
		if (signed == false || type == RrType.RRSIG ||
				(type == RrType.NS && this.delegations.containsKey(ownername)) ||
				this.glues.contains(ownername)) {
			// No need for signing
			return true;
		}

		Rrset rrset = getRrset(ownername, type,	record.getDnsClass());
		rrsetToSign.add(rrset);

		return true;
	}

	public boolean remove(ResourceRecord record) {
		String ownername = record.getOwnername();
		ownername = DomainNameUtil.removeZoneFromName(ownername, name);
		RrList rrList = tree.get(ownername);

		if (rrList == null || !rrList.remove(record)) {
			return false;
		}
		size--;

		if (rrList.isEmpty()) {
			tree.remove(ownername);
			return true;
		}
		
		RrType type = record.getType();
		DnsClass dnsClass = record.getDnsClass();
		if (rrList.rrsetSize(type, dnsClass) == 0) {
			if (type == RrType.NS &&
					!ownername.equals(ResourceRecord.APEX_OWNERNAME)) {
				this.delegations.remove(ownername);
			}
			
			if ((type == RrType.A || type == RrType.AAAA) &&
					this.glues.contains(ownername)) {
				this.glues.remove(ownername);
			}
			// RRSET no longer exists, so no need to resign it
			return true;
		}
		
		if (!signed || type == RrType.RRSIG ||
				(type == RrType.NS && this.delegations.containsKey(ownername))) {
			// Signing not needed
			return true;
		}

		Rrset rrset = getRrset(ownername, type, dnsClass);		
		rrsetToSign.add(rrset);
		return true;
	}

	public void removeDnssecRecords() {
		Rrset toBeRemoved = null;
		for (Map.Entry<String, RrList> entry : tree.entrySet()) {
			RrList rrList = entry.getValue();
			//remove from rrsetTosign
			toBeRemoved = rrList.get(RrType.NSEC, DnsClass.IN);
			rrsetToSign.remove(toBeRemoved);
			//remove from rrlist
			rrList.remove(RrType.NSEC, DnsClass.IN);
			
			//remove from rrsetTosign
			toBeRemoved = rrList.get(RrType.NSEC3, DnsClass.IN);
			rrsetToSign.remove(toBeRemoved);
			//remove from rrlist
			rrList.remove(RrType.NSEC3, DnsClass.IN);
			
			//remove from rrlist
			rrList.remove(RrType.RRSIG, DnsClass.IN);
		}
	}

	public int rrsetSize(String ownername, RrType type, DnsClass dnsClass) {
		RrList rrList = tree.get(ownername);
		if (rrList == null) {
			return 0;
		}
		return rrList.rrsetSize(type, dnsClass);
	}

	public Iterator<ResourceRecord> iterator() {
		return new ZoneIterator();
	}

	public boolean contains(ResourceRecord record) {
		if (record == null) {
			return false;
		}

		RrList rrList = tree.get(record.getOwnername());
		if (rrList == null) {
			return false;
		}

		return rrList.contains(record);
	}

	public Iterator<String> ownernameIterator() {
		return tree.keySet().iterator();
	}

	public boolean hasFirstOwnername() {
		return tree.keySet().iterator().hasNext();
	}
	
	public String firstOwnername() {
		return tree.keySet().iterator().next();
	}

	public boolean hasLastOwnername() {
		if (tree.isEmpty()) {
			return false;
		}
  	
		return true;
	}
	
	public String lastOwnername() {
		NavigableSet<String> n = tree.navigableKeySet();
		return n.last();
	}
	
	public boolean hasOwnername(String ownername) {
		return tree.keySet().contains(ownername);
	}
	
	public boolean hasNextOwnername(String ownername) {
		NavigableSet<String> n = tree.navigableKeySet();
		return (!n.tailSet(ownername, false).isEmpty());
	}

	public String nextOwnername(String ownername) {
		NavigableSet<String> n = tree.navigableKeySet();
		return n.tailSet(ownername, false).first();
	}

	public boolean hasPreviousOwnername(String ownername) {
		NavigableSet<String> n = tree.navigableKeySet();
		return (!n.headSet(ownername, false).isEmpty());
	}

	public String previousOwnername(String ownername) {
		NavigableSet<String> n = tree.navigableKeySet();
		return n.headSet(ownername, false).last();
	}
	
	public boolean isDelegation(String ownername) {
		return this.delegations.containsKey(ownername);
	}

	public boolean isGlue(String ownername) {
		return this.glues.contains(ownername);
	}
	
	public Set<RrType> getTypes(String ownername) {
		RrList rrList = tree.get(ownername);
		if (rrList == null) {
			return EnumSet.noneOf(RrType.class);
		} 
		
		return rrList.getTypes();
	}

	public Rrset getRrset(String ownername, RrType type, DnsClass dnsClass) {
		RrList rrList = tree.get(ownername);
		if (rrList == null) {
			return new Rrset(ownername, type, dnsClass);
		}
		
		Rrset rrset = rrList.get(type, dnsClass);
		if (rrset == null) {
			rrset = new Rrset(ownername, type, dnsClass);
		}
		
		return rrset;
	}

	public Set<Rrset> getRrsets() {	
		Set<Rrset> rrsets = new LinkedHashSet<Rrset>();
		Iterator<String> owns = ownernameIterator();
			
		// Ownername
		while (owns.hasNext()) {
			String ownername = owns.next();			
			RrList rrlist = tree.get(ownername);
			
			rrsets.addAll(rrlist.getRrsets());
		}
		
		return rrsets;
	}
	
	public Set<Rrset> getRrsetsToSign() {
		return Collections.unmodifiableSet(rrsetToSign);
	}

	public void clearRrsetToSign() {
		rrsetToSign.clear();
	}

	public boolean isEmpty() {
		return (size == 0);
	}

	public int size() {
		return size;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = DomainNameUtil.trimDot(name);
	}

	public Soa getSoa() {
		Iterator<ResourceRecord> records = iterator();
		if (records.hasNext()) {
			ResourceRecord record = records.next();
			if (record.getType() == RrType.SOA) {
				return (Soa) record;
			}
		}
		
		return null;
	}
	
	public long getSerial() {
		Soa soa = getSoa();
		if (soa == null) {
			return 0;
		}
		
		return ByteUtil.toUnsigned(soa.getSerial());
	}

	public void clear() {
		tree.clear();
		rrsetToSign.clear();
		size = 0;
	}

	public boolean isSigned() {
		return signed;
	}

	public void setSigned(boolean signed) {
		this.signed = signed;
	}
	
	@Override
	public String toString() {
		Iterator<ResourceRecord> it = iterator();

		StringBuilder result = new StringBuilder();
		while (it.hasNext()) {
			result.append(it.next().toString());
			result.append(System.getProperty("line.separator"));
		}

		return result.toString();
	}

	private class ZoneIterator implements Iterator<ResourceRecord> {
		
		private Iterator<String> ownernameIterator;
		private Iterator<ResourceRecord> records;
		private String currentOwnername;
		private ResourceRecord current;
		
		public ZoneIterator() {
			this.ownernameIterator = tree.keySet().iterator();
		}		

		@Override
		public boolean hasNext() {
			if (isEmpty()) {
				return false;
			}
			
			if (records != null && records.hasNext()) {
				return true;
			}
			
			if (ownernameIterator != null && ownernameIterator.hasNext()) {
				return true;
			}
			
			return false;
		}

		@Override
		public ResourceRecord next() {
			if (records == null) {
				if (!ownernameIterator.hasNext()) {
					throw new NoSuchElementException();
				}
				
				currentOwnername = ownernameIterator.next();
				records = tree.get(currentOwnername).iterator();				
			}
			
			if (!records.hasNext()) {
				records = null;
				return next();
			}
			
			current = records.next();
			return current;
		}

		@Override
		public void remove() {
			// TODO: Remove iterator 
		}

	};
}
