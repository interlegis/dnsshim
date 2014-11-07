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
package br.registro.dnsshim.common.server;

import java.util.ArrayList;
import java.util.List;

import br.registro.dnsshim.xfrd.util.CloseDatabaseSessionFilter;

public class IoFilterChain {
	private List<IoFilter> filterChain = new ArrayList<IoFilter>();
	
	private boolean mayCloseIO = true;

	//TODO: check fix
	public void setMayCloseIO(boolean mayCloseIO) {
		this.mayCloseIO = mayCloseIO;
	}

	public IoFilterChain add(IoFilter filter) {
		filterChain.add(filter);
		return this;
	}
	
	public void filter(Object message, IoSession session) throws DnsshimProtocolException{
		for (IoFilter filter : filterChain) {
			//TODO: check fix
			if (filter instanceof CloseDatabaseSessionFilter){
				((CloseDatabaseSessionFilter)filter).setMayClose(mayCloseIO);
			}
			filter.filter(message, session);
		}
	}
	
	
}
