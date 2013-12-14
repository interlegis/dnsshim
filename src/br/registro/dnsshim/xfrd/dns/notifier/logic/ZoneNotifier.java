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
package br.registro.dnsshim.xfrd.dns.notifier.logic;

import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.xfrd.dns.notifier.client.NotifierClient;
import br.registro.dnsshim.xfrd.dns.notifier.domain.ZoneNotify;
import br.registro.dnsshim.xfrd.dns.protocol.DnsHeader;
import br.registro.dnsshim.xfrd.dns.protocol.DnsMessage;
import br.registro.dnsshim.xfrd.domain.XfrdConfig;
import br.registro.dnsshim.xfrd.domain.logic.XfrdConfigManager;

public final class ZoneNotifier implements Runnable {
	private static final int NUM_CPU = Runtime.getRuntime().availableProcessors();
	private static final double CPU_UTILIZATION = 0.6;
	private static final double WAIT_COMPUTE_RATIO = 90/10;
	private static final int NUM_THREADS = (int) (NUM_CPU * CPU_UTILIZATION * (1 + WAIT_COMPUTE_RATIO));
	private static final ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
	
	private static final Logger logger = Logger.getLogger(ZoneNotifier.class);

	private final CompletionService<ZoneNotify> completionService = new ExecutorCompletionService<ZoneNotify>(executorService);

	private final ConcurrentHashMap<String, ZoneNotify> notifies;

	public ZoneNotifier(ConcurrentHashMap<String, ZoneNotify> notifies) {
		this.notifies = notifies;
	}

	@Override
	public void run() {

		XfrdConfig xfrdConfig = XfrdConfigManager.getInstance();		
		
		while (true) {
			try {
				if (logger.isInfoEnabled()) {
					logger.info("Sleeping "+xfrdConfig.getNotifyRetryInterval()+" seconds");
				}
				
				if (Thread.interrupted()) {
					break;
				}

				Thread.sleep(xfrdConfig.getNotifyRetryInterval() * 1000); // miliseconds
				
				if (logger.isInfoEnabled()) {
					logger.info("Sending notifies.");
				}
				
				for (Entry<String, ZoneNotify> entry : notifies.entrySet()) {
					ZoneNotify notify = entry.getValue();
					if (notify.getNotifySent().incrementAndGet() > xfrdConfig.getNotifyMaxRetries()) {
						// Put back into the queue for retry later
						notifies.remove(entry.getKey());
						continue;
					}
					
					completionService.submit(new Notifier(entry.getValue()));
				}
				
				int count = notifies.size();
				if (logger.isInfoEnabled()) {
					logger.info("Notifies to send: " + count);
				}
				
				int successCount = 0;
				int failCount = count;
				for (int i = 0; i < count; i++) {
					Future<ZoneNotify> future = completionService.poll(xfrdConfig.getNotifyTimeout(), TimeUnit.SECONDS);
					if (future == null) {
						if (logger.isInfoEnabled()) {
							logger.info("Notify timeout.");
						}
						break;
					}
					
					try {
						ZoneNotify zoneNotify = future.get();
						if (notifies.remove(zoneNotify.getZonename() + zoneNotify.getSlaveAddress()) == null) {
							if (logger.isInfoEnabled()) {
								logger.info("Cannot remove notify from queue! Zone:"+	zoneNotify.getZonename()+ " " +
										"slave:"+zoneNotify.getSlaveAddress()+ " " +
										"query id:" + ByteUtil.toUnsigned(zoneNotify.getQueryId()) + " " +
										"Notify queue size=" + notifies.size());
							}
						} else {
							successCount++;
							failCount--;
						}
					} catch (ExecutionException e) {
						failCount++;
						logger.error(e.getMessage(), e);
					}
				}
				
				logger.info("Successful notifies: " + successCount);
				logger.info("Failed notifies: " + failCount);
			} catch (InterruptedException e) {
				break;
			}
		}
		
		executorService.shutdown();
	}
}

class Notifier implements Callable<ZoneNotify> {
	private static final Logger logger = Logger.getLogger(Notifier.class);

	private final ZoneNotify zoneNotify;

	public Notifier(ZoneNotify notify) {
		this.zoneNotify = notify;
	}

	@Override
	public ZoneNotify call() throws Exception {
		NotifierClient client = new NotifierClient();
		DnsMessage request = client.buildNotifyMessage(zoneNotify.getZonename());
		if (logger.isInfoEnabled()) {
			logger.info("Sending notify #"+zoneNotify.getNotifySent()+
					" for zone " + zoneNotify.getZonename() +
					" at " + zoneNotify.getSlaveAddress() + ":" +	zoneNotify.getSlavePort() +
					" query id = " + ByteUtil.toUnsigned(zoneNotify.getQueryId()));
		}
		
		// Set the query id
		request.getPackets().get(0).getHeader().setId(zoneNotify.getQueryId());
		DnsMessage message = client.sendNotify(request,	zoneNotify.getSlaveAddress(), zoneNotify.getSlavePort());
		DnsHeader dnsHeader = message.getFirstPacket().getHeader();
		if (logger.isInfoEnabled()) {
			logger.info("Got notify response for zone "+zoneNotify.getZonename() +
					" query id = " +	ByteUtil.toUnsigned(zoneNotify.getQueryId()) + 
					" response code = " + dnsHeader.getResponseCode());
		}
		return zoneNotify;
	}
}
