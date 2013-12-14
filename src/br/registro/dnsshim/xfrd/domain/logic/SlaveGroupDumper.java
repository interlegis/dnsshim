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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import br.registro.dnsshim.util.DatabaseUtil;
import br.registro.dnsshim.xfrd.dao.filesystem.SlaveGroupDao;
import br.registro.dnsshim.xfrd.domain.Slave;
import br.registro.dnsshim.xfrd.domain.SlaveGroup;
import br.registro.dnsshim.xfrd.domain.XfrdConfig;

public class SlaveGroupDumper implements Runnable {
	private static final Logger logger = Logger.getLogger(SlaveGroupDumper.class);
	private static final String osName = System.getProperty("os.name").toLowerCase();
	
	@Override
	public void run() {
		EntityManager entityManager = DatabaseUtil.getInstance();
		try {
			if (logger.isInfoEnabled()) {
				logger.info("Starting slaves synchronization.");
			}

			XfrdConfig xfrdConfig = XfrdConfigManager.getInstance();

			if (osName.contains("windows")) {
				logger.warn("Cannot synchronize slaves using Windows platform.");
				return;
			}

			if (!xfrdConfig.isSlaveSyncEnabled()) {
				if (logger.isInfoEnabled()) {
					logger.info("DNSSHIM will not synchronize slaves.");
				}
				return;
			}

			entityManager.getTransaction().begin();
			SlaveGroupDao slaveGroupDao = new SlaveGroupDao(entityManager);			
			SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
			String now = df.format(new Date());

			for (SlaveGroup slaveGroup : slaveGroupDao.findAll()) {
				slaveGroupDao.saveZoneSyncCommand(slaveGroup, now);	

				// calling slavesync script
				for (Slave slave: slaveGroup.getSlaves()) {
					String hostAddress = slave.getAddress();
					StringBuilder command = new StringBuilder();
					if (slave.hasTsig() == false) {
						logger.warn("Server " + hostAddress	+ " has no TSIG KEY.");
						continue;
					}

					String path = slaveGroupDao.getBaseDir();
					String addedFile = path + slaveGroup.getName() + SlaveGroupDao.ADDED_COMMAND_EXTENSION;
					String removedFile = path + slaveGroup.getName() + SlaveGroupDao.REMOVED_COMMAND_EXTENSION;
					command.append(xfrdConfig.getSlaveSyncPath());
					command.append(' ');
					command.append(addedFile);
					command.append(' ');
					command.append(removedFile);
					command.append(' ');
					command.append(hostAddress);
					command.append(' ');
					command.append(xfrdConfig.getRndcPort());
					command.append(' ');
					command.append(xfrdConfig.getRndcPath());
					command.append(' ');
					command.append(now);

					logger.debug("Executing SlaveSync: " + command);
					ExecThread e = new ExecThread(command.toString());
					e.start();
				}
			}

			entityManager.getTransaction().commit();
			logger.info("SlaveGroup dump finished");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			entityManager.getTransaction().rollback();
		} finally {
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			
			if (entityManager.isOpen()) {
				entityManager.close();
			}
		}
	}
}
