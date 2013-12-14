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
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.AbstractProtocolDecoder;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.IoSession;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.common.server.Server.TransportType;
import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.Opt;
import br.registro.dnsshim.domain.Rdata;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.ResourceRecordFactory;
import br.registro.dnsshim.domain.RrType;
import br.registro.dnsshim.domain.Tsig;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DomainNameUtil;

public class Decoder extends AbstractProtocolDecoder {

	private static final Logger logger = Logger.getLogger(Decoder.class);
	@Override
	public void decode(IoSession session)
		throws IOException, DnsshimProtocolException {
		DnsMessage dnsMessage = new DnsMessage();
		dnsMessage.reset();
		ByteBuffer buffer;
		
		TransportType transport = session.getTransportType();
		QueryType qtype = null;
		boolean axfrLastPacket = false;
		
		while (true) {
			if (transport == TransportType.TCP) {
				buffer = ByteBuffer.allocate(2);
				readSession(session, buffer);
				int packetLen = ByteUtil.toUnsigned(buffer.getShort());
				buffer = ByteBuffer.allocate(packetLen);
				readSession(session, buffer);

			} else {
				// RFC 1035 - 4.2.1 - Messages carried by UDP are restricted to 512 bytes
				buffer = ByteBuffer.allocate(512);
				readSession(session, buffer);
			}
			
			DnsPacket dnsPacket = new DnsPacket();
			
			// Header
			DnsHeader header = dnsPacket.getHeader();
			header.setId(buffer.getShort());
			
			byte byte1 = buffer.get();
			header.setQuestion(ByteUtil.getBit(byte1, 1)); // 10000000

			byte opcodeByte = ByteUtil.getBits(byte1, 2, 4);
			Opcode opcode = Opcode.fromValue(opcodeByte); // 01111000
			header.setOpcode(opcode);

			header.setAuthoritativeAnswer(ByteUtil.getBit(byte1, 6)); // 00000100
			header.setTruncatedResponse(ByteUtil.getBit(byte1, 7)); // 00000010
			header.setRecursionDesired(ByteUtil.getBit(byte1, 8)); // 00000001

			byte byte2 = buffer.get();
			header.setRecursionAvailable(ByteUtil.getBit(byte2, 1)); // 10000000
			header.setReserved(ByteUtil.getBit(byte2, 2));  // 01000000
			header.setAuthenticData(ByteUtil.getBit(byte2, 3)); // 00100000
			header.setCheckingDisabled(ByteUtil.getBit(byte2, 4)); // 00010000
			header.setResponseCode(ResponseCode.fromValue(ByteUtil.getBits(byte2, 5, 4))); // 00001111
			
			header.setQuestionCount(buffer.getShort());
			header.setAnswerCount(buffer.getShort());
			header.setAuthorityCount(buffer.getShort());
			
			int arcountPos = buffer.position();
			header.setAdditionalCount(buffer.getShort());
			
			DnsQuestion question = dnsPacket.getQuestion();
			if (header.getQuestionCount() == 1) {
				String qname = DomainNameUtil.toPresentationFormat(buffer);
				qname = qname.toLowerCase();
				dnsMessage.setZone(qname);
				question.setQname(qname);
				qtype = QueryType.fromValue(buffer.getShort());
				question.setQtype(qtype);
				question.setQclass(DnsClass.fromValue(buffer.getShort()));

			} else if (header.getQuestionCount() > 1) {
				header.setResponseCode(ResponseCode.FORMERR);
				throw new DnsshimProtocolException(ProtocolStatusCode.DNS_MESSAGE_FORMERR, "Too many questions in DNS message: " +
						header.getQuestionCount(), dnsPacket);
			}

			/* Go through all the RRs in the current packet */
			/* Answer Section */
			for (int count = 0; count < header.getAnswerCount(); count++) {
				ResourceRecord rr = readRecord(buffer);
				if (count > 0 && rr.getType() == RrType.SOA) {
					axfrLastPacket = true;
				}
				dnsPacket.addAnswer(rr);
			}

			/* Authority Section */
			for (int count = 0; count < header.getAuthorityCount(); count++) {
				dnsPacket.addAuthority(readRecord(buffer));
			}

			/* Additional Section */
			for (int count = 0; count < header.getAdditionalCount(); count++) {
				buffer.mark();
				int position = buffer.position();
				
				// In the additional section there might be an OPT
				String ownername = DomainNameUtil.toPresentationFormat(buffer);
				ownername = ownername.toLowerCase();
				RrType type = RrType.fromValue(buffer.getShort());

				/*
				 * RFC 2671 - 4.1 The quantity of OPT pseudo-RRs per message shall be
				 * either zero or one, but not greater.
				 */
				if (type == RrType.OPT) {
					readOpt(ownername, buffer, dnsPacket);
					continue;
				}

				DnsClass dnsClass = DnsClass.fromValue(buffer.getShort());
				int ttl = buffer.getInt();

				/*
				 * RFC 2845 - 3.2 - Upon receipt of a message with a correctly placed
				 * TSIG RR, the TSIG RR is copied to a safe location, removed from the
				 * DNS Message, and decremented out of the DNS message header's
				 * ARCOUNT.
				 */
				if (type == RrType.TSIG) {
					if (count + 1 < header.getAdditionalCount()) {
						logger.warn("TSIG must be last RR");
						header.setResponseCode(ResponseCode.FORMERR);
						throw new DnsshimProtocolException(ProtocolStatusCode.DNS_MESSAGE_FORMERR, "TSIG must be last RR", dnsPacket);
					}

					buffer.putShort(arcountPos, (short) (header.getAdditionalCount() - 1));
					Tsig tsig = readTsig(ownername, dnsClass, ttl, buffer);
					dnsPacket.setTsig(tsig);

					// Remove TSIG RR from new byte buffer 
					byte[] bytes = new byte[position];
					System.arraycopy(buffer.array(), 0, bytes, 0, position);
					dnsPacket.setBytes(bytes);
				} else {
					ResourceRecord rr = 
						ResourceRecordFactory.readRr(ownername, type,	dnsClass, ttl, buffer);
					dnsPacket.addAdditional(rr);
				}
			}

			dnsMessage.addPacket(dnsPacket);
			if (transport == TransportType.UDP ||
					header.isQuestion() ||
					(transport == TransportType.TCP &&
							qtype == QueryType.AXFR &&
							axfrLastPacket == true)) {
				break;
			}
		}
		
		getOutput().write(dnsMessage, session);
  }

	private ResourceRecord readRecord(ByteBuffer buffer)
			throws DnsshimProtocolException {
		return ResourceRecordFactory.readRr(buffer);
	}
	
	private void readOpt(String ownername, ByteBuffer buffer, DnsPacket dnsPacket)
		throws DnsshimProtocolException {
		if (!ownername.isEmpty()) {
			logger.warn("Warning: OPT PSEUDO-RR ownername not empty. DROPPING...");

			dnsPacket.getHeader().setResponseCode(ResponseCode.FORMERR);
			throw new DnsshimProtocolException(ProtocolStatusCode.DNS_MESSAGE_FORMERR, "OPT ownername must be empty", dnsPacket);
		}
		
		Opt opt = new Opt();
		opt.setUdpPayloadSize(buffer.getShort());
		opt.setExtendedRcode(buffer.get());
		opt.setVersion(buffer.get());
		opt.setZ(buffer.getShort());

		int rdlength = (int) buffer.getShort();
		byte[] optRdata = new byte[rdlength];
		buffer.get(optRdata);
		opt.setRdata(new Rdata(optRdata));

		dnsPacket.setOpt(opt);
	}
	
	private Tsig readTsig(String ownername, DnsClass dnsClass, int ttl,
			ByteBuffer buffer)
		throws DnsshimProtocolException, IOException {
		Tsig tsig = Tsig.parseTsig(ownername, dnsClass, ttl, buffer);
		return tsig;
	}
	
}
