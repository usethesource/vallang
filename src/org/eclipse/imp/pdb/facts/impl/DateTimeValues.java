/*******************************************************************************
* Copyright (c) 2009 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Mark Hills (Mark.Hills@cwi.nl) - initial API and implementation
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.imp.pdb.facts.IDateTime;
import org.eclipse.imp.pdb.facts.exceptions.InvalidDateTimeException;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;


/** A concrete instance of IDateTime, representing either a date,
 *  a time, or a date with time.
 *  
 *  NOTE: We currently do not support partial dates and times; i.e.,
 *  it is not possible to represent "July 2009" or "15" (hours).
 *
 */
public class DateTimeValues {

	public static class DateValue extends Value implements IDateTime {

		private int year;
		private int month;
		private int day;
		
		/**
		 * Construct a DateTime object representing a date. 
		 * 
		 * @param year			The year of the date
		 * @param month			The month of the date
		 * @param day			The day of the date
		 */
		public DateValue(int year, int month, int day) {
			super(TypeFactory.getInstance().dateTimeType());

			this.year = year;
			this.month = month;
			this.day = day;

			// Check to make sure the provided value are valid.
			// TODO: Failure here should throw a PDB exception.
			Calendar cal = Calendar.getInstance(TimeZone.getDefault(),Locale.getDefault());
			cal.setLenient(false);
			cal.set(year, month-1, day);
			try {
				cal.get(Calendar.YEAR);
			} catch (IllegalArgumentException iae) {
				throw new InvalidDateTimeException("Cannot create date with provided values."); 
			}
		}

		public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E {
			return v.visitDateTime(this);
		}

		public int compareTo(IDateTime arg0) {
			if (arg0.isDate()) {
				long m1 = this.getInstant();
				long m2 = arg0.getInstant();
				if (m1 == m2)
					return 0;
				else if (m1 < m2)
					return -1;
				else
					return 1;
			} else {
				throw new UnsupportedOperationException("Date and non-Date values are not comparable");
			}				
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getInstant()
		 */
		public long getInstant() {
			Calendar cal = Calendar.getInstance(TimeZone.getDefault(),Locale.getDefault());
			cal.setTime(new Date(0));
			cal.set(this.year, this.month-1, this.day);
			return cal.getTimeInMillis();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getCentury()
		 */
		public int getCentury() {
			return (year - (year % 100)) / 100;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getYear()
		 */
		public int getYear() {
			return this.year;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getMonthOfYear()
		 */
		public int getMonthOfYear() {
			return this.month;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getDayOfMonth()
		 */
		public int getDayOfMonth() {
			return this.day;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getHourOfDay()
		 */
		public int getHourOfDay() {
			throw new UnsupportedOperationException("Cannot get hours on a date value");
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getMinuteOfHour()
		 */
		public int getMinuteOfHour() {
			throw new UnsupportedOperationException("Cannot get minutes on a date value");
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getSecondOfMinute()
		 */
		public int getSecondOfMinute() {
			throw new UnsupportedOperationException("Cannot get seconds on a date value");
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getMillisecondsOfSecond()
		 */
		public int getMillisecondsOfSecond() {
			throw new UnsupportedOperationException("Cannot get milliseconds on a date value");
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getTimezoneOffsetHours()
		 */
		public int getTimezoneOffsetHours() {
			throw new UnsupportedOperationException("Cannot get timezone offset hours on a date value");
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getTimezoneOffsetMinutes()
		 */
		public int getTimezoneOffsetMinutes() {
			throw new UnsupportedOperationException("Cannot get timezone offset minutes on a date value");
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#isDate()
		 */
		public boolean isDate() {
			return true;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#isTime()
		 */
		public boolean isTime() {
			return false;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#isDateTime()
		 */
		public boolean isDateTime() {
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + day;
			result = prime * result + month;
			result = prime * result + year;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DateValue other = (DateValue) obj;
			if (day != other.day)
				return false;
			if (month != other.month)
				return false;
			if (year != other.year)
				return false;
			return true;
		}
	}
	
	public static class TimeValue extends Value implements IDateTime {

		private int hour;
		private int minute;
		private int second;
		private int millisecond;
		private int timezoneHours;
		private int timezoneMinutes;
		
		private final static int millisInAMinute = 1000 * 60;
		private final static int millisInAnHour = TimeValue.millisInAMinute * 60;
		
		/**
		 * Given the hour and minute offset, generate the appropriate Java
		 * timezone string
		 * 
		 * @param hourOffset	The hour offset for the timezone.
		 * @param minuteOffset	The minute offset for the timezone.
		 * 
		 * @return				A string with the proper timezone.
		 */
		private static String getTZString(int hourOffset, int minuteOffset) {
			String tzString = "GMT" + 
				((hourOffset < 0 || (0 == hourOffset && minuteOffset < 0)) ? "-" : "+") + 
				String.format("%02d",hourOffset >= 0 ? hourOffset : hourOffset * -1) +
				String.format("%02d",minuteOffset >= 0 ? minuteOffset : minuteOffset * -1);
			return tzString;
		}
		
		/**
		 * Construct a DateTime object representing a time. 
		 * 
		 * @param hour			The hour of the time
		 * @param minute		The minute of the time
		 * @param second		The second of the time
		 * @param millisecond	The millisecond of the time
		 */
		public TimeValue(int hour, int minute, int second, int millisecond) {
			super(TypeFactory.getInstance().dateTimeType());
			
			this.hour = hour;
			this.minute = minute;
			this.second = second;
			this.millisecond = millisecond;
			
			// Check to make sure the provided values are valid.
			// TODO: Failure here should throw a PDB exception.
			Calendar cal = Calendar.getInstance(TimeZone.getDefault(),Locale.getDefault());
			cal.setLenient(false);
			cal.set(Calendar.HOUR_OF_DAY, hour);
			cal.set(Calendar.MINUTE, minute);
			cal.set(Calendar.SECOND, second);
			cal.set(Calendar.MILLISECOND, millisecond);

			try {
				cal.get(Calendar.HOUR_OF_DAY);
			} catch (IllegalArgumentException iae) {
				throw new InvalidDateTimeException("Cannot create time with provided values."); 
			}
			
			// Get back the time zone information so we can store it with
			// the rest of the date information. This is based on the
			// current default time zone, since none was provided.
			this.timezoneHours = cal.get(Calendar.ZONE_OFFSET) / TimeValue.millisInAnHour;
			this.timezoneMinutes = cal.get(Calendar.ZONE_OFFSET) % TimeValue.millisInAnHour / TimeValue.millisInAMinute;
		}
		
		/**
		 * Construct a DateTime object representing a time with an explicit timezone offset.
		 * 
		 * @param hour			The hour of the time
		 * @param minute		The minute of the time
		 * @param second		The second of the time
		 * @param millisecond	The millisecond of the time
		 * @param hourOffset	The timezone offset of the time, in hours
		 * @param minuteOffset	The timezone offset of the time, in minutes
		 */
		public TimeValue(int hour, int minute, int second, int millisecond, int hourOffset, int minuteOffset) {
			super(TypeFactory.getInstance().dateTimeType());
			
			this.hour = hour;
			this.minute = minute;
			this.second = second;
			this.millisecond = millisecond;
			this.timezoneHours = hourOffset;
			this.timezoneMinutes = minuteOffset;

			// Check to make sure the provided values are valid.
			// TODO: Failure here should throw a PDB exception.
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(getTZString(hourOffset,minuteOffset)),Locale.getDefault());
			cal.setLenient(false);
			cal.set(Calendar.HOUR_OF_DAY, hour);
			cal.set(Calendar.MINUTE, minute);
			cal.set(Calendar.SECOND, second);
			cal.set(Calendar.MILLISECOND, millisecond);

			try {
				cal.get(Calendar.HOUR_OF_DAY);
			} catch (IllegalArgumentException iae) {
				throw new InvalidDateTimeException("Cannot create time with provided values."); 
			}
		}

		public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E {
			return v.visitDateTime(this);
		}

		public int compareTo(IDateTime arg0) {
			if (arg0.isTime()) {
				long m1 = this.getInstant();
				long m2 = arg0.getInstant();
				if (m1 == m2)
					return 0;
				else if (m1 < m2)
					return -1;
				else
					return 1;
			} else {
				throw new UnsupportedOperationException("Time and non-Time values are not comparable");
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getInstant()
		 */
		public long getInstant() {
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(getTZString(this.timezoneHours,this.timezoneMinutes)),Locale.getDefault());
			cal.set(1970, 0, 1, this.hour, this.minute, this.second);
			cal.set(Calendar.MILLISECOND, this.millisecond);
			return cal.getTimeInMillis();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getCentury()
		 */
		public int getCentury() {
			throw new UnsupportedOperationException("Cannot get century on a time value");
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getYear()
		 */
		public int getYear() {
			throw new UnsupportedOperationException("Cannot get year on a time value");
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getMonthOfYear()
		 */
		public int getMonthOfYear() {
			throw new UnsupportedOperationException("Cannot get month on a time value");
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getDayOfMonth()
		 */
		public int getDayOfMonth() {
			throw new UnsupportedOperationException("Cannot get day on a time value");
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getHourOfDay()
		 */
		public int getHourOfDay() {
			return this.hour;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getMinuteOfHour()
		 */
		public int getMinuteOfHour() {
			return this.minute;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getSecondOfMinute()
		 */
		public int getSecondOfMinute() {
			return this.second;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getMillisecondsOfSecond()
		 */
		public int getMillisecondsOfSecond() {
			return this.millisecond;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getTimezoneOffsetHours()
		 */
		public int getTimezoneOffsetHours() {
			return this.timezoneHours;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getTimezoneOffsetMinutes()
		 */
		public int getTimezoneOffsetMinutes() {
			return this.timezoneMinutes;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#isDate()
		 */
		public boolean isDate() {
			return false;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#isTime()
		 */
		public boolean isTime() {
			return true;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#isDateTime()
		 */
		public boolean isDateTime() {
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + hour;
			result = prime * result + millisecond;
			result = prime * result + minute;
			result = prime * result + second;
			result = prime * result + timezoneHours;
			result = prime * result + timezoneMinutes;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TimeValue other = (TimeValue) obj;
			if (hour != other.hour)
				return false;
			if (millisecond != other.millisecond)
				return false;
			if (minute != other.minute)
				return false;
			if (second != other.second)
				return false;
			if (timezoneHours != other.timezoneHours)
				return false;
			if (timezoneMinutes != other.timezoneMinutes)
				return false;
			return true;
		}
	}
	
	public static class DateTimeValue extends Value implements IDateTime {

		private int year;
		private int month;
		private int day;
		private int hour;
		private int minute;
		private int second;
		private int millisecond;
		private int timezoneHours;
		private int timezoneMinutes;

		/**
		 * Construct a DateTime object representing a date and time.
		 *  
		 * @param year			The year of the datetime
		 * @param month			The month of the datetime
		 * @param day			The day of the datetime
		 * @param hour			The hour of the datetime
		 * @param minute		The minute of the datetime
		 * @param second		The second of the datetime
		 * @param millisecond	The millisecond of the datetime
		 */
		public DateTimeValue(int year, int month, int day, int hour, int minute, int second, int millisecond) {
			super(TypeFactory.getInstance().dateTimeType());
			
			this.year = year;
			this.month = month;
			this.day = day;
			this.hour = hour;
			this.minute = minute;
			this.second = second;
			this.millisecond = millisecond;
			
			// Check to make sure the provided values are valid.
			// TODO: Failure here should throw a PDB exception.
			Calendar cal = Calendar.getInstance(TimeZone.getDefault(),Locale.getDefault());
			cal.setLenient(false);
			cal.set(year, month-1, day, hour, minute, second);
			cal.set(Calendar.MILLISECOND, millisecond);

			try {
				cal.get(Calendar.HOUR_OF_DAY);
			} catch (IllegalArgumentException iae) {
				throw new InvalidDateTimeException("Cannot create datetime with provided values."); 
			}
			
			// Get back the time zone information so we can store it with
			// the rest of the date information. This is based on the
			// current default time zone, since none was provided.
			this.timezoneHours = cal.get(Calendar.ZONE_OFFSET) / TimeValue.millisInAnHour;
			this.timezoneMinutes = cal.get(Calendar.ZONE_OFFSET) % TimeValue.millisInAnHour / TimeValue.millisInAMinute;
		}

		/**
		 * Construct a DateTime object representing a date and time, with an explicit timezone.
		 * 
		 * @param year			The year of the datetime
		 * @param month			The month of the datetime
		 * @param day			The day of the datetime
		 * @param hour			The hour of the datetime
		 * @param minute		The minute of the datetime
		 * @param second		The second of the datetime
		 * @param millisecond	The millisecond of the datetime
		 * @param hourOffset	The timezone offset of the time, in hours
		 * @param minuteOffset	The timezone offset of the time, in minutes
		 */
		public DateTimeValue(int year, int month, int day, int hour, int minute, int second, int millisecond, int hourOffset, int minuteOffset) {
			super(TypeFactory.getInstance().dateTimeType());
			this.year = year;
			this.month = month;
			this.day = day;
			this.hour = hour;
			this.minute = minute;
			this.second = second;
			this.millisecond = millisecond;
			this.timezoneHours = hourOffset;
			this.timezoneMinutes = minuteOffset;

			// Check to make sure the provided values are valid.
			// TODO: Failure here should throw a PDB exception.
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(TimeValue.getTZString(hourOffset,minuteOffset)),Locale.getDefault());
			cal.setLenient(false);
			cal.set(year, month-1, day, hour, minute, second);
			cal.set(Calendar.MILLISECOND, millisecond);

			try {
				cal.get(Calendar.HOUR_OF_DAY);
			} catch (IllegalArgumentException iae) {
				throw new InvalidDateTimeException("Cannot create datetime with provided values."); 
			}
		}

		/**
		 * Construct a DateTime object representing the current instant on the date/time
		 * scale (in milliseconds, based on the Java epoch).
		 * 
		 * @param instant The millisecond instant.
		 */
		public DateTimeValue(long instant) {
			super(TypeFactory.getInstance().dateTimeType());
			
			Calendar cal = Calendar.getInstance(TimeZone.getDefault(),Locale.getDefault());
			cal.setLenient(false);
			cal.setTime(new Date(instant));
			
			this.year = cal.get(Calendar.YEAR);
			this.month = cal.get(Calendar.MONTH) + 1;
			this.day = cal.get(Calendar.DAY_OF_MONTH);
			this.hour = cal.get(Calendar.HOUR_OF_DAY);
			this.minute = cal.get(Calendar.MINUTE);
			this.second = cal.get(Calendar.SECOND);
			this.millisecond = cal.get(Calendar.MILLISECOND);
			this.timezoneHours = cal.get(Calendar.ZONE_OFFSET) / TimeValue.millisInAnHour;
			this.timezoneMinutes = cal.get(Calendar.ZONE_OFFSET) % TimeValue.millisInAnHour / TimeValue.millisInAMinute;			
		}

		public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E {
			return v.visitDateTime(this);
		}

		public int compareTo(IDateTime arg0) {
			if (arg0.isDateTime()) {
				long m1 = this.getInstant();
				long m2 = arg0.getInstant();
				if (m1 == m2)
					return 0;
				else if (m1 < m2)
					return -1;
				else
					return 1;
			} else {
				throw new UnsupportedOperationException("DateTime and non-DateTime values are not comparable");
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getInstant()
		 */
		public long getInstant() {
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(TimeValue.getTZString(this.timezoneHours,this.timezoneMinutes)),Locale.getDefault());
			cal.set(this.year, this.month-1, this.day, this.hour, this.minute, this.second);
			cal.set(Calendar.MILLISECOND, this.millisecond);
			return cal.getTimeInMillis();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getCentury()
		 */
		public int getCentury() {
			return (year - (year % 100)) / 100;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getYear()
		 */
		public int getYear() {
			return this.year;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getMonthOfYear()
		 */
		public int getMonthOfYear() {
			return this.month;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getDayOfMonth()
		 */
		public int getDayOfMonth() {
			return this.day;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getHourOfDay()
		 */
		public int getHourOfDay() {
			return this.hour;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getMinuteOfHour()
		 */
		public int getMinuteOfHour() {
			return this.minute;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getSecondOfMinute()
		 */
		public int getSecondOfMinute() {
			return this.second;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getMillisecondsOfSecond()
		 */
		public int getMillisecondsOfSecond() {
			return this.millisecond;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getTimezoneOffsetHours()
		 */
		public int getTimezoneOffsetHours() {
			return this.timezoneHours;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#getTimezoneOffsetMinutes()
		 */
		public int getTimezoneOffsetMinutes() {
			return this.timezoneMinutes;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#isDate()
		 */
		public boolean isDate() {
			return false;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#isTime()
		 */
		public boolean isTime() {
			return false;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.pdb.facts.IDateTime#isDateTime()
		 */
		public boolean isDateTime() {
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + day;
			result = prime * result + hour;
			result = prime * result + millisecond;
			result = prime * result + minute;
			result = prime * result + month;
			result = prime * result + second;
			result = prime * result + timezoneHours;
			result = prime * result + timezoneMinutes;
			result = prime * result + year;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DateTimeValue other = (DateTimeValue) obj;
			if (day != other.day)
				return false;
			if (hour != other.hour)
				return false;
			if (millisecond != other.millisecond)
				return false;
			if (minute != other.minute)
				return false;
			if (month != other.month)
				return false;
			if (second != other.second)
				return false;
			if (timezoneHours != other.timezoneHours)
				return false;
			if (timezoneMinutes != other.timezoneMinutes)
				return false;
			if (year != other.year)
				return false;
			return true;
		}
	}
}
