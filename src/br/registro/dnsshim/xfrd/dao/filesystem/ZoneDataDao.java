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
package br.registro.dnsshim.xfrd.dao.filesystem;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.domain.Zone;
import br.registro.dnsshim.domain.ZoneIncrement;
import br.registro.dnsshim.util.DomainNameUtil;
import br.registro.dnsshim.xfrd.domain.ZoneData;

public class ZoneDataDao {
	private static final Logger logger = Logger.getLogger(ZoneDataDao.class);
	private ZoneDao zoneDao = new ZoneDao();
	private ZoneIncrementDao zoneIncrementDao = new ZoneIncrementDao(); 

	public void save(ZoneData zoneData) throws IOException {
		String zonename = zoneData.getZonename();
		
		if (logger.isDebugEnabled()) {
			logger.debug("Saving ZoneData " + zonename);
		}
		
		Zone axfr = zoneData.getAxfr();
		zoneDao.save(axfr);
				
		Collection<ZoneIncrement> ixfr = zoneData.getIncrements();
		for (ZoneIncrement increment : ixfr) {
			zoneIncrementDao.save(increment);
		}	
		
		zoneIncrementDao.verifySizeLimitIncrements(zonename);
	}

	public void delete(String zonename) throws IOException {
		zoneDao.delete(zonename);
		zoneIncrementDao.removeAll(zonename);
	}

	public ZoneData findByZonename(String zonename)
		throws DnsshimProtocolException, IOException {
		String zn = DomainNameUtil.trimDot(zonename);
		if (logger.isDebugEnabled()) {
			logger.debug("Finding " + zn + " ZoneData");
		}
		
		Zone axfr = zoneDao.findByZonename(zonename);
		if (axfr == null) {
			return null;
		}

		ZoneData zoneData = new ZoneData(axfr);

		Map<Long, ZoneIncrement> ixfrs = zoneIncrementDao.findByZonename(zonename);
		zoneData.setIxfrs(ixfrs);

		return zoneData;
	}
}