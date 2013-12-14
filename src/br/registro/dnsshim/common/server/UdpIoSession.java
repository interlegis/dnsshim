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
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import br.registro.dnsshim.common.server.Server.TransportType;

public class UdpIoSession implements IoSession {
	private DatagramSocket socket;
	private SocketAddress socketAddress;
	private byte[] buffer;
	private int position;
	private Object attachment;

	public SocketAddress getSocketAddress() {
		return socketAddress;
	}

	public void setSocketAddress(SocketAddress socketAddress) {
		this.socketAddress = socketAddress;
	}

	public byte[] getBuffer() {
		return buffer;
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}

	public DatagramSocket getSocket() {
		return socket;
	}

	public void setSocket(DatagramSocket socket) {
		this.socket = socket;
	}

	@Override
	public void close() throws IOException {
		socket.disconnect();
	}

	@Override
	public int read(byte[] b) throws IOException {
		int len = 0;
		//se pediu pra ler mais do q tem...
		if ((position + b.length) > buffer.length) {
			len = buffer.length;
		} else {
			len = b.length - position;
		}
		System.arraycopy(buffer, position, b, 0, len);
		position += len;
		return len;
	}

	@Override
	public void write(byte[] buffer) throws IOException {
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, socketAddress);
		socket.send(packet);
	}

	@Override
	public String toString() {
		return socket.toString();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		if (socketAddress == null) {
			return (InetSocketAddress) socket.getRemoteSocketAddress();
		}
		return (InetSocketAddress) socketAddress;
	}

	@Override
	public TransportType getTransportType() {
		return TransportType.UDP;
	}

	@Override
	public int read() throws IOException {
		int r = buffer[position];
		position++;
		return r;
		
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		// Dummy
		return 0;
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
