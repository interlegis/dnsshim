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

import java.util.Collection;

import br.registro.dnsshim.domain.Opt;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.util.ResourceRecordUtil;
import br.registro.dnsshim.xfrd.dns.util.DnsMessageUtil;
import br.registro.dnsshim.xfrd.domain.ZoneData;

public abstract class DnsMessageBuilder {
	protected static final short ZERO = 0;
	private int maxPacketSize;
	private ZoneData zoneData;
	protected Opt opt;
	
	private DnsMessage dnsMessage;
	protected DnsPacket request;
	protected DnsPacket response;
	
	private int packetLength;
	
	public DnsMessageBuilder() {
		maxPacketSize = DnsPacket.MAX_UDP_PACKET_SIZE;
		zoneData = null;
		dnsMessage = new DnsMessage();
		opt = null;
	}
	
	/**
	 * Added OPT RR from question in response
	 */
	public DnsMessageBuilder(Opt opt) {
		maxPacketSize = DnsPacket.MAX_UDP_PACKET_SIZE;
		zoneData = null;
		dnsMessage = new DnsMessage();
		this.opt = opt;
	}
	
	public DnsMessage getDnsMessage() {
		return dnsMessage;
	}

	public void setDnsMessage(DnsMessage dnsMessage) {
		this.dnsMessage = dnsMessage;
	}

	public DnsPacket getResponsePacket() {
		return response;
	}

	public void setResponsePacket(DnsPacket response) {
		this.response = response;
	}

	public void setZoneData(ZoneData zoneData) {
		dnsMessage.setZone(zoneData.getZonename());
		this.zoneData = zoneData;
	}

	public ZoneData getZoneData() {
		return zoneData;
	}

	public DnsPacket getRequestPacket() {
		return request;
	}

	public void setRequestPacket(DnsPacket request) {
		this.request = request;
	}

	public int getMaxPacketSize() {
		return maxPacketSize;
	}

	public void setMaxPacketSize(int maxPacketSize) {
		this.maxPacketSize = maxPacketSize;
	}

	protected DnsPacket createNewPacket(boolean addQuestion) {
		DnsHeader header = new DnsHeader(request.getHeader());
		header.setQuestion(false);
		header.setAuthoritativeAnswer(true);
		header.setAnswerCount(ZERO);
		header.setAuthorityCount(ZERO);
		header.setAdditionalCount(ZERO);
		if (addQuestion == false) {
			header.setQuestionCount(ZERO);
		}

		DnsPacket response = new DnsPacket();
		packetLength = DnsHeader.HEADER_SIZE;
		if (addQuestion == true) {
			response.setQuestion(new DnsQuestion(request.getQuestion()));
			packetLength += DnsMessageUtil.questionLength(request.getQuestion());
		}
		
		if (opt != null) {
			response.setOpt(opt);
			header.setAdditionalCount((short) 1);
			packetLength += ResourceRecordUtil.optLength(opt);
		}
		
		response.setHeader(header);
		return response;
	}
	
	protected void addResponsePacket(DnsPacket response) {
		dnsMessage.addPacket(response);
	}
	
	protected void addAllAnswerRecords(Collection<ResourceRecord> records) {
		for (ResourceRecord record : records) {
			addAnswerRecord(record);
		}
	}
	
	protected void addAnswerRecord(ResourceRecord record) {
		if (response == null) {
			response = createNewPacket(true);
		}

		int recordLength = ResourceRecordUtil.rrLength(zoneData.getZonename(), record);
	
		if (packetLength + recordLength > maxPacketSize) {
			int answerCount = response.getAnswer().size();
			response.getHeader().setAnswerCount((short) answerCount);
			addResponsePacket(response);
			response = createNewPacket(false);
		}

		packetLength += recordLength;
		response.addAnswer(record);
	}
	
	protected void addAllAuthorityRecords(Collection<ResourceRecord> records) {
		for (ResourceRecord record : records) {
			addAuthorityRecord(record);
		}
	}
	
	protected void addAuthorityRecord(ResourceRecord record) {
		if (response == null) {
			response = createNewPacket(true);
		}

		int recordLength = ResourceRecordUtil.rrLength(zoneData.getZonename(), record);
	
		if (packetLength + recordLength > maxPacketSize) {
			int authorityCount = response.getAuthority().size();
			response.getHeader().setAuthorityCount((short) authorityCount);
			addResponsePacket(response);
			response = createNewPacket(false);
		}

		packetLength += recordLength;
		response.addAuthority(record);
	}

	public abstract void process();

}
