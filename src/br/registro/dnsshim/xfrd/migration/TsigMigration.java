package br.registro.dnsshim.xfrd.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.persistence.EntityManager;

import br.registro.dnsshim.xfrd.dao.filesystem.SlaveDao;
import br.registro.dnsshim.xfrd.domain.Slave;
import br.registro.dnsshim.xfrd.domain.TsigKeyInfo;

public class TsigMigration implements Migration {
	private SlaveDao dao;
	private String path;

	public TsigMigration(EntityManager em) {
		dao = new SlaveDao(em);
		
		StringBuilder filePath = new StringBuilder();
		// {DNSSHIM_HOME|user.home}/dnsshim/xfrd/zones/SHA1#1/SHA1#2/SHA1#3/<zones>.properties
		if (System.getenv("DNSSHIM_HOME") == null) {
			filePath.append(System.getProperty("user.home"));
			filePath.append(File.separatorChar);
			filePath.append("dnsshim");
		} else {
			filePath.append(System.getenv("DNSSHIM_HOME"));
		}
		filePath.append(File.separatorChar);
		filePath.append("xfrd");
		filePath.append(File.separatorChar);
		filePath.append("conf");
		filePath.append(File.separatorChar);
		filePath.append("tsig-key.properties");
		path = filePath.toString();
	}
	
	@Override
	public void migrate() {
		File file = new File(path);
		Properties conf = new Properties();

		try {
			FileInputStream fis = new FileInputStream(file);
			conf.load(fis);

			for (Object key : conf.keySet()) {
				String address = (String) key;

				Slave slave = dao.findByAddress(address);
				if (slave == null) {
					continue;
				}

				String property = conf.getProperty(address);
				if (property == null) {
					continue;
				}

				String[] tsigKeys = property.split(";");
				for (String tsig : tsigKeys) {
					String[] keyParts = tsig.split("#");
					if (keyParts.length != 2) {
						break;
					}

					String keyName = keyParts[0];
					String secret = keyParts[1];
					TsigKeyInfo tsigKey = new TsigKeyInfo(keyName, secret);
					slave.addTsigKey(tsigKey);
				}

				dao.save(slave);
			}
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
