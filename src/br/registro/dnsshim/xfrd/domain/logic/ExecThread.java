package br.registro.dnsshim.xfrd.domain.logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class ExecThread extends Thread {
	private static final Logger logger = Logger.getLogger(ExecThread.class);
	private String command;
	public ExecThread(String command) {
		this.command = command;
	}
	
	public void run() {
		try {
			Process p = Runtime.getRuntime().exec(command);
			InputStream is = p.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ( (line = br.readLine()) != null) {
				logger.debug(line);
			}
			
			int exitValue = p.waitFor();
			logger.debug("Command exited with value " + exitValue);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

}
