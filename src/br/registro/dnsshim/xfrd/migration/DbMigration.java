package br.registro.dnsshim.xfrd.migration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import br.registro.dnsshim.util.DatabaseUtil;


public class DbMigration {

	public static void main(String[] args) throws IOException {
		EntityManager entityManager = DatabaseUtil.getInstance();

		List<Migration> migrationList = new ArrayList<Migration>();
		migrationList.add(new UserMigration(entityManager));
		migrationList.add(new SlaveGroupMigration(entityManager));
		migrationList.add(new TsigMigration(entityManager));
		migrationList.add(new ZoneMigration(entityManager));
		
		for (Migration migration : migrationList) {
			entityManager.getTransaction().begin();
			migration.migrate();
			entityManager.getTransaction().commit();
		}		

		entityManager.close();
	}
}
