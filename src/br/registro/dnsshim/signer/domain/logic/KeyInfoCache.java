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
package br.registro.dnsshim.signer.domain.logic;

import java.io.IOException;

import org.apache.log4j.Logger;

import br.registro.dnsshim.domain.logic.CacheException;
import br.registro.dnsshim.domain.logic.CacheStrategy;
import br.registro.dnsshim.signer.dao.filesystem.KeyInfoDao;
import br.registro.dnsshim.signer.domain.KeyInfo;
import br.registro.dnsshim.signer.repository.KeyInfoRepository;

public class KeyInfoCache extends CacheStrategy<String, KeyInfo>{
	private static final Logger logger = Logger.getLogger(KeyInfoCache.class);
	private KeyInfoRepository repository = new KeyInfoDao();
	private static KeyInfoCache instance; 
	
	@Override
	protected int getMaxEntriesInCache() {
		return 1000;
	}

	@Override
	protected KeyInfo restoreValue(String k) {
		try {
			return repository.findByName(k);
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new CacheException(e.getMessage());
		}
	}

	@Override
	protected void storeValue(KeyInfo v) {
		try {
			repository.save(v);
		}catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new CacheException(e.getMessage());
		}
	}
	
	public static synchronized KeyInfoCache getInstance() {
		if (instance == null) {
			instance = new KeyInfoCache();
		}
		return instance;
	}
}