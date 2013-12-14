package br.registro.dnsshim.xfrd.dao.filesystem;

import javax.persistence.EntityManager;

import br.registro.dnsshim.xfrd.domain.Slave;

public class SlaveDao {
	private EntityManager entityManager;

	public SlaveDao(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public void save(Slave slave) {
		entityManager.persist(slave);
	}

	public Slave findByAddress(String address) {
		return entityManager.find(Slave.class, address);
	}
}