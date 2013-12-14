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

import java.util.ArrayList;

import br.registro.dnsshim.domain.DsInfo;
import br.registro.dnsshim.domain.ResourceRecord;

public class PrintZoneResponse extends Response {
	private String zone;
	private ArrayList<ResourceRecord> rrList;
	private ArrayList<DsInfo> dsList;
	
	public PrintZoneResponse() {
		zone = "";
		rrList = new ArrayList<ResourceRecord>();
		dsList = new ArrayList<DsInfo>();
	}
	
	public String getZone() {
		return zone;
	}
	public void setZone(String zone) {
		this.zone = zone;
	}
	public ArrayList<ResourceRecord> getRrList() {
		return rrList;
	}
	public void setRrList(ArrayList<ResourceRecord> rrList) {
		this.rrList = rrList;
	}
	
	public void addRr(ResourceRecord rr) {
		rrList.add(rr);
	}
	
	public ArrayList<DsInfo> getDsList() {
		return dsList;
	}
	
	public void addDs(DsInfo dsInfo) {
		dsList.add(dsInfo);
	}
}
