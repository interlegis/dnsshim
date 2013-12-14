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
import br.registro.dnsshim.xfrd.ui.protocol.SlaveOperation;
import br.registro.dnsshim.xfrd.ui.protocol.SlaveRequest;

public class SlaveParser extends UiRequestParser {
	private SlaveOperation operation;
	private static SlaveParser instanceAdd;
	private static SlaveParser instanceRemove;
	private static SlaveParser instanceAssign;
	private static SlaveParser instanceUnassign;

	private SlaveParser(SlaveOperation operation) {
		this.operation = operation;
	}

	@Override
	protected InputStream loadXsd() {
		if (xsd == null) {
			ClassLoader classLoader = this.getClass().getClassLoader();
			xsd = classLoader.getResourceAsStream("br/registro/dnsshim/resources/templates/xml/schema/Slave.xsd");
		}
		return xsd;
	}

	@Override
	public Object parse(Document xml)
	throws SAXException, IOException,	DnsshimProtocolException {
		validate(xml);
		Node requestNode = null;
		if (operation == SlaveOperation.ADD) {
			requestNode = extractNodeContent(xml,"addSlave");
		} else if (operation == SlaveOperation.REMOVE) {
			requestNode = extractNodeContent(xml, "removeSlave");
		} else {
			return null;
		}
		
		String sessionId = null;
		String groupName = "";
		String slaveHost = "";
		int slavePort = 53;
		NodeList childNodes = requestNode.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			String nodeName = childNode.getNodeName();
			if (nodeName.equals("slaveGroup")) {
				groupName = childNode.getFirstChild().getNodeValue();
			} else if (nodeName.equals("slave")) {
				slaveHost = childNode.getFirstChild().getNodeValue();
			} else if (nodeName.equals("port")) {
				String nodeValue = childNode.getFirstChild().getNodeValue();
				slavePort = Integer.parseInt(nodeValue);
			} else if (nodeName.equals("sessionId")) {
				sessionId = childNode.getFirstChild().getNodeValue();
			}
		}
		
		SlaveRequest request = new SlaveRequest();
		request.setOperation(this.operation);
		request.setGroupName(groupName);
		request.setAddress(slaveHost);
		request.setPort(slavePort);
		request.setSessionId(sessionId);
		return request;
	}
	
	public static synchronized SlaveParser getInstance(SlaveOperation slaveOperation) {
		switch (slaveOperation) {
		case ADD:
			if (instanceAdd == null) {
				instanceAdd = new SlaveParser(SlaveOperation.ADD);
			}
			return instanceAdd;
		case REMOVE:
			if (instanceRemove == null) {
				instanceRemove = new SlaveParser(SlaveOperation.REMOVE);
			}
			return instanceRemove;
		case ASSIGN:
			if (instanceAssign == null) {
				instanceAssign = new SlaveParser(SlaveOperation.ASSIGN);
			}
			return instanceAssign;
		case UNASSIGN:

			if (instanceUnassign == null) {
				instanceUnassign = new SlaveParser(SlaveOperation.UNASSIGN);
			}
			return instanceUnassign;
			
		default:
			return null;
		}
		
	} 

}
