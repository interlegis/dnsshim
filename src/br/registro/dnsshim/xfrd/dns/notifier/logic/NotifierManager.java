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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import br.registro.dnsshim.xfrd.dns.notifier.domain.ZoneNotify;

public class NotifierManager {
	private static final Logger logger = Logger.getLogger(NotifierManager.class);
	
	private static final ConcurrentHashMap<String, ZoneNotify> notifies = new ConcurrentHashMap<String, ZoneNotify>();
	
	private static Thread notifier = new Thread(new ZoneNotifier(notifies));
	
	static {
		notifier.start();
	}

	public static void shutdown() {
		notifier.interrupt();
	}
	
	public static void addZoneNotify(ZoneNotify zoneNotify) {
		if (logger.isDebugEnabled()) {
			logger.debug("Adding notify: " + zoneNotify);
		}
		
		notifies.put(zoneNotify.getZonename() + zoneNotify.getSlaveAddress(), zoneNotify);
	}

	public static void clear(String zonename) {
		for (Entry<String, ZoneNotify> entry : notifies.entrySet()) {
			ZoneNotify notify = entry.getValue();
			if (notify.getZonename().equals(zonename) && notify.getNotifySent().get() == 0) {
				notifies.remove(notify);
			}
		}
	}
}
