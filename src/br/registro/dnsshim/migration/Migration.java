package br.registro.dnsshim.migration;


public class Migration {
	public static void main(String[] args) {
		SchemaCreator.dropTablesIfExists();
		SchemaCreator.createAllTables();
	}

}
