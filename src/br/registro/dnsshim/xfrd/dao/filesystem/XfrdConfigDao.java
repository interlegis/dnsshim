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
package br.registro.dnsshim.xfrd.dao.filesystem;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;

import br.registro.dnsshim.common.repository.FileSystemUtil;
import br.registro.dnsshim.common.server.ClientConfig;
import br.registro.dnsshim.common.server.Server.TransportType;
import br.registro.dnsshim.common.server.ServerConfig;
import br.registro.dnsshim.xfrd.domain.XfrdConfig;
import br.registro.dnsshim.xfrd.util.SortedProperties;

public class XfrdConfigDao {
	private static final Logger logger = Logger.getLogger(XfrdConfigDao.class);
	private final String BASE_DIR;
	private static final String EXTENSION = ".properties";
	
	
	public XfrdConfigDao() throws IOException {
		StringBuilder root = new StringBuilder();
		// {DNSSHIM_HOME|user.home}/dnsshim/conf/xfrd/xfrd.properties
		if (System.getenv("DNSSHIM_HOME") == null) {
			root.append(System.getProperty("user.home"));
			root.append(File.separatorChar);
			root.append("dnsshim");
		} else {
			root.append(System.getenv("DNSSHIM_HOME"));
		}
		root.append(File.separatorChar);
		root.append("xfrd");
		root.append(File.separatorChar);
		root.append("conf");
		root.append(File.separatorChar);

		File directory = new File(root.toString());
		FileSystemUtil.makePath(directory);
		
		if (logger.isDebugEnabled()) {
			logger.debug("XFRD config base dir:"+root);
		}
		BASE_DIR = root.toString();
	}

	public XfrdConfig load() throws IOException {
		if (logger.isInfoEnabled()) {
			logger.info("Loading XFRD configurations...");
		}
		StringBuilder path = new StringBuilder(BASE_DIR);
		path.append("xfrd");
		path.append(EXTENSION);
		
		File file = new File(path.toString());
		
		if (!file.exists()) {
			if (!file.createNewFile()) {
				throw new IOException("Cannot create file:"+file);
			}
		}
		
		Properties conf = new Properties();
		FileInputStream fis = new FileInputStream(file);
		conf.load(fis);

		String dnsServerPort = conf.getProperty("dns_server_port", String.valueOf(XfrdConfig.DEFAULT_DNSSERVER_PORT));
		String listenAddress = conf.getProperty("listen_address", String.valueOf(XfrdConfig.LISTEN_ADDRESS));
		
		String signerPort = conf.getProperty("signer_port", String.valueOf(XfrdConfig.DEFAULT_SIGNER_PORT));
		String signerHost = conf.getProperty("signer_host", XfrdConfig.DEFAULT_SIGNER_HOST);
		String signerCaPath = conf.getProperty("signer_cert_file", XfrdConfig.DEFAULT_CA_PATH);
		String signerCaPasswd = conf.getProperty("signer_cert_password", XfrdConfig.DEFAULT_CA_PASSWD);
		
		String tsigFudge = conf.getProperty("tsig_fudge", String.valueOf(XfrdConfig.DEFAULT_TSIG_FUDGE));
		String schedulerLowPriority = conf.getProperty("scheduler_low_priority", String.valueOf(XfrdConfig.DEFAULT_SCHEDULER_LOW_LIMIT));
		String schedulerHighPriority = conf.getProperty("scheduler_high_priority", String.valueOf(XfrdConfig.DEFAULT_SCHEDULER_HIGH_LIMIT));
		
		String slaveSyncPeriod = conf.getProperty("slave_sync_period", String.valueOf(XfrdConfig.DEFAULT_SLAVE_SYNC_PERIOD));
		
		String rndcPath = conf.getProperty("rndc_path", XfrdConfig.DEFAULT_RNDC_PATH);
		String rndcPort = conf.getProperty("rndc_port", String.valueOf(XfrdConfig.DEFAULT_RNDC_PORT));
		
		String uiCaPath = conf.getProperty("ui_cert_file", XfrdConfig.DEFAULT_CA_PATH);
		String uiCaPassword = conf.getProperty("ui_cert_password", XfrdConfig.DEFAULT_CA_PASSWD);
		String uiPort = conf.getProperty("ui_port", String.valueOf(XfrdConfig.DEFAULT_UI_PORT));
		String slaveSyncPath = conf.getProperty("slavesync_path", XfrdConfig.DEFAULT_SLAVESYNC_PATH);
		boolean slaveSyncEnabled = Boolean.parseBoolean(conf.getProperty("slave_sync_enabled", "true"));
		
		String notifyTimeout = conf.getProperty("notify_timeout", String.valueOf(XfrdConfig.DEFAULT_NOTIFY_TIMEOUT));
		String notifyRetryInterval = conf.getProperty("notify_retry_interval", String.valueOf(XfrdConfig.DEFAULT_NOTIFY_RETRY_INTERVAL));
		String notifyMaxRetries = conf.getProperty("notify_max_retries", String.valueOf(XfrdConfig.DEFAULT_NOTIFY_MAX_RETRIES));
		
		String cacheSize = conf.getProperty("cache_size", String.valueOf(XfrdConfig.DEFAULT_CACHE_SIZE));
		
		String shutdownSecret = conf.getProperty("shutdown_secret", XfrdConfig.DEFAULT_SHUTDOWN_SECRET);
		
		boolean overwriteMname = Boolean.parseBoolean(conf.getProperty("overwrite_mname", "false"));
		
		String databaseDialect = conf.getProperty("database_dialect");
		String databaseVendor = conf.getProperty("database_vendor");
		String databaseHost = conf.getProperty("database_host");
		String databasePort = conf.getProperty("database_port");
		String databaseName = conf.getProperty("database_name");
		String databaseDriverClass = conf.getProperty("database_driver_class");
		String databaseUsername = conf.getProperty("database_username");
		String databasePassword = conf.getProperty("database_password");
		
		fis.close();
		
		XfrdConfig config = new XfrdConfig();
		ServerConfig dnsConfig = config.getDnsServerConfig();
		ServerConfig uiConfig = config.getUiServerConfig();
		ClientConfig signerConfig = config.getSignerClientConfig();
		
		dnsConfig.setListenAddress(listenAddress);
		dnsConfig.setPort(Integer.parseInt(dnsServerPort));
		uiConfig.setPort(Integer.parseInt(uiPort));
		uiConfig.setCaPasswd(uiCaPassword);
		uiConfig.setCaPath(uiCaPath);
		uiConfig.setTransportType(TransportType.TCP);
		uiConfig.setListenAddress(listenAddress);

		signerConfig.setHost(signerHost);
		signerConfig.setPort(Integer.parseInt(signerPort));
		signerConfig.setCaPasswd(signerCaPasswd);
		signerConfig.setCaPath(signerCaPath);
		signerConfig.setTransportType(TransportType.TCP);
		
		config.setTsigFudge((short) Integer.parseInt(tsigFudge));
		config.setSchedulerLowLimit(Integer.parseInt(schedulerLowPriority));
		config.setSchedulerHighLimit(Integer.parseInt(schedulerHighPriority));
		config.setSlaveSyncPeriod(Integer.parseInt(slaveSyncPeriod));
		config.setSlaveSyncPath(slaveSyncPath);
		config.setRndcPath(rndcPath);
		config.setRndcPort(Integer.parseInt(rndcPort));
		config.setSlaveSyncEnabled(slaveSyncEnabled);
		
		config.setNotifyTimeout(Integer.parseInt(notifyTimeout));
		config.setNotifyRetryInterval(Integer.parseInt(notifyRetryInterval));
		config.setNotifyMaxRetries(Integer.parseInt(notifyMaxRetries));
		
		config.setCacheSize(Integer.parseInt(cacheSize));
		config.setShutdownSecret(shutdownSecret);
		
		config.setOverwriteMname(overwriteMname);
		
		Map<String, String> databaseProperties = new HashMap<String, String>();
				
		databaseProperties.put("database_dialect", databaseDialect);
		databaseProperties.put("database_host", databaseHost);
		databaseProperties.put("database_port", databasePort);
		databaseProperties.put("database_driver_class", databaseDriverClass);
		databaseProperties.put("database_password", databasePassword);
		databaseProperties.put("database_username", databaseUsername);
		databaseProperties.put("database_name", databaseName);
		databaseProperties.put("database_vendor", databaseVendor);
		
		config.setDatabaseProperties(databaseProperties);
		
		// always keep a well-formed config file
		save(config);
		return config;
	}

	public void save(XfrdConfig config) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Saving signer configurations...");
		}
		
		StringBuilder path = new StringBuilder(BASE_DIR);
		path.append("xfrd");
		path.append(EXTENSION);
		
		File file = new File(path.toString());
		
		ServerConfig dnsConfig = config.getDnsServerConfig();
		ServerConfig uiConfig = config.getUiServerConfig();
		ClientConfig signerConfig = config.getSignerClientConfig();
		
		SortedProperties conf = new SortedProperties();
		conf.put("dns_server_port", String.valueOf(dnsConfig.getPort()));
		conf.put("ui_port", String.valueOf(uiConfig.getPort()));
		conf.put("signer_port", String.valueOf(signerConfig.getPort()));
		conf.put("signer_host", signerConfig.getHost());
		conf.put("signer_cert_file", signerConfig.getCaPath());
		conf.put("signer_cert_password", signerConfig.getCaPasswd());
		conf.put("tsig_fudge", String.valueOf(config.getTsigFudge()));
		conf.put("scheduler_low_priority", String.valueOf(config.getSchedulerLowLimit()));
		conf.put("scheduler_high_priority", String.valueOf(config.getSchedulerHighLimit()));
		conf.put("ui_cert_file",uiConfig.getCaPath());
		conf.put("ui_cert_password",uiConfig.getCaPasswd());
		conf.put("slavesync_path", config.getSlaveSyncPath());
		conf.put("rndc_path", config.getRndcPath());
		conf.put("rndc_port", String.valueOf(config.getRndcPort()));
		conf.put("slave_sync_period", String.valueOf(config.getSlaveSyncPeriod()));
		conf.put("slave_sync_enabled", String.valueOf(config.isSlaveSyncEnabled()));
		conf.put("listen_address", dnsConfig.getListenAddress());
		conf.put("notify_timeout", String.valueOf(config.getNotifyTimeout()));
		conf.put("notify_retry_interval", String.valueOf(config.getNotifyRetryInterval()));
		conf.put("notify_max_retries", String.valueOf(config.getNotifyMaxRetries()));
		conf.put("cache_size", String.valueOf(config.getCacheSize()));
		conf.put("shutdown_secret", config.getShutdownSecret());
		conf.put("overwrite_mname", String.valueOf(config.getOverwriteMname()));

		Map<String, String> databaseProperties = config.getDatabaseProperties();
		for (Entry<String, String> entry : databaseProperties.entrySet()) {
			conf.put(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
		}
	
		// First write the properties to a ByteArray to ensure it's correct.
		// Then write to the file, to avoid erasing the existing file.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		conf.store(baos, "Generated by DNSSHIM");
		baos.close();
		FileWriter fw = new FileWriter(file);
		fw.write(baos.toString());
		fw.close();
	}
}
