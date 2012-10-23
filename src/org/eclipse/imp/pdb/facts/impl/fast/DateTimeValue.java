package org.eclipse.imp.pdb.facts.impl.fast;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.imp.pdb.facts.IDateTime;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

public class DateTimeValue extends Value implements IDateTime{
	private final static Type DATE_TIME_TYPE = TypeFactory.getInstance().dateTimeType();
	private static enum DATE_TIME_ENUM {DATE, TIME, DATETIME};
	
	private final Calendar dateTimeComplete;
	private final DATE_TIME_ENUM dateTimeType;
	
	protected DateTimeValue(int year, int month, int day, int hour, int minute, int second, int millisecond){
		super();
		
		dateTimeComplete = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
		dateTimeComplete.setLenient(false);
		dateTimeComplete.set(year, month - 1, day, hour, minute, second);
		dateTimeComplete.set(Calendar.MILLISECOND, millisecond);
		
		dateTimeType = DATE_TIME_ENUM.DATETIME;
	}
	
	protected DateTimeValue(int year, int month, int day, int hour, int minute, int second, int millisecond, int hourOffset, int minuteOffset){
		super();
		
		String tzString = getTZString(hourOffset, minuteOffset);
	
		dateTimeComplete = Calendar.getInstance(TimeZone.getTimeZone(tzString), Locale.getDefault());
		dateTimeComplete.setLenient(false);
		dateTimeComplete.set(year, month - 1, day, hour, minute, second);
		dateTimeComplete.set(Calendar.MILLISECOND, millisecond);
		
		dateTimeType = DATE_TIME_ENUM.DATETIME;
	}
	
	protected DateTimeValue(int year, int month, int day){
		super();

		dateTimeComplete = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
		dateTimeComplete.setLenient(false);
		dateTimeComplete.set(year, month - 1, day, 0, 0, 0);
		dateTimeComplete.set(Calendar.MILLISECOND, 0);
		
		dateTimeType = DATE_TIME_ENUM.DATE;
	}
	
	protected DateTimeValue(int hour, int minute, int second, int millisecond){
		super();

		dateTimeComplete = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
		dateTimeComplete.setLenient(false);
		dateTimeComplete.set(1970, 0, 1, hour, minute, second);
		dateTimeComplete.set(Calendar.MILLISECOND, millisecond);
		
		dateTimeType = DATE_TIME_ENUM.TIME;
	}
	
	protected DateTimeValue(int hour, int minute, int second, int millisecond, int hourOffset, int minuteOffset){
		super();
		
		String tzString = getTZString(hourOffset, minuteOffset);
		
		dateTimeComplete = Calendar.getInstance(TimeZone.getTimeZone(tzString), Locale.getDefault());
		dateTimeComplete.setLenient(false);
		dateTimeComplete.set(1970, 0, 1, hour, minute, second);
		dateTimeComplete.set(Calendar.MILLISECOND, millisecond);
		
		dateTimeType = DATE_TIME_ENUM.TIME;
	}
	
	protected DateTimeValue(long instant){
		super();
		
		dateTimeComplete = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
		dateTimeComplete.setLenient(false);
		dateTimeComplete.setTime(new Date(instant));
		
		dateTimeType = DATE_TIME_ENUM.DATETIME;		
	}
	
	private static String getTZString(int hourOffset, int minuteOffset){
		String tzString = "GMT" + 
			((hourOffset < 0 || (0 == hourOffset && minuteOffset < 0)) ? "-" : "+") + 
			String.format("%02d",hourOffset >= 0 ? hourOffset : hourOffset * -1) +
			String.format("%02d",minuteOffset >= 0 ? minuteOffset : minuteOffset * -1);
		return tzString;
	}

	public Type getType(){
		return DATE_TIME_TYPE;
	}

	public boolean isDateTime(){
		return (dateTimeType == DATE_TIME_ENUM.DATETIME);
	}

	public boolean isDate(){
		return (dateTimeType == DATE_TIME_ENUM.DATE);
	}

	public boolean isTime(){
		return (dateTimeType == DATE_TIME_ENUM.TIME);
	}
	
	public int getCentury(){
		if(dateTimeType == DATE_TIME_ENUM.TIME) return 0;
		
		return (dateTimeComplete.get(Calendar.YEAR) - (dateTimeComplete.get(Calendar.YEAR) % 100)) / 100; 
	}
	
	public int getYear(){
		if(dateTimeType == DATE_TIME_ENUM.TIME) return 0;
		
		return dateTimeComplete.get(Calendar.YEAR);
	}
	
	public int getMonthOfYear(){
		if(dateTimeType == DATE_TIME_ENUM.TIME) return 0;
		
		return dateTimeComplete.get(Calendar.MONTH) + 1;
	}
	
	public int getDayOfMonth(){
		if(dateTimeType == DATE_TIME_ENUM.TIME) return 0;
		
		return dateTimeComplete.get(Calendar.DAY_OF_MONTH);
	}
	
	public int getHourOfDay(){
		if(dateTimeType == DATE_TIME_ENUM.DATE) return 0;
		
		return dateTimeComplete.get(Calendar.HOUR_OF_DAY);
	}
	
	public int getMinuteOfHour(){
		if(dateTimeType == DATE_TIME_ENUM.DATE) return 0;
		
		return dateTimeComplete.get(Calendar.MINUTE);
	}
	
	public int getSecondOfMinute(){
		if(dateTimeType == DATE_TIME_ENUM.DATE) return 0;
		
		return dateTimeComplete.get(Calendar.SECOND);
	}
	
	public int getMillisecondsOfSecond(){
		if(dateTimeType == DATE_TIME_ENUM.DATE) return 0;
		
		return dateTimeComplete.get(Calendar.MILLISECOND);
	}
	
	public int getTimezoneOffsetHours(){
		if(dateTimeType == DATE_TIME_ENUM.DATE) return 0;
		
		return dateTimeComplete.get(Calendar.ZONE_OFFSET) / 3600000;
	}
	
	public int getTimezoneOffsetMinutes(){
		if(dateTimeType == DATE_TIME_ENUM.DATE) return 0;
		
		return (dateTimeComplete.get(Calendar.ZONE_OFFSET) / 60000) % 60;
	}
	
	public long getInstant(){
		return dateTimeComplete.getTimeInMillis();
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitDateTime(this);
	}

	public int hashCode(){
		return dateTimeComplete.hashCode();
	}
	
	public boolean isEqual(IValue v){
		return equals(v);
	}
	
	public boolean equals(Object o){
		if(this == o) return true;
		if(o == null) return false;
		if(o.getClass() != getClass()) return false;
		
		return (dateTimeComplete.compareTo(((DateTimeValue) o).dateTimeComplete) == 0);
	}

	public int compareTo(IDateTime arg0) {
		return dateTimeComplete.getTime().compareTo(((DateTimeValue) arg0).dateTimeComplete.getTime());
	}
}
