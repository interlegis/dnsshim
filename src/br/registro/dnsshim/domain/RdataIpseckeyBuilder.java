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

public class RdataIpseckeyBuilder {
	public static Rdata get(byte precedence, IpseckeyGatewayType gatewayType,
			IpseckeyAlgorithm algorithm, String gateway, byte[] publicKey) {

		byte[] gatewayBytes = DomainNameUtil.toWireFormat(gateway);
		int len = 1 + 1 + 1 + gatewayBytes.length + publicKey.length;

		byte[] ipseckeyBytes = new byte[len];
		int pos = 0;

		ByteUtil.copyBytes(ipseckeyBytes, pos, precedence);
		pos += 1;

		ByteUtil.copyBytes(ipseckeyBytes, pos, gatewayType.getValue());
		pos += 1;

		ByteUtil.copyBytes(ipseckeyBytes, pos, algorithm.getValue());
		pos += 1;

		System.arraycopy(gatewayBytes, 0, ipseckeyBytes, pos, gatewayBytes.length);
		pos += gatewayBytes.length;

		System.arraycopy(publicKey, 0, ipseckeyBytes, pos, publicKey.length);

		return new Rdata(ipseckeyBytes);

	}
}
