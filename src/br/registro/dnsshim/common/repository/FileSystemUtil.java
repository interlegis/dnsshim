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
package br.registro.dnsshim.common.repository;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

public class FileSystemUtil {
	private static final Logger logger = Logger.getLogger(FileSystemUtil.class);

	public static String hash(String name) throws IOException {
		StringBuilder path = new StringBuilder();
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			md.update(name.getBytes());
			byte[] digest = md.digest();
			char[] hash = Hex.encodeHex(digest);

			path.append(hash[0]).append(File.separatorChar).append(
					hash[1]).append(File.separatorChar).append(
					hash[2]).append(File.separatorChar);
			
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage(), e);
		}
		return path.toString();
	}
	
	public static void makePath(File path) throws IOException{
		if (!path.exists()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Creating directory " + path.toString());
			}
			if (!path.mkdirs()){
				throw new IOException("Cannot create path:"+path);
			}
		}
		
		/* pode existir e ser um arquivo...*/
		if (!path.isDirectory()) {
			throw new IOException("Cannot create path:"+path);
		}
	}
}
