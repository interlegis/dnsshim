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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import br.registro.dnsshim.util.ByteUtil;

public class ZoneIncrement {

	private String zonename;

	private Soa fromSoa;
	private Soa toSoa;
	
	private Set<ResourceRecord> addedRecords = new LinkedHashSet<ResourceRecord>();
	private Set<ResourceRecord> removedRecords = new LinkedHashSet<ResourceRecord>();

	public ZoneIncrement(String zonename) {
		this.zonename = zonename;
	}

	public ZoneIncrement(ZoneIncrement ixfr) {
		this.zonename = ixfr.getName();
		this.toSoa = ixfr.getToSoa();
		this.fromSoa = ixfr.getFromSoa();

		addedRecords = ixfr.addedRecords;
		removedRecords = ixfr.removedRecords;
	}

	public void setName(String zonename) {
		this.zonename = zonename;
	}
	
	public String getName() {
		return zonename;
	}

	public void setFromSoa(Soa from) {
		if (toSoa != null) {
			if (from.getSerial() >= toSoa.getSerial()) {
				throw new IllegalArgumentException("fromSerial ("
						+ from.getSerial() + ") >= toSerial (" + toSoa.getSerial()
						+ ")");
			}
		}

		this.fromSoa = from;
	}
	
	public Soa getFromSoa() {
		return fromSoa;
	}

	public void setToSoa(Soa to) {
		if (fromSoa != null) {
			if (ByteUtil.toUnsigned(fromSoa.getSerial()) > ByteUtil.toUnsigned(to.getSerial())) {
				throw new IllegalArgumentException("fromSerial ("
						+ fromSoa.getSerial() + ") >= toSerial (" + to.getSerial()
						+ ")");
			}
		}

		this.toSoa = to;
	}
	
	public Soa getToSoa() {
		return toSoa;
	}
		
	public void setAddedRecords(Zone addedRecords) {
		if (addedRecords == null || !addedRecords.getName().equals(zonename)) {
			throw new IllegalArgumentException();
		}
		
		for (Rrset rrset : addedRecords.getRrsets()) {
			if (rrset.getType() == RrType.SOA) {
				continue;
			}
			
			this.addedRecords.addAll(rrset.getRecords());
		}
	}
	
	public Set<ResourceRecord> getAddedRecords() {
		return Collections.unmodifiableSet(addedRecords);
	}

	public void setRemovedRecords(Zone removedRecords) {
		if (removedRecords == null || !removedRecords.getName().equals(zonename)) {
			throw new IllegalArgumentException();
		}
	
		for (Rrset rrset : removedRecords.getRrsets()) {
			if (rrset.getType() == RrType.SOA) {
				continue;
			}
			
			this.removedRecords.addAll(rrset.getRecords());
		}
	}
	
	public Set<ResourceRecord> getRemovedRecords() {
		return Collections.unmodifiableSet(removedRecords);
	}

	public boolean add(ResourceRecord record) {
		if (record == null) {
			return false;
		}

		if (removedRecords.remove(record)) {
			return true;
		} else {
			return addedRecords.add(record);
		}
		
	}

	public void addAll(Collection<ResourceRecord> records) {
		for (ResourceRecord resourceRecord : records) {
			add(resourceRecord);
		}
	}

	public boolean remove(ResourceRecord record) {
		if (record == null) {
			return false;
		}
		
		if (addedRecords.remove(record)){
			return true;
		} else {
			return removedRecords.add(record);
		}
	}
	
	public void removeAll(Collection<ResourceRecord> records) {
		for (ResourceRecord record : records) {
			remove(record);
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ZoneIncrement) {
			ZoneIncrement zoneIncrement = (ZoneIncrement) obj;
			if (this.zonename.equals(zoneIncrement.getName())){
				if (this.getFromSoa().getSerial() == zoneIncrement.getFromSoa().getSerial()) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return (zonename.hashCode() * fromSoa.getSerial() * toSoa.getSerial());
	}
	
	public boolean isEmpty() {
		if (addedRecords.isEmpty() && removedRecords.isEmpty() && fromSoa == null && toSoa == null) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public String toString() {		
		StringBuilder result = new StringBuilder();
		
		result.append("Zonename    : ").append(zonename).append(System.getProperty("line.separator"));
		result.append("From Serial : ").append(fromSoa.getSerial()).append(System.getProperty("line.separator"));
		result.append("To Serial   : ").append(toSoa.getSerial()).append(System.getProperty("line.separator"));
		result.append("Added Records   : ").append(System.getProperty("line.separator"));
		result.append(addedRecords).append(System.getProperty("line.separator"));
		result.append("Removed Records : ").append(System.getProperty("line.separator"));
		result.append(removedRecords).append(System.getProperty("line.separator"));
		
		return result.toString();
	}
}