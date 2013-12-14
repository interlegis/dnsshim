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
package br.registro.dnsshim.common.server;

public enum ProtocolStatusCode {
	
	OK							(1, Level.SUCCESS),
	BAD_UICLIENT_REQUEST		(2, Level.FATAL),
	INVALID_SIGNER_OPERATION	(3, Level.ERROR),
	EMPTY_RRSET					(4, Level.ERROR),
	INVALID_ALGORITHM			(5, Level.ERROR),
	INVALID_KEY					(6, Level.ERROR),
	SIGNATURE_ERROR				(7, Level.ERROR),
	NO_SOA						(8, Level.ERROR),
	TRANSFER_NOT_ALLOWED		(9, Level.ERROR),
	NOT_AUTHORITATIVE			(10, Level.ERROR),
	DNS_MESSAGE_FORMERR			(11, Level.ERROR),
	BAD_TSIG					(12, Level.ERROR),
	INVALID_DNS_MESSAGE			(13, Level.ERROR),
	REFUSED_OPERATION			(14, Level.ERROR),
	DNS_SERVER_FAILURE			(15, Level.ERROR),
	ZONE_NOT_FOUND				(16, Level.ERROR),
	PUBLICATION_ERROR			(17, Level.ERROR),
	INVALID_SOA_VERSION			(18, Level.ERROR),
	SIGNER_SERVER_FAILURE		(19, Level.ERROR),
	ZONE_ALREADY_EXISTS			(20, Level.ERROR),
	RESOURCE_RECORD_NOT_FOUND	(21, Level.ERROR),
	SLAVE_ALREADY_EXISTS		(22, Level.ERROR),
	SLAVE_NOT_FOUND				(23, Level.ERROR),
	INVALID_KEY_STATUS			(24, Level.ERROR),
	CONNECTION_REFUSED			(25, Level.ERROR),
	UI_SERVER_ERROR				(27, Level.ERROR),
	INVALID_RESOURCE_RECORD		(28, Level.ERROR),
	RESOURCE_RECORD_ADD_ERROR	(29, Level.ERROR),
	SLAVE_GROUP_NOT_FOUND 		(30, Level.ERROR),
	SLAVE_GROUP_ALREADY_EXISTS  (31, Level.ERROR),
	FORBIDDEN					(32, Level.ERROR),
	USER_NOT_FOUND				(50, Level.ERROR),
	INVALID_USER				(51, Level.ERROR),
	USER_ALREADY_EXISTS			(52, Level.ERROR),
	INVALID_PASSWORD			(53, Level.ERROR),
	;
	
	private ProtocolStatusCode(int code, Level level) {
		this.code = code;
		this.level = level;
	}
	
	enum Level {ERROR, SUCCESS, FATAL};
	private Level level;
	private int code;
	
	public int getValue() {
		return code;
	}
	
	public Level getLevel() {
		return level;
	}
}