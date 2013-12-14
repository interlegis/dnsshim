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
import java.nio.ByteBuffer;

import br.registro.dnsshim.common.server.Server.TransportType;

public abstract class AbstractProtocolDecoder {
	private AbstractDecoderOutput output;

	public AbstractDecoderOutput getOutput() {
		return output;
	}

	public void setOutput(AbstractDecoderOutput output) {
		this.output = output;
	}

	protected void readSession(IoSession session, ByteBuffer buffer) throws IOException {
		byte[] data = new byte[buffer.capacity()];

		if (session.getTransportType() == TransportType.TCP) {
			int bytes = 0;
			int off = 0;
			while (bytes < data.length) {
				int read = session.read(data, off, data.length - bytes);
				if (read == -1) {
					break;
				}
				bytes += read;
				off += read;
			}
		} else {
			session.read(data);
		}
		buffer.put(data);
		buffer.flip();
	}

	public abstract void decode(IoSession session) throws IOException, DnsshimProtocolException;

}
