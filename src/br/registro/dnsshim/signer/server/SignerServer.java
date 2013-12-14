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
package br.registro.dnsshim.signer.server;

import java.io.File;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import br.registro.dnsshim.common.server.Server;
import br.registro.dnsshim.common.server.ServerConfig;
import br.registro.dnsshim.signer.domain.logic.SignerConfigManager;
import br.registro.dnsshim.signer.protocol.Decoder;
import br.registro.dnsshim.signer.protocol.DecoderOutput;
import br.registro.dnsshim.signer.protocol.Encoder;
import br.registro.dnsshim.signer.protocol.EncoderOutput;

public class SignerServer {
	private static final Logger logger = Logger.getLogger(SignerServer.class);

	public static void main(String[] args) {
		File propertiesFile = new File("log4j-signer.properties");
		if (propertiesFile.exists()) {
			PropertyConfigurator.configure("log4j-signer.properties");
		}

		Logger rootLogger = Logger.getRootLogger();
		if (!rootLogger.getAllAppenders().hasMoreElements()) {
			// No appenders means log4j is not initialized!
			rootLogger.setLevel(Level.INFO);
			DailyRollingFileAppender appender = new DailyRollingFileAppender();
			appender.setName("signer-appender");
			appender.setLayout(new PatternLayout("%d [%t] %-5p (%13F:%L) - %m%n"));
			appender.setFile("dnsshim-signer.log");
			appender.setDatePattern("'.'yyyy-MM-dd'.log'");
			appender.activateOptions();
		    rootLogger.addAppender(appender);
		}
		
		if (logger.isInfoEnabled()) {
			logger.info("Starting SignerServer...");
		}

		try {
			ServerConfig config = SignerConfigManager.getConfigInstance();

			Server server = new Server(config);
			
			DecoderOutput decoderOutput = new DecoderOutput();
			EncoderOutput encoderOutput = new EncoderOutput();

			Decoder decoder = new Decoder();
			Encoder encoder = new Encoder();

			decoderOutput.setEncoder(encoder);

			decoder.setOutput(decoderOutput);
			encoder.setOutput(encoderOutput);

			server.setDecoder(decoder);

			server.launch();
			
		} catch (Exception e) {
			logger.fatal("Exception in SignerServer...",e);
		}
	}
}