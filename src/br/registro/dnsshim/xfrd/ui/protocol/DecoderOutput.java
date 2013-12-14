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
package br.registro.dnsshim.xfrd.ui.protocol;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import br.registro.dnsshim.common.server.AbstractDecoderOutput;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.IoSession;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.domain.logic.CacheException;
import br.registro.dnsshim.xfrd.domain.PublicationType;
import br.registro.dnsshim.xfrd.domain.XfrdConfig;
import br.registro.dnsshim.xfrd.domain.logic.XfrdConfigManager;
import br.registro.dnsshim.xfrd.server.XfrdLauncher;
import br.registro.dnsshim.xfrd.service.KeyServiceImpl;
import br.registro.dnsshim.xfrd.service.PublicationServiceImpl;
import br.registro.dnsshim.xfrd.service.SlaveGroupServiceImpl;
import br.registro.dnsshim.xfrd.service.TsigKeyServiceImpl;
import br.registro.dnsshim.xfrd.service.UserServiceImpl;
import br.registro.dnsshim.xfrd.service.ZoneServiceImpl;
import br.registro.dnsshim.xfrd.ui.protocol.ZoneUserRequest.ZoneUserRequestType;

public class DecoderOutput extends AbstractDecoderOutput {
	private static final Map<String, Lock> zoneLocks = new ConcurrentHashMap<String, Lock>();
	
	@Override
	public void doOutput(Object message, IoSession session) throws DnsshimProtocolException {
		Lock zoneLock = null;

		ZoneServiceImpl zoneService = new ZoneServiceImpl();
		try {
			if (message instanceof NewZoneRequest) {
				NewZoneRequest request = (NewZoneRequest) message;
				String zonename = request.getZone();
				if ((zoneLock = zoneLocks.get(zonename)) == null) {
					zoneLock = new ReentrantLock();
					zoneLocks.put(zonename, zoneLock);
				}
				zoneLock.lock();
				NewZoneResponse response = zoneService.newZone(request);
				getEncoder().encode(response, session);
				
			} else if (message instanceof ImportZoneRequest) {
				ImportZoneRequest request = (ImportZoneRequest) message;
				ImportZoneResponse response = zoneService.importZone(request);
				getEncoder().encode(response, session);
				
			} else if (message instanceof RrRequest) {
				RrRequest request = (RrRequest) message;
			
				String zonename = request.getZone();
				if ((zoneLock = zoneLocks.get(zonename)) == null) {
					zoneLock = new ReentrantLock();
					zoneLocks.put(zonename, zoneLock);
				}
				zoneLock.lock();
				
				RrResponse response = null;
				if (request.getOp() == RrOperation.ADD) {
					response = zoneService.addRecord(request.getZone(), request.getRr());
				} else {
					response = zoneService.removeRecord(request.getZone(), request.getRr());
				}
				getEncoder().encode(response, session);
				
			} else if (message instanceof ListZonesRequest) {
				ListZonesRequest request = (ListZonesRequest) message;
				ListZonesResponse response = zoneService.listZones(request);
				getEncoder().encode(response, session);

			} else if (message instanceof ListZonesBySlaveGroupRequest) {
				ListZonesBySlaveGroupRequest request = (ListZonesBySlaveGroupRequest) message;
				
				ListZonesBySlaveGroupResponse response = zoneService.listZonesBySlaveGroup(request.getSlaveGroup());
				getEncoder().encode(response, session);
				
			} else if (message instanceof PrintZoneRequest) {
				PrintZoneRequest request = (PrintZoneRequest) message;
				
				PrintZoneResponse response = zoneService.printZone(request.getZone());
				getEncoder().encode(response, session);
				
			} else if (message instanceof PubZoneRequest) {
				PubZoneRequest request = (PubZoneRequest) message;
				
				String zonename = request.getZone();
				if ((zoneLock = zoneLocks.get(zonename)) == null) {
					zoneLock = new ReentrantLock();
					zoneLocks.put(zonename, zoneLock);
				}
				zoneLock.lock();
				
				PublicationServiceImpl service = new PublicationServiceImpl();
				PubZoneResponse response = null;
				if (request.getPubType() == PublicationType.FULL) {
					response = service.full(request.getZone(), request.getSerial());
				} else if (request.getPubType() == PublicationType.INCREMENTAL) {
					response = service.incremental(request.getZone(), request.getSerial());
				}
				
				getEncoder().encode(response, session);
				
			} else if (message instanceof RemoveZoneRequest) {
				RemoveZoneRequest request = (RemoveZoneRequest) message;
				String zonename = request.getZone();
				if ((zoneLock = zoneLocks.get(zonename)) == null) {
					zoneLock = new ReentrantLock();
					zoneLocks.put(zonename, zoneLock);
				}
				zoneLock.lock();
				RemoveZoneResponse response = zoneService.removeZone(request);
				getEncoder().encode(response, session);
				
			} else if (message instanceof ZoneVersionRequest) {
				ZoneVersionRequest request = (ZoneVersionRequest) message;
				ZoneVersionResponse response = zoneService.zoneVersion(request.getZone());
				getEncoder().encode(response, session);
				
			} else if (message instanceof NewKeyRequest) {
				NewKeyRequest request = (NewKeyRequest) message;
				KeyServiceImpl service = new KeyServiceImpl();
				NewKeyResponse response = service.newKey(request);
				getEncoder().encode(response, session);
				
			} else if (message instanceof ImportKeyRequest) {
				ImportKeyRequest request = (ImportKeyRequest) message;
				KeyServiceImpl service = new KeyServiceImpl();
				ImportKeyResponse response = service.importKey(request);
				getEncoder().encode(response, session);
				
			} else if (message instanceof RemoveKeyRequest) {
				RemoveKeyRequest request = (RemoveKeyRequest) message;
				RemoveKeyResponse response = zoneService.removeKey(request.getZone(), request.getKey());
				getEncoder().encode(response, session);
				
			} else if (message instanceof ChangeKeyStatusRequest) {
				ChangeKeyStatusRequest request = (ChangeKeyStatusRequest) message;
				ChangeKeyStatusResponse response = zoneService.changeKey(request.getZone(),
						request.getKey(),
						request.getOldStatus(),
						request.getNewStatus());
				getEncoder().encode(response, session);
				
			} else if (message instanceof ListKeysRequest) {
				ListKeysRequest request = (ListKeysRequest) message;
				KeyServiceImpl service = new KeyServiceImpl();
				ListKeysResponse response = service.listKeys(request.getZone());
				getEncoder().encode(response, session);

			} else if (message instanceof ListSlaveGroupRequest) {
				SlaveGroupServiceImpl service = new SlaveGroupServiceImpl();				
				ListSlaveGroupResponse response = service.listSlaveGroup();
				getEncoder().encode(response, session);
				
			} else if (message instanceof PrintSlaveGroupRequest) {
				PrintSlaveGroupRequest request = (PrintSlaveGroupRequest) message;
				SlaveGroupServiceImpl service = new SlaveGroupServiceImpl();
				
				PrintSlaveGroupResponse response = service.printSlaveGroup(request.getSlaveGroup());
				getEncoder().encode(response, session);
				
			} else if (message instanceof SlaveGroupRequest) {
				SlaveGroupRequest request = (SlaveGroupRequest) message;
				SlaveGroupServiceImpl slaveService = new SlaveGroupServiceImpl();
				SlaveResponse response = null;
				switch(request.getOperation()) {
					case ASSIGN:
						response = zoneService.assignSlaveGroup(request);
						break;
					case UNASSIGN:
						response = zoneService.unassignSlaveGroup(request);
						break;
					case ADD:
						response = slaveService.newSlaveGroup(request);
						break;
					case REMOVE:
						response = slaveService.removeSlaveGroup(request);
						break;
				}

				getEncoder().encode(response, session);
				
			} else if (message instanceof SlaveRequest) {
				SlaveRequest request = (SlaveRequest) message;
				SlaveGroupServiceImpl slaveService = new SlaveGroupServiceImpl();
				SlaveResponse response = null;
				switch(request.getOperation()) {
				case ADD:
					response = slaveService.AddSlave(request);
					break;
				case REMOVE:
					response = slaveService.RemoveSlave(request);
					break;
				}
				
				getEncoder().encode(response, session);
				
			} else if (message instanceof ListSlavesRequest) {
				ListSlavesRequest request = (ListSlavesRequest) message;
				ListSlavesResponse response = zoneService.listSlaves(request.getZone());
				getEncoder().encode(response, session);
				
			} else if (message instanceof SetExpirationPeriodRequest) {
				SetExpirationPeriodRequest request = (SetExpirationPeriodRequest) message;
				SetExpirationPeriodResponse response = zoneService.setExpirationPeriod(request);
				getEncoder().encode(response, session);
				
			} else if (message instanceof NewTsigKeyRequest) {
				NewTsigKeyRequest request = (NewTsigKeyRequest) message;
				TsigKeyServiceImpl service = new TsigKeyServiceImpl();
				NewTsigKeyResponse response = service.newTsigKey(request);
				getEncoder().encode(response, session);
				
			} else if (message instanceof RemoveTsigKeyRequest) {
				RemoveTsigKeyRequest request = (RemoveTsigKeyRequest) message;
				TsigKeyServiceImpl service = new TsigKeyServiceImpl();
				RemoveTsigKeyResponse response = service.removeTsigKey(request);
				getEncoder().encode(response, session);

				
			} else if (message instanceof ListTsigKeysRequest) {
				ListTsigKeysRequest request = (ListTsigKeysRequest) message;
				TsigKeyServiceImpl service = new TsigKeyServiceImpl();
				ListTsigKeysResponse response = service.listTsigKeys(request);
				getEncoder().encode(response, session);
				
			} else if (message instanceof LoginRequest) {
				LoginRequest request = (LoginRequest) message;
				UserServiceImpl service = new UserServiceImpl();
				LoginResponse response = service.login(request, session);
				getEncoder().encode(response, session);
				
			} else if (message instanceof LogoutRequest) {
				LogoutRequest request = (LogoutRequest) message;
				UserServiceImpl service = new UserServiceImpl();
				Response response = service.logout(request, session);
				getEncoder().encode(response, session);

			} else if (message instanceof AddUserRequest) {
				AddUserRequest request = (AddUserRequest) message;
				UserServiceImpl service = new UserServiceImpl();
				Response response = service.addUser(request);
				getEncoder().encode(response, session);

			} else if (message instanceof ChangePasswordRequest) {
				ChangePasswordRequest request = (ChangePasswordRequest) message;
				UserServiceImpl service = new UserServiceImpl();
				Response response = service.changePassword(request);
				getEncoder().encode(response, session);

			} else if (message instanceof HelloRequest) {
				Response response = new Response();
				response.setStatus(ProtocolStatusCode.OK);
				getEncoder().encode(response, session);
				
			} else if (message instanceof ZoneUserRequest) {
				ZoneUserRequest request = (ZoneUserRequest) message;
				ZoneUserResponse response = null;
				String zonename = request.getZone();
				if ((zoneLock = zoneLocks.get(zonename)) == null) {
					zoneLock = new ReentrantLock();
					zoneLocks.put(zonename, zoneLock);
				}
				zoneLock.lock();
				if (request.getType() == ZoneUserRequestType.ADD) {
					response = zoneService.addUser(request);
				} else if (request.getType() == ZoneUserRequestType.REMOVE) {
					response = zoneService.removeUser(request);
				}
				getEncoder().encode(response, session);
			} else if (message instanceof ZoneExistsRequest) {
				ZoneExistsRequest request = (ZoneExistsRequest) message;
				Response response = zoneService.zoneExists(request);
				getEncoder().encode(response, session);
			} else if (message instanceof ShutdownRequest) {
				ShutdownRequest request = (ShutdownRequest) message;
				XfrdConfig config = XfrdConfigManager.getInstance();
				if (request.getSecret().equals(config.getShutdownSecret())) {
					XfrdLauncher.SHUTDOWN = true;
				} else {
					throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_PASSWORD, "Invalid secret");				
				}
				Response response = new Response();
				response.setStatus(ProtocolStatusCode.OK);
				getEncoder().encode(response, session);
				
			} else {
				throw new DnsshimProtocolException(ProtocolStatusCode.UI_SERVER_ERROR, "Invalid message");
			}
			
		} catch (CacheException e) {
			throw new DnsshimProtocolException(ProtocolStatusCode.UI_SERVER_ERROR, e.getMessage());
		} catch (IOException ioe) {
			throw new DnsshimProtocolException(ProtocolStatusCode.UI_SERVER_ERROR, ioe.getMessage());
		} finally {
			if (zoneLock != null) {
				zoneLock.unlock();
			}
		}
	}	
}
