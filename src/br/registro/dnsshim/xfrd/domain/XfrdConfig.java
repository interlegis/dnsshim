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
package br.registro.dnsshim.xfrd.domain;

import java.util.Map;

import br.registro.dnsshim.common.server.ClientConfig;
import br.registro.dnsshim.common.server.ServerConfig;

public class XfrdConfig {
	private ServerConfig uiServerConfig = new ServerConfig();
	private ServerConfig dnsServerConfig = new ServerConfig();
	private ClientConfig signerClientConfig = new ClientConfig();

	public static final int DEFAULT_DNSSERVER_PORT = 53;
	public static final String LISTEN_ADDRESS = "";
	public static final int DEFAULT_UI_PORT = 9999;
	public static final int DEFAULT_SIGNER_PORT = 9797;
	public static final String DEFAULT_SIGNER_HOST = "localhost";
	public static final String DEFAULT_CA_PATH = "";
	public static final String DEFAULT_CA_PASSWD = "";
	public static final int DEFAULT_TSIG_FUDGE = 300;
	public static final int DEFAULT_SCHEDULER_LOW_LIMIT = 10 * 24; // 10 days
	public static final int DEFAULT_SCHEDULER_HIGH_LIMIT = 5 * 24; // 5 days
	public static final int DEFAULT_SLAVE_SYNC_PERIOD = 30 * 60; // 30 minutes
	public static final int DEFAULT_NOTIFY_TIMEOUT = 5; // 5 seconds
	public static final int DEFAULT_NOTIFY_RETRY_INTERVAL = 300; // 300 seconds
	public static final int DEFAULT_NOTIFY_MAX_RETRIES = 5; // 5 times
	public static final String DEFAULT_BINDSYNC_PATH = "./BindSync.sh";
	public static final String DEFAULT_NSDSYNC_PATH = "./NSDSync.sh";
	public static final int DEFAULT_CACHE_SIZE = 2000;
	public static final String DEFAULT_SHUTDOWN_SECRET = "";
	public static final int DEFAULT_MINIMUM_SOA_REFRESH = 86400; // 1 DAYS
	public static final int DEFAULT_MINIMUM_SOA_EXPIRE= 604800; // 7 DAYS

	private short tsigFudge = DEFAULT_TSIG_FUDGE;
	private int schedulerLowLimit = DEFAULT_SCHEDULER_LOW_LIMIT; // low priority
	private int schedulerHighLimit = DEFAULT_SCHEDULER_HIGH_LIMIT; // high priority
	private int slaveSyncPeriod = DEFAULT_SLAVE_SYNC_PERIOD;
	private String bindSyncPath = DEFAULT_BINDSYNC_PATH;
	private String nsdSyncPath = DEFAULT_NSDSYNC_PATH;

	private boolean slaveSyncEnabled = true;
	private boolean overwriteMname = false;
	
	private int notifyTimeout;
	private int notifyRetryInterval;
	private int notifyMaxRetries;
	
	private int cacheSize = DEFAULT_CACHE_SIZE;

	private String shutdownSecret = DEFAULT_SHUTDOWN_SECRET;

	private int minimumSOARefresh = DEFAULT_MINIMUM_SOA_REFRESH;
	private int minimumSOAExpire= DEFAULT_MINIMUM_SOA_EXPIRE;

	private Map<String, String> databaseProperties;
		
	public XfrdConfig() {
		uiServerConfig.setPort(DEFAULT_SIGNER_PORT);
		dnsServerConfig.setPort(DEFAULT_DNSSERVER_PORT);
		signerClientConfig.setHost(DEFAULT_SIGNER_HOST);
		signerClientConfig.setPort(DEFAULT_SIGNER_PORT);
	}
	
	public ServerConfig getUiServerConfig() {
		return uiServerConfig;
	}

	public void setUiServerConfig(ServerConfig uiServerConfig) {
		this.uiServerConfig = uiServerConfig;
	}

	public ServerConfig getDnsServerConfig() {
		return dnsServerConfig;
	}

	public void setDnsServerConfig(ServerConfig dnsServerConfig) {
		this.dnsServerConfig = dnsServerConfig;
	}

	public ClientConfig getSignerClientConfig() {
		return signerClientConfig;
	}

	public void setSignerClientConfig(ClientConfig signerClientConfig) {
		this.signerClientConfig = signerClientConfig;
	}

	public short getTsigFudge() {
		return tsigFudge;
	}

	public void setTsigFudge(short tsigFudge) {
		this.tsigFudge = tsigFudge;
	}

	public int getSchedulerLowLimit() {
		return schedulerLowLimit;
	}

	public void setSchedulerLowLimit(int schedulerLowLimit) {
		this.schedulerLowLimit = schedulerLowLimit;
	}

	public int getSchedulerHighLimit() {
		return schedulerHighLimit;
	}

	public void setSchedulerHighLimit(int schedulerHighLimit) {
		this.schedulerHighLimit = schedulerHighLimit;
	}

	public int getSlaveSyncPeriod() {
		return slaveSyncPeriod;
	}

	public void setSlaveSyncPeriod(int slaveSyncPeriod) {
		this.slaveSyncPeriod = slaveSyncPeriod;
	}

	public String getBindSyncPath() {
		return bindSyncPath;
	}

	public void setBindSyncPath(String bindSyncPath) {
		this.bindSyncPath = bindSyncPath;
	}
	
	public String getNsdSyncPath() {
		return nsdSyncPath;
	}

	public void setNsdSyncPath(String nsdSyncPath) {
		this.nsdSyncPath = nsdSyncPath;
	}

	public boolean isSlaveSyncEnabled() {
		return slaveSyncEnabled;
	}

	public void setSlaveSyncEnabled(boolean slaveSyncEnabled) {
		this.slaveSyncEnabled = slaveSyncEnabled;
	}

	public int getNotifyTimeout() {
		return notifyTimeout;
	}

	public void setNotifyTimeout(int notifyTimeout) {
		this.notifyTimeout = notifyTimeout;
	}

	public int getNotifyRetryInterval() {
		return notifyRetryInterval;
	}

	public void setNotifyRetryInterval(int notifyRetryInterval) {
		this.notifyRetryInterval = notifyRetryInterval;
	}

	public int getNotifyMaxRetries() {
		return notifyMaxRetries;
	}

	public void setNotifyMaxRetries(int notifyMaxRetries) {
		this.notifyMaxRetries = notifyMaxRetries;
	}

	public int getCacheSize() {
		return cacheSize;
	}

	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	public String getShutdownSecret() {
		return shutdownSecret;
	}

	public void setShutdownSecret(String shutdownSecret) {
		this.shutdownSecret = shutdownSecret;
	}

	public int getMinimumSOARefresh() {
		return minimumSOARefresh;
	}

	public void setMinimumSOARefresh(int minimumSOARefresh) {
		this.minimumSOARefresh = minimumSOARefresh;
	}

	public int getMinimumSOAExpire() {
		return minimumSOAExpire;
	}

	public void setMinimumSOAExpire(int minimumSOAExpire) {
		this.minimumSOAExpire = minimumSOAExpire;
	}

	public Map<String, String> getDatabaseProperties() {
		return databaseProperties;
	}

	public void setDatabaseProperties(Map<String, String> databaseProperties) {
		this.databaseProperties = databaseProperties;
	}

	public boolean getOverwriteMname() {
		return overwriteMname;
	}

	public void setOverwriteMname(boolean overwriteMname) {
		this.overwriteMname = overwriteMname;
	}
}