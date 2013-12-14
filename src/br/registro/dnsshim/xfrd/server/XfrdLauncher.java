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
package br.registro.dnsshim.xfrd.server;

import java.io.File;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import br.registro.dnsshim.common.server.Server;
import br.registro.dnsshim.common.server.Server.TransportType;
import br.registro.dnsshim.util.DatabaseUtil;
import br.registro.dnsshim.util.HealthMonitor;
import br.registro.dnsshim.xfrd.dns.notifier.logic.NotifierManager;
import br.registro.dnsshim.xfrd.dns.server.DnsServer;
import br.registro.dnsshim.xfrd.domain.logic.DnsshimSessionCollectorManager;
import br.registro.dnsshim.xfrd.domain.logic.SlaveGroupDumperManager;
import br.registro.dnsshim.xfrd.scheduler.ResignScheduler;
import br.registro.dnsshim.xfrd.ui.server.UiServer;

public class XfrdLauncher {
	private static final Logger logger = Logger.getLogger(XfrdLauncher.class);
	public static boolean SHUTDOWN = false;
	
	public static void main(String[] args) {
		File propertiesFile = new File("log4j-xfrd.properties");
		if (propertiesFile.exists()) {
			PropertyConfigurator.configure("log4j-xfrd.properties");
		}
		
		Logger rootLogger = Logger.getRootLogger();
		if (!rootLogger.getAllAppenders().hasMoreElements()) {
			// No appenders means log4j is not initialized!
			rootLogger.setLevel(Level.INFO);
			DailyRollingFileAppender appender = new DailyRollingFileAppender();
			appender.setName("xfrd-appender");
			appender.setLayout(new PatternLayout("%d [%t] %-5p (%13F:%L) - %m%n"));
			appender.setFile("dnsshim-xfrd.log");
			appender.setDatePattern("'.'yyyy-MM-dd'.log'");
			appender.activateOptions();
			rootLogger.addAppender(appender);	
		}
		
		Thread dnsServerTcp = new Thread(new DnsServer(TransportType.TCP), "dns_server_tcp");
		dnsServerTcp.start();
		
		Thread dnsServerUdp = new Thread(new DnsServer(TransportType.UDP), "dns_server_udp");
		dnsServerUdp.start();
		
		Thread uiServer = new Thread(new UiServer(), "ui_server");
		uiServer.start();
		
		Thread resignScheduler = new Thread(new ResignScheduler(), "resign_scheduler");
		resignScheduler.start();
		SlaveGroupDumperManager.launch();
		DnsshimSessionCollectorManager.launch();
		
		while (SHUTDOWN == false) {
			try {
				Thread.sleep(5000);
				DatabaseUtil.printStatistics();
				HealthMonitor.findDeadLocks();
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}
		resignScheduler.interrupt();
		shutdown();
	}
	
	public static void shutdown() {
		logger.info("Shutting down");
		SlaveGroupDumperManager.shutdown();
		DnsshimSessionCollectorManager.shutdown();
		NotifierManager.shutdown();
		Server.SHUTDOWN = true;
	}
}
