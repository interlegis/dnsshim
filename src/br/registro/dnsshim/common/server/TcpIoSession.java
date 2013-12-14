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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.Server.TransportType;

public class TcpIoSession implements IoSession {
	private static final Logger logger = Logger.getLogger(TcpIoSession.class);

	private Socket clientSocket;
	private Object attachment;

	public Socket getClientSocket() {
		return clientSocket;
	}

	public void setClientSocket(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	@Override
	public int read(byte[] b) throws IOException {
		InputStream inputStream = clientSocket.getInputStream();
		return inputStream.read(b);
	}

	@Override
	public void close() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Closing connection...");
		}
		clientSocket.close();
	}

	@Override
	public String toString() {
		return clientSocket.toString();
	}

	@Override
	public void write(byte[] buffer) throws IOException {
		OutputStream outputStream = clientSocket.getOutputStream();
		outputStream.write(buffer);
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return (InetSocketAddress) clientSocket.getRemoteSocketAddress();
	}

	@Override
	public TransportType getTransportType() {
		return TransportType.TCP;
	}

	@Override
	public int read() throws IOException {
		InputStream inputStream = clientSocket.getInputStream();
		return inputStream.read();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		InputStream inputStream = clientSocket.getInputStream();
		return inputStream.read(b, off, len);
	}

	@Override
	public Object getAttachment() {
		return attachment;
	}

	@Override
	public void setAttachment(Object attachment) {
		this.attachment = attachment;		
	}
}