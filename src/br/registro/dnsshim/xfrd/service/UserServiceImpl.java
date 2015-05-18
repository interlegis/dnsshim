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

import java.util.Random;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.IoSession;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.xfrd.dao.filesystem.UserDao;
import br.registro.dnsshim.xfrd.domain.User;
import br.registro.dnsshim.xfrd.domain.logic.DnsshimSession;
import br.registro.dnsshim.xfrd.domain.logic.DnsshimSessionCache;
import br.registro.dnsshim.xfrd.ui.protocol.AddUserRequest;
import br.registro.dnsshim.xfrd.ui.protocol.ChangePasswordRequest;
import br.registro.dnsshim.xfrd.ui.protocol.LoginRequest;
import br.registro.dnsshim.xfrd.ui.protocol.LoginResponse;
import br.registro.dnsshim.xfrd.ui.protocol.LogoutRequest;
import br.registro.dnsshim.xfrd.ui.protocol.Response;
import br.registro.dnsshim.xfrd.util.ServerContext;

public class UserServiceImpl implements UserService {
	private static final Logger logger = Logger.getLogger(UserServiceImpl.class);

	private UserDao dao;
	private DnsshimSessionCache sessionCache = DnsshimSessionCache.getInstance();

	public UserServiceImpl() {
		EntityManager em = ServerContext.getEntityManager();
		dao = new UserDao(em);
	}

	@Override
	public Response addUser(AddUserRequest request)
		throws DnsshimProtocolException {
		Response response = new Response();

		if (dao.findByUsername(request.getUsername()) != null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.USER_ALREADY_EXISTS, "User already exists");
		}

		User user = new User();
		user.setUsername(request.getUsername());
		user.setPassword(request.getPassword());
		
		dao.save(user);
		
		response.setMsg("User created successfully");
		if (logger.isInfoEnabled()) {
			logger.info("New user " + request.getUsername() + " created");
		}

		return response;
	}

	@Override
	public Response changePassword(ChangePasswordRequest request)
		throws DnsshimProtocolException {
		DnsshimSession dnsshimSession = sessionCache.get(request.getSessionId());
		User user = (User) dnsshimSession.getAttribute("user");
		if (user == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.FORBIDDEN,
					"Forbidden (not logged in)");			
		}
		
		String username = user.getUsername();
		if (!username.equalsIgnoreCase(request.getUsername())) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_USER, "Invalid user");
		}
		
		user = dao.findByUsername(username);
		if (user == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.USER_NOT_FOUND, "User not found");
		}
		
		if (!user.getPassword().equalsIgnoreCase(request.getOldPassword())) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_PASSWORD, "Invalid password");
		}
		
		user.setPassword(request.getNewPassword());
		dao.save(user);

		if (logger.isInfoEnabled()) {
			logger.info("Password changed for user " + username);
		}
		Response response = new Response();
		response.setMsg("Password has been changed");
		return response;
	}

	@Override
	public LoginResponse login(LoginRequest request, IoSession session) 
		throws DnsshimProtocolException {
		User user = dao.findByUsername(request.getUsername());
		if (user == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.USER_NOT_FOUND, "User not found");					
		}
		
		if (!user.getPassword().equalsIgnoreCase(request.getPassword())) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_PASSWORD, "Invalid password");
		}

		Random random = new Random();
		int sessionId = random.nextInt(Integer.MAX_VALUE - 1) + 1; // Must be > 0
		DnsshimSession dnsshimSession = new DnsshimSession();
		dnsshimSession.setId(String.valueOf(sessionId));
		dnsshimSession.setAttribute("user", user);
		dnsshimSession.setRemoteAddress(session.getRemoteAddress());
		sessionCache.put(dnsshimSession);
		
		logger.info("User " + request.getUsername() + " " +
				"logged in (" + String.valueOf(sessionId) + ") " +
				"from " + session.getRemoteAddress());
		LoginResponse response = new LoginResponse();
		response.setSessionId(sessionId);
		return response;
	}

	@Override
	public Response logout(LogoutRequest request, IoSession session)
			throws DnsshimProtocolException {
		DnsshimSession dnsshimSession = sessionCache.remove(request.getSessionId());
		if (dnsshimSession == null) {
			throw new DnsshimProtocolException(ProtocolStatusCode.FORBIDDEN,
					"Forbidden (not logged in)");
		}
		
		if (logger.isInfoEnabled()) {
			logger.info("Session " + request.getSessionId() + " finished");
		}

		Response response = new Response();
		response.setStatus(ProtocolStatusCode.OK);
		return response;
	}

}
