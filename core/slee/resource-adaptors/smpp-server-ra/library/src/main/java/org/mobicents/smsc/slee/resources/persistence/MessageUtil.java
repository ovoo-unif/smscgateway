/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.smsc.slee.resources.persistence;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.cassandra.SmsSet;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppConstants;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class MessageUtil {

	/**
	 * Parsing SMPP date String into a Date
	 * 
	 * @param val
	 * @return Date value (or null if can val==null) 
	 */
	public static Date parseSmppDate(String val) throws ParseException {
		if (val == null || val.length() == 0)
			return null;
		if (val.length() != 16) {
			// TODO: may be we need here try to parse time in
			// "minutes count after current time" ???
			throw new ParseException("Absolute or relative time formats must be 16 characters length", 0);
		}
		char sign = val.charAt(15);
		Date res;
		if (sign == 'R') {
			String yrS = val.substring(0, 2);
			String mnS = val.substring(2, 4);
			String dyS = val.substring(4, 6);
			String hrS = val.substring(6, 8);
			String miS = val.substring(8, 10);
			String scS = val.substring(10, 12);
			int yr = Integer.parseInt(yrS);
			int mn = Integer.parseInt(mnS);
			int dy = Integer.parseInt(dyS);
			int hr = Integer.parseInt(hrS);
			int mi = Integer.parseInt(miS);
			int sc = Integer.parseInt(scS);
			Calendar c = Calendar.getInstance();
			c.setTime(new Date());
			c.add(Calendar.YEAR, yr);
			c.add(Calendar.MONTH, mn);
			c.add(Calendar.DATE, dy);
			c.add(Calendar.HOUR, hr);
			c.add(Calendar.MINUTE, mi);
			c.add(Calendar.SECOND, sc);
			res = c.getTime();
		} else {
//			String s1 = val.substring(0, 12);
			String s2 = val.substring(12, 13);
			String s3 = val.substring(13, 15);

			String yrS = val.substring(0, 2);
			String mnS = val.substring(2, 4);
			String dyS = val.substring(4, 6);
			String hrS = val.substring(6, 8);
			String miS = val.substring(8, 10);
			String scS = val.substring(10, 12);
			int yr = Integer.parseInt(yrS);
			int mn = Integer.parseInt(mnS);
			int dy = Integer.parseInt(dyS);
			int hr = Integer.parseInt(hrS);
			int mi = Integer.parseInt(miS);
			int sc = Integer.parseInt(scS);
			Date date = new Date(yr + 100, mn - 1, dy, hr, mi, sc);

//			SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddhhmmss"); // yyMMddhhmmsstnnp
//			Date date = dateFormat.parse(s1);

			int dSec = Integer.parseInt(s2);
			int tZone = Integer.parseInt(s3);
			int curTimezoneOffset = date.getTimezoneOffset();
			switch (sign) {
			case '+':
				res = new Date(date.getTime() + dSec * 100 - tZone * 15 * 60 * 1000 - curTimezoneOffset * 60 * 1000);
				break;
			case '-':
				res = new Date(date.getTime() + dSec * 100 + tZone * 15 * 60 * 1000 - curTimezoneOffset * 60 * 1000);
				break;
			case 'R':
				res = new Date((new Date()).getTime() + date.getTime() + dSec * 100);
				break;
			default:
				throw new ParseException("16-th character must be '+' or '-' for absolute time format or 'R' for relative time format", 16);
			}
		}			

		return res;
	}

	/**
	 * Creating a String representation of Absolute Time Format
	 * 
	 * @param date
	 * @param timezoneOffset
	 *            current timezone offset in minutes.
	 *            you can get it by "-(new Date()).getTimezoneOffset()"
	 * @return
	 */
	public static String printSmppAbsoluteDate(Date date, int timezoneOffset) {
		StringBuilder sb = new StringBuilder();

		int year = date.getYear() - 100;
		int month = date.getMonth() + 1;
		int day = date.getDate();
		int hour = date.getHours();
		int min = date.getMinutes();
		int sec = date.getSeconds();
		Date d1 = new Date(year + 100, month - 1, day, hour, min, sec);
		int tSec = (int) (date.getTime() - d1.getTime()) / 100;

		addDateToStringBuilder(sb, year, month, day, hour, min, sec);

		if (tSec > 9)
			tSec = 9;
		if (tSec < 0)
			tSec = 0;
		sb.append(tSec);

		int tz = timezoneOffset / 15;
		char sign;
		if (tz < 0) {
			sign = '-';
			tz = -tz;
		} else {
			sign = '+';
		}
		if (tz < 10)
			sb.append("0");
		sb.append(tz);
		sb.append(sign);
		
		return sb.toString();
	}

	/**
	 * Creating a String representation of Relative Time Format
	 * 
	 * @param years
	 * @param months
	 * @param days
	 * @param hours
	 * @param minutes
	 * @param seconds
	 * @return
	 */
	public static String printSmppRelativeDate(int years, int months, int days, int hours, int minutes, int seconds) {
		StringBuilder sb = new StringBuilder();

		addDateToStringBuilder(sb, years, months, days, hours, minutes, seconds);
		sb.append("000R");

		return sb.toString();
	}

	public static void applyValidityPeriod(Sms sms, Date validityPeriod, boolean fromEsme) throws SmscProcessingException {
		SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
		int maxValidityPeriodHours = smscPropertiesManagement.getMaxValidityPeriodHours();
		int defaultValidityPeriodHours = smscPropertiesManagement.getDefaultValidityPeriodHours();

		Date now = new Date();
		if (validityPeriod == null) {
			validityPeriod = addHours(now, defaultValidityPeriodHours);
		} else {
			Date maxValidityPeriod = addHours(now, maxValidityPeriodHours);
			if (validityPeriod.after(maxValidityPeriod)) {
				if (fromEsme)
					throw new SmscProcessingException("Validity period is after than maxValidityPeriod", SmppConstants.STATUS_INVEXPIRY,
							MAPErrorCode.systemFailure, null);
				else
					validityPeriod = maxValidityPeriod;
			}
			if (validityPeriod.before(now)) {
				if (fromEsme)
					throw new SmscProcessingException("Validity period is before than now", SmppConstants.STATUS_INVEXPIRY, MAPErrorCode.systemFailure, null);
				else
					validityPeriod = maxValidityPeriod;
			}
		}
		sms.setValidityPeriod(validityPeriod);
	}

	public static void applyScheduleDeliveryTime(Sms sms, Date scheduleDeliveryTime) throws SmscProcessingException {
		if (scheduleDeliveryTime == null)
			return;

		Date maxSchDelTime = addHours(sms.getValidityPeriod(), -3);
		if (scheduleDeliveryTime.after(maxSchDelTime)) {
			throw new SmscProcessingException("Schedule delivery time is before 3 hours before than validity period expiration", SmppConstants.STATUS_INVSCHED,
					MAPErrorCode.systemFailure, null);
		}

		sms.setScheduleDeliveryTime(scheduleDeliveryTime);
	}

	public static Date addHours(Date time, int hours) {
		long tm = time.getTime();
		tm += hours * 3600 * 1000;
		return new Date(tm);
	}

	/**
	 * Calculate delivery delay for first delay (before first delivery)
	 * @return
	 */
	public static int computeFirstDueDelay() {
		SmscPropertiesManagement spm = SmscPropertiesManagement.getInstance();
		return spm.getFirstDueDelay();
	}

	/**
	 * Calculate next delivery delay for delivery failure
	 * @return
	 */
	public static int computeNextDueDelay(int prevDueDelay) {
		SmscPropertiesManagement spm = SmscPropertiesManagement.getInstance();
		if (prevDueDelay == 0) {
			// this is second delay
			return spm.getSecondDueDelay();
		} else {
			int res = prevDueDelay * spm.getDueDelayMultiplicator() / 100;
			if (res > spm.getMaxDueDelay())
				res = spm.getMaxDueDelay();
			return res;
		}
	}

	/**
	 * Calculate delivery delay for cause SubscriberBusy
	 * @return
	 */
	public static int computeDueDelaySubscriberBusy() {
		SmscPropertiesManagement spm = SmscPropertiesManagement.getInstance();
		return spm.getSubscriberBusyDueDelay();
	}

	public static Date computeDueDate(int dueDelay) {
		Date d = new Date(System.currentTimeMillis() + dueDelay * 1000);
		return d;
	}

	public static int getMaxSolidMessageBytesLength(DataCodingScheme dataCodingScheme) {
		if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM7) {
			return 160;
		} else {
			return 140;
		}
	}

	public static int getMaxSegmentedMessageBytesLength(DataCodingScheme dataCodingScheme) {
		if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM7) {
			return 152;
		} else {
			return 132;
		}
	}

	public static NumberingPlan getSccpNumberingPlan(int npi) {
		NumberingPlan np = NumberingPlan.ISDN_TELEPHONY;
		switch (npi) {
		case SmppConstants.NPI_E164:
			np = NumberingPlan.ISDN_TELEPHONY;
			break;
		}
		return np;
	}

	public static NatureOfAddress getSccpNatureOfAddress(int ton) {
		NatureOfAddress na = NatureOfAddress.INTERNATIONAL;
		switch (ton) {
		case SmppConstants.TON_INTERNATIONAL:
			na = NatureOfAddress.INTERNATIONAL;
			break;
		}
		return na;
	}

	private static void addDateToStringBuilder(StringBuilder sb, int year, int month, int day, int hour, int min, int sec) {
		if (year < 10)
			sb.append("0");
		sb.append(year);
		if (month < 10)
			sb.append("0");
		sb.append(month);
		if (day < 10)
			sb.append("0");
		sb.append(day);
		if (hour < 10)
			sb.append("0");
		sb.append(hour);
		if (min < 10)
			sb.append("0");
		sb.append(min);
		if (sec < 10)
			sb.append("0");
		sb.append(sec);
	}

	public static Date checkScheduleDeliveryTime(final SmsSet smsSet, Date newDueDate) {
		Date minDate = null;
		int smsCount = smsSet.getSmsCount();
		for (int i1 = 0; i1 < smsCount; i1++) {
			Sms sms = smsSet.getSms(i1);
			if (sms.getScheduleDeliveryTime() != null) {
				if (minDate == null)
					minDate = sms.getScheduleDeliveryTime();
				else {
					if (minDate.after(sms.getScheduleDeliveryTime()))
						minDate = sms.getScheduleDeliveryTime();
				}
			} else {
				if (minDate == null)
					minDate = newDueDate;
			}
		}
		if (minDate != null && newDueDate.before(minDate)) {
			newDueDate = minDate;
		}
		return newDueDate;
	}

    /**
     * Checking if SMSC can process this DataCodingScheme *
     * 
     * @param dcs
     * @return null if SMSC can process or String with error description if not
     */
    public static String chechDataCodingSchemeSupport(int dcs) {
        DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(dcs);
        if (dataCodingScheme.getCharacterSet() != CharacterSet.GSM7 && dataCodingScheme.getCharacterSet() != CharacterSet.UCS2)
            return "Only GSM7 and USC2 are supported";
        if (dataCodingScheme.getIsCompressed())
            return "Compressed message are not supported";

        return null;
    }

    private static final String DELIVERY_ACK_ID = "id:";
    private static final String DELIVERY_ACK_SUB = " sub:";
    private static final String DELIVERY_ACK_DLVRD = " dlvrd:";
    private static final String DELIVERY_ACK_SUBMIT_DATE = " submit date:";
    private static final String DELIVERY_ACK_DONE_DATE = " done date:";
    private static final String DELIVERY_ACK_STAT = " stat:";
    private static final String DELIVERY_ACK_ERR = " err:";
    private static final String DELIVERY_ACK_TEXT = " text:";
    private static final String DELIVERY_ACK_STATE_DELIVERED = "DELIVRD";
    private static final String DELIVERY_ACK_STATE_UNDELIVERABLE = "UNDELIV";
    public static final byte ESME_DELIVERY_ACK = 0x04;
    private static final SimpleDateFormat DELIVERY_ACK_DATE_FORMAT = new SimpleDateFormat("yyMMddHHmm");

    public static boolean isReceiptOnSuccess(int registeredDelivery) {
        int code = registeredDelivery & 0x03;
        if (code == 1 || code == 3)
            return true;
        else
            return false;
    }

    public static boolean isReceiptOnFailure(int registeredDelivery) {
        int code = registeredDelivery & 0x03;
        if (code == 1 || code == 2)
            return true;
        else
            return false;
    }

    public static Sms createReceiptSms(Sms sms, boolean devivered) {
        Sms receipt = new Sms();
        receipt.setDbId(UUID.randomUUID());
        receipt.setSourceAddr(sms.getSmsSet().getDestAddr());
        receipt.setSourceAddrNpi(sms.getSmsSet().getDestAddrNpi());
        receipt.setSourceAddrTon(sms.getSmsSet().getDestAddrTon());

        receipt.setSubmitDate(sms.getSubmitDate());

        receipt.setMessageId(sms.getMessageId());
        Date validityPeriod = MessageUtil.addHours(new Date(), 24);
        receipt.setValidityPeriod(validityPeriod);

        StringBuffer sb = new StringBuffer();
        DataCodingScheme dcs = new DataCodingSchemeImpl(sms.getDataCoding());
        if (devivered) {
            sb.append(DELIVERY_ACK_ID).append(sms.getMessageIdText()).append(DELIVERY_ACK_SUB).append("001").append(DELIVERY_ACK_DLVRD).append("001")
                    .append(DELIVERY_ACK_SUBMIT_DATE).append(DELIVERY_ACK_DATE_FORMAT.format(sms.getSubmitDate())).append(DELIVERY_ACK_DONE_DATE)
                    .append(DELIVERY_ACK_DATE_FORMAT.format(new Timestamp(System.currentTimeMillis()))).append(DELIVERY_ACK_STAT)
                    .append(DELIVERY_ACK_STATE_DELIVERED).append(DELIVERY_ACK_ERR).append("000").append(DELIVERY_ACK_TEXT)
                    .append(getFirst20CharOfSMS(sms.getShortMessage(), dcs));
        } else {
            sb.append(DELIVERY_ACK_ID).append(sms.getMessageIdText()).append(DELIVERY_ACK_SUB).append("001").append(DELIVERY_ACK_DLVRD).append("001")
                    .append(DELIVERY_ACK_SUBMIT_DATE).append(DELIVERY_ACK_DATE_FORMAT.format(sms.getSubmitDate())).append(DELIVERY_ACK_DONE_DATE)
                    .append(DELIVERY_ACK_DATE_FORMAT.format(new Timestamp(System.currentTimeMillis()))).append(DELIVERY_ACK_STAT)
                    .append(DELIVERY_ACK_STATE_UNDELIVERABLE).append(DELIVERY_ACK_ERR).append(sms.getSmsSet().getStatus().getCodeText())
                    .append(DELIVERY_ACK_TEXT).append(getFirst20CharOfSMS(sms.getShortMessage(), dcs));
        }

        byte[] textBytes;
        if (dcs.getCharacterSet() == CharacterSet.UCS2) {
            receipt.setDataCoding(8);
            textBytes = CharsetUtil.encode(sb.toString(), CharsetUtil.CHARSET_UCS_2);
        } else {
            receipt.setDataCoding(0);
            textBytes = CharsetUtil.encode(sb.toString(), CharsetUtil.CHARSET_GSM);
        }

        receipt.setShortMessage(textBytes);
        receipt.setEsmClass(ESME_DELIVERY_ACK);

        return receipt;
    }

    private static String getFirst20CharOfSMS(byte[] rawSms, DataCodingScheme dcs) {
        String first20CharOfSms;
        if (dcs.getCharacterSet() == CharacterSet.UCS2) {
            Charset ucs2Charset = Charset.forName("UTF-16BE");
            ByteBuffer bb = ByteBuffer.wrap(rawSms);
            CharBuffer bf = ucs2Charset.decode(bb);
            first20CharOfSms = bf.toString();
        } else {
            first20CharOfSms = new String(rawSms);
        }
        if (first20CharOfSms.length() > 20) {
            first20CharOfSms = first20CharOfSms.substring(0, 20);
        }
        return first20CharOfSms;
    }
}
