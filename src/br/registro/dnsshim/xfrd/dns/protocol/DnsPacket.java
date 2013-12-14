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

import br.registro.dnsshim.domain.Opt;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.Tsig;

/**
 * RFC 1035 - 4. MESSAGES
 * 
 * <pre>
 * All communications inside of the domain protocol are carried in a single
 * format called a message.  The top level format of message is divided
 * into 5 sections (some of which are empty in certain cases) shown below:
 * 
 *  +---------------------+
 *  |        Header       |
 *  +---------------------+
 *  |       Question      | the question for the name server
 *  +---------------------+
 *  |        Answer       | RRs answering the question
 *  +---------------------+
 *  |      Authority      | RRs pointing toward an authority
 *  +---------------------+
 *  |      Additional     | RRs holding additional information
 *  +---------------------+
 * 
 * </pre>
 * 
 * 
 */
public class DnsPacket {
	private DnsHeader header;
	private DnsQuestion question;

	private List<ResourceRecord> answer;
	private List<ResourceRecord> authority;
	private List<ResourceRecord> additional;

	/**
	 * The OPT record is removed from the additional section of the DNS message 
	 * and stored separately for a better design  
	 */
	private Opt opt;
	
	/**
	 * RFC 2845 - 2.1 - TSIG is a meta-RR. </br>
	 * RFC 2845 - 3.2 - Upon receipt of a message with a correctly placed TSIG RR,
	 * the TSIG RR is copied to a safe location, removed from the DNS
	 */
	private Tsig tsig;
	
	private byte[] bytes;
	
	/* 64 * 1024 - tolerance */
	public static final int MAX_TCP_PACKET_SIZE = 65536 - 1000; 
	public static final int MAX_UDP_PACKET_SIZE = 512;

	public DnsPacket() {
		header = new DnsHeader();
		question = new DnsQuestion();

		answer = new ArrayList<ResourceRecord>();
		authority = new ArrayList<ResourceRecord>();
		additional = new ArrayList<ResourceRecord>();

		opt = null;
		tsig = null;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public DnsHeader getHeader() {
		return header;
	}

	public void setHeader(DnsHeader header) {
		this.header = header;
	}

	public DnsQuestion getQuestion() {
		return question;
	}

	public void setQuestion(DnsQuestion question) {
		this.question = question;
	}

	public List<ResourceRecord> getAnswer() {
		return answer;
	}

	public void setAnswer(List<ResourceRecord> answer) {
		this.answer = answer;
	}

	public void addAnswer(ResourceRecord record) {
		this.answer.add(record);
	}
	
	public boolean removeAnswer(ResourceRecord rr) {
		return answer.remove(rr);
	}
	
	public void clearAnswer() {
		this.answer.clear();
	}

	public List<ResourceRecord> getAuthority() {
		return authority;
	}

	public void setAuthority(List<ResourceRecord> authority) {
		this.authority = authority;
	}

	public void addAuthority(ResourceRecord record) {
		authority.add(record);
	}

	public boolean removeAuthority(ResourceRecord rr) {
		return authority.remove(rr);
	}
	
	public void clearAuthority() {
		this.authority.clear();
	}

	public List<ResourceRecord> getAdditional() {
		return additional;
	}
	
	public void setAdditional(List<ResourceRecord> additional) {
		this.additional = additional;
	}

	public void addAdditional(ResourceRecord record) {
		additional.add(record);
	}

	public boolean removeAdditional(ResourceRecord rr) {
		return additional.remove(rr);
	}
	
	public void clearAdditional() {
		this.additional.clear();
	}
	
	public void setOpt(Opt opt) {
		this.opt = opt;
	}
	
	public Opt getOpt() {
		return opt;
	}
	
	public void setTsig(Tsig tsig) {
		this.tsig = tsig;
	}
		
	public Tsig getTsig() {		
		return tsig;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append(header.toString()).append(System.getProperty("line.separator"));
		result.append(question.toString()).append(System.getProperty("line.separator"));

		result.append(System.getProperty("line.separator"));
		result.append("ANSWER: ");
		result.append(System.getProperty("line.separator"));
		
		for (ResourceRecord record : answer) {
			result.append(record.toString()).append(System.getProperty("line.separator"));
		}

		result.append(System.getProperty("line.separator"));
		result.append("AUTHORITY: ");
		result.append(System.getProperty("line.separator"));
		for (ResourceRecord record : authority) {
			result.append(record.toString()).append(System.getProperty("line.separator"));
		}

		result.append(System.getProperty("line.separator"));
		result.append("ADDITIONAL: ");
		result.append(System.getProperty("line.separator"));
		for (ResourceRecord record : additional) {
			result.append(record.toString()).append(System.getProperty("line.separator"));
		}

		result.append(System.getProperty("line.separator"));
		result.append("OPT: ");
		result.append(System.getProperty("line.separator"));
		result.append(opt);

		result.append(System.getProperty("line.separator"));
		result.append("TSIG: ");
		result.append(System.getProperty("line.separator"));
		result.append(tsig);
		
		return result.toString();
	}
}
