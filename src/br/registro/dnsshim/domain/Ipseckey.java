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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Scanner;

import org.apache.commons.codec.binary.Base64;

import br.registro.dnsshim.common.server.DnsshimProtocolException;
import br.registro.dnsshim.common.server.ProtocolStatusCode;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DomainNameUtil;

public class Ipseckey extends ResourceRecord  {
	final private byte precedence;
	final private IpseckeyGatewayType gatewayType;
	final private IpseckeyAlgorithm algorithm;
	final private String gateway;
	final private byte[] publicKey;

	public Ipseckey(String ownername, DnsClass dnsClass, int ttl,
			byte precedence, IpseckeyGatewayType gatewayType,
			IpseckeyAlgorithm algorithm, String gateway, byte[] publicKey) {
		super(ownername, RrType.IPSECKEY, dnsClass, ttl);

		this.precedence = precedence;
		this.gatewayType = gatewayType;
		this.algorithm = algorithm;
		this.gateway = gateway.toLowerCase();
		this.publicKey = publicKey;
		this.rdata = RdataIpseckeyBuilder.get(precedence, gatewayType, algorithm, this.gateway, publicKey);
	}

	public Ipseckey(String ownername, DnsClass dnsClass, int ttl, String rdata) {
		super(ownername, RrType.IPSECKEY, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");
		this.precedence = Byte.parseByte(scanner.next());
		short gatewayTypeShort = Short.parseShort(scanner.next());
		this.gatewayType = IpseckeyGatewayType.fromValue((byte) gatewayTypeShort);
		short algorithmShort = Short.parseShort(scanner.next());
		this.algorithm = IpseckeyAlgorithm.fromValue((byte) algorithmShort);
		this.gateway = scanner.next().toLowerCase();
		String publickeyStr = scanner.next();
		this.publicKey = publickeyStr.getBytes();
		this.rdata = RdataIpseckeyBuilder.get(precedence, gatewayType, algorithm, gateway, publicKey);
		scanner.close();
	}

	public byte getPrecedence() {
		return precedence;
	}

	public IpseckeyGatewayType getGatewayType() {
		return gatewayType;
	}

	public IpseckeyAlgorithm getAlgorithm() {
		return algorithm;
	}

	public String getGateway() {
		return gateway;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		rdata.append(precedence).append(SEPARATOR);
		rdata.append(gatewayType.getValue()).append(SEPARATOR);
		rdata.append(algorithm.getValue()).append(SEPARATOR);
		rdata.append(gateway).append(SEPARATOR);

		Base64 b64 = new Base64();
		rdata.append(new String(b64.encode(this.publicKey)));
		return rdata.toString();
	}

	public static Ipseckey parseIpseckey(String ownername,
			DnsClass dnsClass, int ttl, ByteBuffer buffer) throws DnsshimProtocolException {
		int rdlength = ByteUtil.toUnsigned(buffer.getShort());
		byte precedence = buffer.get();
		byte gatewayTypeByte = buffer.get();
		IpseckeyGatewayType gatewayType = IpseckeyGatewayType.fromValue(gatewayTypeByte);
		byte algorithmByte = buffer.get();
		IpseckeyAlgorithm algorithm = IpseckeyAlgorithm.fromValue(algorithmByte);

		int gatewayLen = 0;
		String gateway = "";
		if (gatewayType == IpseckeyGatewayType.WIRE_ENCODED_DOMAIN_NAME) {
			int pos1 = buffer.position();
			gateway = DomainNameUtil.toPresentationFormat(buffer);
			int pos2 = buffer.position();
			gatewayLen = pos2 - pos1;
		} else if (gatewayType == IpseckeyGatewayType.IPV4) {
			byte[] rdata = new byte[4];
			buffer.get(rdata);
			
			try {
			Inet4Address addr = (Inet4Address) InetAddress.getByAddress(rdata);
			gateway = addr.getHostAddress();
			gatewayLen = 4;
			} catch (UnknownHostException uhe) {
				throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Invalid IPSECKEY Resource Record");
			}
		} else if (gatewayType == IpseckeyGatewayType.IPV6) {
			byte[] rdata = new byte[16];
			buffer.get(rdata);
			
			try {
				Inet6Address addr = (Inet6Address) InetAddress.getByAddress(rdata);
				gateway = addr.getHostAddress();
				gatewayLen = 16;
			} catch (UnknownHostException uhe) {
				throw new DnsshimProtocolException(ProtocolStatusCode.INVALID_RESOURCE_RECORD, "Invalid IPSECKEY Resource Record");
			}
		}
		
		int publickeyLen = rdlength - (gatewayLen + 3);
		byte[] publicKey = new byte[publickeyLen];
		buffer.get(publicKey);
		
		return new Ipseckey(ownername, dnsClass, ttl, precedence, gatewayType,
				algorithm, gateway, publicKey);
	}

}
