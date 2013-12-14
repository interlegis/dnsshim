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
package br.registro.dnsshim.xfrd.ui.protocol.writer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import br.registro.dnsshim.domain.DsInfo;
import br.registro.dnsshim.domain.ResourceRecord;
import br.registro.dnsshim.xfrd.ui.protocol.PrintZoneResponse;

public class PrintZoneResponseWriter extends UiResponseWriter<PrintZoneResponse> {
	private static PrintZoneResponseWriter instance;
	private PrintZoneResponseWriter() {}
	public static synchronized PrintZoneResponseWriter getInstance() {
		if (instance == null) {
			instance = new PrintZoneResponseWriter();
		}
		return instance;
	}
	
	@Override
	public String write(PrintZoneResponse response) throws XMLStreamException, IOException {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		XMLStreamWriter writer = outputFactory.createXMLStreamWriter(stream, ENCODING);
		boolean firtsDsInfo = true;

		writer.writeStartDocument(ENCODING, ENCODING_VERSION);
		writer.writeStartElement("dnsshim");
		writer.writeAttribute("version", DNSSHIM_PROTOCOL_VERSION);
		writer.writeStartElement("response");
		writer.writeStartElement("status");
		writer.writeCharacters(String.valueOf(response.getStatus().getValue()));
		writer.writeEndElement();
		
		if (!response.getMsg().isEmpty()) {
			writer.writeStartElement("msg");
			writer.writeCharacters(response.getMsg());
			writer.writeEndElement();
		}
		
		writer.writeStartElement("printZone");
		writer.writeStartElement("zone");
		writer.writeCharacters(response.getZone());
		writer.writeEndElement();
		writer.writeStartElement("records");
		
		for (ResourceRecord rr : response.getRrList()) {
			writer.writeStartElement("rr");
			writer.writeStartElement("ownername");
			writer.writeCharacters(rr.getOwnername());
			writer.writeEndElement();
			writer.writeStartElement("class");
			writer.writeCharacters(rr.getDnsClass().toString());
			writer.writeEndElement();
			writer.writeStartElement("type");
			writer.writeCharacters(rr.getType().toString());
			writer.writeEndElement();
			writer.writeStartElement("ttl");
			writer.writeCharacters(String.valueOf(rr.getTtl()));
			writer.writeEndElement();
			writer.writeStartElement("rdata");
			writer.writeCharacters(rr.rdataPresentation());
			writer.writeEndElement();
			writer.writeEndElement(); // rr
		}
		
		writer.writeEndElement(); // close -- </records>
		
		for (DsInfo dsInfo : response.getDsList()) {
			if (firtsDsInfo) {
				writer.writeStartElement("dsInfo");
				firtsDsInfo = false;
			}
			writer.writeStartElement("ds");
			writer.writeStartElement("keytag");
			writer.writeCharacters(String.valueOf(dsInfo.getKeytag()));
			writer.writeEndElement();
			writer.writeStartElement("digestType");
			writer.writeCharacters(dsInfo.getDigestType().toString());
			writer.writeEndElement();
			writer.writeStartElement("digest");
			writer.writeCharacters(dsInfo.getDigest());
			writer.writeEndElement();

			writer.writeEndElement();
		}
		
		writer.writeEndDocument();
		writer.flush();
		writer.close();
		
		stream.close();
		return stream.toString();
	}

}
