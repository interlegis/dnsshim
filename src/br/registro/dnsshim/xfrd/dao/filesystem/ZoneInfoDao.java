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
package br.registro.dnsshim.xfrd.dao.filesystem;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import br.registro.dnsshim.xfrd.domain.ZoneInfo;

public class ZoneInfoDao {
	private EntityManager entityManager;

	public ZoneInfoDao(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public void save(ZoneInfo zoneInfo) {
		entityManager.persist(zoneInfo);
	}

	public void delete(ZoneInfo zoneInfo) {
		entityManager.remove(zoneInfo);
	}

	public ZoneInfo findByZonename(String zonename) {
		if (zonename.isEmpty()) {
			throw new IllegalArgumentException("Empty zone name");
		}
		
		ZoneInfo zoneInfo = entityManager.find(ZoneInfo.class, zonename);
		return zoneInfo;
	}
	
	@SuppressWarnings("unchecked")
	public List<ZoneInfo> findBySlavegroup(String groupName) {
		String sql = "SELECT z FROM ZoneInfo z, IN(z.slaveGroups) s " +
				"WHERE s.name = :name";
		Query query = entityManager.createQuery(sql);
		query.setParameter("name", groupName);
		return query.getResultList();		
	}
	
	@SuppressWarnings("unchecked")
	public List<ZoneInfo> findAllExpired(Date dateLimit) {
		String sql = "SELECT z FROM ZoneInfo z " +
				"WHERE z.expirationDate <= :limit OR z.resignDate <= :now " +
				"ORDER BY z.expirationDate ASC";
		Query query = entityManager.createQuery(sql);
		query.setMaxResults(3000);
		query.setParameter("limit", dateLimit);
		query.setParameter("now", Calendar.getInstance().getTime());
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<ZoneInfo> findByUser(String username) {
		String sql = "SELECT z FROM ZoneInfo z, IN(z.users) u " +
				"WHERE u.username = :username";
		Query query = entityManager.createQuery(sql);
		query.setParameter("username", username);
		List<ZoneInfo> result = (List<ZoneInfo>) query.getResultList();
		return result;
	}
}