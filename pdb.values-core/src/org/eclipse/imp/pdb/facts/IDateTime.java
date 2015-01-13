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
package org.eclipse.imp.pdb.facts;

/**
 * Provides methods for accessing the components of a datetime value
 * (e.g., years, hours) and determining if the value represents a
 * date, time, or datetime.
 */
public interface IDateTime extends IValue, Comparable<IDateTime> {
	/**
	 * Retrieve the date and time as an instant, defined as the number of 
	 * milliseconds from 1970-01-01T00:00Z. This is compatible with the
	 * concept of an instant in the Java standard datetime libraries and
	 * in libraries such as Joda Time.
	 * 
	 * @return the number of milliseconds from 1970-01-01T00:00Z
	 */
	public long getInstant();
	
	/**
	 * Get the century. If isTime() == true, this value is undefined.
	 * 
	 * @return the century (all but the last 2 digits of the year)
	 */
	public int getCentury();
	
	/**
	 * Get the year. If isTime() == true, this value is undefined.
	 *  
	 * @return the year
	 */
	public int getYear();
		
	/**
	 * Get the month of year. If isTime() == true, this value is undefined. 
	 *  
	 * @return the month of year
	 */
	public int getMonthOfYear();
	
	/**
	 * Get the day of month. If isTime() == true, this value is undefined. 
	 * 
	 * @return the day of month
	 */
	public int getDayOfMonth();
	
	/**
	 * Get the hour of day value. Hours should be based on a 24 hour
	 * clock, as this interface does not provide am/pm information.
	 * If isDate() == true, this value is undefined.
	 * 
	 * @return the hour of the day
	 */
	public int getHourOfDay();
	
	/**
	 * Get the minute of hour value. If isDate() == true, this value is 
	 * undefined. 
	 * 
	 * @return the minute of the hour
	 */
	public int getMinuteOfHour();
	
	/**
	 * Get the second of minute value. If isDate() == true, this value 
	 * is undefined.
	 * 
	 * @return the second of the minute
	 */
	public int getSecondOfMinute();
	
	/**
	 * Get the milliseconds of second value. If isDate() == true, this 
	 * value is undefined.
	 * 
	 * @return the milliseconds of the second
	 */
	public int getMillisecondsOfSecond();
		
	/**
	 * Get the signed numeric value representing the hour offset from
	 * UTC for the time, based on the timezone assigned to the time.
	 * If isDate() == true, this value is undefined.
	 * 
	 * @return the hour offset from UTC for the timezone of the time
	 */
	public int getTimezoneOffsetHours();
	
	/**
	 * Get the value representing the minute offset from UTC for the time, 
	 * based on the timezone assigned to the time. This should be positive
	 * unless getTimezoneOffsetHours() == 0.
	 * If isDate() == true, this value is undefined.
	 * 
	 * @return the minute offset from UTC for the timezone of the time
	 */
	public int getTimezoneOffsetMinutes();

	/**
	 * Indicates if this DateTime represents just a date (with no time).
	 * 
	 * @return true if this is just a date, false otherwise
	 */
	public boolean isDate();
	
	/**
	 * Indicates if this DateTime represents just a time (with no date).
	 * 
	 * @return true if this is just a time, false otherwise
	 */
	public boolean isTime();
	
	/**
	 * Indicates if this DateTime represents a date and time.
	 * 
	 * @return true if this is a datetime, false otherwise
	 */
	public boolean isDateTime();
}
