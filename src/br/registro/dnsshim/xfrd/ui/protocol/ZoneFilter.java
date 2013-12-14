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
package br.registro.dnsshim.xfrd.ui.protocol;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.IoFilter;
import br.registro.dnsshim.common.server.IoSession;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneInfoDao;
import br.registro.dnsshim.xfrd.domain.User;
import br.registro.dnsshim.xfrd.domain.ZoneInfo;
import br.registro.dnsshim.xfrd.domain.logic.DnsshimSession;
import br.registro.dnsshim.xfrd.domain.logic.DnsshimSessionCache;
import br.registro.dnsshim.xfrd.util.ServerContext;

public class ZoneFilter implements IoFilter {

	@Override
	public void filter(Object message, IoSession session)
			throws DnsshimProtocolException {
		if (message instanceof ZoneOperation) {
			ZoneOperation operation = (ZoneOperation) message;
			DnsshimSessionCache sessionCache = DnsshimSessionCache.getInstance();
			DnsshimSession dnsshimSession = sessionCache.get(operation.getSessionId());
			if (dnsshimSession == null) {
				throw new DnsshimProtocolException(ProtocolStatusCode.FORBIDDEN, "forbidden");
			}
			
			User user= (User) dnsshimSession.getAttribute("user");
			if (user != null) {
				ZoneInfoDao dao = new ZoneInfoDao(ServerContext.getEntityManager());
				ZoneInfo zoneInfo = dao.findByZonename(operation.getZone());
				if (zoneInfo == null) {
					throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "Zone not found");
				}
				
				if (zoneInfo.hasUser(user) == false) {
					throw new DnsshimProtocolException(ProtocolStatusCode.FORBIDDEN, "permission denied (invalid user)");				
				}
			} else {
				throw new DnsshimProtocolException(ProtocolStatusCode.FORBIDDEN, "permission denied (invalid user)");
			}
		}
	}
}
