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
import java.util.NoSuchElementException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.ResourceRecordFactory;
import br.registro.dnsshim.domain.RrType;
import br.registro.dnsshim.xfrd.ui.protocol.RrOperation;
import br.registro.dnsshim.xfrd.ui.protocol.RrRequest;

public class RrParser extends UiRequestParser {
	private RrOperation operation;	
	
	private static RrParser instanceAdd;
	private static RrParser instanceRemove;
	
	private RrParser(RrOperation operation) {
		this.operation = operation;
	}

	@Override
	protected InputStream loadXsd() {
		if (xsd == null) {
			ClassLoader classLoader = this.getClass().getClassLoader();
			xsd = classLoader.getResourceAsStream("br/registro/dnsshim/resources/templates/xml/schema/Rr.xsd");	
		}
		return xsd;
	}

	@Override
	public Object parse(Document xml)
	throws SAXException, IOException, DnsshimProtocolException {
		validate(xml);
		Node requestNode = null;
		if (operation == RrOperation.ADD) {
			requestNode = extractNodeContent(xml,"addRr");
		} else {
			requestNode = extractNodeContent(xml, "removeRr");
		}
		NodeList childNodes = requestNode.getChildNodes();
		
		String zoneName = "";
		String ownername = "";
		RrType type = RrType.A;
		int ttl = 0;
		String rdataStr = "";
		String sessionId = null;
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			
			String nodeName = childNode.getNodeName();
			if (nodeName.equals("zone")) {
				zoneName = childNode.getFirstChild().getNodeValue();
			} else if (nodeName.equals("rr")) {
				NodeList rrNodes = childNode.getChildNodes();
				for (int j = 0; j < rrNodes.getLength(); j++) {
					Node rrParamNode = rrNodes.item(j);
					if (rrParamNode.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					
					String rrParamName = rrParamNode.getNodeName();
					Node valueNode = rrParamNode.getFirstChild();
					String rrParamValue = "";
					if (valueNode != null) {
						rrParamValue = valueNode.getNodeValue();
					}
					if (rrParamName.equals("ownername")) {
						ownername = rrParamValue;
					} else if (rrParamName.equals("ttl")) {
						ttl = Integer.parseInt(rrParamValue);
					} else if (rrParamName.equals("type")) {
						type = RrType.valueOf(rrParamValue);
					} else if (rrParamName.equals("rdata")) {
						rdataStr = rrParamValue;
					}
				}
			} else if (nodeName.equals("sessionId")) {
				sessionId = childNode.getFirstChild().getNodeValue();
			}
		}
		
		ResourceRecord rr = null;
		try {
			rr = ResourceRecordFactory.getRr(ownername, type, DnsClass.IN, ttl, rdataStr);
		} catch (NoSuchElementException nsee) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Invalid Resource Record");
		} catch (NumberFormatException nfe) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Invalid Resource Record");
		}
		
		RrRequest request = new RrRequest();
		request.setOp(this.operation);
		request.setZone(zoneName);
		request.setRr(rr);
		request.setSessionId(sessionId);
		return request;
	}
	
	public static synchronized RrParser getInstance(RrOperation rrOperation) {
		switch (rrOperation) {
		case ADD:
			if (instanceAdd == null) {
				instanceAdd = new RrParser(RrOperation.ADD);
			}
			return instanceAdd;
		case REMOVE:
			if (instanceRemove == null) {
				instanceRemove = new RrParser(RrOperation.REMOVE);
				instanceRemove.operation = RrOperation.REMOVE;
			}
			return instanceRemove;
		}
		return null;
	}

}
