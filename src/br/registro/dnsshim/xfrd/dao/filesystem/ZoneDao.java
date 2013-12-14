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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.repository.FileSystemUtil;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.Zone;
import br.registro.dnsshim.util.DomainNameUtil;
import br.registro.dnsshim.xfrd.util.ZoneParser;

public class ZoneDao {
	private final String BASE_DIR;
	private static final Logger logger = Logger.getLogger(ZoneDao.class);
	private static final String EXTENSION = ".axfr";
	private String suffix = "";

	public ZoneDao() {
		StringBuilder root = new StringBuilder();
		try {
			// {DNSSHIM_HOME|user.home}/dnsshim/xfrd/zones/SHA1#1/SHA1#2/SHA1#3/<zonename>[<suffix>].axfr
			if (System.getenv("DNSSHIM_HOME") == null) {
				root.append(System.getProperty("user.home"));
				root.append(File.separatorChar);
				root.append("dnsshim");
			} else {
				root.append(System.getenv("DNSSHIM_HOME"));
			}
			root.append(File.separatorChar);
			root.append("xfrd");
			root.append(File.separatorChar);
			root.append("zones");
			root.append(File.separatorChar);

			File directory = new File(root.toString());
			FileSystemUtil.makePath(directory);

			if (logger.isDebugEnabled()) {
				logger.debug("AXFR base dir:" + root);
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		BASE_DIR = root.toString();
	}
	
	private void loadZonesRecursively(File rootDirectory, Map<String, Zone> zones)
		throws DnsshimProtocolException {
		File[] files = rootDirectory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				loadZonesRecursively(file, zones);
			} else {
				if (file.getName().endsWith(suffix + EXTENSION)) {
					Zone zone = loadZoneFile(file);
					zones.put(zone.getName(), zone);
					if (logger.isDebugEnabled()) {
						logger.debug("Zone "+zone.getName()+" loaded");
					}
				}
			}
		}
	}

	private Zone loadZoneFile(File file) throws DnsshimProtocolException {
		if (logger.isDebugEnabled()) {
			logger.debug("Loading zone records...");
		}
		ZoneParser parser = new ZoneParser();
		String zonename = file.getName().substring(0, file.getName().lastIndexOf(suffix + EXTENSION));
		return parser.parse(file, zonename);
	}

	public Map<String, Zone> findAll() throws DnsshimProtocolException {
		if (logger.isInfoEnabled()) {
			logger.info("Loading all zones...");
		}
		
		File rootDirectory = new File(BASE_DIR);
		Map<String, Zone> zones = new LinkedHashMap<String, Zone>();
		loadZonesRecursively(rootDirectory, zones);
		return zones;
	}
	
	public Zone findByZonename(String zonename)
		throws DnsshimProtocolException, IOException {
		String zn = DomainNameUtil.trimDot(zonename);
		StringBuilder path = new StringBuilder(BASE_DIR);
		path.append(FileSystemUtil.hash(zn));
		
		File dir = new File(path.toString());
		FileSystemUtil.makePath(dir);	
		path.append(zn).append(suffix).append(EXTENSION);	
		
		File zoneFile = new File(path.toString());
		if (zoneFile.exists()) {
			Zone zone = loadZoneFile(new File(path.toString()));
			return zone;
		}
		return null;
	}
	
	private void write(Zone zone, File file) throws IOException {
		StringBuilder content = new StringBuilder();
		Iterator<ResourceRecord> iterator = zone.iterator();
		while (iterator.hasNext()) {
			ResourceRecord record = iterator.next();
			content.append(record.toString());
			content.append(System.getProperty("line.separator"));
		}
		
		if (content.length() == 0) {
			return;
		}
		
		BufferedWriter bufferedWriter = null;
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(file));
			bufferedWriter.write(content.toString());
		} finally {
			if (bufferedWriter != null) {
				bufferedWriter.close();
			}	
		}
	}
	
	public void save(Zone zone) throws IOException {
		String zn = DomainNameUtil.trimDot(zone.getName());
		StringBuilder path = new StringBuilder(BASE_DIR);
		path.append(FileSystemUtil.hash(zn));
		
		File dir = new File(path.toString());
		FileSystemUtil.makePath(dir);
		
		path.append(zn).append(suffix).append(EXTENSION);
		if (logger.isDebugEnabled()) {
			logger.debug("Saving zone:" + path);
		}
		File zoneFile = new File(path.toString());
		write(zone, zoneFile);
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}
	
	public void delete(String zonename) throws IOException {
		StringBuilder path = new StringBuilder(BASE_DIR);
		path.append(FileSystemUtil.hash(zonename));
		File dir = new File(path.toString());
		path.append(zonename).append(suffix).append(EXTENSION);
		File file = new File(path.toString());
		if (file.delete() == false) {
			logger.error("Error deleting file " + path.toString());
		}
		
		while (dir.list().length == 0) {
			File parent = dir.getParentFile();
			dir.delete();
			dir = parent;
		}
	}
}