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

import java.io.IOException;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.domain.logic.CacheException;
import br.registro.dnsshim.domain.logic.CacheStrategy;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneDataPrePublishDao;
import br.registro.dnsshim.xfrd.domain.XfrdConfig;
import br.registro.dnsshim.xfrd.domain.ZoneDataPrePublish;

public class ZoneDataPrePublishCache extends CacheStrategy<String, ZoneDataPrePublish>{
	private static final Logger logger = Logger.getLogger(ZoneDataPrePublishCache.class);
	private final ZoneDataPrePublishDao dao = new ZoneDataPrePublishDao();
	
	private static ZoneDataPrePublishCache instance;

	private ZoneDataPrePublishCache() { /* private constructor. use getInstance */ }
	
	@Override
	protected int getMaxEntriesInCache() {
		XfrdConfig xfrdConfig = XfrdConfigManager.getInstance();
		return xfrdConfig.getCacheSize();
	}

	@Override
	protected ZoneDataPrePublish restoreValue(String k) {
		try {
			return dao.findByZonename(k);
		} catch (DnsshimProtocolException dpe) {
			logger.error(dpe.getMessage(), dpe);
			throw new CacheException(dpe.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new CacheException(e.getMessage());
		}
	}

	@Override
	protected void storeValue(ZoneDataPrePublish v) {
		try {
			dao.save(v);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new CacheException(e.getMessage());
		}
	}
	
	public static synchronized ZoneDataPrePublishCache getInstance() {
		if (instance == null) {
			instance = new ZoneDataPrePublishCache();
		}
		
		return instance;
	}
	

}
