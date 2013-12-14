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
package br.registro.dnsshim.xfrd.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.ResourceRecordFactory;
import br.registro.dnsshim.domain.RrType;
import br.registro.dnsshim.domain.Zone;

public class ZoneParser {
	private static final Logger logger = Logger.getLogger(ZoneParser.class);

	public Zone parse(File input, String zonename) throws DnsshimProtocolException {
		if (logger.isDebugEnabled()) {
			logger.debug("Parsing zone file: " + input);
		}

		Scanner scanner = null;
		try {
			scanner = new Scanner(input);
		} catch (FileNotFoundException fnfe) {
			throw new DnsshimProtocolException(ProtocolStatusCode.ZONE_NOT_FOUND, "File " + input.getName() + " not found");
		}
		
		scanner.useDelimiter(System.getProperty("line.separator"));

		Zone zone = new Zone(zonename);

		while (scanner.hasNext()) {
			String line = scanner.next();
			Scanner lineScanner = new Scanner(line);
			lineScanner.useDelimiter("\\s+");

			String ownername = lineScanner.next();

			String dnsClassStr = null;
			if ("IN".equals(ownername)) {
				dnsClassStr = ownername;
				ownername = ResourceRecord.APEX_OWNERNAME;
			} else {
				dnsClassStr = lineScanner.next();
			}

			int ttl = Integer.parseInt(lineScanner.next());
			String typeStr = lineScanner.next();
			DnsClass dnsClass = DnsClass.valueOf(dnsClassStr);
			RrType type = RrType.valueOf(typeStr);
			if (type.isDnssec()) {
				zone.setSigned(true);
			}

			lineScanner.useDelimiter(System.getProperty("line.separator"));
			String rdataStr = lineScanner.next().trim();
			if (rdataStr.trim().length() == 0) {
				lineScanner.close();
				throw new DnsshimProtocolException(ProtocolStatusCode.BAD_TSIG, "Zone file corrupted:" + zonename);
			}
			ResourceRecord rr = ResourceRecordFactory.getRr(ownername, type, dnsClass, ttl, rdataStr);
			zone.add(rr);
			lineScanner.close();
			
		}
		
		scanner.close();
		return zone;
	}
}