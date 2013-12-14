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
package br.registro.dnsshim.util;

/**
 * RFC 4648
 * Base 32 Encoding with Extended Hex Alphabet
 */
public class Base32 {

	public static String base32HexEncode(byte[] hash){
	  // base32hex dictionary
	  final char[] b32hex = "0123456789abcdefghijklmnopqrstuv".toCharArray();
	
	  // total length must be a multiple of 5
	  int hashLength = hash.length;
	  if (hashLength % 5 != 0) {
	  	hashLength += 5 - (hashLength % 5);
	  }
	
	  StringBuilder output = new StringBuilder();
	  byte[] paddedInput = new byte[hashLength];
	  short[] paddedUnsignedInput = new short[hashLength];
	  System.arraycopy(hash, 0, paddedInput, 0, hash.length);
	
	  // Convert the input hash to unsigned
	  for (int i = 0; i < hashLength; i++) {
	  	paddedUnsignedInput[i] = ByteUtil.toUnsigned(paddedInput[i]);
	  }
	  
	  // Each 5 bytes corresponds to 8 new characters
	  for (int i = 0; i < hashLength; i += 5) {
	    int index = 0;
	    index = (paddedUnsignedInput[i + 0] >> 3) & 0x1f;
	    output.append(b32hex[index]);

	    index = (((paddedUnsignedInput[i + 0] & 0x7) << 2) | (paddedUnsignedInput[i + 1] >> 6)) & 0x1f;
	    output.append(b32hex[index]);
	
	    index = (paddedUnsignedInput[i + 1] >> 1) & 0x1f;
	    output.append(b32hex[index]);
	
	    index = (((paddedUnsignedInput[i + 1] & 0x1) << 4) | (paddedUnsignedInput[i + 2] >> 4)) & 0x1f;
	    output.append(b32hex[index]);
	
	    index = (((paddedUnsignedInput[i + 2] & 0xf) << 1) | (paddedUnsignedInput[i + 3] >> 7)) & 0x1f;
	    output.append(b32hex[index]);
	
	    index = (paddedUnsignedInput[i + 3] >> 2) & 0x1f;
	    output.append(b32hex[index]);
	
	    index = (((paddedUnsignedInput[i + 3] & 0x3) << 3) | (paddedUnsignedInput[i + 4] >> 5)) & 0x1f;
	    output.append(b32hex[index]);
	
	    index = paddedUnsignedInput[i + 4] & 0x1f;
	    output.append(b32hex[index]);
	  }
	
	  String result = output.toString();
	  return result;
	}

	public static byte[] base32HexDecode(String hash) {
	  if (hash.length() % 8 != 0) {
	    return null;
	  }
	
	  hash = hash.toLowerCase();
	  byte[] hashBytes = hash.getBytes();
	
	  int outputLen = 5*hash.length()/8;
	  byte[] output = new byte[outputLen];
	  int index = 0;
	
	  // Each 8 characters gives 5 binary octets
	  for (int i = 0; i < hashBytes.length; i+= 8) {
	  	byte[] b = new byte[8];
	  	for (int j = 0; j < 8; j++) {
	      if (hashBytes[i + j] >= 48 && hashBytes[i + j] <= 57) {
	      	// Digits 0-9
	      	b[j] = (byte) (hashBytes[i + j] - 48);
	      } else if (hashBytes[i + j] >= 97 && hashBytes[i + j] <= 118) {
	      	// Letters a-v
	      	b[j] = (byte) (hashBytes[i + j] - 87);
	      }
	    }
	
	    for (int j = 0; j < 5; j++) {
	    	byte current = 0;
	      switch (j) {
	      case 0:
	      	current = (byte) ((b[0] << 3) | (b[1] >> 2));
	      	break;
	      case 1:
	      	current = (byte) ((b[1] << 6) | (b[2] << 1) | (b[3] >> 4));
	      	break;
	      case 2:
	      	current = (byte) ((b[3] << 4) | (b[4] >> 1));
	      	break;
	      case 3:
	      	current = (byte) ((b[4] << 7) | (b[5] << 2) | (b[6] >> 3));
	      	break;
	      case 4:
	      	current = (byte) ((b[6] << 5) | b[7]);
	      	break;
	      }
	      
	      output[index++] = current;
	    }
	  }
	
		return output;
	}

}
