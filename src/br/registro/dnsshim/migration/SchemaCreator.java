package br.registro.dnsshim.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SchemaCreator {
	
	public static void dropTablesIfExists() {
		try {
			Class.forName("org.sqlite.JDBC");
			Connection conn = DriverManager.getConnection("jdbc:sqlite:dnsshim.db");
			Statement stat = conn.createStatement();
			
			stat.executeUpdate("DROP TABLE IF EXISTS zone;");
			stat.executeUpdate("DROP TABLE IF EXISTS key;");
			stat.executeUpdate("DROP TABLE IF EXISTS user;");
			stat.executeUpdate("DROP TABLE IF EXISTS zone_user;");
			stat.executeUpdate("DROP TABLE IF EXISTS slave_group;");
			stat.executeUpdate("DROP TABLE IF EXISTS slave_group_zone;");
			stat.executeUpdate("DROP TABLE IF EXISTS slave;");
			
			
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void createAllTables() {
		try {
			Class.forName("org.sqlite.JDBC");
			Connection conn = DriverManager.getConnection("jdbc:sqlite:dnsshim.db");
			Statement stat = conn.createStatement();
						
			stat.executeUpdate(buildZoneSQLStatement());

			stat.executeUpdate(buildKeySQLStatement());

			stat.executeUpdate(buildUserSQLStatement());

			stat.executeUpdate(buildZoneUserSQLStatement());
			
			stat.executeUpdate(buildSlaveSQLStatement());
			
			stat.executeUpdate(buildSlaveGroupSQLStatement());
			
			stat.executeUpdate(buildSlaveGroupZoneSQLStatement());
						
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String buildZoneSQLStatement() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("CREATE TABLE zone (")
			.append("name VARCHAR(255) PRIMARY KEY,")
			.append("increments INT,")
			.append("expiration INT,")
			.append("resign_date INT,")
			.append("validity INT,")
			.append("signed BOOLEAN,")
			.append("soa_minimum INT,")
			.append("ttl_dnskey INT,")
			.append("ttl_zone INT")
			.append(");");
		
		return builder.toString();
	}
	
	private static String buildKeySQLStatement() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("CREATE TABLE key (")
				.append("name VARCHAR(255),")
				.append("zone VARCHAR(255),")
				.append("status VARCHAR(255)")
				.append(");");
		
		return builder.toString();
	}
	
	private static String buildUserSQLStatement() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("CREATE TABLE user (")
			.append("username VARCHAR(255),")
			.append("password VARCHAR(255)")
			.append(");");
		
		return builder.toString();
	}
	
	private static String buildZoneUserSQLStatement() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("CREATE TABLE zone_user (")
				.append("username VARCHAR(255),") 
				.append("zone VARCHAR(255)") 
				.append(");");
		
		return builder.toString();
	}
	
	private static String buildSlaveSQLStatement() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("CREATE TABLE slave (")
				.append("id INT PRIMARY KEY,")
				.append("host VARCHAR(255),") 
				.append("port INT,")
				.append("tsig_name varchar(255),")
				.append("tsig TEXT")
				.append(");");
		
		return builder.toString();
	}
	
	private static String buildSlaveGroupSQLStatement() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("CREATE TABLE slave_group (")
				.append("name VARCHAR(255),")
				.append("host VARCHAR(255),")
				.append("port INT")
				.append(");");
		
		return builder.toString();
	}
	
	private static String buildSlaveGroupZoneSQLStatement() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("CREATE TABLE slave_group_zone (")
				.append("name VARCHAR(255),")
				.append("zone VARCHAR(255)")
				.append(");");
		
		return builder.toString();
	}
	
	
}
