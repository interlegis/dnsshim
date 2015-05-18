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

import java.io.IOException;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.AbstractDecoderOutput;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.IoSession;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.common.server.Server.TransportType;
import br.registro.dnsshim.domain.Opt;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.RrType;
import br.registro.dnsshim.domain.Soa;
import br.registro.dnsshim.domain.Tsig;
import br.registro.dnsshim.domain.logic.TsigCalc;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DomainNameUtil;
import br.registro.dnsshim.xfrd.dao.filesystem.SlaveDao;
import br.registro.dnsshim.xfrd.dao.filesystem.ZoneInfoDao;
import br.registro.dnsshim.xfrd.domain.Slave;
import br.registro.dnsshim.xfrd.domain.ZoneData;
import br.registro.dnsshim.xfrd.domain.ZoneInfo;
import br.registro.dnsshim.xfrd.domain.logic.ZoneDataCache;
import br.registro.dnsshim.xfrd.util.ServerContext;

public class DecoderOutput extends AbstractDecoderOutput {

	private static final Logger logger = Logger.getLogger(DecoderOutput.class);

	@Override
	public void doOutput(Object message, IoSession session)
	throws DnsshimProtocolException {
		if (!(message instanceof DnsMessage)) {
			throw new DnsshimProtocolException(ProtocolStatusCode.DNS_MESSAGE_FORMERR, "Invalid DNS message");
		}

		DnsMessage request = (DnsMessage) message;
		DnsPacket dnsPacket = request.getFirstPacket();
		DnsQuestion question = dnsPacket.getQuestion();
		DnsPacket firstPacket = request.getFirstPacket();
		QueryType queryType = firstPacket.getQuestion().getQtype();
		InetAddress remote = session.getRemoteAddress().getAddress();
		String hostAddress = remote.getHostAddress();
		if (logger.isDebugEnabled()) {
			logger.debug("Got query: " + question + " from " + hostAddress);
		}

		if (firstPacket.getHeader().isQuestion() && 
				(queryType == QueryType.AXFR || queryType == QueryType.IXFR) &&
				firstPacket.getTsig() == null) {			
			SlaveDao dao = new SlaveDao(ServerContext.getEntityManager());
			Slave slave = dao.findByAddress(hostAddress);
			if (slave != null && slave.hasTsig()) {
				logger.warn("Got an unauthorized XFR request from " + hostAddress);
				firstPacket.getHeader().setResponseCode(ResponseCode.NOTAUTH);
				throw new DnsshimProtocolException(ProtocolStatusCode.DNS_MESSAGE_FORMERR, "This host must query with TSIG", firstPacket);
			}
		}
		
		ResponseCode tsigStatus = checkTsig(hostAddress, request);
		if (tsigStatus != ResponseCode.NOERROR) {
			firstPacket.getHeader().setResponseCode(tsigStatus);
			throw new DnsshimProtocolException(ProtocolStatusCode.BAD_TSIG);			
		}

		// Add the same OPT record of the question in the answer
		Opt opt = dnsPacket.getOpt();

		try {
			String zonename = DomainNameUtil.trimDot(question.getQname());
			if ((question.getQtype() == QueryType.AXFR ||
					question.getQtype() == QueryType.IXFR)) {
				EntityManager entityManager = ServerContext.getEntityManager();
				ZoneInfoDao dao = new ZoneInfoDao(entityManager);
				ZoneInfo zoneInfo = dao.findByZonename(zonename);
				if (zoneInfo == null) {
					dnsPacket.getHeader().setResponseCode(ResponseCode.REFUSED);
					throw new DnsshimProtocolException(ProtocolStatusCode.NOT_AUTHORITATIVE, "Not authoritative for " + zonename,
							dnsPacket);
				}
			
				if (remote.getCanonicalHostName().equals("localhost") == false && 
						zoneInfo.hasSlave(remote.getHostAddress()) == false) {
					dnsPacket.getHeader().setResponseCode(ResponseCode.REFUSED);
					throw new DnsshimProtocolException(ProtocolStatusCode.NOT_AUTHORITATIVE, "Unauthorized request from " +
							remote.getHostAddress(), dnsPacket);
				}
			}

			DnsMessageBuilder messageBuilder = null;

			ZoneDataCache zoneDataCache = ZoneDataCache.getInstance();
			ZoneData zoneData = zoneDataCache.get(zonename);
			if (zoneData == null) {
				dnsPacket.getHeader().setResponseCode(ResponseCode.REFUSED);
				throw new DnsshimProtocolException(ProtocolStatusCode.REFUSED_OPERATION, "Zone not found " + zonename, dnsPacket);
			}

			switch (question.getQtype()) {
			case AXFR:
				if (logger.isInfoEnabled()) {
					String remoteAddr = session.getRemoteAddress().getAddress().getHostAddress();
					logger.info("Got AXFR request for zone " + zonename + " from " + remoteAddr);
				}
				messageBuilder = new AxfrMessageBuilder();
				break;
			case IXFR:
				if (logger.isInfoEnabled()) {
					String remoteAddr = session.getRemoteAddress().getAddress().getHostAddress();
					logger.info("Got IXFR request for zone " + zonename + " from " + remoteAddr);
				}
				int serial = 0;
				if (!dnsPacket.getAuthority().isEmpty()) {
					for (ResourceRecord rr : dnsPacket.getAuthority()) {
						if (rr.getType() == RrType.SOA) {
							Soa soa = (Soa) rr;
							serial = soa.getSerial();
							break;
						}
					}
				}

				if (zoneData.containsIxfrSerial(serial)) {
					messageBuilder = new IxfrMessageBuilder(serial, opt);
				} else {
					messageBuilder = new AxfrMessageBuilder();
				}

				break;
			case ANY:
				messageBuilder = new AnyMessageBuilder(opt);
				break;
			default:
				messageBuilder = new RecordMessageBuilder(opt);
				break;
			}

			if (session.getTransportType() == TransportType.TCP) {
				messageBuilder.setMaxPacketSize(DnsPacket.MAX_TCP_PACKET_SIZE);
			} else {
				int udpSize = DnsPacket.MAX_UDP_PACKET_SIZE;
				if (dnsPacket.getOpt() != null) {
					udpSize = ByteUtil.toUnsigned(dnsPacket.getOpt().getUdpPayloadSize());
				}

				messageBuilder.setMaxPacketSize(udpSize);
			}

			messageBuilder.setZoneData(zoneData);
			messageBuilder.setRequestPacket(dnsPacket);
			messageBuilder.process();
			
			session.setAttachment(request.getFirstPacket().getTsig());
			DnsMessage response = messageBuilder.getDnsMessage();
			getEncoder().encode(response, session);
			
		} catch (IOException e) {
			dnsPacket.getHeader().setResponseCode(ResponseCode.SERVFAIL);
			throw new DnsshimProtocolException(ProtocolStatusCode.DNS_SERVER_FAILURE, e.getMessage(), request);
		}	
	}
	
	private ResponseCode checkTsig(String address, DnsMessage dnsMessage)	{
		for (DnsPacket packet : dnsMessage.getPackets()) {
			Tsig tsig = packet.getTsig();
			if (tsig == null) {
				continue;
			}

			String keyName = tsig.getOwnername();
			String secret = TsigCalc.getTsigKey(address, DomainNameUtil.trimDot(keyName));
			if (secret == null) {
				return ResponseCode.NOTAUTH;
			}						

			boolean firstPacket = (packet == dnsMessage.getFirstPacket());
			byte[] bytes = packet.getBytes();

			try {
				ResponseCode status = TsigCalc.checkDigest(bytes, tsig, secret, firstPacket);

				if (logger.isInfoEnabled()) {
					logTsigStatus(status, address);
				}			

				if (status == ResponseCode.BADSIG ||
						status == ResponseCode.BADKEY ||
						status == ResponseCode.BADTIME) {
					return ResponseCode.NOTAUTH;
				}
			} catch (InvalidKeyException e) {
				logger.info("Invalid Tsig key", e);
				return ResponseCode.SERVFAIL;				
			} catch (NoSuchAlgorithmException e) {
				logger.info(e.getMessage(), e);
				return ResponseCode.SERVFAIL;
			}
		}
		
		return ResponseCode.NOERROR;
	}
	
	private void logTsigStatus(ResponseCode tsigStatus, String hostAddress) {
		switch (tsigStatus) {
		case BADSIG:
			logger.warn("TSIG mac check error from '" + hostAddress);
			break;

		case BADKEY:
			logger.warn("TSIG key check error from '" + hostAddress);
			break;

		case BADTIME:
			logger.warn("TSIG time check error from '" + hostAddress);
			break;
		
		default:
			break;
		}
	}
}