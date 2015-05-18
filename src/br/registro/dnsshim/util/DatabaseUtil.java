package br.registro.dnsshim.util;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.hibernate.ejb.EntityManagerFactoryImpl;

import br.registro.dnsshim.xfrd.dao.filesystem.XfrdConfigDao;
import br.registro.dnsshim.xfrd.domain.XfrdConfig;

public class DatabaseUtil {
	private static final Logger logger = Logger.getLogger(DatabaseUtil.class);
	private static EntityManagerFactory entityManagerFactory;
	private DatabaseUtil() {}
	
	public static synchronized EntityManager getInstance() {
		if (entityManagerFactory == null) {
			entityManagerFactory = Persistence.createEntityManagerFactory("dnsshim_db", buildDatabaseProperties());
		}
		return entityManagerFactory.createEntityManager();
	}

	public static void printStatistics() {
		if (entityManagerFactory != null) {
			EntityManagerFactoryImpl empImpl = (EntityManagerFactoryImpl) entityManagerFactory;
			if (logger.isInfoEnabled()) {
				logger.info(empImpl.getSessionFactory().getStatistics());
			}
		}
	}
	
	public static Map<String, String> buildDatabaseProperties() {
		HashMap<String, String> props = new HashMap<String, String>();
		
		try {
			XfrdConfigDao dao = new XfrdConfigDao();
			XfrdConfig xfrdConfig = dao.load();
			Map<String, String> xfrdDdProps = xfrdConfig.getDatabaseProperties();
			props.put("hibernate.connection.driver_class", xfrdDdProps.get("database_driver_class"));
			props.put("hibernate.connection.password", xfrdDdProps.get("database_password"));
			props.put("hibernate.connection.url", "jdbc:" + xfrdDdProps.get("database_vendor") + "://" + xfrdDdProps.get("database_host") + ":" + xfrdDdProps.get("database_port") + "/" + xfrdDdProps.get("database_name"));
			props.put("hibernate.connection.username", xfrdDdProps.get("database_username"));
			props.put("hibernate.dialect", xfrdDdProps.get("database_dialect"));
			props.put("hibernate.hbm2ddl.auto", "validate"); // overriding this option to avoid catastrophes 
		} catch (Exception e) {
			logger.error(e, e);
		} 
		
		return props;
	}
}
