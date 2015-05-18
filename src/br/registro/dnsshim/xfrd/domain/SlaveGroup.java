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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import br.registro.dnsshim.domain.logic.Cacheable;
import br.registro.dnsshim.xfrd.domain.ZoneSync.Operation;

@Entity
@Table(name="slavegroup")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE, region="dnsshim")
public class SlaveGroup implements Cacheable<String> {
	
	@Id
	private String name;
	
	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name="slavegroup_slave", joinColumns=@JoinColumn(name="slavegroup", referencedColumnName="name"),
      inverseJoinColumns=@JoinColumn(name="slave", referencedColumnName="address")
      )
	@Cache(usage=CacheConcurrencyStrategy.READ_WRITE, region="dnsshim")
	private List<Slave> slaves = new ArrayList<Slave>();	
	
	@OneToMany(cascade=CascadeType.ALL, orphanRemoval=true, mappedBy="slaveGroup")
	@Cache(usage=CacheConcurrencyStrategy.READ_WRITE, region="dnsshim")
	@MapKey(name="zonename")
	private Map<String, ZoneSync> syncZones = new ConcurrentHashMap<String, ZoneSync>();
	
	private String vendor;
	
	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public List<Slave> getSlaves() {
		return new ArrayList<Slave>(slaves);
	}
	
	public boolean hasSlave(String address) {
		for (Slave slave : slaves) {
			if (slave.getAddress().equals(address))	{
				return true;
			}
		}
		
		return false;
	}
	
	public boolean addSlave(Slave slave) {
		return slaves.add(slave);
	}

	public boolean removeSlave(Slave slave) {
		return slaves.remove(slave);
	}

	public void addZone(String zonename) {
		ZoneSync sync = syncZones.get(zonename);
		if (sync != null) {
			if (sync.getOperation() == Operation.REMOVE) {
				syncZones.remove(zonename);
			}

			return;
		}
		
		sync = new ZoneSync();
		sync.setZonename(zonename);
		sync.setOperation(Operation.ADD);
		sync.setSlaveGroup(this);
		syncZones.put(zonename, sync);
	}
	
	public void removeZone(String zonename) {
		ZoneSync sync = syncZones.get(zonename);
		if (sync != null) {
			if (sync.getOperation() == Operation.ADD) {
				syncZones.remove(zonename);
			}

			return;
		}
		
		sync = new ZoneSync();
		sync.setZonename(zonename);
		sync.setOperation(Operation.REMOVE);
		sync.setSlaveGroup(this);
		syncZones.put(zonename, sync);
	}
	
	public List<ZoneSync> getSyncZones() {
		List<ZoneSync> zones = new ArrayList<ZoneSync>();
		for (Entry<String, ZoneSync> entry : syncZones.entrySet()) {
			zones.add(entry.getValue());
		}
		
		return zones;
	}
	
	public void removeZoneSync(String zonename) {
		syncZones.remove(zonename);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SlaveGroup other = (SlaveGroup) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String getCacheKey() {
		return name;
	}
}