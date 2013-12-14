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

import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DomainNameUtil;


/**
 * Based from RFC 1035
 * <pre>
 *                                  1  1  1  1  1  1
 *    0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  |                                               |
 *  /                                               /
 *  /                      NAME                     /
 *  |                                               |
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  |                      TYPE                     |
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  |                     CLASS                     |
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  |                      TTL                      |
 *  |                                               |
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  |                   RDLENGTH                    |
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--|
 *  /                     RDATA                     /
 *  /                                               /
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  
 *  where:
 *  
 * NAME            an owner name, i.e., the name of the node to which this
 *                 resource record pertains.
 *                  
 * TYPE            two octets containing one of the RR TYPE codes.
 * 
 * CLASS           two octets containing one of the RR CLASS codes.
 * 
 * TTL             a 32 bit signed integer that specifies the time interval
 *                 that the resource record may be cached before the source
 *                 of the information should again be consulted.  Zero
 *                 values are interpreted to mean that the RR can only be
 *                 used for the transaction in progress, and should not be
 *                 cached.  For example, SOA records are always distributed
 *                 with a zero TTL to prohibit caching.  Zero values can
 *                 also be used for extremely volatile data.
 *                 
 * RDLENGTH        an unsigned 16 bit integer that specifies the length in
 *                 octets of the RDATA field.
 *                 
 * RDATA           a variable length string of octets that describes the
 *                 resource.  The format of this information varies
 *                 according to the TYPE and CLASS of the resource record.
 *                 
 * </pre>
 *
 */
public abstract class ResourceRecord implements Comparable<ResourceRecord> {
	public static final String APEX_OWNERNAME = "";
	
	private String ownername;	
	final private RrType type;
	private DnsClass dnsClass;
	private int ttl;
	protected Rdata rdata;
	
	protected static final char SEPARATOR = ' '; // separador para presentation format
	protected static final char TAB = '\t';
	
	public ResourceRecord(String ownername,
			RrType type, DnsClass dnsClass, int ttl) {
		this.ownername = DomainNameUtil.trimDot(ownername).toLowerCase();
		this.type = type;
		this.dnsClass = dnsClass;
		this.ttl = ttl;
	}
	
	public ResourceRecord(String ownername, RrType type, DnsClass dnsClass, int ttl, Rdata rdata) {
		this(ownername, type, dnsClass, ttl);
		this.rdata = new Rdata(rdata);	
	}
	
	public String getOwnername() {
		return ownername;
	}
	public void setOwnername(String ownername) {
		this.ownername = ownername.toLowerCase();
	}
	
	public boolean isApex() {
		return ResourceRecord.APEX_OWNERNAME.equals(ownername);
	}

	public RrType getType() {
		return type;
	}

	public DnsClass getDnsClass() {
		return dnsClass;
	}

	public void setDnsClass(DnsClass dnsClass) {
		this.dnsClass = dnsClass;
	}

	public int getTtl() {
		return ttl;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public Rdata getRdata() {
		return rdata;
	}
	
	public String rdataPresentation() {
		return rdata.toString();
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		long ttl = ByteUtil.toUnsigned(getTtl());		
		result.append(getOwnername()).append(TAB);
		result.append(getDnsClass()).append(SEPARATOR);
		
		if (ttl > 0) {
			result.append(ttl).append(SEPARATOR);
		}
		
		result.append(getType()).append(SEPARATOR);
		result.append(rdataPresentation());

		return result.toString();
	}
	
	@Override
	public int compareTo(ResourceRecord rr) {
		OwnernameComparator ownComp = new OwnernameComparator();
		int ownRes = ownComp.compare(this.ownername, rr.ownername);

		if (ownRes != 0) {
			return ownRes;
		}

		RrTypeComparator typeComp = new RrTypeComparator();
		int typeRes = typeComp.compare(this.type, rr.type);

		if (typeRes != 0) {
			return typeRes;
		}

		return this.rdata.compareTo(rr.rdata);
	}

	/**
	 * Ignore TTL when comparing RRs
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ResourceRecord) {
			ResourceRecord rr = (ResourceRecord) obj;
			if (this.ownername.equals(rr.ownername) &&
				this.type == rr.type	&&
				this.dnsClass == rr.dnsClass &&
				this.rdata.equals(rr.rdata)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return ownername.hashCode() * type.getValue() * dnsClass.getValue() * rdata.hashCode();
	}
}
