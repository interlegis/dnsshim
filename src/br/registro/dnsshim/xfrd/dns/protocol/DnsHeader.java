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
package br.registro.dnsshim.xfrd.dns.protocol;

import br.registro.dnsshim.util.ByteUtil;

/**
 * Based from RFC 1035 and RFC 4035
 * 
 * <pre>
 *                               1  1  1  1  1  1
 * 0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * |                      ID                       |
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * |QR|   Opcode  |AA|TC|RD|RA| Z|AD|CD|   RCODE   |
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * |                    QDCOUNT                    |
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * |                    ANCOUNT                    |
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * |                    NSCOUNT                    |
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * |                    ARCOUNT                    |
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * </pre>
 * 
 * 
 */
public class DnsHeader {
	public static final int HEADER_SIZE = 12; // bytes

	private short id;
	private boolean query; // response flag
	private Opcode operationalCode; // purpose of message (only necessary 4 bits)

	/*
	 * Registry Name: DNS Header Flags Reference: [RFC1035] Registration
	 * Procedures: Standards Action
	 * 
	 * Note: In DNS query header there is a flag field in the second 16 bit word
	 * in query from bit 5 through bit 11 ([RFC1035] section 4.1.1)
	 * 
	 * http://www.iana.org/assignments/dns-parameters
	 */

	private boolean authoritativeAnswer; // Authoritative Answer [RFC1035]
	private boolean truncatedResponse; // Truncated Response [RFC1035]
	private boolean recursionDesired; // Recursion Desired [RFC1035]
	private boolean recursionAvailable; // Recursion Allowed [RFC1035]
	private boolean reserved; // Reserved Z bit
	private boolean authenticData; // Authentic Data [RFC4035]
	private boolean checkingDisabled; // Checking Disabled [RFC4035]

	private ResponseCode responseCode;
	private short questionCount; // number of question entries
	private short answerCount; // number of answer entries
	private short authorityCount; // number of authority entries
	private short additionalCount; // number of resource entries

	public DnsHeader() {
		id = 0;
		query = false;

		operationalCode = Opcode.QUERY;

		authoritativeAnswer = false;
		truncatedResponse = false;
		recursionDesired = false;
		recursionAvailable = false;
		reserved = false;
		authenticData = false;
		checkingDisabled = false;

		responseCode = ResponseCode.NOERROR;

		questionCount = 0;
		answerCount = 0;
		authorityCount = 0;
		additionalCount = 0;
	}

	public DnsHeader(DnsHeader header) {
		id = header.id;
		query = header.query;

		operationalCode = header.operationalCode;

		authoritativeAnswer = header.authoritativeAnswer;
		truncatedResponse = header.truncatedResponse;
		recursionDesired = header.recursionDesired;
		recursionAvailable = header.recursionAvailable;
		reserved = header.reserved;
		authenticData = header.authenticData;
		checkingDisabled = header.checkingDisabled;

		responseCode = header.responseCode;

		questionCount = header.questionCount;
		answerCount = header.answerCount;
		authorityCount = header.authorityCount;
		additionalCount = header.additionalCount;
	}

	/**
	 * Query identification number
	 */
	public short getId() {
		return id;
	}

	/**
	 * Query identification number
	 */
	public void setId(short id) {
		this.id = id;
	}

	/**
	 * Return true if query is an question
	 */
	public boolean isQuestion() {
		return (query == false);
	}

	/**
	 * Return true if query is an answer
	 */
	public boolean isAnswer() {
		return query;
	}

	public void setQuestion(boolean qr) {
		this.query = qr;
	}

	public Opcode getOpcode() {
		return operationalCode;
	}

	public void setOpcode(Opcode opcode) {
		this.operationalCode = opcode;
	}

	public boolean isAuthoritativeAnswer() {
		return authoritativeAnswer;
	}

	public void setAuthoritativeAnswer(boolean aa) {
		this.authoritativeAnswer = aa;
	}

	public boolean isTruncatedResponse() {
		return truncatedResponse;
	}

	public void setTruncatedResponse(boolean tc) {
		this.truncatedResponse = tc;
	}

	public boolean isRecursionDesired() {
		return recursionDesired;
	}

	public void setRecursionDesired(boolean rd) {
		this.recursionDesired = rd;
	}

	public boolean isRecursionAvailable() {
		return recursionAvailable;
	}

	public void setRecursionAvailable(boolean ra) {
		this.recursionAvailable = ra;
	}

	public boolean isReserved() {
		return reserved;
	}

	public void setReserved(boolean z) {
		this.reserved = z;
	}

	public boolean isAuthenticData() {
		return authenticData;
	}

	public void setAuthenticData(boolean ad) {
		this.authenticData = ad;
	}

	public boolean isCheckingDisabled() {
		return checkingDisabled;
	}

	public void setCheckingDisabled(boolean cd) {
		this.checkingDisabled = cd;
	}

	public ResponseCode getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(ResponseCode rcode) {
		this.responseCode = rcode;
	}

	/**
	 * Number of question entries
	 */
	public short getQuestionCount() {
		return questionCount;
	}

	public void setQuestionCount(short qdcount) {
		this.questionCount = qdcount;
	}

	/**
	 * Number of answer entries
	 */
	public short getAnswerCount() {
		return answerCount;
	}

	public void setAnswerCount(short ancount) {
		this.answerCount = ancount;
	}

	/**
	 * Number of authority entries
	 */
	public short getAuthorityCount() {
		return authorityCount;
	}

	public void setAuthorityCount(short nscount) {
		this.authorityCount = nscount;
	}

	/**
	 * Number of resource entries
	 */
	public short getAdditionalCount() {
		return additionalCount;
	}

	public void setAdditionalCount(short arcount) {
		this.additionalCount = arcount;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		
		result.append(" id: ").append(ByteUtil.toUnsigned(id)).append(System.getProperty("line.separator"));
		
		result.append(" QR: ").append(query).append(" | ");
		result.append(" opcode: ").append(operationalCode).append(" | ");
		result.append(" aa: ").append(authoritativeAnswer).append(" | ");
		result.append(" tc: ").append(truncatedResponse).append(" | ");
		result.append(" rd: ").append(recursionDesired).append(" | ");
		result.append(" ra: ").append(recursionAvailable).append(" | ");
		result.append(" z: ").append(reserved).append(" | ");
		result.append(" ad: ").append(authenticData).append(" | ");
		result.append(" cd: ").append(checkingDisabled).append(" | ");
		result.append(" rcode: ").append(responseCode).append(System.getProperty("line.separator"));
		
		result.append(" QUERY: ").append(ByteUtil.toUnsigned(questionCount));
		result.append(" | ");
		result.append(" ANSWER: ").append(ByteUtil.toUnsigned(answerCount));
		result.append(" | ");
		result.append(" AUTHORITY: ").append(ByteUtil.toUnsigned(authorityCount));
		result.append(" | ");
		result.append(" ADDITIONAL: ").append(ByteUtil.toUnsigned(additionalCount));
		
		return result.toString();
	}
}
