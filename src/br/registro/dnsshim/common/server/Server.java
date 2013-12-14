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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.log4j.Logger;

public class Server {
	public enum TransportType {
		TCP, UDP
	};
	
	public static boolean SHUTDOWN = false;

	private int port;
	private AbstractProtocolDecoder decoder;
	private ServerConfig config;
	private final TransportType transportType;
	private final boolean useTls;
	private static final Logger logger = Logger.getLogger(Server.class);
	private ServerSocket tcpServerSocket;
	private DatagramSocket udpServerSocket;

	public Server(ServerConfig config) {
		this.config = config;
		this.port = config.getPort();
		this.transportType = config.getTransportType();
		this.useTls = config.isTls();
	}
	
	public Server(ServerConfig config, TransportType transportType) {
		this.config = config;
		this.port = config.getPort();
		this.transportType = transportType;
		this.useTls = config.isTls();
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public AbstractProtocolDecoder getDecoder() {
		return decoder;
	}

	public void setDecoder(AbstractProtocolDecoder decoder) {
		this.decoder = decoder;
	}

	public ServerConfig getConfig() {
		return config;
	}
	
	public void setConfig(ServerConfig config) {
		this.config = config;
	}
	
	public void launch() throws IOException, Exception {
		if (transportType == TransportType.TCP) {
			if (useTls) {
				if (config == null) {
					throw new Exception("Cannot use TLS without config file");
				}
				
				KeyStore keyStore = KeyStore.getInstance("JKS");
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
				
				InputStream inputStream = null;
				
				if (config == null || config.getCaPath().isEmpty()) {
					inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("br/registro/dnsshim/resources/ca/dnsshim_cert");
					keyStore.load(inputStream, "dnsshim".toCharArray());
					kmf.init(keyStore, "dnsshim".toCharArray());
					if (logger.isInfoEnabled()) {
						logger.info("Using embedded TLS configuration (server)");
					}
				} else {
					inputStream = new FileInputStream(config.getCaPath());
					keyStore.load(inputStream, config.getCaPasswd().toCharArray());
					kmf.init(keyStore, config.getCaPasswd().toCharArray());
				}
				
				SSLContext context = SSLContext.getInstance("TLS");
				context.init(kmf.getKeyManagers(), null, new SecureRandom());
				
				SSLServerSocketFactory sslserversocketfactory = context.getServerSocketFactory();
				String listenAddress = config.getListenAddress();
				if (listenAddress.trim().length() == 0) {
					tcpServerSocket = sslserversocketfactory.createServerSocket(port);
				} else {
					InetAddress inetAddress = InetAddress.getByName(listenAddress);
					tcpServerSocket = sslserversocketfactory.createServerSocket(port, 0, inetAddress);
				}
				
				if (logger.isInfoEnabled()) {
					logger.info("Waiting for TCP (with TLS) connections on port " + port + " ...");
				}
			} else {
				tcpServerSocket = new ServerSocket();
				String listenAddress = config.getListenAddress();

				if (listenAddress.trim().length() == 0) {
					tcpServerSocket.bind(new InetSocketAddress(port));
				} else {
					InetAddress inetAddress = InetAddress.getByName(listenAddress);
                    tcpServerSocket.bind(new InetSocketAddress(inetAddress, port));
				}
				
				if (logger.isInfoEnabled()) {
					logger.info("Waiting for TCP connections on port " + port + " ...");
				}
			}
			
			tcpServerSocket.setSoTimeout(5000);
			TcpAcceptor tcpAcceptor = new TcpAcceptor(tcpServerSocket, decoder);
			while (SHUTDOWN == false) {
				try {
					if (config != null) {
						tcpAcceptor.accept(config);
					} else {
						tcpAcceptor.accept();
					}
				} catch (SocketTimeoutException e) {}
			}
			tcpAcceptor.shutdown();
			
		} else if (transportType == TransportType.UDP) {
			String listenAddress = config.getListenAddress();
			
			if (listenAddress.trim().length() == 0) {
				udpServerSocket = new DatagramSocket(port);
			} else {
				InetAddress inetAddress = InetAddress.getByName(listenAddress);
				udpServerSocket = new DatagramSocket(port, inetAddress);
			}
			
			if (logger.isInfoEnabled()) {
				logger.info("Waiting for UDP connections on port " + port + " ...");
			}
			
			udpServerSocket.setSoTimeout(5000);
			UdpAcceptor udpAcceptor = new UdpAcceptor(udpServerSocket, decoder);
			while (SHUTDOWN == false) {
				try {
					udpAcceptor.accept();
				} catch (SocketTimeoutException e) {}
			}
			udpAcceptor.shutdown();
		}
	}
}
