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
package br.registro.dnsshim.xfrd.scheduler.domain;

import java.util.Date;



public class ZoneResign implements Comparable<ZoneResign> {
	private String zonename;
	private long expiration;
	private long scheduledDate; // defined by the scheduler

	public ZoneResign() {
		Date now = new Date(); 
		expiration = now.getTime();
	}
	
	public String getZonename() {
		return zonename;
	}

	public void setZonename(String zonename) {
		this.zonename = zonename;
	}

	public long getExpiration() {
		return expiration;
	}

	public void setExpiration(long expiration) {
		this.expiration = expiration;
	}

	public long getScheduledDate() {
		return scheduledDate;
	}

	public void setScheduledDate(long scheduledDate) {
		this.scheduledDate = scheduledDate;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ZoneResign) {
			ZoneResign s = (ZoneResign) obj;
			return zonename.equals(s.zonename);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return zonename.hashCode();
	}

	@Override
	public int compareTo(ZoneResign o) {
		if (this.zonename.equals(o.zonename)) {
			return 0;
		} else {
			return (this.expiration > o.expiration ? 1 : -1);
		}	
	}

}
