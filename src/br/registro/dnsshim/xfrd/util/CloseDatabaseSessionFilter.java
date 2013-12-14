package br.registro.dnsshim.xfrd.util;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.IoFilter;
import br.registro.dnsshim.common.server.IoSession;

public class CloseDatabaseSessionFilter implements IoFilter {

	private static final Logger logger = Logger.getLogger(CloseDatabaseSessionFilter.class);

	@Override
	public void filter(Object message, IoSession session) throws DnsshimProtocolException {

		EntityManager entityManager = ServerContext.getEntityManager();
		
		if (entityManager == null) {
			logger.error("Connection is null. Cannot close database connection in filter.");
			return;
		}
		
		if (entityManager.getTransaction().isActive()) {
			entityManager.getTransaction().commit();
		}
			
		if (entityManager.isOpen()) {
			entityManager.close();
		}
	}
}