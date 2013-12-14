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

import br.registro.dnsshim.domain.DnsClass;
import br.registro.dnsshim.domain.DnskeyAlgorithm;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.domain.Soa;
import br.registro.dnsshim.xfrd.ui.protocol.NewZoneRequest;

public class NewZoneParser extends UiRequestParser {
	private static NewZoneParser instance;
	
	private NewZoneParser() {}

	@Override
	public Object parse(Document xml) throws SAXException, IOException {
		validate(xml);
		Node requestNode = extractNodeContent(xml,"newZone");
		NodeList childNodes = requestNode.getChildNodes();
			
		String zoneName = "";
		boolean dnssec = true;
		int ttl = 0;
		
		String mname = "";
		String rname = "";
		int serial = 0;
		int refresh = 0;
		int retry = 0;
		int expire = 0;
		int minimum = 0;
		
		int keySize = 1024;
		DnskeyAlgorithm keyAlgorithm = DnskeyAlgorithm.RSASHA1;
		int expirationPeriod = 0;
		
		String sessionId = null;
		String slaveGroup = null;
		
		boolean autoBalance = false;
		
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			
			String nodeName = childNode.getNodeName();
			if (nodeName.equals("zone")) {
				zoneName = childNode.getFirstChild().getNodeValue();
				zoneName = zoneName.toLowerCase();
			} else if (nodeName.equals("dnssec")) {
				if (childNode.getFirstChild().getNodeValue().equals("0")) {
					dnssec = false;
				} else {
					dnssec = true;
				}
			} else if (nodeName.equals("soa")) {
				NodeList soaNodes = childNode.getChildNodes();
				for (int j = 0; j < soaNodes.getLength(); j++) {
					Node soaParamNode = soaNodes.item(j);
					if (soaParamNode.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					
					String soaParamName = soaParamNode.getNodeName();
					String soaParamValue = soaParamNode.getFirstChild().getNodeValue();
					if (soaParamName.equals("ttl")) {
						ttl = Integer.parseInt(soaParamValue);
					} else if (soaParamName.equals("mname")) {
						mname = soaParamValue;
					} else if (soaParamName.equals("rname")) {
						rname = soaParamValue;
					} else if (soaParamName.equals("serial")) {
						serial = Integer.parseInt(soaParamValue);
					} else if (soaParamName.equals("refresh")) {
						refresh = Integer.parseInt(soaParamValue);
					} else if (soaParamName.equals("retry")) {
						retry = Integer.parseInt(soaParamValue);
					} else if (soaParamName.equals("expire")) {
						expire = Integer.parseInt(soaParamValue);
					} else if (soaParamName.equals("minimum")) {
						minimum = Integer.parseInt(soaParamValue);
					}
				}				
			} else if (nodeName.equals("key")) {
				NodeList keyNodes = childNode.getChildNodes();
				for (int j = 0; j < keyNodes.getLength(); j++) {
					Node keyParamNode = keyNodes.item(j);
					if (keyParamNode.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					
					String keyParamName = keyParamNode.getNodeName();
					String keyParamValue = keyParamNode.getFirstChild().getNodeValue();
					if (keyParamName.equals("size")) {
						keySize = Integer.parseInt(keyParamValue);
					} else if (keyParamName.equals("algorithm")) {
						keyAlgorithm = DnskeyAlgorithm.fromValue(Byte.parseByte(keyParamValue));
					} else if (keyParamName.equals("expirationPeriod")) {
						expirationPeriod = Integer.parseInt(keyParamValue);
					}
				}
			} else if (nodeName.equals("sessionId")) {
				sessionId = childNode.getFirstChild().getNodeValue();
			} else if (nodeName.equals("slaveGroup")) {
				slaveGroup = childNode.getFirstChild().getNodeValue();
			} else if (nodeName.equals("autoBalance")) {
				autoBalance = Boolean.parseBoolean(childNode.getFirstChild().getNodeValue());
			}
		}

		Soa soa = new Soa(ResourceRecord.APEX_OWNERNAME, DnsClass.IN, ttl, mname, rname, serial, refresh, retry, expire, minimum);
		NewZoneRequest request = new NewZoneRequest();
		request.setZone(zoneName);
		request.setSoa(soa);
		request.setSlaveGroup(slaveGroup);
		request.setAutoBalance(autoBalance);
		
		if (dnssec == true) {
			request.setSigned(true);
			request.setKeySize(keySize);
			request.setAlgorithm(keyAlgorithm);
			request.setExpirationPeriod(expirationPeriod);
		}
		request.setSessionId(sessionId);
		return request;
	}

	@Override
	public InputStream loadXsd() {
		if (xsd == null) {
			xsd = this.getClass().getClassLoader().getResourceAsStream("br/registro/dnsshim/resources/templates/xml/schema/NewZone.xsd");
		}
		return xsd;
	}

	public static synchronized NewZoneParser getInstance() {
		if (instance == null) {
			instance = new NewZoneParser();
		}
		return instance;
	}
}
