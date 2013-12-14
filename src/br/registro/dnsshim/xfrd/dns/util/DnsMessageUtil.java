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
package br.registro.dnsshim.xfrd.dns.util;

import br.registro.dnsshim.domain.Opt;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.Tsig;
import br.registro.dnsshim.util.DomainNameUtil;
import br.registro.dnsshim.util.ResourceRecordUtil;
import br.registro.dnsshim.xfrd.dns.protocol.DnsHeader;
import br.registro.dnsshim.xfrd.dns.protocol.DnsPacket;
import br.registro.dnsshim.xfrd.dns.protocol.DnsQuestion;

public class DnsMessageUtil {

	public static int packetLength(String zonename, DnsPacket message) {

		int length = packetLengthWithoutTsig(zonename, message);
		
		Tsig tsig = message.getTsig();
		if (tsig != null) {
			length += ResourceRecordUtil.rrLength(tsig);
		}
		
		return length;
	}
	
	public static int packetLengthWithoutTsig(String zonename, DnsPacket message) {
		int length = 0;
		
		length += DnsHeader.HEADER_SIZE;
		length += questionLength(message.getQuestion());
		
		for (ResourceRecord record: message.getAnswer()) {
			length += ResourceRecordUtil.rrLength(zonename, record);
		}
		
		for (ResourceRecord record: message.getAuthority()) {
			length += ResourceRecordUtil.rrLength(zonename, record);
		}

		for (ResourceRecord record: message.getAdditional()) {
			length += ResourceRecordUtil.rrLength(zonename, record);
		}
		
		Opt opt = message.getOpt();		
		if (opt != null) {
			length += ResourceRecordUtil.optLength(opt);
		}
		
		return length;
	}
	
	public static int questionLength(DnsQuestion question) {
		int length = 0;
		
		length += DomainNameUtil.toWireFormat(question.getQname()).length;
		length += 2 + 2; // DnsQueryType, DnsClass
		
		return length;
	}
}
