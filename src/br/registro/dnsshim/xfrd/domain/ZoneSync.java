package br.registro.dnsshim.xfrd.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name="zone_sync")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE, region="dnsshim")
public class ZoneSync {
	
	public enum Operation {
		ADD,
		REMOVE
	}
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	
	private String zonename;
	
	@ManyToOne
	@JoinColumn(name="slavegroup")
	@Cache(usage=CacheConcurrencyStrategy.READ_WRITE, region="dnsshim")
	private SlaveGroup slaveGroup;
	
	private Operation operation;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getZonename() {
		return zonename;
	}

	public void setZonename(String zonename) {
		this.zonename = zonename;
	}

	public SlaveGroup getSlaveGroup() {
		return slaveGroup;
	}

	public void setSlaveGroup(SlaveGroup slaveGroup) {
		this.slaveGroup = slaveGroup;
	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}
}
