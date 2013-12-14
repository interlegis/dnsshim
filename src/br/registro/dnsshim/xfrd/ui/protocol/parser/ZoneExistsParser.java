package br.registro.dnsshim.xfrd.ui.protocol.parser;

import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import br.registro.dnsshim.xfrd.ui.protocol.ZoneExistsRequest;

public class ZoneExistsParser extends UiRequestParser {
	private static ZoneExistsParser instance;

	private ZoneExistsParser() {
	}

	@Override
	protected InputStream loadXsd() {
		if (xsd == null) {
			xsd = this.getClass().getClassLoader().getResourceAsStream("br/registro/dnsshim/resources/templates/xml/schema/ZoneExists.xsd");
		}
		return xsd;
	}

	@Override
	public Object parse(Document xml) throws SAXException, IOException {
		validate(xml);
		Node requestNode = extractNodeContent(xml, "zoneExists");
		NodeList childNodes = requestNode.getChildNodes();

		ZoneExistsRequest request = new ZoneExistsRequest();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			String nodeName = childNode.getNodeName();
			Node valueNode = childNode.getFirstChild();
			if (nodeName.equals("zone")) {
				request.setZone(valueNode.getNodeValue());
			} else if (nodeName.equals("sessionId")) {
				request.setSessionId(valueNode.getNodeValue());
			}
		}
		return request;
	}

	public static synchronized ZoneExistsParser getInstance() {
		if (instance == null) {
			instance = new ZoneExistsParser();
		}
		return instance;
	}

}
