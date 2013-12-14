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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.domain.Zone;
import br.registro.dnsshim.domain.ZoneIncrement;
import br.registro.dnsshim.xfrd.util.ZoneParser;


public class ZoneIncrementPrePublishDao extends ZoneIncrementDao {
	
	public ZoneIncrementPrePublishDao(){
		setSuffix(".pre-publish");
		// Remove serials from file name
		setIncludeSuffixSerial(false); 
	}
	
	@Override
	protected Map<Long, ZoneIncrement> loadZoneFile(final String zonename, String path) throws IOException, DnsshimProtocolException {
		Map<Long, ZoneIncrement> response = new TreeMap<Long, ZoneIncrement>();
		
		final StringBuilder fileAdd = new StringBuilder(path);
		fileAdd.append(zonename);
		fileAdd.append(".pre-publish");
		fileAdd.append(EXTENSION);
		fileAdd.append(EXTENSION_ADD);
		
		final StringBuilder fileDel = new StringBuilder(path);
		fileDel.append(zonename);
		fileDel.append(".pre-publish");
		fileDel.append(EXTENSION);
		fileDel.append(EXTENSION_DEL);
		
		File[] files = new File[2];
		files[0] = new File(fileAdd.toString());
		files[1] = new File(fileDel.toString());
		
		ZoneParser parser = new ZoneParser();
		
		long fromSerial = 0;

		for (File file : files) {
			if (!file.exists()) {
				continue;
			}
			
			String fileName = file.getName();
			
			Zone zone = parser.parse(file, zonename);
				
			ZoneIncrement increment = response.get(fromSerial);
			if (increment == null) {
				increment = new ZoneIncrement(zonename);
			}	
				
			if (fileName.endsWith(EXTENSION_ADD)) {
				increment.setToSoa(zone.getSoa());
				increment.setAddedRecords(zone);
			} else if (fileName.endsWith(EXTENSION_DEL)) {
				increment.setFromSoa(zone.getSoa());
				increment.setRemovedRecords(zone);
			}	
				
			response.put(fromSerial, increment);
		}

		return response;
	}
}