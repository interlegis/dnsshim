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
package br.registro.dnsshim.common.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

public class UdpAcceptor {
	private static final int NUM_CPU = Runtime.getRuntime().availableProcessors();
	private static final double CPU_UTILIZATION = 0.8;
	private static final double WAIT_COMPUTE_RATIO = 95/5;
	private static final int NUM_THREADS = (int) (NUM_CPU * CPU_UTILIZATION * (1 + WAIT_COMPUTE_RATIO));
	private static final ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
	
	private DatagramSocket serverSocket;
	private AbstractProtocolDecoder decoder;
	private static final int MAX_UDP_PACKET_SIZE = 1024;
	private static final Logger logger = Logger.getLogger(UdpAcceptor.class);
	
	public UdpAcceptor(DatagramSocket serverSocket,
			AbstractProtocolDecoder decoder) {
		
		this.serverSocket = serverSocket;
		this.decoder = decoder;
	}

	public DatagramSocket getSocket() {
		return this.serverSocket;
	}

	public void accept() throws IOException {
		byte[] buffer = new byte[MAX_UDP_PACKET_SIZE];
		final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		serverSocket.receive(packet);
		
		logger.debug("UDP request from :" + packet.getSocketAddress());
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				UdpIoSession session = new UdpIoSession();

				try {
					session.setBuffer(packet.getData());
					session.setSocket(serverSocket);
					session.setSocketAddress(packet.getSocketAddress());
					decoder.decode(session);

				} catch (DnsshimProtocolException e) {
					logger.error(e.getMessage(), e);
					try {
						ErrorMessage message = new ErrorMessage(e.getStatus());
						message.setMessage(e.getMessage());
						message.setAttachment(e.getAttachment());
						decoder.getOutput().getEncoder().encodeErrorMessage(
								message, session);
					} catch (Exception e1) {
						logger.error(e1.getMessage(), e1);
					}

				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				} finally {
					if (serverSocket.isConnected()) {
						if (logger.isDebugEnabled()) {
							logger.debug("Closing connection");
						}
						serverSocket.disconnect();
					}
				}
			}
		});
	}

	public void shutdown() {
		executorService.shutdown();
	}
}
