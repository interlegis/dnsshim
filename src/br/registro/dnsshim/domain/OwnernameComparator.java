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

import java.util.Comparator;
import java.util.regex.Pattern;

public class OwnernameComparator implements Comparator<String> {
	private static final Pattern pattern = Pattern.compile("\\.");
	
	@Override
	public int compare(String first, String second) {
		String[] firstParts = pattern.split(first);
		String[] secondParts = pattern.split(second);

		int min = firstParts.length < secondParts.length ? firstParts.length : secondParts.length;

		for (int i = 1; i <= min; i++) {
			if (firstParts[firstParts.length - i].equals(secondParts[secondParts.length - i])) {
				continue;
			}
			return firstParts[firstParts.length - i].compareTo(secondParts[secondParts.length - i]) > 0 ? 1	: -1;

		}
		if (firstParts.length == secondParts.length) {
			return 0;
		} else if (firstParts.length > secondParts.length) {
			return 1;
		} else {
			return -1;
		}

	}
}