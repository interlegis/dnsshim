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

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.xfrd.domain.DnskeyStatus;
import br.registro.dnsshim.xfrd.ui.protocol.ChangeKeyStatusResponse;
import br.registro.dnsshim.xfrd.ui.protocol.ImportZoneRequest;
import br.registro.dnsshim.xfrd.ui.protocol.ImportZoneResponse;
import br.registro.dnsshim.xfrd.ui.protocol.ListSlavesResponse;
import br.registro.dnsshim.xfrd.ui.protocol.ListZonesRequest;
import br.registro.dnsshim.xfrd.ui.protocol.ListZonesResponse;
import br.registro.dnsshim.xfrd.ui.protocol.NewZoneRequest;
import br.registro.dnsshim.xfrd.ui.protocol.NewZoneResponse;
import br.registro.dnsshim.xfrd.ui.protocol.PrintZoneResponse;
import br.registro.dnsshim.xfrd.ui.protocol.RemoveKeyResponse;
import br.registro.dnsshim.xfrd.ui.protocol.RemoveZoneRequest;
import br.registro.dnsshim.xfrd.ui.protocol.RemoveZoneResponse;
import br.registro.dnsshim.xfrd.ui.protocol.Response;
import br.registro.dnsshim.xfrd.ui.protocol.RrResponse;
import br.registro.dnsshim.xfrd.ui.protocol.SetExpirationPeriodRequest;
import br.registro.dnsshim.xfrd.ui.protocol.SetExpirationPeriodResponse;
import br.registro.dnsshim.xfrd.ui.protocol.SlaveGroupRequest;
import br.registro.dnsshim.xfrd.ui.protocol.SlaveResponse;
import br.registro.dnsshim.xfrd.ui.protocol.ZoneExistsRequest;
import br.registro.dnsshim.xfrd.ui.protocol.ZoneUserRequest;
import br.registro.dnsshim.xfrd.ui.protocol.ZoneUserResponse;
import br.registro.dnsshim.xfrd.ui.protocol.ZoneVersionResponse;

public interface ZoneService {
	
	public NewZoneResponse newZone(NewZoneRequest request)
		throws DnsshimProtocolException, IOException;
	
	public ImportZoneResponse importZone(ImportZoneRequest request)
		throws DnsshimProtocolException, IOException;
	
	public RrResponse addRecord(String zonename,
				ResourceRecord rr) throws DnsshimProtocolException, IOException;
	
	public RrResponse removeRecord(String zonename,
				ResourceRecord rr) throws IOException, DnsshimProtocolException;
	
	public ListZonesResponse listZones(ListZonesRequest request) 
			throws IOException, DnsshimProtocolException;
	
	public PrintZoneResponse printZone(String zonename)
			throws DnsshimProtocolException, IOException;
	
	public RemoveZoneResponse removeZone(RemoveZoneRequest request) 
		throws DnsshimProtocolException, IOException;

	public ZoneVersionResponse zoneVersion(String zonename)
		throws DnsshimProtocolException, IOException;
	
	public SlaveResponse assignSlaveGroup(SlaveGroupRequest request)
		throws DnsshimProtocolException, IOException;
	
	public SlaveResponse unassignSlaveGroup(SlaveGroupRequest request)
		throws DnsshimProtocolException, IOException;
	
	public ListSlavesResponse listSlaves(String zonename)
			throws DnsshimProtocolException, IOException;
	
	public RemoveKeyResponse removeKey(String zonename,	String keyName)
		throws IOException, DnsshimProtocolException;
	
	public ChangeKeyStatusResponse changeKey(String zonename,
				String keyName,
				DnskeyStatus oldStatus,
				DnskeyStatus newStatus) throws DnsshimProtocolException, IOException;
	
	public SetExpirationPeriodResponse setExpirationPeriod(SetExpirationPeriodRequest request)
		throws DnsshimProtocolException;
	
	public ZoneUserResponse addUser(ZoneUserRequest request) throws DnsshimProtocolException;
	public ZoneUserResponse removeUser(ZoneUserRequest request) throws DnsshimProtocolException;
		
	public Response zoneExists(ZoneExistsRequest request) throws DnsshimProtocolException;
}
