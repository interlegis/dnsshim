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

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

public class Rrset implements Comparable<Rrset> {
	
	private String ownername;
	private RrType type;
	private DnsClass dnsClass;
	private int ttl; 

	private Set<ResourceRecord> records = new TreeSet<ResourceRecord>();

	public Rrset(String ownername, RrType type, DnsClass dnsClass) {
		if (ownername == null || type == null || dnsClass == null) {
			throw new IllegalArgumentException();
		}
		
		this.ownername = ownername;
		this.type = type;
		this.dnsClass = dnsClass;
	}

	public String getOwnername() {
		return ownername;
	}

	public RrType getType() {
		return type;
	}

	public DnsClass getDnsClass() {
		return dnsClass;
	}

	public int getTtl() {
		return ttl;
	}

	public Set<ResourceRecord> getRecords() {
		return records;
	}

	/**
	 * Insere o resource record no conjunto somente se tiver o mesmo tipo,
	 * ownername e nao existir nenhum record igual no mesmo
	 * 
	 * @param record
	 * @return true se inserir e false se nao inserir
	 */
	public boolean add(ResourceRecord record) {
		if (record != null && ownername.equals(record.getOwnername()) && 
				record.getDnsClass() == this.dnsClass && 
				record.getType() == this.type) {

			if (records.isEmpty()) {
				this.ttl = record.getTtl();
			}	
			
			if (record.getType() != RrType.RRSIG) {
				// Only RRSIGs can have different TTL values within the RRSET
				record.setTtl(ttl); 
			}
			
			return records.add(record);
		}

		return false;
	}
	
	public void addAll(Collection<ResourceRecord> c) {
		for (ResourceRecord record : c) {
			add(record);
		}	
	}
	
	public boolean remove(ResourceRecord record) {
		boolean rem = records.remove(record); 
		if (records.isEmpty()) {
			this.ttl = 0;
		}
		return rem;
	}
	
	public boolean contains(ResourceRecord resourceRecord) {
		return records.contains(resourceRecord);
	}
	
	public ResourceRecord first() {
		if (records.isEmpty()) {
			throw new NoSuchElementException();
		}
		
		return records.iterator().next();
	}
	
	public boolean isApex() {
		return ResourceRecord.APEX_OWNERNAME.equals(ownername);
	}
	
	public boolean isEmpty() {
		return records.isEmpty();
	}
	
	public int size() {
		return records.size();
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Ownername: [" + ownername + "] - ");
		result.append("Type: [" + type + "] - ");
		result.append("Class: [" + dnsClass + "] ");
		result.append(System.getProperty("line.separator"));
		
		for (ResourceRecord record: records) {
			result.append(record).append(System.getProperty("line.separator"));
		}
		
		result.append(System.getProperty("line.separator"));
		return result.toString();
	}

	/**
	 * <b>Only</b> compare ownername, type and (dns)class. 
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Rrset) {
			Rrset rrset = (Rrset) obj;

			if (ownername.equals(rrset.ownername) && 
				type == rrset.type && dnsClass == rrset.dnsClass) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return (ownername.hashCode() + type.hashCode() + dnsClass.hashCode()) * 31;
	}
	
	@Override
	public int compareTo(Rrset o) {
		int c = ownername.compareTo(o.ownername);
		if (c == 0) {
			c = type.compareTo(o.type);
			if (c == 0) {
				c = dnsClass.compareTo(o.dnsClass);
			}
		}

		return c;
	}
	
}
