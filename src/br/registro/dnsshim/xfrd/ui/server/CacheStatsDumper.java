package br.registro.dnsshim.xfrd.ui.server;

import br.registro.dnsshim.util.DatabaseUtil;
import br.registro.dnsshim.xfrd.domain.logic.DnsshimSessionCache;
import br.registro.dnsshim.xfrd.domain.logic.ZoneDataCache;
import br.registro.dnsshim.xfrd.domain.logic.ZoneDataPrePublishCache;

public class CacheStatsDumper implements Runnable {

	@Override
	public void run() {
		while (true) {
			ZoneDataCache zoneDataCache = ZoneDataCache.getInstance();
			zoneDataCache.logStats();
			
			ZoneDataPrePublishCache zoneDataPrePublishCache = ZoneDataPrePublishCache.getInstance();
			zoneDataPrePublishCache.logStats();
			
			DnsshimSessionCache sessionCache = DnsshimSessionCache.getInstance();
			sessionCache.logStats();
			
			DatabaseUtil.printStatistics();
			
			if (Thread.interrupted()) {
				break;
			}
			
			try {
				Thread.sleep(60 * 1000);// 1 minute
			} catch (InterruptedException e) {
				break;
			}
		}
	}
}
