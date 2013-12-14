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

import br.registro.dnsshim.xfrd.ui.protocol.writer.ChangeKeyStatusResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.DefaultResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.ImportKeyResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.ImportZoneResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.ListKeysResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.ListSlaveGroupResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.ListSlavesResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.ListTsigKeysResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.ListZonesBySlaveGroupResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.ListZonesResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.LoginResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.NewKeyResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.NewTsigKeyResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.NewZoneResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.PrintSlaveGroupResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.PrintZoneResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.PubZoneResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.RemoveKeyResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.RemoveTsigKeyResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.RemoveZoneResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.RrResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.SetExpirationPeriodResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.SlaveResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.UiResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.ZoneVersionResponseWriter;

public class UiResponseFactory {
	@SuppressWarnings("rawtypes")
	public static UiResponseWriter get(Object message) {
		UiResponseWriter uiResponseWriter = null;

		if (message instanceof NewZoneResponse) {
			uiResponseWriter = NewZoneResponseWriter.getInstance();
		} else if (message instanceof ImportZoneResponse) {
			uiResponseWriter = ImportZoneResponseWriter.getInstance();
		} else if (message instanceof RrResponse) {
			uiResponseWriter = RrResponseWriter.getInstance();
		} else if (message instanceof ListZonesResponse) {
			uiResponseWriter = ListZonesResponseWriter.getInstance();
		} else if (message instanceof ListZonesBySlaveGroupResponse) {
			uiResponseWriter = ListZonesBySlaveGroupResponseWriter.getInstance();
		} else if (message instanceof PrintZoneResponse) {
			uiResponseWriter = PrintZoneResponseWriter.getInstance();
		} else if (message instanceof PubZoneResponse) {
			uiResponseWriter = PubZoneResponseWriter.getInstance();
		} else if (message instanceof RemoveZoneResponse) {
			uiResponseWriter = RemoveZoneResponseWriter.getInstance();
		} else if (message instanceof ZoneVersionResponse) {
			uiResponseWriter = ZoneVersionResponseWriter.getInstance();
		} else if (message instanceof NewKeyResponse) {
			uiResponseWriter = NewKeyResponseWriter.getInstance();
		} else if (message instanceof ImportKeyResponse) {
			uiResponseWriter = ImportKeyResponseWriter.getInstance();
		} else if (message instanceof RemoveKeyResponse) {
			uiResponseWriter = RemoveKeyResponseWriter.getInstance();
		} else if (message instanceof ChangeKeyStatusResponse) {
			uiResponseWriter = ChangeKeyStatusResponseWriter.getInstance();
		} else if (message instanceof ListKeysResponse) {
			uiResponseWriter = ListKeysResponseWriter.getInstance();
		} else if (message instanceof SlaveResponse) {
			uiResponseWriter = SlaveResponseWriter.getInstance();
		} else if (message instanceof ListSlaveGroupResponse) {
			uiResponseWriter = ListSlaveGroupResponseWriter.getInstance();
		} else if (message instanceof PrintSlaveGroupResponse) {
			uiResponseWriter = PrintSlaveGroupResponseWriter.getInstance();
		} else if (message instanceof ListSlavesResponse) {
			uiResponseWriter = ListSlavesResponseWriter.getInstance();
		} else if (message instanceof SetExpirationPeriodResponse) {
			uiResponseWriter = SetExpirationPeriodResponseWriter.getInstance();
		} else if (message instanceof NewTsigKeyResponse) {
			uiResponseWriter = NewTsigKeyResponseWriter.getInstance();
		} else if (message instanceof RemoveTsigKeyResponse) {
			uiResponseWriter = RemoveTsigKeyResponseWriter.getInstance();
		} else if (message instanceof ListTsigKeysResponse) {
			uiResponseWriter = ListTsigKeysResponseWriter.getInstance();
		} else if (message instanceof LoginResponse) {
			uiResponseWriter = LoginResponseWriter.getInstance();
		} else if (message instanceof Response) {
			uiResponseWriter = DefaultResponseWriter.getInstance();
		}
		return uiResponseWriter;
	}
}
