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

import br.registro.dnsshim.xfrd.ui.protocol.RemoveKeyRequest;

public class RemoveKeyParser extends UiRequestParser {
	private static RemoveKeyParser instance;
	
	private RemoveKeyParser() {}
	
	@Override
	protected InputStream loadXsd() {
		if (xsd == null){
			xsd = this.getClass().getClassLoader().getResourceAsStream("br/registro/dnsshim/resources/templates/xml/schema/RemoveKey.xsd");
		}
		return xsd;
	}

	@Override
	public Object parse(Document xml) throws SAXException, IOException {
		validate(xml);
		Node requestNode = extractNodeContent(xml,"removeKey");
		NodeList childNodes = requestNode.getChildNodes();
		
		RemoveKeyRequest request = new RemoveKeyRequest();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			
			String nodeName = childNode.getNodeName();
			Node valueNode = childNode.getFirstChild();
			if (nodeName.equals("zone")) {
				request.setZone(valueNode.getNodeValue());
			} else if (nodeName.equals("keyName")) {
				request.setKey(valueNode.getNodeValue());
			} else if (nodeName.equals("sessionId")) {
				request.setSessionId(valueNode.getNodeValue());
			}
		}
		return request;
	}
	
	public static synchronized RemoveKeyParser getInstance() {
		if (instance == null) {
			instance = new RemoveKeyParser();
		}
		return instance;
	}

}
