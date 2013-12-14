package br.registro.dnsshim.xfrd.util;

import javax.persistence.EntityManager;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.IoFilter;
import br.registro.dnsshim.common.server.IoSession;
import br.registro.dnsshim.util.DatabaseUtil;

public class OpenDatabaseSessionFilter implements IoFilter {

	@Override
	public void filter(Object message, IoSession session) throws DnsshimProtocolException {
		EntityManager entityManager = DatabaseUtil.getInstance();
		entityManager.getTransaction().begin();
		ServerContext.setEntityManager(entityManager);
	}

}
