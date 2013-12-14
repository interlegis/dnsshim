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
package br.registro.dnsshim.xfrd.ui.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.AbstractProtocolEncoder;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ErrorMessage;
import br.registro.dnsshim.common.server.IoSession;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.xfrd.ui.protocol.writer.DefaultResponseWriter;
import br.registro.dnsshim.xfrd.ui.protocol.writer.UiResponseWriter;
import br.registro.dnsshim.xfrd.util.ServerContext;

public class Encoder extends AbstractProtocolEncoder {
	private static final Logger logger = Logger.getLogger(Decoder.class);

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void encode(Object message, IoSession session)
		throws IOException, DnsshimProtocolException {
		try {
			UiResponseWriter uiResponseWriter = UiResponseFactory.get(message);
			Response response = (Response) message;
			String xmlresponse = uiResponseWriter.write(response);
			if (logger.isDebugEnabled()) {
				logger.debug("XML response: " + xmlresponse);
			}
			int length = xmlresponse.getBytes().length;
			ByteBuffer buffer = ByteBuffer.allocate(length + 4);

			buffer.putInt(length);
			buffer.put(xmlresponse.getBytes());
			buffer.flip();
			getOutput().write(buffer, session);
			return;
		} catch (Exception e) {
			logger.error("Unexpected error", e);
		}
		throw new DnsshimProtocolException(ProtocolStatusCode.UI_SERVER_ERROR, "Response error");
	}

	@Override
	public void encodeErrorMessage(ErrorMessage message, IoSession session)
			throws IOException, DnsshimProtocolException {
		
		DefaultResponseWriter defaultResponseWriter = DefaultResponseWriter.getInstance();
		Response response = new Response();
		response.setStatus(message.getStatus());
		response.setMsg(message.getMessage());

		try {
			EntityManager entityManager = ServerContext.getEntityManager();
			if (entityManager != null) {
				if (entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				if (entityManager.isOpen()) {
					entityManager.close();
				}
			}

			String xmlresponse = defaultResponseWriter.write(response);
			int length = xmlresponse.length();
			ByteBuffer buffer = ByteBuffer.allocate(length + 4);
			buffer.putInt(length);
			buffer.put(xmlresponse.getBytes());
			buffer.flip();
			getOutput().write(buffer, session);
		} catch (Exception e) {
			logger.error("Error while writing error response", e);
		}
		
		
	}
}
