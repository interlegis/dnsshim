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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import br.registro.dnsshim.common.server.AbstractProtocolDecoder;
import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.IoSession;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.xfrd.ui.protocol.parser.RequestParserFactory;
import br.registro.dnsshim.xfrd.ui.protocol.parser.UiRequestParser;

public class Decoder extends AbstractProtocolDecoder {
	private static final Logger logger = Logger.getLogger(Decoder.class);

	@Override
	public void decode(IoSession session) throws IOException, DnsshimProtocolException {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		readSession(session, buffer);
		int len = buffer.getInt();

		long msgLen = ByteUtil.toUnsigned(len);
		if (msgLen > 5000) {
			throw new DnsshimProtocolException(ProtocolStatusCode.BAD_UICLIENT_REQUEST, "The message is too large:"+msgLen+"");
		}
		buffer = ByteBuffer.allocate((int) msgLen);
		readSession(session, buffer);
		String xml = new String(buffer.array());
		xml = xml.trim();
		if (logger.isDebugEnabled()) {
			logger.debug("XML request: " + xml);
		}
		
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document doc = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
			
			NodeList reqNodeList = doc.getElementsByTagName("request");
			
			if (reqNodeList.getLength() != 1) {
				throw new DnsshimProtocolException(ProtocolStatusCode.BAD_UICLIENT_REQUEST, "Invalid request");
			}

			Node reqNode = reqNodeList.item(0);
			NodeList requests = reqNode.getChildNodes();
			for (int i = 0; i < requests.getLength(); i++) {
				Node requestNode = requests.item(i);
				if (requestNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}

				String nodeName = requestNode.getNodeName();
				UiRequestParser parser = RequestParserFactory.getInstance(nodeName);
				getOutput().write(parser.parse(doc), session);
			}

		} catch (SAXException se) {
			throw new DnsshimProtocolException(ProtocolStatusCode.BAD_UICLIENT_REQUEST, "Error parsing the request");
		} catch (ParserConfigurationException pce) {
			throw new DnsshimProtocolException(ProtocolStatusCode.BAD_UICLIENT_REQUEST, "Error parsing the request");
		}
	}
}