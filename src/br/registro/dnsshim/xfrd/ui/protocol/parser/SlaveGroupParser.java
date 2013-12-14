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

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.xfrd.ui.protocol.SlaveGroupRequest;
import br.registro.dnsshim.xfrd.ui.protocol.SlaveOperation;

public class SlaveGroupParser extends UiRequestParser {
	private SlaveOperation operation;
	
	private static SlaveGroupParser instanceAdd;
	private static SlaveGroupParser instanceRemove;
	private static SlaveGroupParser instanceAssign;
	private static SlaveGroupParser instanceUnassign;
	
	private SlaveGroupParser(SlaveOperation operation) {
		this.operation = operation;
	}

	@Override
	protected InputStream loadXsd() {
		if (xsd == null) {
			ClassLoader classLoader = this.getClass().getClassLoader();
			xsd = classLoader.getResourceAsStream("br/registro/dnsshim/resources/templates/xml/schema/SlaveGroup.xsd");
		}
		return xsd;
	}

	@Override
	public Object parse(Document xml)
	throws SAXException, IOException,	DnsshimProtocolException {
		validate(xml);
		Node requestNode = null;
		if (operation == SlaveOperation.ADD) {
			requestNode = extractNodeContent(xml,"newSlaveGroup");
		} else if (operation == SlaveOperation.REMOVE) {
			requestNode = extractNodeContent(xml, "removeSlaveGroup");
		} else if (operation == SlaveOperation.ASSIGN) {
			requestNode = extractNodeContent(xml, "assignSlaveGroup");
		} else if (operation == SlaveOperation.UNASSIGN) {
			requestNode = extractNodeContent(xml, "unassignSlaveGroup");
		} else {
			return null;
		}
		
		String sessionId = null;
		String groupName = "";
		String zonename = "";
		NodeList childNodes = requestNode.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			String nodeName = childNode.getNodeName();
			if (nodeName.equals("slaveGroup")) {
				groupName = childNode.getFirstChild().getNodeValue();
			} else if (nodeName.equals("zone")) {
				zonename = childNode.getFirstChild().getNodeValue();
			} else if (nodeName.equals("sessionId")) {
				sessionId = childNode.getFirstChild().getNodeValue();
			}
		}
		
		SlaveGroupRequest request = new SlaveGroupRequest();
		request.setOperation(this.operation);
		request.setGroupName(groupName);
		if (operation == SlaveOperation.ASSIGN ||
				operation == SlaveOperation.UNASSIGN) {
			request.setZone(zonename);
		}
		request.setSessionId(sessionId);
		return request;
	}
	
	public static synchronized SlaveGroupParser getInstance(SlaveOperation slaveOperation) {
		switch (slaveOperation) {
		case ADD:
			if (instanceAdd == null) {
				instanceAdd = new SlaveGroupParser(SlaveOperation.ADD);
			}
			return instanceAdd;
		case REMOVE:
			if (instanceRemove == null) {
				instanceRemove = new SlaveGroupParser(SlaveOperation.REMOVE);
			}
			return instanceRemove;
		case ASSIGN:
			if (instanceAssign == null) {
				instanceAssign = new SlaveGroupParser(SlaveOperation.ASSIGN);
			}
			return instanceAssign;
		case UNASSIGN:

			if (instanceUnassign == null) {
				instanceUnassign = new SlaveGroupParser(SlaveOperation.UNASSIGN);
			}
			return instanceUnassign;
			
		default:
			return null;
		}
		
	} 

}
