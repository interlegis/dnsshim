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

import java.util.Calendar;
import java.util.Date;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

public class DnsshimSessionCollector implements Runnable {
	private DnsshimSessionCache cache = DnsshimSessionCache.getInstance();
	private static final Logger logger = Logger.getLogger(DnsshimSessionCollector.class);
	
	@Override
	public void run() {
		Calendar c = Calendar.getInstance();
		Date now = new Date();
		for (Entry<String, DnsshimSession> entry : cache.entrySet()) {
			DnsshimSession session = entry.getValue();
			c.setTime(session.getLastAccessedTime());
			c.add(Calendar.MINUTE, 30); // session timeout = 30min
			if (now.getTime() > c.getTime().getTime()) {
				if (logger.isInfoEnabled()) {
					logger.info("Session "+ session.getCacheKey() + " has expired");
				}
				cache.remove(session.getCacheKey());
			}
		}
		
	}

}
