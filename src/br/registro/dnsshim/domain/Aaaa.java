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
package br.registro.dnsshim.domain;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.apache.commons.validator.routines.InetAddressValidator;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.util.ByteUtil;

public class Aaaa extends ResourceRecord {
	final private Inet6Address addr;

	public Aaaa(String ownername, DnsClass dnsClass, int ttl, String ipv6)
		throws DnsshimProtocolException {
		super(ownername, RrType.AAAA, dnsClass, ttl);
		try {
			// is a IPv4 ?
		    if (InetAddressValidator.getInstance().isValid(ipv6)){
		    	throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Invalid IPv6 address: " + ipv6);
		    }
		    InetAddress inet = InetAddress.getByName(ipv6);
		    
		    if ((inet instanceof Inet6Address) == false) {
		    	throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Invalid IPv6 address: " + ipv6);
		    }
		    
			this.addr = (Inet6Address) InetAddress.getByName(ipv6);
		} catch (UnknownHostException uhe) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Invalid IPv6 address: " + ipv6);
		}
		
		this.rdata = RdataAaaaBuilder.get(ipv6);
	}

	public String getIpv6() {
		return addr.getHostAddress();
	}
	
	public String rdataPresentation() {
		return addr.getHostAddress();
	}

	public static Aaaa parseAaaa(String ownername, DnsClass dnsClass, int ttl,
			ByteBuffer buffer)
		throws DnsshimProtocolException {
		int rdLength = ByteUtil.toUnsigned(buffer.getShort());
		byte[] rdata = new byte[rdLength];
		buffer.get(rdata);
		try {
			Inet6Address addr = (Inet6Address) InetAddress.getByAddress(rdata);
			String ipv6 = addr.getHostAddress();
			return new Aaaa(ownername, dnsClass, ttl, ipv6);
		} catch (UnknownHostException uhe) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Invalid AAAA Resource Record");
		}
	}
}

