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
package br.registro.dnsshim.xfrd.service;

import java.security.NoSuchAlgorithmException;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.domain.logic.TsigCalc;
import br.registro.dnsshim.xfrd.dao.filesystem.SlaveDao;
import br.registro.dnsshim.xfrd.domain.Slave;
import br.registro.dnsshim.xfrd.domain.TsigKeyInfo;
import br.registro.dnsshim.xfrd.ui.protocol.ListTsigKeysRequest;
import br.registro.dnsshim.xfrd.ui.protocol.ListTsigKeysResponse;
import br.registro.dnsshim.xfrd.ui.protocol.NewTsigKeyRequest;
import br.registro.dnsshim.xfrd.ui.protocol.NewTsigKeyResponse;
import br.registro.dnsshim.xfrd.ui.protocol.RemoveTsigKeyRequest;
import br.registro.dnsshim.xfrd.ui.protocol.RemoveTsigKeyResponse;
import br.registro.dnsshim.xfrd.util.ServerContext;

public class TsigKeyServiceImpl implements TsigKeyService {
	private static final Logger logger = Logger.getLogger(TsigKeyServiceImpl.class);
	private SlaveDao slaveDao;
	
	public TsigKeyServiceImpl() {
		EntityManager entityManager = ServerContext.getEntityManager();
		slaveDao = new SlaveDao(entityManager);
	}

	@Override
	public NewTsigKeyResponse newTsigKey(NewTsigKeyRequest request)
		throws DnsshimProtocolException {
		String host = request.getHost();
		Slave slave = slaveDao.findByAddress(host);
		if (slave == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.SLAVE_NOT_FOUND, 
			"Slave not found");
		}
		
		byte[] key = new byte[0];
		try {
			key = TsigCalc.generateKey();
		} catch (NoSuchAlgorithmException nsae) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_ALGORITHM, 
					"Unknown TSIG algorithm while generating key");
		}
		
		String secret = new String(key);
		String keyName = request.getKeyName();
		TsigKeyInfo tsig = new TsigKeyInfo(keyName, secret);
		slave.addTsigKey(tsig);
		slaveDao.save(slave);
		
		if (logger.isInfoEnabled()) {
			logger.info("New TSIG key " + keyName + " created for host " + host);
		}
		
		NewTsigKeyResponse response = new NewTsigKeyResponse();
		response.setHost(host);
		response.setKeyName(keyName);
		response.setSecret(secret);
		return response;
	}

	@Override
	public RemoveTsigKeyResponse removeTsigKey(RemoveTsigKeyRequest request)
		throws DnsshimProtocolException {
		String host = request.getHost();
		Slave slave = slaveDao.findByAddress(host);
		if (slave == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.SLAVE_NOT_FOUND, 
			"Slave not found");
		}

		String key = request.getKeyName();
		if (slave.removeTsigKey(key) == false) {
			throw new DnsshimProtocolException(ProtocolStatusCode.BAD_TSIG, "Host " + host + 
					" has no TSIG key named " + key);
		}
		slaveDao.save(slave);
		
		if (logger.isInfoEnabled()) {
			logger.info("TSIG key " + key + " removed from host " + host);
		}
		RemoveTsigKeyResponse response = new RemoveTsigKeyResponse();
		return response;
	}

	@Override
	public ListTsigKeysResponse listTsigKeys(ListTsigKeysRequest request)
		throws DnsshimProtocolException {
		String host = request.getHost();
		Slave slave = slaveDao.findByAddress(host);
		if (slave == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.SLAVE_NOT_FOUND, "Slave not found");
		}
	
		ListTsigKeysResponse response = new ListTsigKeysResponse();
		response.setHost(host);
		response.setKeys(slave.getTsigKeys());
		return response;
	}
}
