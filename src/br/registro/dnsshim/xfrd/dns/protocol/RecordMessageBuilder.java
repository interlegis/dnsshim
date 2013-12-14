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

import java.util.ArrayList;
import java.util.List;

import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.Opt;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.RrType;
import br.registro.dnsshim.domain.Rrset;
import br.registro.dnsshim.domain.Rrsig;
import br.registro.dnsshim.domain.Zone;
import br.registro.dnsshim.util.DomainNameUtil;

public class RecordMessageBuilder extends DnsMessageBuilder {

	public RecordMessageBuilder(Opt opt) {
		this.opt = opt;
	}

	@Override
	public void process() {
		Zone axfr = getZoneData().getAxfr();
		DnsQuestion question = getRequestPacket().getQuestion();
		
		String ownername = DomainNameUtil.removeZoneFromName(question.getQname(), axfr.getName());		
		RrType type = RrType.fromValue(question.getQtype().getValue());
		DnsClass clazz = getRequestPacket().getQuestion().getQclass();
		
		// TODO: Don't support Wildcard ("*") in positive responses
		Rrset rrset = axfr.getRrset(ownername, type, clazz);
		
		boolean isAnswer = true;
		
		if (rrset.isEmpty()) {
			isAnswer = false;
			rrset = axfr.getRrset(ownername, RrType.NS, clazz);
			if (rrset.isEmpty()) { // zone isn't delegate
				rrset = axfr.getRrset(ownername, RrType.SOA, clazz);
			}
			addAllAuthorityRecords(rrset.getRecords());
		} else {
			addAllAnswerRecords(rrset.getRecords());
		}
				
		// Verifies EDNS0
		Opt opt = getRequestPacket().getOpt();
		if (opt != null && opt.isDnssecOk()) { // bit DO
			
			List<Rrset> rrsets = new ArrayList<Rrset>();
			rrsets.add(rrset);
			
			if (!isAnswer) {
				if (axfr.hasOwnername(ownername)) {
					// Prove the non-existence of a type
					rrset = axfr.getRrset(ownername, RrType.NSEC, clazz);
					rrsets.add(rrset);
					addAllAuthorityRecords(rrset.getRecords());
				} else {
					// Prove the non-existence of a name
					if (axfr.hasPreviousOwnername(ownername)) {
						String previousOwnername = axfr.previousOwnername(ownername);
						rrset = axfr.getRrset(previousOwnername, RrType.NSEC, clazz);
						rrsets.add(rrset);
						addAllAuthorityRecords(rrset.getRecords());
						
						// Prove the non-existence of a wildcard
						if (!previousOwnername.equals(ResourceRecord.APEX_OWNERNAME)) {
							rrset = axfr.getRrset(ResourceRecord.APEX_OWNERNAME, RrType.NSEC, clazz);
							rrsets.add(rrset);
							addAllAuthorityRecords(rrset.getRecords());
						}
					}
				}
			}
			
			for (Rrset rs: rrsets) {
				if (rs.isEmpty()) {
					continue;
				}
					
				Rrset rrsetRRsig = axfr.getRrset(rs.getOwnername(), RrType.RRSIG, rs.getDnsClass());
				for (ResourceRecord record : rrsetRRsig.getRecords()) {
					Rrsig rrsig = (Rrsig) record;
					if (rrsig.getTypeCovered() == rs.getType()) {
						if(isAnswer) {
							addAnswerRecord(record);
						} else {
							addAuthorityRecord(record);
						}
					}
				}	
			}
			
		}
		
		DnsPacket responsePacket = getResponsePacket();
		// If there's no record (ex. it wasn't published yet, create an empty message)
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