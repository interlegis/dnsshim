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
package br.registro.dnsshim.xfrd.dns.server;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.server.IoFilterChain;
import br.registro.dnsshim.common.server.Server;
import br.registro.dnsshim.common.server.Server.TransportType;
import br.registro.dnsshim.xfrd.dns.protocol.Decoder;
import br.registro.dnsshim.xfrd.dns.protocol.DecoderOutput;
import br.registro.dnsshim.xfrd.dns.protocol.Encoder;
import br.registro.dnsshim.xfrd.dns.protocol.EncoderOutput;
import br.registro.dnsshim.xfrd.domain.XfrdConfig;
import br.registro.dnsshim.xfrd.domain.logic.XfrdConfigManager;
import br.registro.dnsshim.xfrd.util.CloseDatabaseSessionFilter;
import br.registro.dnsshim.xfrd.util.OpenDatabaseSessionFilter;

public class DnsServer implements Runnable {

	private static final Logger logger = Logger.getLogger(DnsServer.class);
	private TransportType transportType;

	public DnsServer(TransportType transportType) {
		this.transportType = transportType;
	}

	@Override
	public void run() {
		if (logger.isInfoEnabled()) {
			logger.info("Starting DnsServer ("+transportType+")");
		}

		try {
			XfrdConfig config = XfrdConfigManager.getInstance();
			Server server = new Server(config.getDnsServerConfig(), transportType);
			if (logger.isInfoEnabled()) {
				logger.info("DNS--"+ config.getDnsServerConfig().getListenAddress());
				logger.info("DNS--"+ config.getDnsServerConfig().getPort());
			}

			DecoderOutput decoderOutput = new DecoderOutput();
			EncoderOutput encoderOutput = new EncoderOutput();
			
			Decoder decoder = new Decoder();
			Encoder encoder = new Encoder();

			decoderOutput.setEncoder(encoder);

			decoder.setOutput(decoderOutput);
			encoder.setOutput(encoderOutput);

			IoFilterChain decoderFilterChain = new IoFilterChain();
			decoderFilterChain.add(new OpenDatabaseSessionFilter());
			
			IoFilterChain encoderFilterChain = new IoFilterChain();
			encoderFilterChain.add(new CloseDatabaseSessionFilter());
			
			decoderOutput.setFilterChain(decoderFilterChain);
			encoderOutput.setFilterChain(encoderFilterChain);
			
			server.setDecoder(decoder);

			server.launch();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
