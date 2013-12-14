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
package br.registro.dnsshim.xfrd.dns.notifier.client;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.UdpClientIoSession;
import br.registro.dnsshim.domain.DnsClass;
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

public class NotifierClient {
	private static final short ZERO = 0;

	public DnsMessage sendNotify(DnsMessage request, String host, int port)
		throws IOException, DnsshimProtocolException {
		UdpClientIoSession session = new UdpClientIoSession();
		ClientDecoderOutput clientDecoderOutput = new ClientDecoderOutput();
		try {
			DatagramSocket clientSocket = new DatagramSocket();
			clientSocket.setSoTimeout(5000);
			clientSocket.connect(new InetSocketAddress(host, port));

			session.setSocketAddress(clientSocket.getLocalSocketAddress());
			session.setSocket(clientSocket);

			Encoder encoder = new Encoder();
			Decoder decoder = new Decoder();

			EncoderOutput clientEncoderOutput = new EncoderOutput();

			encoder.setOutput(clientEncoderOutput);
			decoder.setOutput(clientDecoderOutput);

			encoder.encode(request, session);
			decoder.decode(session);

		} finally {
			session.close();
		}	

		return (DnsMessage) clientDecoderOutput.getResponse();
	}

	public DnsMessage buildNotifyMessage(String zonename) {
		DnsPacket packet = new DnsPacket();
		
		DnsHeader header = new DnsHeader();		
		header.setQuestion(true);
		header.setOpcode(Opcode.NOTIFY);
		header.setAuthoritativeAnswer(true);
		header.setTruncatedResponse(false);
		header.setRecursionDesired(false);
		header.setRecursionAvailable(false);
		header.setResponseCode(ResponseCode.NOERROR);
		header.setQuestionCount((short) 1);
		header.setAnswerCount(ZERO);
		header.setAuthorityCount(ZERO);
		header.setAdditionalCount(ZERO);
		packet.setHeader(header);

		DnsQuestion question = new DnsQuestion();
		question.setQname(zonename);
		question.setQclass(DnsClass.IN);
		question.setQtype(QueryType.SOA);
		packet.setQuestion(question);
		
		DnsMessage request = new DnsMessage();
		request.setZone(zonename);
		request.addPacket(packet);
		
		return request;
	}

}
