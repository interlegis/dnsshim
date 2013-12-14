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

import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.util.DomainNameUtil;

/**
 * Based from RFC 1035
 * 
 * <pre>
 *                                 1  1  1  1  1  1
 *   0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * |                                               |
 * /                     QNAME                     /
 * /                                               /
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * |                     QTYPE                     |
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * |                     QCLASS                    |
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * </pre>
 * 
 */
public class DnsQuestion {

	private String qname;
	private QueryType qtype;
	private DnsClass qclass;

	public DnsQuestion() {
		qname = "";
		qtype = QueryType.SOA;
		qclass = DnsClass.IN;
	}
	
	public DnsQuestion(DnsQuestion question) {
		qname = question.qname;
		qtype = question.qtype;
		qclass = question.qclass;
	}

	public String getQname() {
		return qname;
	}

	public void setQname(String qname) {
		this.qname = qname;
	}

	public QueryType getQtype() {
		return qtype;
	}

	public void setQtype(QueryType qtype) {
		this.qtype = qtype;
	}

	public DnsClass getQclass() {
		return qclass;
	}

	public void setQclass(DnsClass qclass) {
		this.qclass = qclass;
	}
	
	public int lengthWirFormat() {
		int length = 0;
		
		length += DomainNameUtil.toWireFormat(qname).length;
		length += 2 + 2;
		
		return length;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(qname).append(" ");
		result.append(qtype).append(" ");
		result.append(qclass);
		
		return result.toString();
	}
}
