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

import java.nio.ByteBuffer;
import java.util.regex.Pattern;

public class DomainNameUtil {
	private static final Pattern dot = Pattern.compile("\\.");
	private static final int LABEL_LENGTH_MAX = 63;
	private static final int DOMAIN_NAME_LENGTH_MAX = 255;
	
	public static String trimDot(String name) {
		if (name.isEmpty() || (name.charAt(0) != '.' && name.charAt(name.length() - 1) != '.')) {
			return name;
		}
	
		StringBuilder own = new StringBuilder(name);
		while ((own.length() > 0) && (own.charAt(0) == '.')) {
			own.deleteCharAt(0);
		}
		while ((own.length() > 0) && (own.charAt(own.length() - 1) == '.')) {
			own.deleteCharAt(own.length() - 1);
		}
		return own.toString();
	}

	public static String toFullName(String name, String zonename) {
		if (name.isEmpty()) {
			return zonename;
		}
		
		StringBuilder fullname = new StringBuilder(trimDot(name));
		fullname.append('.').append(trimDot(zonename));
	
		if ((fullname.length() > 0)
				&& (fullname.charAt(fullname.length() - 1) != '.')) {
			fullname.append('.');
		}
	
		return fullname.toString();
	}

	public static byte countLabels(String ownername, String zonename) {
		String fullname = toFullName(ownername, zonename);
		return (byte) dot.split(fullname).length;
	}

	public static byte[] toWireFormat(String name) {
		return DomainNameUtil.toWireFormat(name, "");
	}

	/**
	 * label validation (rfc 1034 sec.3.1)
	 * @param fqdn
	 * @return true or false
	 */
	public static boolean validate(String fqdn) {
		if (fqdn.length() > DOMAIN_NAME_LENGTH_MAX) {
			return false;
		}
		
		String[] labels = dot.split(fqdn);
		for (String label : labels) {
			if (label.isEmpty() || label.length() > LABEL_LENGTH_MAX) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * ownername validation (rfc 1034 sec.3.1)
	 * @param ownername
	 * @return true or false
	 */
	public static boolean validateOwnername(String ownername) {
		if (ownername.length() > DOMAIN_NAME_LENGTH_MAX) {
			return false;
		}
		
		String[] labels = dot.split(ownername);
		for (String label : labels) {
			// ownername can be empty (apex)
			if (label.length() > LABEL_LENGTH_MAX) {
				return false;
			}
		}
		return true;
	}
	
	
	
	public static byte[] toWireFormat(String name, String zonename) {
		String fqdn = trimDot(toFullName(name, zonename));
		
		byte[] data = new byte[fqdn.length() + 2];

		String[] labels = dot.split(fqdn);
		int curr = 0;
		for (String label : labels) {
			data[curr] = (byte) label.length();
			byte[] labelBytes = label.getBytes();
			System.arraycopy(labelBytes, 0, data, curr + 1, labelBytes.length);
			curr += labelBytes.length + 1;
		}
		data[data.length - 1] = 0;
	
		return data;
	}

	/**
	 * Read name in wire-format from buffer. </br>
	 * Based RFC 1035 - 4.1.4
	 */
	public static String toPresentationFormat(ByteBuffer buffer) {
		int position = buffer.position();
		
		StringBuffer name = new StringBuffer();
		DomainNameUtil.toPresentationFormat(name, buffer, position);
		
		return name.toString();
	}

	/**
	 * Read name in wire-format from buffer
	 * and return in variable 'name'. </br>
	 * 
	 * Based RFC 1035 - 4.1.4
	 */
	private static void toPresentationFormat(StringBuffer name,
			ByteBuffer buffer,
			int position) {
		buffer.position(position);
		byte firstByte = buffer.get();
		
		if ((firstByte & 0xC0) == 0xC0) {
			byte secondByte = buffer.get();
			
			ByteBuffer offset = ByteBuffer.allocate(2);
			offset.put(ByteUtil.getBits(firstByte, 3, 6));
			offset.put(secondByte);
			offset.rewind();

			toPresentationFormat(name, buffer, (int) offset.getShort());
			buffer.position(position + 2);
			
		} else {			
			byte labelLength = ByteUtil.getBits(firstByte, 3, 6);
			
			if (labelLength == 0) {
				return;
			}
			
			byte[] labelBytes = new byte[labelLength];
			buffer.get(labelBytes);
			
			name.append(new String(labelBytes));
			name.append('.');
			toPresentationFormat(name, buffer, buffer.position());
		}		
	}
	
	public static boolean isInsideZone(String fullname, String zone) {
		fullname = trimDot(fullname);
		zone = trimDot(zone);
		
		if (fullname.endsWith(zone)) {
			return true;
		}
		
		return false;
	}

	public static String removeZoneFromName(String name, String zone) {
		name = trimDot(name);
		zone = trimDot(zone);
		
		if (name.equals(zone)) {
			return "";
		}
		
		if (name.endsWith(zone)) {
			int ownernameLen = name.length();
			int zoneLen = zone.length();
			name = name.substring(0, ownernameLen - zoneLen);
			return trimDot(name);
		}
		
		return name;
	}
}
