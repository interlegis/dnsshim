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
package br.registro.dnsshim.util;

import br.registro.dnsshim.domain.Opt;
import br.registro.dnsshim.domain.ResourceRecord;

public class ResourceRecordUtil {
	
	public static int rrLength(ResourceRecord record) {
		return rrLength("", record);
	}
	
	public static int rrLength(String zonename, ResourceRecord record) {
		int length = 0;
		
		length += DomainNameUtil.toWireFormat(record.getOwnername(), zonename).length;
		length += 2 + 2 + 4; // RrType, dnsClass, ttl
		length += record.getRdata().getRdlen() + 2; // rdata, rdlen
		
		return length;
	}
	
	public static int optLength(Opt opt) {
		int length = 0;
		length += 1; // Ownername ""
		length += 2 + 2 + 4; // RrType, UdpPayloadSize, ExtendedRcodeAndFlags
		length += opt.getRdata().getRdlen() + 2; // rdata, rdlen
		
		return length;
	}
}