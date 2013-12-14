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

import br.registro.dnsshim.xfrd.ui.protocol.ListSlaveGroupResponse;

public class ListSlaveGroupResponseWriter extends UiResponseWriter<ListSlaveGroupResponse> {

	private static ListSlaveGroupResponseWriter instance;
	private ListSlaveGroupResponseWriter() {}
	public static synchronized ListSlaveGroupResponseWriter getInstance() {
		if (instance == null) {
			instance = new ListSlaveGroupResponseWriter();
		}
		return instance;
	}
	
	@Override
	public String write(ListSlaveGroupResponse response) throws XMLStreamException, IOException {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		XMLStreamWriter writer = outputFactory.createXMLStreamWriter(stream, ENCODING);
		
		writer.writeStartDocument(ENCODING,ENCODING_VERSION);
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
		
		writer.writeStartElement("listSlaveGroup");
		
		for (String slaveGroupName : response.getSlaveGroups()) {
			writer.writeStartElement("slaveGroup");
			writer.writeCharacters(slaveGroupName);
			writer.writeEndElement();
		}
		
		writer.writeEndElement();
		writer.writeEndDocument();
		writer.flush();
		writer.close();
		
		stream.close();
		return stream.toString();
	}
}		