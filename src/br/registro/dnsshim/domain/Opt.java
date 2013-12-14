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
import br.registro.dnsshim.xfrd.dns.protocol.DnsPacket;

/**
 * OPT doesn't extend ResourceRecord because it is a pseudo-RR,
 * using the ResourceRecord attributes in different manner, as follows
 * 
 * <pre>
 * Field Name   Field Type     Description
 * ------------------------------------------------------
 * NAME         domain name    empty (root domain)
 * TYPE         u_int16_t      OPT
 * CLASS        u_int16_t      sender's UDP payload size
 * TTL          u_int32_t      extended RCODE and flags
 * RDLEN        u_int16_t      describes RDATA
 * RDATA        octet stream   {attribute,value} pairs
 * 
 * <br/>
 * 
 * The extended RCODE and flags (which OPT stores in the RR TTL field)
 * are structured as follows:
 *              +0 (MSB)                            +1 (LSB)
 *    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 * 0: |         EXTENDED-RCODE        |            VERSION            |
 *    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 * 2: |                               Z                               |
 *    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
 * 
 * </pre>
 * 
 */
public class Opt {
	private static final String ownername = "";
	private final RrType type = RrType.OPT;
	
	private short udpPayloadSize;
	private byte extendedRcode;
	private byte version;
	private short z;
	
	private Rdata rdata;
	
	public Opt() {
		udpPayloadSize = DnsPacket.MAX_UDP_PACKET_SIZE;
		extendedRcode = 0;
		
		byte[] data = new byte[0]; 
		rdata = new Rdata(data);
	}
	
	public String getOwnername() {
		return ownername;
	}
	
	public RrType getType() {
		return type;
	}
	
	public short getUdpPayloadSize() {
		return udpPayloadSize;
	}
	
	public void setUdpPayloadSize(short udpPayloadSize) {
		this.udpPayloadSize = udpPayloadSize;
	}
	
	public byte getExtendedRcode() {
		return extendedRcode;
	}

	public void setExtendedRcode(byte extendedRcode) {
		this.extendedRcode = extendedRcode;
	}

	public byte getVersion() {
		return version;
	}

	public void setVersion(byte version) {
		this.version = version;
	}

	public short getZ() {
		return z;
	}

	public void setZ(short z) {
		this.z = z;
	}
	
	/**
	 * Setting the DO bit to one in a query indicates to the server that the
	 * resolver is able to accept DNSSEC security RRs. The DO bit cleared (set to
	 * zero) indicates the resolver is unprepared to handle DNSSEC security RRs
	 * and those RRs MUST NOT be returned in the response (unless DNSSEC security
	 * RRs are explicitly queried for).
	 */
	public boolean isDnssecOk() {
		return (ByteUtil.getBit(z, 1));
	}
	
	public Rdata getRdata() {
		return rdata;
	}
	
	public void setRdata(Rdata rdata) {
		this.rdata = rdata;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		int udpPayLoadSize = ByteUtil.toUnsigned(getUdpPayloadSize()); 
		short extendedRcode = ByteUtil.toUnsigned(this.extendedRcode);
		short version = ByteUtil.toUnsigned(this.version);
		int z = ByteUtil.toUnsigned(this.z);
		int rdlength = (int) this.rdata.getRdlen();
		
		result.append(getOwnername()).append(ResourceRecord.TAB);
		result.append(udpPayLoadSize).append(ResourceRecord.SEPARATOR);
		result.append(extendedRcode).append(ResourceRecord.SEPARATOR);
		result.append(version).append(ResourceRecord.SEPARATOR);
		result.append(z).append(ResourceRecord.SEPARATOR);
		result.append(rdlength).append(ResourceRecord.SEPARATOR);
		result.append(rdata);

		return result.toString();
	}
}
