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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.xfrd.domain.logic.XfrdConfigManager;

@Entity
@Table(name="zone")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE, region="dnsshim")
public class ZoneInfo {	
	public static final String DEFAULT_ZONENAME = "";
	public static final int DEFAULT_TTL_ZONE = 86400;
	public static final int DEFAULT_TTL_DNSKEY = 86400;
	public static final int DEFAULT_SOA_MINIMUM = 900;
	public static final int DEFAULT_VALIDITY = 30 * 24 * 60 * 60; // 1 month
	public static final int MIN_EXPIRATION = 2 * 86400; // 2 days
	public static final int MAX_EXPIRATION = 5 * 31536000; // 5 years
	public static final int DEFAULT_MAX_INCREMENTS = 5;
	
	public static final boolean DEFAULT_SIGNED = true;
	public static final boolean DEFAULT_VERIFY_SIGNATURE = false;
	
	@Id
	private String zonename = DEFAULT_ZONENAME;
	
	private int ttlZone = DEFAULT_TTL_ZONE;
	private int ttlDnskey = DEFAULT_TTL_DNSKEY;
	private int soaMinimum = DEFAULT_SOA_MINIMUM;
	private int validity = DEFAULT_VALIDITY;
	
	private int maxIncrements = DEFAULT_MAX_INCREMENTS; 
	
	@Column(nullable = false)
	@Type(type = "org.hibernate.type.NumericBooleanType")
	private boolean signed = DEFAULT_SIGNED;

	@Column(nullable = false)
	@Type(type = "org.hibernate.type.NumericBooleanType")
	private boolean verifySignature = DEFAULT_VERIFY_SIGNATURE;
	
	@Column(nullable = false)
	private Date expirationDate;
	
	@Column(nullable = false)
	private Date resignDate;
	
	@OneToMany(cascade=CascadeType.ALL, orphanRemoval=true, mappedBy="zone")
	@Cache(usage=CacheConcurrencyStrategy.READ_WRITE, region="dnsshim")
	private List<DnskeyInfo> keys;
	
	@ManyToMany
	@JoinTable(name="zone_slavegroup", joinColumns=@JoinColumn(name="zonename", referencedColumnName="zonename"),
      inverseJoinColumns=@JoinColumn(name="slavegroup", referencedColumnName="name")
      )
	@Cache(usage=CacheConcurrencyStrategy.READ_WRITE, region="dnsshim")
	private List<SlaveGroup> slaveGroups;
	
	@ManyToMany
	@JoinTable(name="zone_user", joinColumns=@JoinColumn(name="zonename", referencedColumnName="zonename"),
        inverseJoinColumns=@JoinColumn(name="username", referencedColumnName="username")
        )
	@Cache(usage=CacheConcurrencyStrategy.READ_WRITE, region="dnsshim")
	private List<User> users;
	
	public ZoneInfo() {
		keys = new ArrayList<DnskeyInfo>();
		slaveGroups = new ArrayList<SlaveGroup>();
		users = new ArrayList<User>();
		Calendar cal = Calendar.getInstance();
		resignDate = cal.getTime();
		expirationDate = cal.getTime();
	}

	public String getZonename() {
		return zonename;
	}

	public void setZonename(String zonename) {
		this.zonename = zonename;
	}

	public int getTtlZone() {
		return ttlZone;
	}

	public void setTtlZone(int ttlZone) {
		this.ttlZone = ttlZone;
	}

	public int getTtlDnskey() {
		return ttlDnskey;
	}

	public void setTtlDnskey(int ttlDnskey) {
		this.ttlDnskey = ttlDnskey;
	}

	public int getSoaMinimum() {
		return soaMinimum;
	}

	public void setSoaMinimum(int soaMinimum) {
		this.soaMinimum = soaMinimum;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public int getValidity() {
		return validity;
	}

	public void setValidity(int validity) {
		if (validity < 0 || validity > MAX_EXPIRATION) {
			this.validity = MAX_EXPIRATION;
			return;
		}
		
		if (validity < MIN_EXPIRATION) {
			this.validity = MIN_EXPIRATION;
			return;
		}

		this.validity = validity;
	}

	public void setMaxIncrements(int maxIncrements) {
		if (maxIncrements < 0) {
			throw new IllegalArgumentException("Invalid value for MaxIncrements!");
		}
		
		this.maxIncrements = maxIncrements;
	}

	public int getMaxIncrements() {
		return maxIncrements;
	}
	
	public boolean isSigned() {
		return signed;
	}

	public void setSigned(boolean signed) {
		this.signed = signed;
	}

	public boolean isVerifySignature() {
		return verifySignature;
	}

	public void setVerifySignature(boolean verifySignature) {
		this.verifySignature = verifySignature;
	}
	
	public List<DnskeyInfo> getKeys() {
		return new ArrayList<DnskeyInfo>(keys);
	}
	
	public DnskeyInfo getKey(short keyTag) {
		String keyTagStr = "" + ByteUtil.toUnsigned(keyTag);
		for (DnskeyInfo key : keys) {
			if (key.getName().contains(keyTagStr)) {
				return key;
			}
		}
		
		return null;
	}

	public boolean hasSlave(String address) {
		for (SlaveGroup sg : slaveGroups) {
			if (sg.hasSlave(address))	{
				return true;
			}
		}
		
		return false;
	}
	
	public List<SlaveGroup> getSlaveGroups() {
		return slaveGroups;
	}

	public void assignSlaveGroup(SlaveGroup group) {
		this.slaveGroups.add(group);
	}
	
	public boolean unassignSlaveGroup(SlaveGroup group) {
		return slaveGroups.remove(group);
	}
	
	public void addUser(User user) {
		this.users.add(user);
	}
	
	public void removeUser(User user) {
		this.users.remove(user);
	}
	
	public boolean hasUser(User user) {
		if (users.contains(user)) {
			return true;
		}
		return false;
	}
		
	public List<User> getUsers() {
		return users;
	}

	public Date getResignDate() {
		return resignDate;
	}

	public void setResignDate(Date resignDate) {
		this.resignDate = resignDate;
	}
	
	public void scheduleResignDate() {
		XfrdConfig xfrdConfig = XfrdConfigManager.getInstance();
		
		int firstBoundary = xfrdConfig.getSchedulerHighLimit();
		int secondBoundary = xfrdConfig.getSchedulerLowLimit();

		Calendar cal = Calendar.getInstance();

		Date now = cal.getTime();
		cal.add(Calendar.HOUR, firstBoundary);
		Date highPriorityBoudary = cal.getTime();
		
		long highPriorityMark = expirationDate.getTime() - highPriorityBoudary.getTime();

		cal.setTime(now);
		cal.add(Calendar.HOUR, secondBoundary);
		Date lowPriorityBoundary = cal.getTime();
		long lowPriorityMark = expirationDate.getTime() - lowPriorityBoundary.getTime(); 
		
		long interval = lowPriorityMark + (long) (Math.random() * (highPriorityMark - lowPriorityMark));
		
		cal.setTimeInMillis(now.getTime() + interval);
		this.resignDate = cal.getTime();
		if (resignDate.getTime() < now.getTime()) {
			this.resignDate = highPriorityBoudary;
		}
	}

	public DnskeyInfo getKey(String keyname) {
		for (DnskeyInfo key : keys) {
			if (key.getName().equals(keyname)) {
				return key;
			}
		}
		
		return null;
	}
	
	public boolean hasKey(String keyname) {
		if (getKey(keyname) == null) {
			return false;
		}
		
		return true;
	}
	
	public boolean hasSigningKey() {
		for (DnskeyInfo key : keys) {
			if (key.getStatus() == DnskeyStatus.SIGN) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean addKey(DnskeyInfo key) {
		if (hasKey(key.getName()) == true) {
			return false;
		}
		
		keys.add(key);
		return true;
	}
	
	public boolean removeKey(DnskeyInfo key) {
		return keys.remove(key);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ZoneInfo) {
			ZoneInfo z = (ZoneInfo) obj;
			
			if (z.zonename.equals(zonename) && 
					z.ttlZone == ttlZone && 
					z.ttlDnskey == ttlDnskey && 
					z.soaMinimum == soaMinimum && 
					z.validity == validity && 
					
					z.maxIncrements == maxIncrements &&
					
					z.signed == signed &&
					z.verifySignature == verifySignature &&
					
					z.keys.equals(keys) &&
					
					z.slaveGroups.equals(slaveGroups) && 
					z.users.equals(users)) {
				return true;
			}
		}
		
		return false;
	}

}
