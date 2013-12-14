package br.registro.dnsshim.xfrd.migration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import br.registro.dnsshim.xfrd.dao.filesystem.SlaveGroupDao;
import br.registro.dnsshim.xfrd.domain.Slave;
import br.registro.dnsshim.xfrd.domain.SlaveGroup;

public class SlaveGroupMigration implements Migration {
	private SlaveGroupDao dao;
	private String baseDir;
	private final String ZONES_EXTENSION = ".zones";
	private final String HOSTS_EXTENSION = ".hosts";
	
	public SlaveGroupMigration(EntityManager em) {
		dao = new SlaveGroupDao(em);
		
		StringBuilder path = new StringBuilder();
		// {DNSSHIM_HOME|user.home}/dnsshim/xfrd/zones/SHA1#1/SHA1#2/SHA1#3/<zones>.properties
		if (System.getenv("DNSSHIM_HOME") == null) {
			path.append(System.getProperty("user.home"));
			path.append(File.separatorChar);
			path.append("dnsshim");
		} else {
			path.append(System.getenv("DNSSHIM_HOME"));
		}
		path.append(File.separatorChar);
		path.append("xfrd");
		path.append(File.separatorChar);
		path.append("conf");
		path.append(File.separatorChar);
		path.append("slaves");
		path.append(File.separatorChar);
		baseDir = path.toString();
	}
	
	public void migrate() {
		File groupPath = new File(baseDir);
		File[] list = groupPath.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return (name.endsWith(ZONES_EXTENSION));
			}
		});
		
		for (File file : list) {
			String filename = file.getName();
			String groupName = filename.substring(0, filename.length() - ZONES_EXTENSION.length());

			try {
				List<Slave> slaves = parseHostFile(groupName);
				SlaveGroup slaveGroup = new SlaveGroup();
				slaveGroup.setName(groupName);
				for (Slave slave : slaves) {
					slaveGroup.addSlave(slave);
				}

				dao.save(slaveGroup);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
	}
	
	private List<Slave> parseHostFile(String groupName) throws IOException {
		StringBuilder path = new StringBuilder(baseDir);
		path.append(groupName);
		path.append(HOSTS_EXTENSION);
		
		List<Slave> slaves = new ArrayList<Slave>();
		BufferedReader reader;
		String line;
		File hostFile = new File(path.toString());
		reader = new BufferedReader(new FileReader(hostFile));
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty()) {
				continue;
			}
			
			String[] addressAndPort = line.split(" ");
			if (addressAndPort.length != 2) {
				continue;
			}
			
			String address = addressAndPort[0];
			int port = Integer.parseInt(addressAndPort[1]);
			Slave slave = new Slave();
			slave.setAddress(address);
			slave.setPort(port);
			slaves.add(slave);
		}
		reader.close();
		
		return slaves;
	}

}
