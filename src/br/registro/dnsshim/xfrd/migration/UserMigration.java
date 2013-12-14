package br.registro.dnsshim.xfrd.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.persistence.EntityManager;

import br.registro.dnsshim.xfrd.dao.filesystem.UserDao;
import br.registro.dnsshim.xfrd.domain.User;

public class UserMigration implements Migration {
	private UserDao dao;
	private String path;
	
	public UserMigration(EntityManager em) {
		dao = new UserDao(em);
		
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
		filePath.append("users.properties");
		path = filePath.toString();
	}
	
	public void migrate() {
		File file = new File(path);
		Properties conf = new Properties();

		try {
			FileInputStream fis = new FileInputStream(file);
			conf.load(fis);

			for (Object key : conf.keySet()) {
				String username = (String) key;
				String password = conf.getProperty(username);
				User user = new User();
				user.setUsername(username);
				user.setPassword(password);
				dao.save(user);
			}
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
