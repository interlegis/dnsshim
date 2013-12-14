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
package br.registro.dnsshim.domain.logic;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Calendar;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import br.registro.dnsshim.domain.Tsig;
import br.registro.dnsshim.util.ByteUtil;
import br.registro.dnsshim.util.DateUtil;
import br.registro.dnsshim.util.DomainNameUtil;
import br.registro.dnsshim.xfrd.dao.filesystem.SlaveDao;
import br.registro.dnsshim.xfrd.dns.protocol.ResponseCode;
import br.registro.dnsshim.xfrd.domain.Slave;
import br.registro.dnsshim.xfrd.domain.TsigKeyInfo;
import br.registro.dnsshim.xfrd.util.ServerContext;

public class TsigCalc {
	private static final Logger logger = Logger.getLogger(TsigCalc.class);

	public static String getTsigKey(String host, String key) {
		SlaveDao dao = new SlaveDao(ServerContext.getEntityManager());
		Slave slave = dao.findByAddress(host);
		if (slave == null) {
			logger.warn("Unauthorized request from host " + host + " with TSIG key " + key);
			return null;
		}
		
		TsigKeyInfo tsig= slave.getTsigKey(key);
		if (tsig== null) {
			logger.warn("Unauthorized request from host " + host + " with TSIG key " + key);
			return null;
		} 

		return tsig.getSecret();
	}
	
	public static byte[] generateTsigMac(byte[] bufferDnsMessage, Tsig tsigRequest, Tsig tsigResponse, String keySecret, boolean completeTsig) throws NoSuchAlgorithmException, InvalidKeyException {
		byte[] tsigKey = new Base64().decode(keySecret.getBytes());
		
		SecretKeySpec keySpec = new SecretKeySpec(tsigKey, "HmacMD5");

		// Get instance of Mac object implementing HMAC-MD5, and
		// initialize it with the above secret key
		Mac mac = Mac.getInstance("HmacMD5");
		mac.init(keySpec);
		
		mac.update(ByteUtil.toByteArray(tsigRequest.getMacSize()));
		mac.update(tsigRequest.getMac());
		
		return calcMac(mac, bufferDnsMessage, tsigResponse, completeTsig);
	}
	
	public static ResponseCode checkDigest(byte[] bufferDnsMessage, Tsig tsig,
			String keySecret, boolean completeTsig)
		throws NoSuchAlgorithmException, InvalidKeyException {
		
		if (tsig == null) {
			return ResponseCode.BADSIG;			
		} else if (keySecret == null || keySecret.isEmpty()) {		
			return ResponseCode.BADKEY;
		}
		
		// * Check time signed with fudge *
		long serverTime = DateUtil.removeMillisFromEpoch(Calendar.getInstance().getTime().getTime());
		long queryTsigTimeSigned = tsig.getTimeSigned(); // Not necessary "toUnsigned"
		int fudge = ByteUtil.toUnsigned(tsig.getFudge());

		if ((queryTsigTimeSigned + fudge) < serverTime ||
				(queryTsigTimeSigned - fudge) > serverTime) {

			return ResponseCode.BADTIME;
		}
		
		Base64 base64 = new Base64();
		byte[] tsigKey = base64.decode(keySecret.getBytes());

		SecretKeySpec keySpec = new SecretKeySpec(tsigKey, "HmacMD5");

		// Get instance of Mac object implementing HMAC-MD5, and
		// initialize it with the above secret key
		Mac mac = Mac.getInstance("HmacMD5");
		mac.init(keySpec);

		byte[] tsigMac = calcMac(mac, bufferDnsMessage, tsig, completeTsig);
		
		/*
		 * Compare calculate request mac with actual request mac. Find mac offset:
		 * skip AlgName, Time Signed, Fudge and Mac size
		 */
		if (!Arrays.equals(tsig.getMac(), tsigMac)) {
			if (logger.isDebugEnabled()) {
				StringBuilder msg = new StringBuilder();

				msg.append(System.getProperty("line.separator"));

				msg.append("Msg Dns   : ");
				msg.append(new String(base64.encode(bufferDnsMessage)));
				msg.append(System.getProperty("line.separator"));

				msg.append("Tsig rdata: ");
				msg.append(tsig.getRdata().toStringHex());
				msg.append(System.getProperty("line.separator"));

				msg.append("Key secret: ");
				msg.append(keySecret);
				msg.append(System.getProperty("line.separator"));

				msg.append("Tsig mac          : ");
				msg.append(new String(base64.encode(tsig.getMac())));
				msg.append(System.getProperty("line.separator"));

				msg.append("Tsig mac (generate) : ");
				msg.append(new String(base64.encode(tsigMac)));
				msg.append(System.getProperty("line.separator"));

				logger.debug(msg.toString());
			}

			return ResponseCode.BADSIG;
		}

		return ResponseCode.NOERROR;
	}

	private static byte[] calcMac(Mac mac, byte[] bufferDnsMessage,Tsig tsig, boolean completeTsig) throws NoSuchAlgorithmException, InvalidKeyException {
			
		// Dns message before Tsig
		mac.update(bufferDnsMessage);
	
		if (completeTsig) {
			// Keyname (ownername)
			mac.update(DomainNameUtil.toWireFormat(tsig.getOwnername().toLowerCase()));

			// Class
			mac.update(ByteUtil.toByteArray(tsig.getDnsClass().getValue()));

			// Ttl
			mac.update(ByteUtil.toByteArray(tsig.getTtl()));

			// Algorithm Name
			mac.update(DomainNameUtil.toWireFormat(tsig.getAlgorithmName()));
		}
		
		// Time Signed
		byte[] timeSignedBytes = ByteUtil.toByteArray(tsig.getTimeSigned());
		byte[] timeSignedBytes6 = new byte[6];
		System.arraycopy(timeSignedBytes, 2, timeSignedBytes6, 0, 6);
		mac.update(timeSignedBytes6);
		
		// Fudge
		mac.update(ByteUtil.toByteArray(tsig.getFudge()));
						
		if (completeTsig) {
			// Error
			mac.update(ByteUtil.toByteArray(tsig.getError().getValue()));

			// Other Len
			mac.update(ByteUtil.toByteArray(tsig.getOtherLen()));

			if (ByteUtil.toUnsigned(tsig.getOtherLen()) > 0) {
				// Other Data
				mac.update(tsig.getOtherData());
			}
		}
		
		return mac.doFinal();
	}
	
	public static byte[] generateKey() throws NoSuchAlgorithmException {
		KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacMD5");
		SecretKey key = keyGenerator.generateKey();
	     
		Base64 base64 = new Base64();
		return base64.encode(key.getEncoded());
	}
	
}