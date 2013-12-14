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

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

public class RrList implements Iterable<ResourceRecord>{
	
	private Map<RrType, Map<DnsClass, Rrset>> records = new TreeMap<RrType, Map<DnsClass, Rrset>>(new RrTypeComparator());
	private int size;
	
	public boolean insert(ResourceRecord record) {
		RrType rrType = record.getType();
		
		Map<DnsClass, Rrset> classes = records.get(rrType);
		if (classes == null) {
			classes = new TreeMap<DnsClass, Rrset>();
			records.put(rrType, classes);
		}
		
		DnsClass dnsClass = record.getDnsClass();
		Rrset rrset = classes.get(dnsClass);
		if (rrset == null) {
			rrset = new Rrset(record.getOwnername(), rrType, dnsClass);
			classes.put(dnsClass, rrset);
		}

		boolean status = rrset.add(record); 
		if(status) {
			size++;
		}
		return status;
	}

	public int remove(RrType rrType, DnsClass dnsClass) {
		Map<DnsClass, Rrset> classes = records.get(rrType);
		if (classes == null) {
			return 0;	
		}
		
		Rrset rrset = classes.get(dnsClass);
		if (rrset == null) {
			return 0;
		}
		
		int size = rrset.size();
		classes.remove(dnsClass);
		this.size -= size;
		
		if (classes.isEmpty()) {
			records.remove(rrType);
		}
		
		return size;
	}

	public boolean remove(ResourceRecord record) {
		Map<DnsClass, Rrset> classes = records.get(record.getType());
		if (classes == null) {
			return false;	
		}
		
		Rrset rrset = classes.get(record.getDnsClass());
		if (rrset == null) {
			return false;
		}
		
		boolean status = rrset.remove(record);
		if (status) {
			size--;
		}
		
		if (rrset.isEmpty()) {
			classes.remove(record.getDnsClass());
		}
		
		if (classes.isEmpty()) {
			records.remove(record.getType());
		}
		
		return status;
	}

	public boolean contains(ResourceRecord record) {
		Map<DnsClass, Rrset> classes = records.get(record.getType());
		if (classes == null) {
			return false;	
		}
		
		Rrset rrset = classes.get(record.getDnsClass());
		if (rrset == null) {
			return false;
		}
		
		return rrset.contains(record);
	}

	public Rrset get(RrType type, DnsClass dnsClass) {
		Map<DnsClass, Rrset> classes = records.get(type);
		if (classes == null) {
			return null;	
		}
		
		return classes.get(dnsClass);
	}

	public Set<RrType> getTypes() {
		if (records.isEmpty()) {
			return EnumSet.noneOf(RrType.class);
		} else {
			return EnumSet.copyOf(records.keySet());
		}
	}

	public Set<DnsClass> getClasses(RrType type) {
		Map<DnsClass, Rrset> classes = records.get(type);
		if (classes == null || classes.isEmpty()) {
			return EnumSet.noneOf(DnsClass.class);
		}
		return EnumSet.copyOf(classes.keySet());
	}
	
	public int rrsetSize(RrType rrType, DnsClass dnsClass) {
		Map<DnsClass, Rrset> classes = records.get(rrType);
		if (classes == null) {
			return 0;	
		}
		
		Rrset rrset = classes.get(dnsClass);
		if (rrset == null) {
			return 0;
		}
	
		return rrset.size();
	}
	
	public Set<Rrset> getRrsets() {
		Set<Rrset> rrsets = new LinkedHashSet<Rrset>();
		for (Map.Entry<RrType, Map<DnsClass, Rrset>> type : records.entrySet()) {
			for (Map.Entry<DnsClass, Rrset> entry : type.getValue().entrySet()) {
				rrsets.add(entry.getValue());
			}
		}
		return rrsets;
	}

	public boolean isEmpty() {
		for (RrType type : records.keySet()) {
			if (type != RrType.RRSIG && type != RrType.NSEC) {
				return false;
			}
		}
		
		return true;
	}
	
	public int size() {
		return this.size;
	}

	@Override
	public Iterator<ResourceRecord> iterator() {
		return new RrListIterator();
	}
	
	private class RrListIterator implements Iterator<ResourceRecord> {
		private Iterator<RrType> types;
		private Iterator<DnsClass> classes;
		private Iterator<ResourceRecord> rrs;
		
		private RrType currentType;
		private DnsClass currentDnsClass;		
		
		private ResourceRecord current;
		
		public RrListIterator() {
			// Type
			if (types == null) {
				types = records.keySet().iterator();
			}
			
		}
		
		@Override
		public boolean hasNext() {
			if (rrs != null && rrs.hasNext()) {
				return true;
			}
			
			if (classes != null && classes.hasNext()) {
				return true;
			}
			
			if (types != null && types.hasNext()) {
				return true;
			}
			
			return false;
		}
		
		private RrType getNextType() {
			if (types == null) {
				types = records.keySet().iterator();
			}
			
			if (!types.hasNext()) {
				currentType = null;
				return currentType;
			}
		
			currentType = types.next();
			return currentType;
		}
				
		private DnsClass getNextDnsClass() {
			if (classes == null) {
				RrType type = getNextType();
				if (type == null) {
					currentDnsClass = null;
					return currentDnsClass;
				}
				
				classes = records.get(type).keySet().iterator();
			}
			
			if (!classes.hasNext()) {
				classes = null;
				currentDnsClass = null;
				return getNextDnsClass();
			}
			
			currentDnsClass = classes.next();
			return currentDnsClass;
		}
		
		private ResourceRecord getNextRecord() {
			if (rrs == null) {
				DnsClass dnsClass = getNextDnsClass();
				if (dnsClass == null) {
					throw new NoSuchElementException();
				}
				
				rrs = records.get(currentType).get(currentDnsClass).getRecords().iterator();
			}
			
			if (!rrs.hasNext()) {
				rrs = null;
				return getNextRecord();
			}
			
			current = rrs.next();
			return current;
		}		
		
		@Override
		public ResourceRecord next() {
			return getNextRecord();			
		}	

		@Override
		public void remove() {
			if (current != null && currentType != null && currentDnsClass != null) {
				records.get(currentType).get(currentDnsClass).remove(current);
			}
		}
		
	}

}
