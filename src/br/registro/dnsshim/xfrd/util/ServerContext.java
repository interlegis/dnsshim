package br.registro.dnsshim.xfrd.util;

import javax.persistence.EntityManager;

public class ServerContext {
	private static final ThreadLocal<EntityManager> context = new ThreadLocal<EntityManager>();
	
	public static void setEntityManager(EntityManager entityManager) {
		context.set(entityManager);
	}
	
	public static EntityManager getEntityManager() {
		return context.get();
	}
}
