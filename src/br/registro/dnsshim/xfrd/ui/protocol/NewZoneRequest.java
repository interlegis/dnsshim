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
package br.registro.dnsshim.xfrd.ui.protocol;

import br.registro.dnsshim.domain.DnskeyAlgorithm;
import br.registro.dnsshim.domain.Soa;
import br.registro.dnsshim.util.DomainNameUtil;
import br.registro.dnsshim.xfrd.domain.ZoneInfo;

public class NewZoneRequest extends RestrictOperation {
	private String zone;
	private Soa soa;
	private boolean signed;
	private int keySize;
	private DnskeyAlgorithm algorithm;
	private int expirationPeriod;
	private String slaveGroup;
	private boolean autoBalance;

	public NewZoneRequest() {
		this.soa = null;
		this.zone = "";
		this.signed = false;
		this.keySize = 0;
		this.algorithm = DnskeyAlgorithm.RSASHA1;
		this.expirationPeriod = 0;
	}

	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = DomainNameUtil.trimDot(zone.trim().toLowerCase());
	}

	public Soa getSoa() {
		return soa;
	}

	public void setSoa(Soa soa) {
		this.soa = soa;
	}
	
	public boolean isSigned() {
		return signed;
	}

	public void setSigned(boolean signed) {
		this.signed = signed;
	}

	public int getKeySize() {
		return keySize;
	}
	public void setKeySize(int keySize) {
		this.keySize = keySize;
	}

	public DnskeyAlgorithm getAlgorithm() {
		return algorithm;
	}
	public void setAlgorithm(DnskeyAlgorithm algorithm) {
		this.algorithm = algorithm;
	}
	
	public int getExpirationPeriod() {
		return expirationPeriod;
	}
	
	public String getSlaveGroup() {
		return slaveGroup;
	}

	public void setSlaveGroup(String slaveGroup) {
		this.slaveGroup = slaveGroup;
	}
	
	public boolean isAutoBalance() {
		return autoBalance;
	}

	public void setAutoBalance(boolean autoBalance) {
		this.autoBalance = autoBalance;
	}

	public void setExpirationPeriod(int expirationPeriod) {
		if (expirationPeriod < 0 || expirationPeriod > ZoneInfo.MAX_EXPIRATION) {
			this.expirationPeriod = ZoneInfo.MAX_EXPIRATION;
			return;
		}
		
		if (expirationPeriod < ZoneInfo.MIN_EXPIRATION) {
			this.expirationPeriod = ZoneInfo.MIN_EXPIRATION;
			return;
		}
		
		this.expirationPeriod = expirationPeriod;
	}
}
