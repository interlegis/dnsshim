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

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.util.DomainNameUtil;

public class ResourceRecordFactory {

	public static ResourceRecord getRr(String ownername, RrType type,
			DnsClass dnsClass, int ttl, String rdataStr)
			throws DnsshimProtocolException {

		if (!DomainNameUtil.validateOwnername(ownername)) {
			throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Invalid ownername:" + ownername);
		}
		
		switch (type) {
		case A:
			return new A(ownername, dnsClass, ttl, rdataStr);
		case NS:
			return new Ns(ownername, dnsClass, ttl, rdataStr);
		case SOA:
			return new Soa(ownername, dnsClass, ttl, rdataStr);
		case MINFO:
			return new Minfo(ownername, dnsClass, ttl, rdataStr);
		case HINFO:
			return new Hinfo(ownername, dnsClass, ttl, rdataStr);
		case AAAA:
			return new Aaaa(ownername, dnsClass, ttl, rdataStr);
		case LOC:
			return new Loc(ownername, dnsClass, ttl, rdataStr);
		case NAPTR:
			return new Naptr(ownername, dnsClass, ttl, rdataStr);
		case CERT:
			return new Cert(ownername, dnsClass, ttl, rdataStr);
		case MX:
			return new Mx(ownername, dnsClass, ttl, rdataStr);
		case PTR:
			return new Ptr(ownername, dnsClass, ttl, rdataStr);
		case CNAME:
			return new Cname(ownername, dnsClass, ttl, rdataStr);
		case DNAME:
			return new Dname(ownername, dnsClass, ttl, rdataStr);
		case TXT:
			return new Txt(ownername, dnsClass, ttl, rdataStr);
		case DNSKEY:
			return new Dnskey(ownername, dnsClass, ttl, rdataStr);
		case IPSECKEY:
			return new Ipseckey(ownername, dnsClass, ttl, rdataStr);
		case RRSIG:
			return new Rrsig(ownername, dnsClass, ttl, rdataStr);
		case NSEC:
			return new Nsec(ownername, dnsClass, ttl, rdataStr);
		case NSEC3:
			return new Nsec3(ownername, dnsClass, ttl, rdataStr);
		case NSEC3PARAM:
			return new Nsec3Param(ownername, dnsClass, ttl, rdataStr);
		case DS:
			return new Ds(ownername, dnsClass, ttl, rdataStr);
		case SSHFP:
			return new Sshfp(ownername, dnsClass, ttl, rdataStr);
		case TSIG:
			return new Tsig(ownername, dnsClass, ttl, rdataStr);
		case SRV:
			return new Srv(ownername, dnsClass, ttl, rdataStr);
		case TLSA:
			return new Tlsa(ownername, dnsClass, ttl, rdataStr);
		default:
			Rdata rdata = new Rdata(rdataStr.getBytes());
			return new GenericResourceRecord(ownername, type, dnsClass, ttl, rdata);
		}
	}

	public static ResourceRecord readRr(String ownername, RrType type,
			DnsClass dnsClass, int ttl, ByteBuffer buffer)
			throws DnsshimProtocolException {
		switch (type) {
		case A:
			return A.parseA(ownername, dnsClass, ttl, buffer);
		case NS:
			return Ns.parseNs(ownername, dnsClass, ttl, buffer);
		case SOA:
			return Soa.parseSoa(ownername, dnsClass, ttl, buffer);
		case MINFO:
			return Minfo.parseMinfo(ownername, dnsClass, ttl, buffer);
		case HINFO:
			return Hinfo.parseHinfo(ownername, dnsClass, ttl, buffer);
		case AAAA:
			return Aaaa.parseAaaa(ownername, dnsClass, ttl, buffer);
		case LOC:
			return Loc.parseLoc(ownername, dnsClass, ttl, buffer);
		case NAPTR:
			return Naptr.parseNaptr(ownername, dnsClass, ttl, buffer);
		case CERT:
			return Cert.parseCert(ownername, dnsClass, ttl, buffer);
		case MX:
			return Mx.parseMx(ownername, dnsClass, ttl, buffer);
		case PTR:
			return Ptr.parsePtr(ownername, dnsClass, ttl, buffer);
		case CNAME:
			return Cname.parseCname(ownername, dnsClass, ttl, buffer);
		case DNAME:
			return Dname.parseDname(ownername, dnsClass, ttl, buffer);
		case TXT:
			return Txt.parseTxt(ownername, dnsClass, ttl, buffer);
		case DNSKEY:
			return Dnskey.parseDnskey(ownername, dnsClass, ttl, buffer);
		case IPSECKEY:
			return Ipseckey.parseIpseckey(ownername, dnsClass, ttl, buffer);
		case RRSIG:
			return Rrsig.parseRrsig(ownername, dnsClass, ttl, buffer);
		case NSEC:
			return Nsec.parseNsec(ownername, dnsClass, ttl, buffer);
		case NSEC3:
			return Nsec3.parseNsec3(ownername, dnsClass, ttl, buffer);
		case NSEC3PARAM:
			return Nsec3Param.parseNsec3Param(ownername, dnsClass, ttl, buffer);
		case DS:
			return Ds.parseDs(ownername, dnsClass, ttl, buffer);
		case SSHFP:
			return Sshfp.parseSshfp(ownername, dnsClass, ttl, buffer);
		case TSIG:
			return Tsig.parseTsig(ownername, dnsClass, ttl, buffer);
		case SRV:
			return Srv.parseSrv(ownername, dnsClass, ttl, buffer);
		case TLSA:
			return Tlsa.parseTlsa(ownername, dnsClass, ttl, buffer);
		default:
			int rdlength = buffer.getShort();
			byte[] data = new byte[rdlength];
			buffer.get(data);
			Rdata rdata = new Rdata(data);
			return new GenericResourceRecord(ownername, type, dnsClass, ttl, rdata);
		}
	}

	public static ResourceRecord readRr(ByteBuffer buffer)
			throws DnsshimProtocolException {
		String ownername = DomainNameUtil.toPresentationFormat(buffer);
		ownername = ownername.toLowerCase();
		short typeShort = buffer.getShort();
		RrType type = RrType.fromValue(typeShort);
		DnsClass dnsClass = DnsClass.fromValue(buffer.getShort());
		int ttl = buffer.getInt();

		return readRr(ownername, type, dnsClass, ttl, buffer);
	}
}
