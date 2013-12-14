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
package br.registro.dnsshim.xfrd.ui.protocol.parser;

import br.registro.dnsshim.xfrd.ui.protocol.RrOperation;
import br.registro.dnsshim.xfrd.ui.protocol.SlaveOperation;

public class RequestParserFactory {
	
	public static UiRequestParser getInstance(String operation) {
		if (operation.equals("newZone")) {
			return NewZoneParser.getInstance();
		} else if (operation.equals("importZone")) {
			return ImportZoneParser.getInstance();
			
		} else if (operation.equals("addRr")) {
			return RrParser.getInstance(RrOperation.ADD);
			
		} else if (operation.equals("removeRr")) {
			return RrParser.getInstance(RrOperation.REMOVE);

		} else if (operation.equals("listZones")) {
			return ListZonesParser.getInstance();
			
		} else if (operation.equals("listZonesBySlaveGroup")) {
			return new ListZonesBySlaveGroupParser();			
			
		} else if (operation.equals("printZone")) {
			return PrintZoneParser.getInstance();
			
		} else if (operation.equals("pubZone")) {
			return PubZoneParser.getInstance();
			
		} else if (operation.equals("removeZone")) {
			return RemoveZoneParser.getInstance();
			
		} else if (operation.equals("zoneVersion")) {
			return ZoneVersionParser.getInstance();
			
		} else if (operation.equals("newKey")) {
			return NewKeyParser.getInstance();
			
		} else if (operation.equals("importKey")) {
			return ImportKeyParser.getInstance();
			
		} else if (operation.equals("removeKey")) {
			return RemoveKeyParser.getInstance();
			
		} else if (operation.equals("changeKeyStatus")) {
			return ChangeKeyStatusParser.getInstance();
			
		} else if (operation.equals("listKeys")) {
			return ListKeysParser.getInstance();

		} else if (operation.equals("listSlaveGroup")) {
			return ListSlaveGroupParser.getInstance();
			
		} else if (operation.equals("printSlaveGroup")) {
			return PrintSlaveGroupParser.getInstance();
			
		} else if (operation.equals("assignSlave")) {
			return null;
			
		} else if (operation.equals("newSlaveGroup")) {
			return SlaveGroupParser.getInstance(SlaveOperation.ADD);
			
		} else if (operation.equals("removeSlaveGroup")) {
			return SlaveGroupParser.getInstance(SlaveOperation.REMOVE);
			
		} else if (operation.equals("addSlave")) {
			return SlaveParser.getInstance(SlaveOperation.ADD);
			 
		} else if (operation.equals("removeSlave")) {
			return SlaveParser.getInstance(SlaveOperation.REMOVE);
			
		} else if (operation == "assignSlaveGroup") {
			return SlaveGroupParser.getInstance(SlaveOperation.ASSIGN);
			
		} else if (operation.equals("unassignSlaveGroup")) {
			return SlaveGroupParser.getInstance(SlaveOperation.UNASSIGN);
			
		} else if (operation.equals("listSlaves")) {
			return ListSlaveParser.getInstance();
			
		} else if (operation.equals("setExpirationPeriod")) {
			return SetExpirationPeriodParser.getInstance();
			
		} else if (operation.equals("newTsigKey")) {
			return NewTsigKeyParser.getInstance();
			
		} else if (operation.equals("removeTsigKey")) {
			return RemoveTsigKeyParser.getInstance();
			
		} else if (operation.equals("listTsigKeys")) {
			return ListTsigKeysParser.getInstance();
			
		} else if (operation.equals("login")) {
			return LoginParser.getInstance();
			
		} else if (operation.equals("logout")) {
			return LogoutParser.getInstance();
			
		} else if (operation.equals("addUser")) {
			return AddUserParser.getInstance();
			
		} else if (operation.equals("changePassword")) {
			return ChangePasswordParser.getInstance();
			
		} else if (operation.equals("hello")) {
			return HelloParser.getInstance();
			
		} else if (operation.equals("addZoneUser")) {
			return AddZoneUserParser.getInstance();
			
		} else if (operation.equals("removeZoneUser")) {
			return RemoveZoneUserParser.getInstance();
			
		} else if (operation.equals("zoneExists")) {
			return ZoneExistsParser.getInstance();
			
		} else if (operation.equals("shutdown")) {
			return ShutdownParser.getInstance();
			
		} else {
			throw new IllegalArgumentException("There's no parser named: " + operation);
		}
	}	
}
