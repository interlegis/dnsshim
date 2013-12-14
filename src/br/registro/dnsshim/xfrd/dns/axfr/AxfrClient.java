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
package br.registro.dnsshim.xfrd.dns.axfr;

import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.common.server.TcpIoSession;
import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.RrType;
import br.registro.dnsshim.domain.Rrset;
import br.registro.dnsshim.domain.Soa;
import br.registro.dnsshim.domain.Zone;
import br.registro.dnsshim.util.DomainNameUtil;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneInfoDao;
import br.registro.dnsshim.xfrd.dns.protocol.ClientDecoderOutput;
import br.registro.dnsshim.xfrd.dns.protocol.Decoder;
import br.registro.dnsshim.xfrd.dns.protocol.DnsHeader;
import br.registro.dnsshim.xfrd.dns.protocol.DnsMessage;
import br.registro.dnsshim.xfrd.dns.protocol.DnsPacket;
import br.registro.dnsshim.xfrd.dns.protocol.DnsQuestion;
import br.registro.dnsshim.xfrd.dns.protocol.Encoder;
import br.registro.dnsshim.xfrd.dns.protocol.EncoderOutput;
import br.registro.dnsshim.xfrd.dns.protocol.Opcode;
import br.registro.dnsshim.xfrd.dns.protocol.QueryType;
import br.registro.dnsshim.xfrd.dns.protocol.ResponseCode;
import br.registro.dnsshim.xfrd.domain.User;
import br.registro.dnsshim.xfrd.domain.ZoneDataPrePublish;
import br.registro.dnsshim.xfrd.domain.ZoneInfo;
import br.registro.dnsshim.xfrd.domain.logic.DnsshimSession;
import br.registro.dnsshim.xfrd.domain.logic.DnsshimSessionCache;
import br.registro.dnsshim.xfrd.domain.logic.ZoneDataPrePublishCache;
import br.registro.dnsshim.xfrd.util.ServerContext;

public class AxfrClient {
	private String host;
	private int port;
	private static final Logger logger = Logger.getLogger(AxfrClient.class);

	public AxfrClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void requestAxfr(String zonename, String sessionId) 
		throws IOException, DnsshimProtocolException {
		TcpIoSession session = new TcpIoSession();
		try {
			Socket socket = new Socket(host, port);
			session.setClientSocket(socket);
			Decoder decoder = new Decoder();
			Encoder encoder = new Encoder();
			ClientDecoderOutput decoderOutput = new ClientDecoderOutput();
			EncoderOutput encoderOutput = new EncoderOutput();
			
			decoder.setOutput(decoderOutput);
			encoder.setOutput(encoderOutput);
			
			DnsMessage request = buildAxfrRequest(zonename);
			encoder.encode(request, session);
			decoder.decode(session);
	
			DnsMessage response = (DnsMessage) decoderOutput.getResponse();
			Zone zone = parseAxfr(response);
			
			Rrset soaRrset = zone.getRrset(ResourceRecord.APEX_OWNERNAME, RrType.SOA, DnsClass.IN);
			Iterator<ResourceRecord> soaIt = soaRrset.getRecords().iterator();
			
			Soa soa = null;
			if (soaIt.hasNext()) {
				soa = (Soa) soaIt.next();
			} else {
				logger.warn("No SOA RR found in the AXFR response");
				throw new DnsshimProtocolException(ProtocolStatusCode.NO_SOA);
			}
			
			ZoneInfo zoneInfo = new ZoneInfo();
			zoneInfo.setZonename(zonename);
			zoneInfo.setSoaMinimum(soa.getMinimum());
			zoneInfo.setTtlZone(soa.getTtl());
			zoneInfo.setTtlDnskey(soa.getTtl());
			zoneInfo.setValidity(2592000); // 30 days
			zoneInfo.setSigned(false);
			
			DnsshimSessionCache dnsshimSessionCache = DnsshimSessionCache.getInstance();
			DnsshimSession dnsshimSession = dnsshimSessionCache.get(sessionId);
			User user= (User) dnsshimSession.getAttribute("user");
			if (user!= null) {
				zoneInfo.addUser(user);
			}
			
			ZoneInfoDao zoneInfoDao = new ZoneInfoDao(ServerContext.getEntityManager());
			zoneInfoDao.save(zoneInfo);

			ZoneDataPrePublish zoneDataPre = new ZoneDataPrePublish(zone);
			ZoneDataPrePublishCache zoneDataPrePubCache = ZoneDataPrePublishCache.getInstance();
			zoneDataPrePubCache.put(zoneDataPre);
		} finally {
			session.close();
		}	
	}

	public DnsMessage buildAxfrRequest(String zonename) {
		DnsPacket packet = new DnsPacket();
		
		DnsHeader header = new DnsHeader();
		header.setQuestion(true);
		header.setOpcode(Opcode.QUERY);
		header.setAuthoritativeAnswer(false);
		header.setTruncatedResponse(false);
		header.setRecursionDesired(false);
		header.setRecursionAvailable(false);
		header.setResponseCode(ResponseCode.NOERROR);
		header.setQuestionCount((short) 1);
		header.setAnswerCount((short) 0);
		header.setAuthorityCount((short) 0);
		header.setAdditionalCount((short) 0);
		packet.setHeader(header);
		
		DnsQuestion question = new DnsQuestion();
		question.setQname(zonename);
		question.setQclass(DnsClass.IN);
		question.setQtype(QueryType.AXFR);
		packet.setQuestion(question);
		
		DnsMessage request = new DnsMessage();
		request.setZone(zonename);
		request.addPacket(packet);
		return request;
	}
	
	private Zone parseAxfr(DnsMessage message)
		throws DnsshimProtocolException {
		List<DnsPacket> packets = message.getPackets();
		
		String zonename = DomainNameUtil.trimDot(message.getZone());
		Zone zone = new Zone(zonename);
		for (DnsPacket packet : packets) {
			DnsHeader header = packet.getHeader();
			if (header.getResponseCode() != ResponseCode.NOERROR) {
				logger.warn("Transfer of zone " + zonename +
						" not allowed by server");
				throw new DnsshimProtocolException(ProtocolStatusCode.TRANSFER_NOT_ALLOWED);
			}

			if (header.isAuthoritativeAnswer() == false) {
				logger.warn("Got a response without the AA bit set");
				throw new DnsshimProtocolException(ProtocolStatusCode.NOT_AUTHORITATIVE);
			}

			List<ResourceRecord> rrList = packet.getAnswer();
			for (ResourceRecord rr : rrList) {
				RrType type = rr.getType();
				if (packet == packets.get(0) && rr == rrList.get(0) && type != RrType.SOA) {
					throw new DnsshimProtocolException(ProtocolStatusCode.NO_SOA, "First RR in the Answer section isn't a SOA");
				}

				if (rr != rrList.get(0) && type == RrType.SOA) {
					// Another SOA means last RR
					break;
				}

				if (type.isDnssec()) {
					continue;
				}

				String ownername = rr.getOwnername();
				ownername =	DomainNameUtil.removeZoneFromName(ownername, zonename);
				rr.setOwnername(ownername);
				zone.add(rr);
			}
		}
		
		return zone;
	}
}
