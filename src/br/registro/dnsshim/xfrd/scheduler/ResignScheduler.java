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
package br.registro.dnsshim.xfrd.scheduler;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.util.DatabaseUtil;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneInfoDao;
import br.registro.dnsshim.xfrd.domain.XfrdConfig;
import br.registro.dnsshim.xfrd.domain.ZoneInfo;
import br.registro.dnsshim.xfrd.domain.logic.XfrdConfigManager;
import br.registro.dnsshim.xfrd.service.PublicationService;
import br.registro.dnsshim.xfrd.service.PublicationServiceImpl;
import br.registro.dnsshim.xfrd.ui.protocol.PubZoneResponse;
import br.registro.dnsshim.xfrd.util.ServerContext;

public class ResignScheduler implements Runnable {
	private static final int NUM_CPU = Runtime.getRuntime().availableProcessors();
	private static final double CPU_UTILIZATION = 0.6;
	private static final double WAIT_COMPUTE_RATIO = 90/10;
	private static final int NUM_THREADS = (int) (NUM_CPU * CPU_UTILIZATION * (1 + WAIT_COMPUTE_RATIO));
	private static final ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
	private final CompletionService<PubZoneResponse> completionService = new ExecutorCompletionService<PubZoneResponse>(executorService);
	
	private static final int SLEEP_INTERVAL = 180 * 1000; // 180 seconds
	private static final Logger logger = Logger.getLogger(ResignScheduler.class);
	
	private int firstBoundary; 
	
	public ResignScheduler() {
		XfrdConfig xfrdConfig = XfrdConfigManager.getInstance();			
		this.firstBoundary = xfrdConfig.getSchedulerHighLimit();
	}

	@Override
	public void run() {
		while (true) {
			EntityManager em = DatabaseUtil.getInstance();
			try {
				TimeZone timeZoneUTC = TimeZone.getTimeZone("UTC");
				TimeZone.setDefault(timeZoneUTC);
				Calendar calendar = Calendar.getInstance();		
				calendar.add(Calendar.HOUR, firstBoundary);
				Date highPriorityBoudary = calendar.getTime();				
				if (logger.isInfoEnabled()) {
					logger.info("Running resign scheduler with high priority="+highPriorityBoudary);
				}
								
				ZoneInfoDao zoneInfoDao = new ZoneInfoDao(em);				
				List<ZoneInfo> zoneInfos = zoneInfoDao.findAllExpired(highPriorityBoudary);
				for (ZoneInfo zoneInfo : zoneInfos) {
					completionService.submit(new Resigner(zoneInfo.getZonename()));
				}				
				
				if (Thread.interrupted()) {
					break;
				}
				
				if (Thread.interrupted()) {
					break;
				}
				
				Thread.sleep(SLEEP_INTERVAL);
			
			} catch (InterruptedException e) {
				break;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				em.close();
			}
		}

		executorService.shutdown();
	}
}

class Resigner implements Callable<PubZoneResponse> {
	private String zonename;
	private static final Logger logger = Logger.getLogger(Resigner.class);

	public Resigner(String zonename) {
		this.zonename = zonename;
	}

	@Override
	public PubZoneResponse call() {
		EntityManager entityManager = DatabaseUtil.getInstance();
		ServerContext.setEntityManager(entityManager);
		entityManager.getTransaction().begin();
		PublicationService service = new PublicationServiceImpl();
		try {
			PubZoneResponse response = service.full(zonename, 0);
			entityManager.getTransaction().commit();
			return response;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			entityManager.getTransaction().rollback();
		} catch (DnsshimProtocolException e) {
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
		return null;
	}
}
