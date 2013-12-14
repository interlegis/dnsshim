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
package br.registro.dnsshim.xfrd.signerclient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.common.server.TcpIoSession;
import br.registro.dnsshim.signer.protocol.GenerateKeyRequest;
import br.registro.dnsshim.signer.protocol.ImportKeyRequest;
import br.registro.dnsshim.signer.protocol.KeyResponse;
import br.registro.dnsshim.signer.protocol.RemoveKeyRequest;
import br.registro.dnsshim.signer.protocol.RemoveKeyResponse;
import br.registro.dnsshim.signer.protocol.SignRequest;
import br.registro.dnsshim.signer.protocol.SignResponse;

public class SignerClient {
	private final String host;
	private final int port;
	private final String caPath;
	private final String caPasswd;
	private static final Logger logger = Logger.getLogger(SignerClient.class);

	public SignerClient(String host, int port, String caPath, String caPasswd) {
		this.host = host;
		this.port = port;
		this.caPath = caPath;
		this.caPasswd = caPasswd;
	}

	private Socket launch() throws IOException, DnsshimProtocolException {
	
		try {
			KeyStore keyStore;
			keyStore = KeyStore.getInstance("JKS");
			
			InputStream inputStream = null;
			
			if (caPasswd.isEmpty() || caPath.isEmpty()) {
				inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("br/registro/dnsshim/resources/ca/dnsshim_cert");
				keyStore.load(inputStream, "dnsshim".toCharArray());
				if (logger.isDebugEnabled()) {
					logger.debug("Using embedded TLS configuration for SignerClient");
				}
			} else {
				inputStream = new FileInputStream(caPath);
				keyStore.load(inputStream, caPasswd.toCharArray());
			}
			
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(keyStore);
			
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, tmf.getTrustManagers(), new SecureRandom());
			SSLSocketFactory sslsocketfactory = context.getSocketFactory();

			if (inputStream != null) {
				inputStream.close();
			}
			
			if (logger.isDebugEnabled()) {
				logger.debug("Socket open...");
			}
				
			return sslsocketfactory.createSocket(host, port);

		} catch (Exception e) {
			throw new DnsshimProtocolException(ProtocolStatusCode.CONNECTION_REFUSED, "Error connecting to signer");
		}
	}

	public KeyResponse generateKey(GenerateKeyRequest request) throws IOException, DnsshimProtocolException {
		Socket clientSocket = launch();
		TcpIoSession session = new TcpIoSession();
		session.setClientSocket(clientSocket);

		Encoder encoder = new Encoder();
		Decoder decoder = new Decoder();

		DecoderOutput clientDecoderOutput = new DecoderOutput();
		EncoderOutput clientEncoderOutput = new EncoderOutput();
		try {
			encoder.setOutput(clientEncoderOutput);
			decoder.setOutput(clientDecoderOutput);
			encoder.encode(request, session);
			decoder.decode(session);
		} catch (DnsshimProtocolException e) {
			logger.error(e.getMessage(), e);
		} finally {
			session.close();
		}
		return (KeyResponse) clientDecoderOutput.getResponse();
	}

	@SuppressWarnings("unchecked")
	public List<SignResponse> sign(SignRequest request)
		throws IOException,	DnsshimProtocolException {
		
		Socket clientSocket = launch();
		TcpIoSession session = new TcpIoSession();
		session.setClientSocket(clientSocket);
		
		Encoder encoder = new Encoder();
		Decoder decoder = new Decoder();

		DecoderOutput clientDecoderOutput = new DecoderOutput();
		EncoderOutput clientEncoderOutput = new EncoderOutput();

		encoder.setOutput(clientEncoderOutput);
		decoder.setOutput(clientDecoderOutput);

		try {
			encoder.encode(request, session);
			decoder.decode(session);
		} finally {
			session.close();
		}
		
		return (List<SignResponse>) clientDecoderOutput.getResponse();

	}

	public KeyResponse importKey(ImportKeyRequest request) throws IOException, DnsshimProtocolException {
		Socket clientSocket = launch();
		TcpIoSession session = new TcpIoSession();
		session.setClientSocket(clientSocket);

		Encoder encoder = new Encoder();
		Decoder decoder = new Decoder();

		DecoderOutput clientDecoderOutput = new DecoderOutput();
		EncoderOutput clientEncoderOutput = new EncoderOutput();
		try {
			encoder.setOutput(clientEncoderOutput);
			decoder.setOutput(clientDecoderOutput);
			encoder.encode(request, session);
			decoder.decode(session);
		} catch (DnsshimProtocolException e) {
			logger.error(e.getMessage(), e);
		} finally {
			session.close();
		}

		return (KeyResponse) clientDecoderOutput.getResponse();
	}
	
	public RemoveKeyResponse removeKey(RemoveKeyRequest request)
		throws IOException, DnsshimProtocolException {
		Socket clientSocket = launch();
		TcpIoSession session = new TcpIoSession();
		session.setClientSocket(clientSocket);

		Encoder encoder = new Encoder();
		Decoder decoder = new Decoder();

		DecoderOutput clientDecoderOutput = new DecoderOutput();
		EncoderOutput clientEncoderOutput = new EncoderOutput();
		try {
			encoder.setOutput(clientEncoderOutput);
			decoder.setOutput(clientDecoderOutput);
			encoder.encode(request, session);
			decoder.decode(session);
		} catch (DnsshimProtocolException e) {
			logger.error(e.getMessage(), e);
		} finally {
			session.close();
		}

		return (RemoveKeyResponse) clientDecoderOutput.getResponse();
	}
}
