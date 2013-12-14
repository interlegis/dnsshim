/* Copyright (C) 2009 Registro.br. All rights reserved. 
* 
* Redistribution and use in source and binary forms, with or without 
* modification, are permitted provided that the following conditions are 
* met:
* 1. Redistribution of source code must retain the above copyright 
*    notice, this list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in the
*    documentation and/or other materials provided with the distribution.
* 
* THIS SOFTWARE IS PROVIDED BY REGISTRO.BR ``AS IS'' AND ANY EXPRESS OR
* IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIE OF FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
* EVENT SHALL REGISTRO.BR BE LIABLE FOR ANY DIRECT, INDIRECT,
* INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
* BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
* OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
* TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
* USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
* DAMAGE.
 */
package br.registro.dnsshim.xfrd.service;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.xfrd.dao.filesystem.SlaveDao;
import br.registro.dnsshim.xfrd.dao.filesystem.SlaveGroupDao;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneInfoDao;
import br.registro.dnsshim.xfrd.domain.Slave;
import br.registro.dnsshim.xfrd.domain.SlaveGroup;
import br.registro.dnsshim.xfrd.domain.ZoneInfo;
import br.registro.dnsshim.xfrd.ui.protocol.ListSlaveGroupResponse;
import br.registro.dnsshim.xfrd.ui.protocol.PrintSlaveGroupResponse;
import br.registro.dnsshim.xfrd.ui.protocol.SlaveGroupRequest;
import br.registro.dnsshim.xfrd.ui.protocol.SlaveRequest;
import br.registro.dnsshim.xfrd.ui.protocol.SlaveResponse;
import br.registro.dnsshim.xfrd.util.ServerContext;

public class SlaveGroupServiceImpl implements SlaveGroupService {
	private static final Logger logger = Logger.getLogger(SlaveGroupServiceImpl.class);

	private EntityManager entityManager;
	private SlaveGroupDao slaveGroupDao;
	
	public SlaveGroupServiceImpl() {
		entityManager = ServerContext.getEntityManager();
		slaveGroupDao = new SlaveGroupDao(entityManager);
	}
	
	@Override
	public SlaveResponse AddSlave(SlaveRequest request) throws DnsshimProtocolException, IOException {
		Slave slave;
		SlaveDao slaveDao = new SlaveDao(entityManager);
		slave = slaveDao.findByAddress(request.getAddress());
		if (slave == null) {
			slave = new Slave();
			slave.setAddress(request.getAddress());
			slave.setPort(request.getPort());
		}

		SlaveGroup slaveGroup = slaveGroupDao.findByName(request.getGroupName());
		if (slaveGroup == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.SLAVE_GROUP_NOT_FOUND, "Slave group not found");		
		}
		
		if (slaveGroup.hasSlave(slave.getAddress()) == false) {
			if (slaveGroup.addSlave(slave)) {
				slaveGroupDao.save(slaveGroup);
			}
		}
		
		if (logger.isInfoEnabled()) {
			logger.info("Slave " + request.getAddress() + " added to slavegroup " + request.getGroupName());
		}

		SlaveResponse response = new SlaveResponse();
		return response;
	}

	@Override
	public SlaveResponse RemoveSlave(SlaveRequest request) throws DnsshimProtocolException, IOException {

		Slave slave = new Slave();
		slave.setAddress(request.getAddress());
		slave.setPort(request.getPort());
		
		SlaveGroup slaveGroup = slaveGroupDao.findByName(request.getGroupName());
		if (slaveGroup == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.SLAVE_GROUP_NOT_FOUND, "Slave group not found");		
		}
		
		if (slaveGroup.removeSlave(slave)) {
			slaveGroupDao.save(slaveGroup);
		}
		
		if (logger.isInfoEnabled()) {
			logger.info("Slave " + request.getAddress() + " removed from slavegroup " + request.getGroupName());
		}
		
		SlaveResponse response = new SlaveResponse();
		return response;
	}

	@Override
	public SlaveResponse newSlaveGroup(SlaveGroupRequest request)
	throws DnsshimProtocolException, IOException {
		SlaveGroup slaveGroup = slaveGroupDao.findByName(request.getGroupName());
		if (slaveGroup != null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.SLAVE_ALREADY_EXISTS, "Slave group already exists");
		}
		
		slaveGroup = new SlaveGroup();
		slaveGroup.setName(request.getGroupName());
		
		slaveGroupDao.save(slaveGroup);
		
		if (logger.isInfoEnabled()) {
			logger.info("New slavegroup " + slaveGroup.getName() + " created");
		}
		return new SlaveResponse();
	}

	@Override
	public SlaveResponse removeSlaveGroup(SlaveGroupRequest request) throws DnsshimProtocolException, IOException {
		SlaveGroup slaveGroup = slaveGroupDao.findByName(request.getGroupName());
		if (slaveGroup == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.SLAVE_GROUP_NOT_FOUND, "Slave group does not exist");
		}
		
		ZoneInfoDao zoneInfoDao = new ZoneInfoDao(entityManager);
		List<ZoneInfo> zones = zoneInfoDao.findBySlavegroup(slaveGroup.getName());
		for (ZoneInfo zone : zones) {
			zone.unassignSlaveGroup(slaveGroup);
			zoneInfoDao.save(zone);
		}
		slaveGroupDao.delete(request.getGroupName());

		if (logger.isInfoEnabled()) {
			logger.info("Slavegroup " + slaveGroup.getName() + " removed");
		}
		return new SlaveResponse();
	}

	@Override
	public PrintSlaveGroupResponse printSlaveGroup(String slaveGroupName)
			throws DnsshimProtocolException, IOException {
		
		PrintSlaveGroupResponse response = new PrintSlaveGroupResponse();
		response.setSlaveGroup(slaveGroupName);

		SlaveGroup slaveGroup = slaveGroupDao.findByName(response.getSlaveGroup());
		if (slaveGroup == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.SLAVE_GROUP_NOT_FOUND, "Slave group does not exist");
		}
		
		for (Slave slave: slaveGroup.getSlaves()) {
			response.addSlave(slave);
		}
		
		return response;
	}

	public ListSlaveGroupResponse listSlaveGroup() throws IOException {
		ListSlaveGroupResponse response = new ListSlaveGroupResponse();
		List<SlaveGroup> groups = slaveGroupDao.findAll();
		for (SlaveGroup g : groups) {
			response.addSlaveGroup(g.getName());
		}
		
		return response;

	}

}
