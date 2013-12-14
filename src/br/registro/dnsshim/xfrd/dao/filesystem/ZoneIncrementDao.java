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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.repository.FileSystemUtil;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.Zone;
import br.registro.dnsshim.domain.ZoneIncrement;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DomainNameUtil;
import br.registro.dnsshim.xfrd.domain.ZoneData;
import br.registro.dnsshim.xfrd.domain.ZoneInfo;
import br.registro.dnsshim.xfrd.domain.logic.ZoneDataCache;
import br.registro.dnsshim.xfrd.util.ServerContext;
import br.registro.dnsshim.xfrd.util.ZoneParser;

public class ZoneIncrementDao {
	protected final String BASE_DIR;
	private static final Logger logger = Logger.getLogger(ZoneIncrementDao.class);
	
	protected static final String EXTENSION = ".ixfr";
	protected static final String EXTENSION_ADD = "-add";
	protected static final String EXTENSION_DEL = "-del";
	protected String suffix = "";
	protected boolean includeSuffixSerial = true;
	
	public ZoneIncrementDao() {
		StringBuilder root = new StringBuilder();
		try {
			// {DNSSHIM_HOME|user.home}/dnsshim/xfrd/zones/SHA1#1/SHA1#2/SHA1#3/<zonename>-<fromID>-<toID>[<suffix>].ixfr
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
				logger.debug("IXFR base dir:" + root);
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		BASE_DIR = root.toString();
	}
	
	protected Map<Long, ZoneIncrement> loadZoneFile(final String zonename, String path)
		throws IOException, DnsshimProtocolException {
		if (logger.isDebugEnabled()) {
			logger.debug("Loading zone increment records...");
		}

		Map<Long, ZoneIncrement> response = new TreeMap<Long, ZoneIncrement>();
		
		final StringBuilder extensionAdd = new StringBuilder();
		extensionAdd.append(EXTENSION);
		extensionAdd.append(EXTENSION_ADD);
		
		final StringBuilder extensionDel = new StringBuilder();
		extensionDel.append(EXTENSION);
		extensionDel.append(EXTENSION_DEL);
		
		File dir = new File(path);
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(zonename) && !name.contains("pre") &&
						(name.endsWith(extensionAdd.toString()) || name.endsWith(extensionDel.toString()));
			}
		});
		
		ZoneParser parser = new ZoneParser();
		
		long fromSerial = 0;

		for (File file : files) {
			String fileName = file.getName();
			String splices[] = fileName.split("\\.");

			if (splices.length < 2) {
				throw new IOException("Incorrect name format (file: " + fileName + ")");
			}
				
			String part = splices[splices.length - 2];
				
			String serials[] = part.split("\\-");
			if (serials.length != 2) {
				throw new IOException("Incorrect name format(file: " + fileName + ")");
			}
			
			fromSerial = Long.parseLong(serials[0]);

			Zone zone = parser.parse(file, zonename);
				
			ZoneIncrement increment = response.get(fromSerial);
			if (increment == null) {
				increment = new ZoneIncrement(zonename);
			}	
				
			if (fileName.endsWith(extensionAdd.toString())) {
				increment.setToSoa(zone.getSoa());
				increment.setAddedRecords(zone);
			} else if (fileName.endsWith(extensionDel.toString())) {
				increment.setFromSoa(zone.getSoa());
				increment.setRemovedRecords(zone);
			}	
				
			response.put(fromSerial, increment);
		}

		return response;
	}
	
	public Map<Long, ZoneIncrement> findByZonename(String zonename)
		throws IOException, DnsshimProtocolException {
		String zn = DomainNameUtil.trimDot(zonename);
		StringBuilder path = new StringBuilder(BASE_DIR);
		path.append(FileSystemUtil.hash(zn));
		
		return loadZoneFile(zonename, path.toString());
	}
	
	public void save(ZoneIncrement zoneIncrement) throws IOException {
		String zonename = DomainNameUtil.trimDot(zoneIncrement.getName());
		StringBuilder path = new StringBuilder(BASE_DIR);
		path.append(FileSystemUtil.hash(zonename));
		path.append(zonename);
		
		if (includeSuffixSerial) {
			path.append('.');
			path.append(ByteUtil.toUnsigned(zoneIncrement.getFromSoa().getSerial()));
			path.append('-');
			path.append(ByteUtil.toUnsigned(zoneIncrement.getToSoa().getSerial()));
		}
		
		path.append(suffix);
		
		path.append(EXTENSION);
		
		File[] files = new File[2];
		files[0] = new File(path.toString() + EXTENSION_DEL);
		files[1] = new File(path.toString() + EXTENSION_ADD);
		
		if (zoneIncrement.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug(zoneIncrement.getName() + " increment is empty...");
			}
			
			for (File file : files) {
				if (file.exists()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Trying to delete " + files[0]);
					}
					//  If the increment is empty and the file already exists, delete it.
					if (!file.delete()) {
						logger.warn("Cannot delete file:" + file);
					}
				}
			}	
			return;
		}
		
		Iterator<ResourceRecord> iterator;
		
		for (int i = 0; i < files.length; i++) {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(files[i]));
			
			if (i == 0) {
				if (zoneIncrement.getFromSoa() != null) {
					bufferedWriter.write(zoneIncrement.getFromSoa().toString());
					bufferedWriter.write(System.getProperty("line.separator"));
				}	
				iterator = zoneIncrement.getRemovedRecords().iterator();
			} else {
				if (zoneIncrement.getToSoa() != null) {
					bufferedWriter.write(zoneIncrement.getToSoa().toString());
					bufferedWriter.write(System.getProperty("line.separator"));	
				}
				iterator = zoneIncrement.getAddedRecords().iterator();
			}
			
			while (iterator.hasNext()) {
				ResourceRecord record = iterator.next();
				bufferedWriter.write(record.toString());
				bufferedWriter.write(System.getProperty("line.separator"));
			}

			bufferedWriter.close();
		}
	}
	
	public void verifySizeLimitIncrements(String zonename) throws IOException {		
		ZoneDataCache zoneDataCache = ZoneDataCache.getInstance();
		EntityManager entityManager = ServerContext.getEntityManager();
		ZoneInfoDao zoneInfoDao = new ZoneInfoDao(entityManager);
		
		ZoneData zoneData = zoneDataCache.get(zonename);
		ZoneInfo zoneInfo = zoneInfoDao.findByZonename(zonename);

		if (zoneData != null && zoneInfo != null) {
			Collection<ZoneIncrement> ixfrs = zoneData.getIncrements();
			
			StringBuilder path = new StringBuilder(BASE_DIR);
			path.append(FileSystemUtil.hash(zonename));
			path.append(zonename);

			path.append('.');

			while (ixfrs.size() > zoneInfo.getMaxIncrements()) {
				Iterator<ZoneIncrement> it = ixfrs.iterator();
				if (it.hasNext()) {
					ZoneIncrement ixfr = it.next();

					// Removes from memory structure
					zoneData.removeIxfr(ixfr);

					// Removes from file system
					StringBuilder filePath = new StringBuilder(path);
					filePath.append(ByteUtil.toUnsigned(ixfr.getFromSoa().getSerial()));
					filePath.append('-');
					filePath.append(ByteUtil.toUnsigned(ixfr.getToSoa().getSerial()));

					filePath.append(suffix);
					filePath.append(EXTENSION);

					File[] files = new File[2];
					files[0] = new File(filePath.toString() + EXTENSION_ADD);
					files[1] = new File(filePath.toString() + EXTENSION_DEL);
					
					for (File file : files) {
						if (file.exists()) {
							if (logger.isDebugEnabled()) {
								logger.debug("Trying to delete increment file:" + file);
							}
							if (!file.delete()) {
								logger.warn("Cannot delete increment file:"	+ file);
							}
						}
					}					
				}
			}
		}
	}
	
	
	/**
	 * Doesn't remove <b>pre-publish</b> increments
	 */
	public void remove(ZoneIncrement ixfr) throws IOException {
		String zonename = DomainNameUtil.trimDot(ixfr.getName());
		
		ZoneDataCache zoneDataCache = ZoneDataCache.getInstance();
		ZoneData zoneData = zoneDataCache.get(zonename);
		
		// Removes from memory structure
		zoneData.removeIxfr(ixfr);

		// Removes from file system
		StringBuilder path = new StringBuilder(BASE_DIR);
		path.append(FileSystemUtil.hash(zonename));
		path.append(zonename);
		
		path.append('.');
		path.append(ByteUtil.toUnsigned(ixfr.getFromSoa().getSerial()));
		path.append('-');
		path.append(ByteUtil.toUnsigned(ixfr.getToSoa().getSerial()));

		path.append(suffix);
		path.append(EXTENSION);
		
		File[] files = new File[2];
		files[0] = new File(path.toString() + EXTENSION_ADD);
		files[1] = new File(path.toString() + EXTENSION_DEL);
		
		for (File file : files) {
			if (file.exists()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Trying to delete increment file:" + file);
				}
				if (!file.delete()) {
					logger.warn("Cannot delete increment file:" + file);
				}
			}
		}					
	}
	
	
	/**
	 * Doesn't remove <b>pre-publish</b> increments
	 */
	public void removeAll(final String zonename) throws IOException {
		String zi = DomainNameUtil.trimDot(zonename);
		StringBuilder path = new StringBuilder(BASE_DIR);
		path.append(FileSystemUtil.hash(zi));
		
		File ixfrBaseDir = new File(path.toString());
		
		final StringBuilder extensionAdd = new StringBuilder();
		extensionAdd.append(EXTENSION);
		extensionAdd.append(EXTENSION_ADD);
		
		final StringBuilder extensionDel = new StringBuilder();
		extensionDel.append(EXTENSION);
		extensionDel.append(EXTENSION_DEL);
		
		File[] files = ixfrBaseDir.listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(zonename) && (name.endsWith(extensionAdd.toString()) || name.endsWith(extensionDel.toString()));
			}
			
		});
		for (File fileIxfr: files) {
			if (logger.isDebugEnabled()) {
				logger.debug("Trying to delete increment file:"	+ fileIxfr);
			}
			if (!fileIxfr.delete()) {
				logger.warn("Cannot delete increment file:"	+ fileIxfr);
			}
		}		
	}
	
	
	public String getSuffix() {
		return suffix;
	}

	
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public boolean isIncludeSuffixSerial() {
		return includeSuffixSerial;
	}

	public  void setIncludeSuffixSerial(boolean includeSuffixSerial) {
		this.includeSuffixSerial = includeSuffixSerial;
	}
}
