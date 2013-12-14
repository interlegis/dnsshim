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

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Scanner;

import br.registro.dnsshim.util.ByteUtil;

public class Loc extends ResourceRecord {
	private final byte version;
	private final byte size;
	private final byte horizPre;
	private final byte vertPre;
	private final int latitude;
	private final int longitude;
	private final int altitude;

	private final int poweroften[] = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000};

	public Loc(String ownername, DnsClass dnsClass, int ttl, byte version,
			byte size, byte horizPre, byte vertPre, int latitude,
			int longitude, int altitude) {
		super(ownername, RrType.LOC, dnsClass, ttl);
		this.version = version;
		this.size = size;
		this.horizPre = horizPre;
		this.vertPre = vertPre;
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.rdata = RdataLocBuilder.get(version, size, horizPre, vertPre, latitude, longitude, altitude);
	}

	public Loc(String ownername, DnsClass dnsClass, int ttl, String rdata) {
		super(ownername, RrType.LOC, dnsClass, ttl);
		Scanner scanner = new Scanner(rdata);
		scanner.useDelimiter("\\s+");

		// For now the version isn't stored in the presentation format and will be zero
		this.version = 0;
		
		// Latitude
		String strLatitudeHour = scanner.next();
		String strLatitudeMinute = scanner.next();
		String strLatitudeSecondMilisecond = scanner.next();
		String strLatitudeSecond = strLatitudeSecondMilisecond.split("\\.")[0];
		String strLatitudeMilisecond = strLatitudeSecondMilisecond.split("\\.")[1];
		
		scanner.next(); // North and South ignored
		
		int latitudeHour = Integer.parseInt(strLatitudeHour);
		int latitudeMinute = Integer.parseInt(strLatitudeMinute) + (latitudeHour * 60);
		int latitudeSecond = Integer.parseInt(strLatitudeSecond) + (latitudeMinute * 60);
		int latitudeMilisecond = Integer.parseInt(strLatitudeMilisecond) + (latitudeSecond * 1000);
		this.latitude = (int) (latitudeMilisecond + ByteUtil.toUnsigned(1 << 31));

		// Longitude
		String strLongitudeHour = scanner.next();
		String strLongitudeMinute = scanner.next();
		String strLongitudeSecondMilisecond = scanner.next();
		String strLongitudeSecond = strLongitudeSecondMilisecond.split("\\.")[0];
		String strLongitudeMilisecond = strLongitudeSecondMilisecond.split("\\.")[1];
		
		scanner.next(); // East and West ignored
		
		int longitudeHour = Integer.parseInt(strLongitudeHour);
		int longitudeMinute = Integer.parseInt(strLongitudeMinute) + (longitudeHour * 60);
		int longitudeSecond = Integer.parseInt(strLongitudeSecond) + (longitudeMinute * 60);
		int longitudeMilisecond = Integer.parseInt(strLongitudeMilisecond) + (longitudeSecond * 1000);
		this.longitude = (int) (longitudeMilisecond + ByteUtil.toUnsigned(1 << 31));
		
		// Altitude
		String strAltitude = scanner.next().replace("m", "");
		this.altitude = (int) ((Double.parseDouble(strAltitude) * 100) + (100000 * 100));
		
		// The next fields (size, horizPre and vertPre) are going to be read with 
		// the same strategy used in the BIND 9.6.0b1 (loc_29.c)
		
		long [] powerOften = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000};
		
		// Size
		String strSizeTmp = scanner.next().replace("m", "");
		long sizeTmp = Integer.parseInt(strSizeTmp.split("\\.")[0]);
		long sizeTmpDecimal = Integer.parseInt(strSizeTmp.split("\\.")[1]);
		
		int sizeFinal = 0;
		int exp = 0;
		if (sizeTmp > 0) {
			for (exp = 0; exp < 7; exp++){
				if (sizeTmp < powerOften[exp + 1]) {
					break;					
				}
			}
			
			sizeFinal = (int) (sizeTmp / powerOften[exp]);
			exp += 2;
		} else {
			if (sizeTmpDecimal >= 10) {
				sizeFinal = (int) (sizeTmpDecimal / 10);
				exp = 1;
			} else {
				sizeFinal = (int) sizeTmpDecimal;
				exp = 0;
			}
		}
				
		this.size = (byte) ((sizeFinal << 4) + exp);
		
		// Horizontal Precision
		String strHorizPreTmp = scanner.next().replace("m", "");
		long horizPreTmp = Integer.parseInt(strHorizPreTmp.split("\\.")[0]);
		long horizPreTmpDecimal = Integer.parseInt(strHorizPreTmp.split("\\.")[1]);
		
		int horizPreFinal = 0;
		exp = 0;
		if (horizPreTmp > 0) {
			for (exp = 0; exp < 7; exp++){
				if (horizPreTmp < powerOften[exp + 1]) {
					break;			
				}
			}
			
			horizPreFinal = (int) (horizPreTmp / powerOften[exp]);
			exp += 2;
		} else {
			if (horizPreTmpDecimal >= 10) {
				horizPreFinal = (int) (horizPreTmpDecimal / 10);
				exp = 1;
			} else {
				horizPreFinal = (int) horizPreTmpDecimal;
				exp = 0;
			}
		}
				
		this.horizPre = (byte) ((horizPreFinal << 4) + exp);
		
		// Vertical precision
		String strVertPreTmp = scanner.next().replace("m", "");
		long vertPreTmp = Integer.parseInt(strVertPreTmp.split("\\.")[0]);
		long vertPreTmpDecimal = Integer.parseInt(strVertPreTmp.split("\\.")[1]);
		
		int vertPreFinal = 0;
		exp = 0;
		if (vertPreTmp > 0) {
			for (exp = 0; exp < 7; exp++){
				if (vertPreTmp < powerOften[exp + 1]) {
					break;			
				}
			}
			
			vertPreFinal = (int) (vertPreTmp / powerOften[exp]);
			exp += 2;
		} else {
			if (vertPreTmpDecimal >= 10) {
				vertPreFinal = (int) (vertPreTmpDecimal / 10);
				exp = 1;
			} else {
				vertPreFinal = (int) vertPreTmpDecimal;
				exp = 0;
			}
		}
				
		this.vertPre = (byte) ((vertPreFinal << 4) + exp);
		
		this.rdata = RdataLocBuilder.get(version, size, horizPre, vertPre, latitude, longitude, altitude);
		scanner.close();
	}

	public byte getVersion() {
		return version;
	}

	public byte getSize() {
		return size;
	}

	public byte getHorizPre() {
		return horizPre;
	}

	public byte getVertPre() {
		return vertPre;
	}

	public int getLatitude() {
		return latitude;
	}

	public int getLongitude() {
		return longitude;
	}

	public int getAltitude() {
		return altitude;
	}

	@Override
	public String rdataPresentation() {
		StringBuilder rdata = new StringBuilder();
		
//		The diameter of a sphere enclosing the described entity, in
//      centimeters, expressed as a pair of four-bit unsigned
//      integers, each ranging from zero to nine, with the most
//      significant four bits representing the base and the second
//      number representing the power of ten by which to multiply
//      the base.  This allows sizes from 0e0 (<1cm) to 9e9
//      (90,000km) to be expressed.  This representation was chosen
//      such that the hexadecimal representation can be read by
//      eye; 0x15 = 1e5.  Four-bit values greater than 9 are
//      undefined, as are values with a base of zero and a non-zero
//      exponent.
		
		byte sizeBase = ByteUtil.getBits(getSize(), 1, 4);	// 11110000
		byte sizeExp =  ByteUtil.getBits(getSize(), 5, 4);	// 00001111
		double size = sizeBase * Math.pow(10, sizeExp) / 100;
		
//		The horizontal precision of the data, in centimeters,
//      expressed using the same representation as SIZE.  This is
//      the diameter of the horizontal "circle of error", rather
//      than a "plus or minus" value.  (This was chosen to match
//      the interpretation of SIZE; to get a "plus or minus" value,
//      divide by 2.)

		byte horizPreBase =  ByteUtil.getBits(getHorizPre(), 1, 4);	// 11110000
		byte horizPreExp = ByteUtil.getBits(getHorizPre(), 5, 4);	// 00001111
		double horizPre = horizPreBase * Math.pow(10, horizPreExp) / 100;
		
//		The vertical precision of the data, in centimeters,
//      expressed using the sane representation as for SIZE.  This
//      is the total potential vertical error, rather than a "plus
//      or minus" value.  (This was chosen to match the
//      interpretation of SIZE; to get a "plus or minus" value,
//      divide by 2.)  Note that if altitude above or below sea
//      level is used as an approximation for altitude relative to
//      the [WGS 84] ellipsoid, the precision value should be
//      adjusted.

		byte vertPreBase = ByteUtil.getBits(getVertPre(), 1, 4);	// 11110000
		byte vertPreExp = ByteUtil.getBits(getVertPre(), 5, 4);		// 00001111
		double vertPre = vertPreBase * Math.pow(10, vertPreExp) / 100;
		
//		The latitude of the center of the sphere described by the
//      SIZE field, expressed as a 32-bit integer, most significant
//      octet first (network standard byte order), in thousandths
//      of a second of arc.  2^31 represents the equator; numbers
//      above that are north latitude.

		int latitudeMiliseconds, latitudeSeconds, latitudeMinutes, latitudeHours;
		long latitudeTmp = getLatitude() - ByteUtil.toUnsigned(1 << 31);
		
		boolean isNorth = true;
		if (latitudeTmp > 0) {
			isNorth = false;
		}
		
		latitudeMiliseconds = ((int) latitudeTmp) % 1000;
		latitudeSeconds = ((int) latitudeTmp) / 1000;
		latitudeMinutes = latitudeSeconds / 60;
		latitudeHours = latitudeMinutes / 60;
		latitudeMinutes = latitudeMinutes % 60;
		latitudeSeconds = latitudeSeconds % 60;

//		The longitude of the center of the sphere described by the
//      SIZE field, expressed as a 32-bit integer, most significant
//      octet first (network standard byte order), in thousandths
//      of a second of arc, rounded away from the prime meridian.
//      2^31 represents the prime meridian; numbers above that are
//      east longitude.

		int longitudeMiliseconds, longitudeSeconds, longitudeMinutes, longitudeHours;
		long longitudeTmp = getLongitude() - ByteUtil.toUnsigned(1 << 31);
		
		boolean isEast = true;
		if (longitudeTmp < 0) {
			isEast = false;
			longitudeTmp = -longitudeTmp;
		}
		
		longitudeMiliseconds = ((int) longitudeTmp) % 1000;
		longitudeSeconds = ((int) longitudeTmp) / 1000;
		longitudeMinutes = longitudeSeconds / 60;
		longitudeHours = longitudeMinutes / 60;
		longitudeMinutes = longitudeMinutes % 60;
		longitudeSeconds = longitudeSeconds % 60;
		
//		The altitude of the center of the sphere described by the
//      SIZE field, expressed as a 32-bit integer, most significant
//      octet first (network standard byte order), in centimeters,
//      from a base of 100,000m below the [WGS 84] reference
//      spheroid used by GPS (semimajor axis a=6378137.0,
//      reciprocal flattening rf=298.257223563).
		
		int altitudeCm = getAltitude() - (100000 * 100);
		int altitudeMeters = altitudeCm / 100;
		
//		The LOC record is expressed in a master file in the following format:
//
//		   <owner> <TTL> <class> LOC ( d1 [m1 [s1]] {"N"|"S"} d2 [m2 [s2]]
//		                               {"E"|"W"} alt["m"] [siz["m"] [hp["m"]
//		                               [vp["m"]]]] )
//
//		   (The parentheses are used for multi-line data as specified in [RFC
//		   1035] section 5.1.)
//
//		   where:
//
//		       d1:     [0 .. 90]            (degrees latitude)
//		       d2:     [0 .. 180]           (degrees longitude)
//		       m1, m2: [0 .. 59]            (minutes latitude/longitude)
//		       s1, s2: [0 .. 59.999]        (seconds latitude/longitude)
//		       alt:    [-100000.00 .. 42849672.95] BY .01 (altitude in meters)
//		       siz, hp, vp: [0 .. 90000000.00] (size/precision in meters)

		NumberFormat threeDigits = new DecimalFormat("000");		
		NumberFormat twoDigits = new DecimalFormat("0.00");
		
		rdata.append(latitudeHours).append(SEPARATOR);
		rdata.append(latitudeMinutes).append(SEPARATOR);
		rdata.append(latitudeSeconds + "." + threeDigits.format(latitudeMiliseconds)).append(SEPARATOR);
		
		if (isNorth == true) {
			rdata.append("N").append(SEPARATOR);			
		} else {
			rdata.append("S").append(SEPARATOR);
		}
		
		rdata.append(longitudeHours).append(SEPARATOR);
		rdata.append(longitudeMinutes).append(SEPARATOR);
		rdata.append(longitudeSeconds + "." + threeDigits.format(longitudeMiliseconds)).append(SEPARATOR);
		
		if (isEast == true) {
			rdata.append("E").append(SEPARATOR);			
		} else {
			rdata.append("W").append(SEPARATOR);
		}
		
		rdata.append(twoDigits.format(altitudeMeters) + "m").append(SEPARATOR);
		rdata.append(twoDigits.format(size) + "m").append(SEPARATOR);
		rdata.append(twoDigits.format(horizPre) + "m").append(SEPARATOR);
		rdata.append(twoDigits.format(vertPre) + "m").append(SEPARATOR);

		return rdata.toString();
	}

	public int pricsize(byte prec) {
		long val;
		int mantissa;
		int exponent;

		mantissa = (prec >> 4 & 0x0f) % 10;
		exponent = (prec >> 0 & 0x0f) % 10;

		val = mantissa * poweroften[exponent];

		return Integer.parseInt(val/100 + "" + val%100);
	}

	public static Loc parseLoc(String ownername, DnsClass dnsClass, int ttl,
			ByteBuffer buffer) {
		buffer.getShort(); // rdlength
		byte version = buffer.get();
		byte size = buffer.get();
		byte horizPre = buffer.get();
		byte vertPre = buffer.get();
		int latitude = buffer.getInt();
		int longitude = buffer.getInt();
		int altitude = buffer.getInt();
		return new Loc(ownername, dnsClass, ttl, version, size, horizPre,
				vertPre, latitude, longitude, altitude);
	}

}
