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
package br.registro.dnsshim.xfrd.ui.protocol.parser;

import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import br.registro.dnsshim.domain.DnskeyAlgorithm;
import br.registro.dnsshim.domain.DnskeyFlags;
import br.registro.dnsshim.domain.DnskeyProtocol;
import br.registro.dnsshim.domain.DnskeyType;
import br.registro.dnsshim.xfrd.domain.DnskeyStatus;
import br.registro.dnsshim.xfrd.ui.protocol.NewKeyRequest;

public class NewKeyParser extends UiRequestParser {

	private static NewKeyParser instance;
	
	private NewKeyParser() {}
	@Override
	protected InputStream loadXsd() {
		if (xsd == null) {
			xsd = this.getClass().getClassLoader().getResourceAsStream("br/registro/dnsshim/resources/templates/xml/schema/NewKey.xsd");
		}
		return xsd;
	}

	@Override
	public Object parse(Document xml) throws SAXException, IOException {
		validate(xml);
		Node requestNode = extractNodeContent(xml,"newKey");
		NodeList childNodes = requestNode.getChildNodes();
		
		NewKeyRequest request = new NewKeyRequest();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			String nodeName = childNode.getNodeName();
			Node valueNode = childNode.getFirstChild();
			if (nodeName.equals("zone")) {
				request.setZone(valueNode.getNodeValue());
			} else if (nodeName.equals("size")) {
				int value = Integer.parseInt(valueNode.getNodeValue());
				request.setKeySize(value);
			} else if (nodeName.equals("algorithm")) {
				byte value = Byte.parseByte(valueNode.getNodeValue());
				request.setAlgorithm(DnskeyAlgorithm.fromValue(value));
			} else if (nodeName.equals("type")) {
				String keyType = valueNode.getNodeValue();
				request.setKeyType(DnskeyType.valueOf(keyType));
			} else if (nodeName.equals("flags")) {
				short flags = Short.parseShort(valueNode.getNodeValue());
				request.setFlags(DnskeyFlags.fromValue(flags));
			} else if (nodeName.equals("status")) {
				String status = valueNode.getNodeValue();
				request.setKeyStatus(DnskeyStatus.valueOf(status));
			} else if (nodeName.equals("protocol")) {
				byte value = Byte.parseByte(valueNode.getNodeValue());
				request.setProtocol(DnskeyProtocol.fromValue(value));
				
			} else if (nodeName.equals("sessionId")) {
				request.setSessionId(valueNode.getNodeValue());
			}
		}

		return request;
	}
	
	public static synchronized NewKeyParser getInstance() {
		if (instance == null) {
			instance = new NewKeyParser();
		}
		return instance;
	}

}
