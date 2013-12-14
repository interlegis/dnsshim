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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name="slave")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE, region="dnsshim")
public class Slave {
	@Id
	private String address;
	private int port;
	
	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name="address")
	@Cache(usage=CacheConcurrencyStrategy.READ_WRITE, region="dnsshim")
	private List<TsigKeyInfo> tsigKeys = new ArrayList<TsigKeyInfo>();

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public TsigKeyInfo getTsigKey(String keyname) {
		for (TsigKeyInfo tsig : tsigKeys) {
			if (tsig.getName().equals(keyname)) {
				return tsig;
			}
		}
		
		return null;
	}
	
	public boolean hasTsig() {
		if (tsigKeys.isEmpty()) {
			return false;
		}
		
		return true;
	}

	public void addTsigKey(TsigKeyInfo tsig) {
		tsigKeys.add(tsig);
	}
	
	public boolean removeTsigKey(String keyname) {
		TsigKeyInfo tsig = getTsigKey(keyname);
		if (tsig == null) {
			return false;
		}
		
		return tsigKeys.remove(tsig);
	}
	
	public Map<String, String> getTsigKeys() {
		Map<String, String> keys = new HashMap<String, String>();
		for (TsigKeyInfo tsig : tsigKeys) {
			keys.put(tsig.getName(), tsig.getSecret());
		}
		
		return keys;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Slave other = (Slave) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (port != other.port)
			return false;
		return true;
	}
	
	
}
