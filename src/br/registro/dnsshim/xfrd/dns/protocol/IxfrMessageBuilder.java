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

import br.registro.dnsshim.domain.Opt;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.RrType;
import br.registro.dnsshim.domain.Soa;
import br.registro.dnsshim.domain.ZoneIncrement;
import br.registro.dnsshim.xfrd.domain.ZoneData;

public class IxfrMessageBuilder extends DnsMessageBuilder {
 
	private int fromSerial;
	
	public IxfrMessageBuilder(int fromSerial) {
		this.fromSerial = fromSerial;
	}
	
	public IxfrMessageBuilder(int fromSerial, Opt opt) {
		this.fromSerial = fromSerial;
		this.opt = opt;
	}

	@Override
	public void process() {
		ZoneData zoneData = getZoneData();
				
		// Get current SOA
		Iterator<ResourceRecord> it = zoneData.getAxfr().iterator();
		ResourceRecord record = null;
		if (it.hasNext()) {
			record = it.next();
		}
		
		if (record == null || record.getType() != RrType.SOA) {
			throw new IllegalStateException("Invalid zone (SOA not found)");
		}
		
		Soa soa = (Soa) record;
		
		// Get all the RRs in the increment
		ZoneIncrement ixfr = zoneData.getIxfr(fromSerial, soa.getSerial());
		
		addAnswerRecord(soa);
		
		Soa fromSoa = ixfr.getFromSoa();
		addAnswerRecord(fromSoa);
		Iterator<ResourceRecord> records = ixfr.getRemovedRecords().iterator();
		while (records.hasNext()) {
			addAnswerRecord(records.next());
		}
		
		Soa toSoa = ixfr.getToSoa();
		addAnswerRecord(toSoa);
		records = ixfr.getAddedRecords().iterator();
		while (records.hasNext()) {
			addAnswerRecord(records.next());
		}
		
		addAnswerRecord(soa);
		
		DnsPacket responsePacket = getResponsePacket();
		
		// If no RR is present, create an empty message
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
