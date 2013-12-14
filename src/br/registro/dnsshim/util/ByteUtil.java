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

public class ByteUtil {

	public static byte[] toByteArray(long value) {
		byte[] b = new byte[8];
		for (int i = 0; i < 8; i++) {
			int offset = (8 - 1 - i) * 8;
			b[i] = (byte) (value >>> offset & 0xFF);
		}
		return b;
	}

	public static byte[] toByteArray(int value) {
		byte[] b = new byte[4];
		for (int i = 0; i < 4; i++) {
			int offset = (4 - 1 - i) * 8;
			b[i] = (byte) (value >>> offset & 0xFF);
		}
		return b;
	}

	public static byte[] toByteArray(short value) {
		byte[] b = new byte[2];
		for (int i = 0; i < 2; i++) {
			int offset = (2 - 1 - i) * 8;
			b[i] = (byte) (value >>> offset & 0xFF);
		}
		return b;
	}

	public static void copyBytes(byte[] dst, int dstPos, int value) {
		byte[] valueBytes = toByteArray(value);

		System.arraycopy(valueBytes, 0, dst, dstPos, valueBytes.length);
	}

	public static void copyBytes(byte[] dst, int dstPos, short value) {
		byte[] valueBytes = toByteArray(value);
		System.arraycopy(valueBytes, 0, dst, dstPos, valueBytes.length);
	}

	public static void copyBytes(byte[] dst, int dstPos, byte value) {
		dst[dstPos] = value;
	}

	public static void copyBytes(byte[] dst, int dstPos, String value) {
		byte[] valueBytes = value.getBytes();
		System.arraycopy(valueBytes, 0, dst, dstPos, valueBytes.length);
	}

	public static long toLong(byte[] b) {
		long value = 0;
		for (int x = 0; x < b.length; x++) {
			value |= b[x] & 0xFF;
			// if it's the last one, don't shift
			if (x != b.length - 1) {
				value <<= 8;
			}
		}
		return value;
	}

	public static int toInt(byte[] b) {
		int value = 0;
		for (int x = 0; x < b.length; x++) {
			value |= b[x] & 0xFF;
			// if it's the last one, don't shift
			if (x != b.length - 1) {
				value <<= 8;
			}
		}
		return value;
	}

	public static int toInt(byte b) {
		byte[] x = { b };
		return toInt(x);
	}

	public static short toShort(byte[] b) {
		short value = 0;
		for (int x = 0; x < b.length; x++) {
			value |= b[x] & 0xFF;
			// if it's the last one, don't shift
			if (x != b.length - 1) {
				value <<= 8;
			}
		}
		return value;
	}

	public static short toUnsigned(byte b) {
		byte[] x = { b };
		return toShort(x);
	}

	public static int toUnsigned(short s) {
		return toInt(toByteArray(s));
	}

	public static long toUnsigned(int i) {
		return toLong(toByteArray(i));
	}

	/**
	 * Returns the value of the bit with the specified index.
	 *
	 * <pre>
	 *  1  2  3  4  5  6  7  8
	 * +--+--+--+--+--+--+--+--+
	 * |  |  |  |  |  |  |  |  |
	 * +--+--+--+--+--+--+--+--+
	 * </pre>
	 *
	 * @return true if bit with the specified index is 1.<br>
	 *         false if bit with the specified index is 0.
	 */
	public static boolean getBit(byte in, int position) {
		int check = (in & 0xFF) >>> 8 - position;
		return (check & 1) == 1;
	}

	public static boolean getBit(short in, int position) {
		int check = (in & 0xFFFF) >>> 16 - position;
		return (check & 1) == 1;
	}

	public static byte getBits(byte in, int position, int size) {
		byte check = (byte) ((in & 0xFF) << position - 1);
		check = (byte) (check >>> 1 & 0x7F);
		check = (byte) (check >>> 8 - size - 1);
		return check;
	}

	public static short getBits(short in, int position, int size) {
		short check = (short) ((in & 0xFFFF) << position - 1);
		check = (byte) (check >>> 1 & 0x7FFF);
		check = (short) (check >>> 16 - size - 1);
		return check;
	}

	public static byte setBit(byte in, int position, boolean value) {
		if (!value) {
			return (byte) (in & (0xFF ^ 1 << 8 - position));
		} else {
			return setBit(in, position);
		}
	}

	public static short setBit(short in, int position, boolean value) {
		if (!value) {
			return (short) (in & (0xFFFF ^ 1 << 16 - position));
		} else {
			return setBit(in, position);
		}
	}

	public static byte setBit(byte in, int position) {
		in |= 1 << 8 - position;
		return in;
	}

	public static short setBit(short in, int position) {
		in |= 1 << 16 - position;
		return in;
	}

	public static byte setBits(byte in, byte data, int position, int fillBits) {
		if (position - fillBits == 0) {
			fillBits++;
		}
		in |= data << 9 - position - fillBits;
		return in;
	}

	public static short setBits(short in, short data, int position, int fillBits) {
		if (position - fillBits == 0) {
			fillBits++;
		}
		in |= data << 17 - position - fillBits;
		return in;
	}
}