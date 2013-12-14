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
package br.registro.dnsshim.xfrd.dns.protocol;

import java.util.Iterator;

import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.Soa;

public class AxfrMessageBuilder extends DnsMessageBuilder {
	
	@Override
	public void process() {
		// Iterate thru all Records in the zone
		Iterator<ResourceRecord> it = getZoneData().getAxfr().iterator();
		Soa soa = null;
		while (it.hasNext()) {
			ResourceRecord rr = it.next();
			// First RR must be the SOA
			if (soa == null) {
				soa = (Soa) rr;
			}

			addAnswerRecord(rr);
		}

		// SOA at the end
		addAnswerRecord(soa);
		
		DnsPacket responsePacket = getResponsePacket();
		// If there's no record (ex. wasn't published yet, create a new empty message)
		if (responsePacket == null) {
			responsePacket = createNewPacket(true);
		}
		
		DnsHeader header = responsePacket.getHeader();
		header.setAnswerCount((short) responsePacket.getAnswer().size());
		header.setAuthorityCount((short) responsePacket.getAuthority().size());
		header.setAdditionalCount((short) responsePacket.getAdditional().size());
		
		addResponsePacket(responsePacket);
	}
}
