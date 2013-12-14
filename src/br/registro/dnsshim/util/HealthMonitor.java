package br.registro.dnsshim.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.apache.log4j.Logger;

public class HealthMonitor {
	private static final Logger logger = Logger.getLogger(HealthMonitor.class);

	public static void findDeadLocks() {
		ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
		long[] ids = tmx.findDeadlockedThreads();
		if (ids != null) {
			ThreadInfo[] infos = tmx.getThreadInfo(ids, true, true);
			logger.fatal("The following threads are deadlocked:");
			for (ThreadInfo ti : infos) {
				logger.fatal(ti.toString());
			}
		}
	}
}
