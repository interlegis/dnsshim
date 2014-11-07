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
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.AbstractProtocolEncoder;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ErrorMessage;
import br.registro.dnsshim.common.server.IoSession;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.common.server.Server.TransportType;
import br.registro.dnsshim.domain.Opt;
import br.registro.dnsshim.domain.Rdata;
import br.registro.dnsshim.domain.RdataTsigBuilder;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.Tsig;
import br.registro.dnsshim.domain.logic.TsigCalc;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DateUtil;
import br.registro.dnsshim.util.DomainNameUtil;
import br.registro.dnsshim.xfrd.dns.util.DnsMessageUtil;
import br.registro.dnsshim.xfrd.domain.XfrdConfig;
import br.registro.dnsshim.xfrd.domain.logic.XfrdConfigManager;
import br.registro.dnsshim.xfrd.util.ServerContext;

public class Encoder extends AbstractProtocolEncoder {
	protected static final short ZERO = 0;
	static Logger logger = Logger.getLogger(Encoder.class);

	@Override
	public void encode(Object message, IoSession session)
		throws DnsshimProtocolException, IOException {
		
		try {

		DnsMessage dnsMessage = (DnsMessage) message;
		String zonename = dnsMessage.getZone();
		TransportType transport = session.getTransportType();
		
		// If there's more than 1 packet and transport is UDP, send only the first
		// and set TC bit
		List<DnsPacket> packets = dnsMessage.getPackets();
		if (transport == TransportType.UDP && packets.size() > 1) {
			DnsPacket packet = packets.get(0);
			packet.getHeader().setTruncatedResponse(true);
			packets.clear();
			packets.add(packet);
		}

		for (int packetNumber = 0; packetNumber < packets.size(); packetNumber++) {
			DnsPacket packet = packets.get(packetNumber);
			
			// Java does not have unsigned short, but we need it anyway. Using char...
			char responseLength = (char) DnsMessageUtil.packetLengthWithoutTsig(zonename, packet);

			// responseLength + tolerance for tsig (1000)
			ByteBuffer buffer = ByteBuffer.allocate(responseLength + 1000);
			
			if (transport == TransportType.TCP) {
				buffer.putChar(responseLength);
			} else if (transport == TransportType.UDP) {
				int maxUdpSize = DnsPacket.MAX_UDP_PACKET_SIZE;
				if (packet.getOpt() != null) {
					maxUdpSize = ByteUtil.toUnsigned(packet.getOpt().getUdpPayloadSize());
				}
				
				if (responseLength > maxUdpSize) {
					packet.getHeader().setTruncatedResponse(true);
					packet.clearAnswer();
					packet.clearAuthority();
					packet.clearAdditional();
				}
			}

			Opt opt = packet.getOpt();
			
			// Header
			DnsHeader header = packet.getHeader();
			buffer.putShort(header.getId());

			short headerShort = 0;
			headerShort = ByteUtil.setBit(headerShort, 1, header.isQuestion());
			headerShort = ByteUtil.setBits(headerShort, header.getOpcode().getValue(), 2, 4);
			headerShort = ByteUtil.setBit(headerShort, 6, header.isAuthoritativeAnswer());
			headerShort = ByteUtil.setBit(headerShort, 7, header.isTruncatedResponse());
			headerShort = ByteUtil.setBit(headerShort, 8, header.isRecursionDesired());
			headerShort = ByteUtil.setBit(headerShort, 9, header.isRecursionAvailable());
			headerShort = ByteUtil.setBit(headerShort, 10, header.isReserved());
			headerShort = ByteUtil.setBit(headerShort, 11, header.isAuthenticData());
			headerShort = ByteUtil.setBit(headerShort, 12, header.isCheckingDisabled());
			headerShort = ByteUtil.setBits(headerShort, header.getResponseCode().getValue(), 13, 4);

			buffer.putShort(headerShort);
			
			buffer.putShort(header.getQuestionCount());
			buffer.putShort(header.getAnswerCount());
			buffer.putShort(header.getAuthorityCount());
			short additionalCount = header.getAdditionalCount();
			if (opt != null) {
				additionalCount++;
			}
			int positionAdditionalCount = buffer.position();
			buffer.putShort(additionalCount);

			if (header.getQuestionCount() > 0) {
				// Question
				DnsQuestion question = packet.getQuestion();
				buffer.put(DomainNameUtil.toWireFormat(question.getQname()));
				buffer.putShort(question.getQtype().getValue());
				buffer.putShort(question.getQclass().getValue());
			}

			// Answer
			for (ResourceRecord record : packet.getAnswer()) {
				inputRecord(buffer, zonename, record);
			}

			// Authority
			for (ResourceRecord record : packet.getAuthority()) {
				inputRecord(buffer, zonename, record);
			}
			
			// Additional
			for (ResourceRecord record : packet.getAdditional()) {
				inputRecord(buffer, zonename, record);
			}		
			
			if (opt != null) {
				inputOptRecord(buffer, zonename, opt);
			}
			
			Tsig tsigRequest = (Tsig) session.getAttachment();
			if (tsigRequest != null) {

				// Generate TSIG				
				InetAddress remote = session.getRemoteAddress().getAddress();
				String host = remote.getHostAddress();
				String keyName = DomainNameUtil.trimDot(tsigRequest.getOwnername());

				String tsigKey = TsigCalc.getTsigKey(host, keyName);				
								
				try {
					byte[] data;
					if (session.getTransportType() == TransportType.TCP) {
						data = new byte[buffer.position() - 2];
						buffer.position(2);						
					} else {
						data = new byte[buffer.position()];
						buffer.position(0);
					}
					
					buffer.get(data);
					
					/*
					 * RFC 2845 - 4.4 - The TSIG MUST be included on the first and last
					 * DNS envelopes. It is expensive to include it on every envelopes,
					 * but it MUST be placed on at least every 100'th envelope.
					 */
					boolean completeTsig = false;
					if (packetNumber == 0 || packetNumber + 1 == packets.size() ||
							packetNumber % 100 == 0) {
						completeTsig = true;
					} 
					
					Tsig tsigResponse = generateTsigResponse(header.getId(), tsigRequest, tsigKey, completeTsig, data);

					inputTsigRecord(buffer, zonename, tsigResponse);
					buffer.mark();
					buffer.putShort(positionAdditionalCount, (short) (buffer.getShort(positionAdditionalCount) + 1));
					if (session.getTransportType() == TransportType.TCP) {
						buffer.putShort(0, (short) (buffer.reset().position() - 2));
					}	
					
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					packet.getHeader().setResponseCode(ResponseCode.SERVFAIL);
					throw new DnsshimProtocolException(ProtocolStatusCode.DNS_SERVER_FAILURE, "Error generating TSIG", packet);
				}
			}
			
			buffer.flip();
			
			// if  transport is UDP and packet + TSIG > maxUdp, set TC bit and recalculate TSIG
			if (transport == TransportType.UDP) {
				int maxUdpSize = DnsPacket.MAX_UDP_PACKET_SIZE;
				if (packet.getOpt() != null) {
					maxUdpSize = ByteUtil.toUnsigned(packet.getOpt().getUdpPayloadSize());
				}
				if (buffer.limit() > maxUdpSize) {
					packet.getHeader().setTruncatedResponse(true);
					packet.clearAnswer();
					packet.clearAuthority();
					packet.clearAdditional();

					encode(dnsMessage, session);
					return;
				}
			}
			//TODO: check fix
			getOutput().write(buffer, session, false);
		}
		//TODO: check fix
		}
		finally {

			EntityManager entityManager = ServerContext.getEntityManager();

			if (entityManager != null && entityManager.isOpen()) {
				entityManager.close();
			}
		}
	}

	/**
	 * Format Records: All RRs have the same top level format shown below:
	 * 
	 * <pre>
	 *                                     1  1  1  1  1  1
	 *       0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
	 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
	 *     |                                               |
	 *     /                                               /
	 *     /                      NAME                     /
	 *     |                                               |
	 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
	 *     |                      TYPE                     |
	 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
	 *     |                     CLASS                     |
	 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
	 *     |                      TTL                      |
	 *     |                                               |
	 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
	 *     |                   RDLENGTH                    |
	 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--|
	 *     /                     RDATA                     /
	 *     /                                               /
	 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
	 * </pre>
	 */
	private void inputRecord(ByteBuffer buffer, String zonename, ResourceRecord record) {
		buffer.put(DomainNameUtil.toWireFormat(record.getOwnername(), zonename));
		buffer.putShort(record.getType().getValue());
		buffer.putShort(record.getDnsClass().getValue());
		buffer.putInt(record.getTtl());
		buffer.putShort(record.getRdata().getRdlen());
		buffer.put(record.getRdata().getData());
	}

	private void inputOptRecord(ByteBuffer buffer, String zonename, Opt opt) {
		buffer.put((byte) 0);
		buffer.putShort(opt.getType().getValue());
		buffer.putShort(opt.getUdpPayloadSize());
		buffer.put(opt.getExtendedRcode());
		buffer.put(opt.getVersion());
		buffer.putShort(opt.getZ());
		buffer.putShort(opt.getRdata().getRdlen());
		buffer.put(opt.getRdata().getData());
	}
	
	private int inputTsigRecord(ByteBuffer buffer, String zonename, Tsig tsig) {
		byte[] ownername = DomainNameUtil.toWireFormat(tsig.getOwnername());
		
		buffer.put(ownername);
		buffer.putShort(tsig.getType().getValue());
		buffer.putShort(tsig.getDnsClass().getValue());
		buffer.putInt(tsig.getTtl());
		buffer.putShort(tsig.getRdata().getRdlen());
		buffer.put(tsig.getRdata().getData());
		
		return ownername.length + 2 + 2 + 4 + 2 + tsig.getRdata().getRdlen();
	}
	
	private Tsig generateTsigResponse(short originalId, Tsig tsigRequest, String key, boolean completeTsig, byte[] data) throws InvalidKeyException, NoSuchAlgorithmException {

		long timeSigned = DateUtil.removeMillisFromEpoch(Calendar.getInstance().getTime().getTime());
		short fudge;
		
		XfrdConfig config = XfrdConfigManager.getInstance();
		fudge = config.getTsigFudge();
												
		ResponseCode error = tsigRequest.getError(); 							

		byte[] dummyMac = new byte[0];
		Rdata envelope = RdataTsigBuilder.get(tsigRequest.getAlgorithmName(), timeSigned, fudge, dummyMac, originalId, error, tsigRequest.getOtherData());
		Tsig dummyTsig = new Tsig(tsigRequest.getOwnername(), envelope);
		
		byte[] mac = TsigCalc.generateTsigMac(data, tsigRequest, dummyTsig, key, completeTsig);
		Rdata tsigRdata = RdataTsigBuilder.get(tsigRequest.getAlgorithmName(), timeSigned, fudge, mac, originalId, error, tsigRequest.getOtherData());
			
		Tsig tsig = new Tsig(tsigRequest.getOwnername(), tsigRdata);
		
		return tsig;
	}
	
	@Override
	public void encodeErrorMessage(ErrorMessage message, IoSession session)
			throws IOException {
		EntityManager entityManager = ServerContext.getEntityManager();
		if (entityManager != null && entityManager.getTransaction().isActive()) {
			entityManager.getTransaction().rollback();
		}
		
		if (entityManager != null && entityManager.isOpen()) {
			entityManager.close();
		}
		
		Object attach = message.getAttachment();
		if (attach == null || !(attach instanceof DnsPacket)) {
			// Simply drop the packet
			return;
		}
	
		DnsPacket dnsPacket = (DnsPacket) attach;
		EmptyMessageBuilder messageBuilder = new EmptyMessageBuilder();
		messageBuilder.setRequestPacket(dnsPacket);
		messageBuilder.process();
		DnsMessage response = messageBuilder.getDnsMessage();
		response.setZone("");
		try {
			encode(response, session);
		} catch (DnsshimProtocolException e) {
			logger.error("Error sending error response message");
		}
	}

}
