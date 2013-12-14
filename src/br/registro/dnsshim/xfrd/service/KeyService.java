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
import br.registro.dnsshim.xfrd.ui.protocol.ImportKeyRequest;
import br.registro.dnsshim.xfrd.ui.protocol.ImportKeyResponse;
import br.registro.dnsshim.xfrd.ui.protocol.ListKeysResponse;
import br.registro.dnsshim.xfrd.ui.protocol.NewKeyRequest;
import br.registro.dnsshim.xfrd.ui.protocol.NewKeyResponse;

public interface KeyService {
	public NewKeyResponse newKey(NewKeyRequest request)
		throws DnsshimProtocolException, IOException;
	public ListKeysResponse listKeys(String zone)
		throws IOException, DnsshimProtocolException;
	public ImportKeyResponse importKey(ImportKeyRequest request)
		throws IOException, DnsshimProtocolException;
}
