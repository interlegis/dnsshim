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
package br.registro.dnsshim.xfrd.dns.protocol;

import java.util.ArrayList;
import java.util.List;

public class DnsMessage {
	private String zone;
	private List<DnsPacket> packets = new ArrayList<DnsPacket>();
	
	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

	public List<DnsPacket> getPackets() {
		return packets;
	}
	
	public DnsPacket getFirstPacket() {
		return packets.get(0);
	}
	
	public DnsPacket getLastPacket() {
		int size = packets.size();
		if (size > 0) {
			return packets.get(size - 1);
		}
		
		return null;
	}

	public void setPackets(List<DnsPacket> packets) {
		this.packets = packets;
	}
	
	public void addPacket(DnsPacket packet) {
		this.packets.add(packet);
	}
	
	public void reset() {
		zone = "";
		packets.clear();
	}
	
	@Override
	public String toString() {
		StringBuilder msg = new StringBuilder();
		
		msg.append(zone).append(System.getProperty("line.separator"));
		for (DnsPacket packet: packets) {
			msg.append(packet).append(System.getProperty("line.separator"));
		}
		
		return msg.toString();
	}
}