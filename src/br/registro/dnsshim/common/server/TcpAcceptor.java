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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

public class TcpAcceptor {
	private static final int NUM_CPU = Runtime.getRuntime().availableProcessors();
	private static final double CPU_UTILIZATION = 0.8;
	private static final double WAIT_COMPUTE_RATIO = 95/5;
	private static final int NUM_THREADS = (int) (NUM_CPU * CPU_UTILIZATION * (1 + WAIT_COMPUTE_RATIO));
	private static final ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
	
	private ServerSocket serverSocket;
	private final AbstractProtocolDecoder decoder;
	private static final Logger logger = Logger.getLogger(TcpAcceptor.class);

	public TcpAcceptor(ServerSocket serverSocket,
			AbstractProtocolDecoder decoder) {
		this.serverSocket = serverSocket;
		this.decoder = decoder;
	}

	public ServerSocket getServerSocket() {
		return serverSocket;
	}

	public void setServerSocket(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	public void accept() throws IOException {
		final Socket clientSocket = serverSocket.accept();

		if (logger.isDebugEnabled()) {
			logger.debug("TCP connection from :" + clientSocket);
		}

		executorService.execute(new Runnable() {
			@Override
			public void run() {
				TcpIoSession session = new TcpIoSession();
				try {
					clientSocket.setSoTimeout(15000);
					session.setClientSocket(clientSocket);
					decoder.decode(session);
				} catch (DnsshimProtocolException e) {
					logger.error(e.getMessage(), e);
					try {
						ErrorMessage message = new ErrorMessage(e.getStatus());
						String msg = "".equals(e.getMessage()) ? "Unknown error (see log for more information)"
								: e.getMessage();
						message.setMessage(msg);
						message.setAttachment(e.getAttachment());
						decoder.getOutput().getEncoder().encodeErrorMessage(message, session);
						if (message.getStatus().getLevel() == ProtocolStatusCode.Level.FATAL) {
							session.close();
						}
					} catch (DnsshimProtocolException e1) {
						logger.error(e1.getMessage(), e1);
					} catch (IOException e1) {
						logger.error(e1.getMessage(), e1);					
					}
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				} finally {
					if (clientSocket.isConnected() || !clientSocket.isClosed()) {
						close(session);
					}
				}
			}
		});
	}

	public void accept(final ServerConfig config) throws IOException {
		final Socket clientSocket = serverSocket.accept();
		if (logger.isDebugEnabled()) {
			logger.debug("TCP connection from: " + clientSocket.getInetAddress().getHostAddress());
		}

		executorService.execute(new Runnable() {
			@Override
			public void run() {
				TcpIoSession session = new TcpIoSession();
				try {
					clientSocket.setSoTimeout(5000);
					session.setClientSocket(clientSocket);
					decoder.decode(session);
				} catch (DnsshimProtocolException e) {
					logger.error(e.getMessage(), e);
					try {
						ErrorMessage message = new ErrorMessage(e.getStatus());
						String msg = "".equals(e.getMessage()) ? "Unknown error (see log for more information)"
								: e.getMessage();
						message.setMessage(msg);
						message.setAttachment(e.getAttachment());
						decoder.getOutput().getEncoder().encodeErrorMessage(message, session);
						if (message.getStatus().getLevel() == ProtocolStatusCode.Level.FATAL) {
							session.close();
						}
					} catch (DnsshimProtocolException e1) {
						logger.error(e1.getMessage(), e1);
					} catch (IOException e1) {
						logger.error(e1.getMessage(), e1);					
					}
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				} finally {
					if (clientSocket.isConnected() || !clientSocket.isClosed()) {
						close(session);
					}
				}
			}
		});
	}

	private void close(IoSession session) {
		try {
			// waiting (SO_TIMEOUT) for active close of the connection
			session.read();
		} catch (SocketTimeoutException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} finally {
			try {
				session.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	public void shutdown() {
		executorService.shutdown();
	}
}