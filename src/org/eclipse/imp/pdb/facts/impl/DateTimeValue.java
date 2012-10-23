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
import org.eclipse.imp.pdb.facts.impl.DateTimeValue;
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
public class DateTimeValue extends Value implements IDateTime {

	private enum DateTimeEnum { DATE, TIME, DATETIME };
	
	private final Calendar fDateTimeComplete;
	
	private final DateTimeEnum fDateTimeType;
	
	/**
	 * Construct a DateTime object representing a date. 
	 * 
	 * @param year			The year of the date
	 * @param month			The month of the date
	 * @param day			The day of the date
	 */
	public DateTimeValue(int year, int month, int day) {
		super(TypeFactory.getInstance().dateTimeType());

		fDateTimeComplete = Calendar.getInstance(TimeZone.getDefault(),Locale.getDefault());
		fDateTimeComplete.setLenient(false);
		fDateTimeComplete.set(year, month-1, day, 0, 0, 0);
		fDateTimeComplete.set(Calendar.MILLISECOND, 0);
		
		fDateTimeType = DateTimeEnum.DATE;
	}

	/**
	 * Construct a DateTime object representing a time. 
	 * 
	 * @param hour			The hour of the time
	 * @param minute		The minute of the time
	 * @param second		The second of the time
	 * @param millisecond	The millisecond of the time
	 */
	public DateTimeValue(int hour, int minute, int second, int millisecond) {
		super(TypeFactory.getInstance().dateTimeType());

		fDateTimeComplete = Calendar.getInstance(TimeZone.getDefault(),Locale.getDefault());
		fDateTimeComplete.setLenient(false);
		fDateTimeComplete.set(1970, 0, 1, hour, minute, second);
		fDateTimeComplete.set(Calendar.MILLISECOND, millisecond);
		
		fDateTimeType = DateTimeEnum.TIME;
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
	public DateTimeValue(int hour, int minute, int second, int millisecond, int hourOffset, int minuteOffset) {
		super(TypeFactory.getInstance().dateTimeType());
		
		String tzString = getTZString(hourOffset, minuteOffset);
		
		fDateTimeComplete = Calendar.getInstance(TimeZone.getTimeZone(tzString),Locale.getDefault());
		fDateTimeComplete.setLenient(false);
		fDateTimeComplete.set(1970, 0, 1, hour, minute, second);
		fDateTimeComplete.set(Calendar.MILLISECOND, millisecond);
		
		fDateTimeType = DateTimeEnum.TIME;
		
	}

	/**
	 * Given the hour and minute offset, generate the appropriate Java
	 * timezone string
	 * 
	 * @param hourOffset	The hour offset for the timezone.
	 * @param minuteOffset	The minute offset for the timezone.
	 * 
	 * @return				A string with the proper timezone.
	 */
	private String getTZString(int hourOffset, int minuteOffset) {
		String tzString = "GMT" + 
			((hourOffset < 0 || (0 == hourOffset && minuteOffset < 0)) ? "-" : "+") + 
			String.format("%02d",hourOffset >= 0 ? hourOffset : hourOffset * -1) +
			String.format("%02d",minuteOffset >= 0 ? minuteOffset : minuteOffset * -1);
		return tzString;
	}
	
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
		
		fDateTimeComplete = Calendar.getInstance(TimeZone.getDefault(),Locale.getDefault());
		fDateTimeComplete.setLenient(false);
		fDateTimeComplete.set(year, month-1, day, hour, minute, second);
		fDateTimeComplete.set(Calendar.MILLISECOND, millisecond);
		
		fDateTimeType = DateTimeEnum.DATETIME;
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
	public DateTimeValue(int year, int month, int day, int hour, int minute,
			int second, int millisecond, int hourOffset, int minuteOffset) {
		super(TypeFactory.getInstance().dateTimeType());
		
		String tzString = getTZString(hourOffset, minuteOffset);
	
		fDateTimeComplete = Calendar.getInstance(TimeZone.getTimeZone(tzString),Locale.getDefault());
		fDateTimeComplete.setLenient(false);
		fDateTimeComplete.set(year, month-1, day, hour, minute, second);
		fDateTimeComplete.set(Calendar.MILLISECOND, millisecond);
		
		fDateTimeType = DateTimeEnum.DATETIME;
	}

	/**
	 * Construct a DateTime object representing the current instant on the date/time
	 * scale (in milliseconds, based on the Java epoch).
	 * 
	 * @param instant The millisecond instant.
	 */
	public DateTimeValue(long instant) {
		super(TypeFactory.getInstance().dateTimeType());
		
		fDateTimeComplete = Calendar.getInstance(TimeZone.getDefault(),Locale.getDefault());
		fDateTimeComplete.setLenient(false);
		fDateTimeComplete.setTime(new Date(instant));
		
		fDateTimeType = DateTimeEnum.DATETIME;		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.imp.pdb.facts.IDateTime#getCentury()
	 */
	public int getCentury() {
		if (DateTimeEnum.DATE == fDateTimeType || DateTimeEnum.DATETIME == fDateTimeType) {
			return (fDateTimeComplete.get(Calendar.YEAR) - (fDateTimeComplete.get(Calendar.YEAR) % 100)) / 100; 
		} else {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.pdb.facts.IDateTime#getDayOfMonth()
	 */
	public int getDayOfMonth() {
		if (DateTimeEnum.DATE == fDateTimeType || DateTimeEnum.DATETIME == fDateTimeType) {
			return fDateTimeComplete.get(Calendar.DAY_OF_MONTH);
		} else {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.pdb.facts.IDateTime#getHourOfDay()
	 */
	public int getHourOfDay() {
		if (DateTimeEnum.TIME == fDateTimeType || DateTimeEnum.DATETIME == fDateTimeType) {
			return fDateTimeComplete.get(Calendar.HOUR_OF_DAY);
		} else {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.pdb.facts.IDateTime#getInstant()
	 */
	public long getInstant() {
		return fDateTimeComplete.getTimeInMillis();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.pdb.facts.IDateTime#getMillisecondsOfSecond()
	 */
	public int getMillisecondsOfSecond() {
		if (DateTimeEnum.TIME == fDateTimeType || DateTimeEnum.DATETIME == fDateTimeType) {
			return fDateTimeComplete.get(Calendar.MILLISECOND);
		} else {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.pdb.facts.IDateTime#getMinuteOfHour()
	 */
	public int getMinuteOfHour() {
		if (DateTimeEnum.TIME == fDateTimeType || DateTimeEnum.DATETIME == fDateTimeType) {
			return fDateTimeComplete.get(Calendar.MINUTE);
		} else {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.pdb.facts.IDateTime#getMonthOfYear()
	 */
	public int getMonthOfYear() {
		if (DateTimeEnum.DATE == fDateTimeType || DateTimeEnum.DATETIME == fDateTimeType) {
			return fDateTimeComplete.get(Calendar.MONTH) + 1;
		} else {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.pdb.facts.IDateTime#getSecondOfMinute()
	 */
	public int getSecondOfMinute() {
		if (DateTimeEnum.TIME == fDateTimeType || DateTimeEnum.DATETIME == fDateTimeType) {
			return fDateTimeComplete.get(Calendar.SECOND);
		} else {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.pdb.facts.IDateTime#getTimezoneOffsetHours()
	 */
	public int getTimezoneOffsetHours() {
		if (DateTimeEnum.TIME == fDateTimeType || DateTimeEnum.DATETIME == fDateTimeType) {
			int hourOffset = fDateTimeComplete.get(Calendar.ZONE_OFFSET) / 3600000; // 1000 milliseconds * 60 seconds * 60 minutes in an hour
			return hourOffset;
		} else {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.pdb.facts.IDateTime#getTimezoneOffsetMinutes()
	 */
	public int getTimezoneOffsetMinutes() {
		if (DateTimeEnum.TIME == fDateTimeType || DateTimeEnum.DATETIME == fDateTimeType) {
			int minuteOffset = (fDateTimeComplete.get(Calendar.ZONE_OFFSET) / 60000)%60; // 1000 milliseconds * 60 seconds in a minute, %60 to keep number within an hour
			return minuteOffset;
		} else {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.pdb.facts.IDateTime#getYear()
	 */
	public int getYear() {
		if (DateTimeEnum.DATE == fDateTimeType || DateTimeEnum.DATETIME == fDateTimeType) {
			return fDateTimeComplete.get(Calendar.YEAR);
		} else {
			return 0;
		}
	}

	public boolean isDate() {
		return (DateTimeEnum.DATE == fDateTimeType);
	}

	public boolean isDateTime() {
		return (DateTimeEnum.DATETIME == fDateTimeType);
	}

	public boolean isTime() {
		return (DateTimeEnum.TIME == fDateTimeType);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.pdb.facts.IValue#accept(org.eclipse.imp.pdb.facts.visitors.IValueVisitor)
	 */
	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitDateTime(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((fDateTimeComplete == null) ? 0 : fDateTimeComplete
						.hashCode());
		result = prime * result
				+ ((fDateTimeType == null) ? 0 : fDateTimeType.hashCode());
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
		if (fDateTimeComplete == null) {
			if (other.fDateTimeComplete != null)
				return false;
		} else if (0 != fDateTimeComplete.compareTo(other.fDateTimeComplete))
			return false;
		if (fDateTimeType == null) {
			if (other.fDateTimeType != null)
				return false;
		} else if (!fDateTimeType.equals(other.fDateTimeType))
			return false;
		return true;
	}

	public int compareTo(IDateTime arg0) {
		return fDateTimeComplete.getTime().compareTo(((DateTimeValue) arg0).fDateTimeComplete.getTime());
	}
}
