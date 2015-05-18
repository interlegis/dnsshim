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
package br.registro.dnsshim.domain;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.util.DomainNameUtil;
import br.registro.dnsshim.xfrd.domain.XfrdConfig;
import br.registro.dnsshim.xfrd.domain.logic.XfrdConfigManager;

public class Soa extends ResourceRecord {

	private String mname;
	final private String rname;
	final private int serial;
	final private int refresh;
	final private int retry;
	final private int expire;
	final private int minimum;
	
	public Soa(String ownername, DnsClass dnsClass, int ttl,
			String mname, String rname,
			int serial, int refresh, int retry, int expire, int minimum) {
		super(ownername, RrType.SOA, dnsClass, ttl);
		this.mname = mname.toLowerCase();
		this.rname = rname.toLowerCase();
		this.serial = serial;
		
		// Refresh minimum
		XfrdConfig config = XfrdConfigManager.getInstance();
		if (refresh < config.getMinimumSOARefresh()) {
			this.refresh = config.getMinimumSOARefresh();		
		} else {
			this.refresh = refresh;
		}
		
		this.retry = retry;
		
		// Expire minimum
		if (expire < config.getMinimumSOAExpire()) {
			this.expire = config.getMinimumSOAExpire();
		} else {
			this.expire = expire;
		}
		
		this.minimum = minimum;
		this.rdata = RdataSoaBuilder.get(this.mname,	this.rname,
				this.serial, this.refresh, this.retry, this.expire, this.minimum);
	}
	
	
	
	public Soa(String ownername, DnsClass dnsClass, int ttl, String rdata) throws DnsshimProtocolException {
		super(ownername, RrType.SOA, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");
		
		mname = scanner.next().toLowerCase();
		rname = scanner.next().toLowerCase();
		
		if (!DomainNameUtil.validate(mname)) {
			scanner.close();
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Invalid domain name:"+mname);
		}
		
		if (!DomainNameUtil.validate(rname)) {
			scanner.close();
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Invalid domain name:"+rname);
		}
		
		String next = scanner.next();
		if (next.equals("(")) {
			serial = Integer.parseInt(scanner.next());
		} else {
			serial = Integer.parseInt(next);
		}
		
		refresh = Integer.parseInt(scanner.next());
		retry = Integer.parseInt(scanner.next());
		expire = Integer.parseInt(scanner.next());
		minimum = Integer.parseInt(scanner.next());
		
		this.rdata = RdataSoaBuilder.get(mname, rname,
				serial, refresh, retry, expire, minimum);
		scanner.close();
	}
	public String getMname() {
		return mname;
	}
	
	public void setMname(String mname) {
		this.mname = mname;
	}

	public String getRname() {
		return rname;
	}

	public int getSerial() {
		return serial;
	}

	public int getRefresh() {
		return refresh;
	}
	
	public int getRetry() {
		return retry;
	}

	public int getExpire() {
		return expire;
	}

	public int getMinimum() {
		return minimum;
	}
	
	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		rdata.append(mname).append(SEPARATOR);
		rdata.append(rname).append(SEPARATOR);
		rdata.append(serial).append(SEPARATOR);
		rdata.append(refresh).append(SEPARATOR);
		rdata.append(retry).append(SEPARATOR);
		rdata.append(expire).append(SEPARATOR);
		rdata.append(minimum);
	
		return rdata.toString();
	}

	public static Soa parseSoa(String ownername, DnsClass dnsClass, int ttl,
			ByteBuffer buffer) {
		buffer.getShort(); // rdlength
		String mname = DomainNameUtil.toPresentationFormat(buffer);
		String rname = DomainNameUtil.toPresentationFormat(buffer);
		int serial = buffer.getInt();
		int refresh = buffer.getInt();
		int retry = buffer.getInt();
		int expire = buffer.getInt();
		int minimum = buffer.getInt();
		
		return new Soa(ownername, dnsClass, ttl,
				mname, rname, serial, refresh, retry, expire, minimum);
	}
	
	public static long getNextSerial(long oldSerial) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDD");
		String julianToday = sdf.format(cal.getTime());
		String oldSerialStr = Long.toString(oldSerial);
		long newSerial = oldSerial + 1;
		
		// Check if the current serial is in julian date format
		if (oldSerialStr.length() < 10 ||
				oldSerialStr.substring(0, 7).equals(julianToday) == false) {
			String newSerialStr = julianToday + "000";
			newSerial = Long.parseLong(newSerialStr);
			if (oldSerial > newSerial) {
				newSerial = oldSerial + 1;
			}
		}
		return newSerial;
	}
}
