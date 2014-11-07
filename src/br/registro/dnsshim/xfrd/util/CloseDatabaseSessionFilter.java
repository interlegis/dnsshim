package br.registro.dnsshim.xfrd.util;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.IoFilter;
import br.registro.dnsshim.common.server.IoSession;

public class CloseDatabaseSessionFilter implements IoFilter {

	private static final Logger logger = Logger.getLogger(CloseDatabaseSessionFilter.class);

	private volatile boolean mayClose = true;

	//TODO: check fix
	public void setMayClose(boolean mayClose) {
		this.mayClose = mayClose;
	}

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
		//TODO: check fix
		if (mayClose){
			if (entityManager.isOpen()) {
				entityManager.close();
			}
		}
	}
}