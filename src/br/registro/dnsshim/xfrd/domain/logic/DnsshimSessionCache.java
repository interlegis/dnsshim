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
package br.registro.dnsshim.xfrd.domain.logic;

import java.util.Date;

import br.registro.dnsshim.domain.logic.CacheStrategy;

public final class DnsshimSessionCache extends CacheStrategy<String, DnsshimSession> {
	private static DnsshimSessionCache instance;
	
	private DnsshimSessionCache() {}
	
	@Override
	public synchronized DnsshimSession get(String k) {
		DnsshimSession session = super.get(k);
		if (session != null) {
			session.setLastAccessedTime(new Date());
		}	
		return session;
	}
	
	@Override
	public synchronized void put(DnsshimSession v) {
		v.setLastAccessedTime(new Date());
		super.put(v);
	}
	
	@Override
	protected int getMaxEntriesInCache() {
		return 0; // unlimited
	}

	@Override
	protected DnsshimSession restoreValue(String k) {
		return null; // There's no persistence
	}

	@Override
	protected void storeValue(DnsshimSession v) {
		// There's no persistence
	}
	
	public static synchronized DnsshimSessionCache getInstance() {
		if (instance == null) {
			instance = new DnsshimSessionCache();
		}
		return instance;
	}

	
}
