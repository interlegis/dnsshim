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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.repository.FileSystemUtil;
import br.registro.dnsshim.xfrd.domain.SlaveGroup;
import br.registro.dnsshim.xfrd.domain.XfrdConfig;
import br.registro.dnsshim.xfrd.domain.ZoneSync;
import br.registro.dnsshim.xfrd.domain.ZoneSync.Operation;
import br.registro.dnsshim.xfrd.domain.logic.XfrdConfigManager;

public class SlaveGroupDao {
	private static final Logger logger = Logger.getLogger(SlaveGroupDao.class);

	public enum ExtensionType { REMOVED_COMMAND, REMOVED_ERROR, ADDED_COMMAND, ADDED_ERROR};
	
	private final String BASE_DIR;

	private EntityManager entityManager;

	public SlaveGroupDao(EntityManager em) {
		entityManager = em;
		
		StringBuilder root = new StringBuilder();
		try {
			// {DNSSHIM_HOME|user.home}/dnsshim/conf/xfrd/slaves/<groupname>.slaves
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
			root.append("conf");
			root.append(File.separatorChar);
			root.append("slaves");
			root.append(File.separatorChar);

			File directory = new File(root.toString());
			FileSystemUtil.makePath(directory);

		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		BASE_DIR = root.toString();
	}
	
	public String getFileExtension(SlaveGroup slaveGroup, ExtensionType extensionType) {
		if (extensionType == ExtensionType.ADDED_ERROR) {
			return ".added." + slaveGroup.getVendor() + ".err";
		} else if (extensionType == ExtensionType.ADDED_COMMAND) {
			return ".added." + slaveGroup.getVendor();
		} else if (extensionType == ExtensionType.REMOVED_ERROR) {
			return ".removed." + slaveGroup.getVendor() + ".err";
		} else if (extensionType == ExtensionType.REMOVED_COMMAND) {
			return ".removed." + slaveGroup.getVendor();
		}
		
		return "";
	}

	public void save(SlaveGroup slaveGroup) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Saving SlaveGroup...");
		}

		entityManager.persist(slaveGroup);
	}

	@SuppressWarnings("unchecked")
	public List<SlaveGroup> findAll() throws IOException {
		String sql = "SELECT s FROM SlaveGroup s";
		Query query = entityManager.createQuery(sql);
		return query.getResultList();
	}
	
	public SlaveGroup findMostIdleGroup() throws IOException {
		List<SlaveGroup> groups = findAll();
		String slavegroupName = "";
		int slavegroupCount = Integer.MAX_VALUE;
		for (SlaveGroup g : groups) {
			Query query = entityManager.createNativeQuery("SELECT COUNT(*) FROM zone_slavegroup WHERE slavegroup = :groupname").setParameter("groupname", g.getName());
			BigInteger result = (BigInteger) query.getSingleResult();
			
			int count;
			if (result == null) {
				count = 0;
			} else {
				count = Integer.parseInt(result.toString());	
			}
			
			if (count < slavegroupCount) {
				slavegroupCount = count;
				slavegroupName = g.getName();
			}
			
			if (logger.isDebugEnabled()) {
				logger.debug("Slavegroup: " + g.getName() + " has " + count + " zones");
			}
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("The most idle slavegroup is " + slavegroupName + " with " + slavegroupCount + " zones");
		}

		if ("".equals(slavegroupName)) {
			return null;
		}
		
		return findByName(slavegroupName);
	}

	public SlaveGroup findByName(String name) throws IOException {
		SlaveGroup slaveGroup = entityManager.find(SlaveGroup.class, name);
		return slaveGroup;
	}

	public void delete(String name) throws IOException {
		entityManager.remove(entityManager.getReference(SlaveGroup.class, name));
	}

	private String buildFilePath(String groupName, String extension) {
		StringBuilder path = new StringBuilder(BASE_DIR);
		path.append(groupName);
		path.append(extension);
		return path.toString();
	}

	private void parseAddedZoneFile(SlaveGroup slaveGroup) throws IOException {
		String path = buildFilePath(slaveGroup.getName(), getFileExtension(slaveGroup, ExtensionType.ADDED_ERROR));

		File file = new File(path);
		if (file.exists() == false) {
			return;
		}

		File tmp = new File(file.getAbsolutePath() + ".tmp");
		file.renameTo(tmp);

		BufferedReader reader = new BufferedReader(new FileReader(tmp));
		String line;
		while ((line = reader.readLine()) != null) {
			String zone = line.trim();
			if (zone.isEmpty()) {
				continue;
			}
			slaveGroup.addZone(zone);
		}

		reader.close();
		file.delete();
		tmp.delete();
	}

	private void parseRemovedZoneFile(SlaveGroup slaveGroup)
			throws IOException {
		String path = buildFilePath(slaveGroup.getName(), getFileExtension(slaveGroup, ExtensionType.REMOVED_ERROR));
		File file = new File(path);
		if (file.exists() == false) {
			return;
		}

		File tmp = new File(file.getAbsolutePath() + ".tmp");
		file.renameTo(tmp);

		BufferedReader reader = new BufferedReader(new FileReader(tmp));
		String line;
		while ((line = reader.readLine()) != null) {
			String zone = line.trim();
			if (zone.isEmpty()) {
				continue;
			}
			slaveGroup.removeZone(zone);
		}

		reader.close();
		file.delete();
		tmp.delete();
	}

	public void saveZoneSyncCommand(SlaveGroup slaveGroup, String timestamp)
			throws IOException {
		loadSyncErrors(slaveGroup);

		InetAddress localhost = InetAddress.getLocalHost();
		if (localhost.isLoopbackAddress()) {
			logger.warn("Could not determine local IP address. Using loopback");
		}
		
		XfrdConfig xfrdConfig = XfrdConfigManager.getInstance();
		String port = String.valueOf(xfrdConfig.getDnsServerConfig().getPort());

		StringBuilder addedZones = new StringBuilder();
		StringBuilder removedZones = new StringBuilder();
		List<ZoneSync> syncZones = slaveGroup.getSyncZones();

		for (ZoneSync zoneSync : syncZones) {
			String zonename = zoneSync.getZonename();
			if (zoneSync.getOperation() == Operation.ADD) {
				if (slaveGroup.getVendor().equals("bind")) {
					addedZones.append(zonename);
					addedZones.append(" '{ type slave; masters { ");
					addedZones.append(localhost.getHostAddress());
					addedZones.append(" port ");
					addedZones.append(port);
					addedZones.append("; }; file \"dnsshim");
					addedZones.append(File.separatorChar);
					addedZones.append(FileSystemUtil.hash(zonename));
					addedZones.append(zonename);
					addedZones.append("\"; };'");
					addedZones.append(System.getProperty("line.separator"));
				} else if (slaveGroup.getVendor().equals("nsd")) {
					addedZones.append(zonename);
					addedZones.append(" ");
					addedZones.append(FileSystemUtil.hash(zonename));
					addedZones.append(System.getProperty("line.separator"));
				}
			} else {
				removedZones.append(zonename);
				removedZones.append(System.getProperty("line.separator"));
			}
			slaveGroup.removeZoneSync(zonename);
		}
		
		StringBuilder path = new StringBuilder(BASE_DIR);
		path.append(slaveGroup.getName());
		
		path.append(getFileExtension(slaveGroup, ExtensionType.ADDED_COMMAND));
		path.append(timestamp);
		if (addedZones.length() > 0) {
			File file = new File(path.toString());
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(addedZones.toString());
			bw.close();
		}

		path = new StringBuilder(BASE_DIR);
		path.append(slaveGroup.getName());
		
		path.append(getFileExtension(slaveGroup, ExtensionType.REMOVED_COMMAND));
		
		path.append(timestamp);
		if (removedZones.length() > 0) {
			File file = new File(path.toString());
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(removedZones.toString());
			bw.close();
		}

		save(slaveGroup);
	}

	private void loadSyncErrors(SlaveGroup slaveGroup) throws IOException {
		if (slaveGroup == null) {
			return;
		}
		parseAddedZoneFile(slaveGroup);
		parseRemovedZoneFile(slaveGroup);
	}

	public String getBaseDir() {
		return BASE_DIR;
	}
}