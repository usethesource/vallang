/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Mark Hills (Mark.Hills@cwi.nl) - initial API and implementation
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.vallang.impl.primitive;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;
import io.usethesource.vallang.IDateTime;
import io.usethesource.vallang.exceptions.InvalidDateTimeException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;


/** A concrete instance of IDateTime, representing either a date,
 *  a time, or a date with time.
 *  
 *  NOTE: We currently do not support partial dates and times; i.e.,
 *  it is not possible to represent "July 2009" or "15" (hours).
 *
 */
/*package*/ class DateTimeValues {

    private static final Type DATE_TIME_TYPE = TypeFactory.getInstance().dateTimeType();

    /*package*/ static IDateTime newDate(int year, int month, int day) {
        return new DateTimeValues.DateValue(year, month, day);
    }


    private static class DateValue implements IDateTime {
        private final LocalDate actual;

        /**
         * Construct a DateTime object representing a date. 
         * 
         * @param year			The year of the date
         * @param month			The month of the date
         * @param day			The day of the date
         */
        private DateValue(int year, int month, int day) {
            try {
                actual = LocalDate.of(year, month, day);
            }
            catch (DateTimeException dt) {
                throw new InvalidDateTimeException("Cannot create date with provided values.", dt); 
            }
        }

        @Override
        public Type getType() {
            return DATE_TIME_TYPE;
        }

        @Override
        public int compareTo(IDateTime arg0) {
            if (arg0 instanceof DateValue) {
                return actual.compareTo(((DateValue)arg0).actual);
            }
            if (arg0.isDate()) {
                return Long.compare(getInstant(), arg0.getInstant());
            }
            throw new UnsupportedOperationException("Date and non-Date values are not comparable");
        }

        /* (non-Javadoc)
         * @see IDateTime#getInstant()
         */
        @Override
        public long getInstant() {
            return actual
                .atTime(LocalTime.MIN)
                .atZone(ZoneId.systemDefault())
                .toEpochSecond() * 1000;
        }

        /* (non-Javadoc)
         * @see IDateTime#getCentury()
         */
        @Override
        public int getCentury() {
            return (getYear() - (getYear() % 100)) / 100;
        }

        /* (non-Javadoc)
         * @see IDateTime#getYear()
         */
        @Override
        public int getYear() {
            return actual.getYear();
        }

        /* (non-Javadoc)
         * @see IDateTime#getMonthOfYear()
         */
        @Override
        public int getMonthOfYear() {
            return actual.getMonthValue();
        }

        /* (non-Javadoc)
         * @see IDateTime#getDayOfMonth()
         */
        @Override
        public int getDayOfMonth() {
            return actual.getDayOfMonth();
        }

        /* (non-Javadoc)
         * @see IDateTime#getHourOfDay()
         */
        @Override
        public int getHourOfDay() {
            throw new UnsupportedOperationException("Cannot get hours on a date value");
        }

        /* (non-Javadoc)
         * @see IDateTime#getMinuteOfHour()
         */
        @Override
        public int getMinuteOfHour() {
            throw new UnsupportedOperationException("Cannot get minutes on a date value");
        }

        /* (non-Javadoc)
         * @see IDateTime#getSecondOfMinute()
         */
        @Override
        public int getSecondOfMinute() {
            throw new UnsupportedOperationException("Cannot get seconds on a date value");
        }

        /* (non-Javadoc)
         * @see IDateTime#getMillisecondsOfSecond()
         */
        @Override
        public int getMillisecondsOfSecond() {
            throw new UnsupportedOperationException("Cannot get milliseconds on a date value");
        }

        /* (non-Javadoc)
         * @see IDateTime#getTimezoneOffsetHours()
         */
        @Override
        public int getTimezoneOffsetHours() {
            throw new UnsupportedOperationException("Cannot get timezone offset hours on a date value");
        }

        /* (non-Javadoc)
         * @see IDateTime#getTimezoneOffsetMinutes()
         */
        @Override
        public int getTimezoneOffsetMinutes() {
            throw new UnsupportedOperationException("Cannot get timezone offset minutes on a date value");
        }

        /* (non-Javadoc)
         * @see IDateTime#isDate()
         */
        @Override
        public boolean isDate() {
            return true;
        }

        /* (non-Javadoc)
         * @see IDateTime#isTime()
         */
        @Override
        public boolean isTime() {
            return false;
        }

        /* (non-Javadoc)
         * @see IDateTime#isDateTime()
         */
        @Override
        public boolean isDateTime() {
            return false;
        }

        @Override
        public int hashCode() {
            return actual.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof DateValue) {
                return ((DateValue)obj).actual.equals(actual);
            }
            return false;
        }

        @Override
        public String toString() {
            return defaultToString();
        }
    }

    /*package*/ static IDateTime newTime(int hour, int minute, int second, int millisecond) {
        return new DateTimeValues.TimeValue(hour, minute, second, millisecond);
    }

    /*package*/ static IDateTime newTime(int hour, int minute, int second, int millisecond,
            int hourOffset, int minuteOffset) {
        return new DateTimeValues.TimeValue(hour, minute, second, millisecond, hourOffset, minuteOffset);
    }

    private static long currentTotalOffset() {
        return OffsetDateTime.now().getOffset().getTotalSeconds();
    }

    private static int currentHourOffset() {
        return (int) TimeUnit.HOURS.convert(currentTotalOffset(), TimeUnit.SECONDS);
    }

    private static int currentMinuteOffset() {
        return (int) (TimeUnit.MINUTES.convert(currentTotalOffset(), TimeUnit.SECONDS) % 60);
    }

    private static ZoneOffset toOffset(int hourOffset, int minuteOffset) {
        return ZoneOffset.ofHoursMinutes(hourOffset, minuteOffset);
    }
    private static class TimeValue  implements IDateTime {
        private final OffsetTime actual;

        /**
         * Construct a DateTime object representing a time. 
         * 
         * @param hour			The hour of the time
         * @param minute		The minute of the time
         * @param second		The second of the time
         * @param millisecond	The millisecond of the time
         */
        private TimeValue(int hour, int minute, int second, int millisecond) {
            this(hour, minute, second, millisecond, currentHourOffset(), currentMinuteOffset());
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
        private TimeValue(int hour, int minute, int second, int millisecond, int hourOffset, int minuteOffset) {
            try {
                actual = OffsetTime.of(hour, minute, second, (int)TimeUnit.MILLISECONDS.toNanos(millisecond), toOffset(hourOffset, minuteOffset));
            }
            catch (DateTimeException dt) {
                throw new InvalidDateTimeException("Cannot create date with provided values.", dt); 
            }
        }

        @Override
        public Type getType() {
            return DATE_TIME_TYPE;
        }

        @Override
        public int compareTo(IDateTime arg0) {
            if (arg0 instanceof TimeValue) {
                return actual.compareTo(((TimeValue)arg0).actual);
            }
            if (arg0.isTime()) {
                return Long.compare(getInstant(), arg0.getInstant());
            }
            throw new UnsupportedOperationException("Time and non-Time values are not comparable");
        }

        /* (non-Javadoc)
         * @see IDateTime#getInstant()
         */
        @Override
        public long getInstant() {
            return actual
                .atDate(LocalDate.of(1970, 1, 1))
                .toEpochSecond() * 1000
                ;
        }

        /* (non-Javadoc)
         * @see IDateTime#getCentury()
         */
        @Override
        public int getCentury() {
            throw new UnsupportedOperationException("Cannot get century on a time value");
        }

        /* (non-Javadoc)
         * @see IDateTime#getYear()
         */
        @Override
        public int getYear() {
            throw new UnsupportedOperationException("Cannot get year on a time value");
        }

        /* (non-Javadoc)
         * @see IDateTime#getMonthOfYear()
         */
        @Override
        public int getMonthOfYear() {
            throw new UnsupportedOperationException("Cannot get month on a time value");
        }

        /* (non-Javadoc)
         * @see IDateTime#getDayOfMonth()
         */
        @Override
        public int getDayOfMonth() {
            throw new UnsupportedOperationException("Cannot get day on a time value");
        }

        /* (non-Javadoc)
         * @see IDateTime#getHourOfDay()
         */
        @Override
        public int getHourOfDay() {
            return actual.getHour();
        }

        /* (non-Javadoc)
         * @see IDateTime#getMinuteOfHour()
         */
        @Override
        public int getMinuteOfHour() {
            return actual.getMinute();
        }

        /* (non-Javadoc)
         * @see IDateTime#getSecondOfMinute()
         */
        @Override
        public int getSecondOfMinute() {
            return actual.getSecond();
        }

        /* (non-Javadoc)
         * @see IDateTime#getMillisecondsOfSecond()
         */
        @Override
        public int getMillisecondsOfSecond() {
            return (int) TimeUnit.MILLISECONDS.convert(actual.getNano(), TimeUnit.NANOSECONDS);
        }

        /* (non-Javadoc)
         * @see IDateTime#getTimezoneOffsetHours()
         */
        @Override
        public int getTimezoneOffsetHours() {
            return (int)TimeUnit.HOURS.convert(actual.getOffset().getTotalSeconds(), TimeUnit.SECONDS);
        }

        /* (non-Javadoc)
         * @see IDateTime#getTimezoneOffsetMinutes()
         */
        @Override
        public int getTimezoneOffsetMinutes() {
            return (int)(TimeUnit.MINUTES.convert(actual.getOffset().getTotalSeconds(), TimeUnit.SECONDS) % 60);
        }

        /* (non-Javadoc)
         * @see IDateTime#isDate()
         */
        @Override
        public boolean isDate() {
            return false;
        }

        /* (non-Javadoc)
         * @see IDateTime#isTime()
         */
        @Override
        public boolean isTime() {
            return true;
        }

        /* (non-Javadoc)
         * @see IDateTime#isDateTime()
         */
        @Override
        public boolean isDateTime() {
            return false;
        }

        @Override
        public int hashCode() {
            return actual.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof TimeValue) {
                return actual.equals(((TimeValue)obj).actual);
            }
            return false;
        }

        @Override
        public String toString() {
            return defaultToString();
        }
    }

    /*package*/ static IDateTime newDateTime(int year, int month, int day, int hour,
            int minute, int second, int millisecond) {
        return new DateTimeValues.DateTimeValue(year, month, day, hour, minute, second, millisecond);
    }

    /*package*/ static IDateTime newDateTime(int year, int month, int day, int hour,
            int minute, int second, int millisecond, int hourOffset,
            int minuteOffset) {
        return new DateTimeValues.DateTimeValue(year, month, day, hour, minute, second, millisecond, hourOffset, minuteOffset);
    }

    /*package*/ static IDateTime newDateTime(long instant) {
        return new DateTimeValues.DateTimeValue(instant, 0, 0);
    }

    /*package*/ static IDateTime newDateTime(long instant, int timezoneHours, int timezoneMinutes) {
        return new DateTimeValues.DateTimeValue(instant, timezoneHours, timezoneMinutes);
    }

    private static class DateTimeValue  implements IDateTime {
        private final OffsetDateTime actual;

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
        private DateTimeValue(int year, int month, int day, int hour, int minute, int second, int millisecond) {
            this(year, month, day, hour, minute, second, millisecond, currentHourOffset(), currentMinuteOffset());
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
        private DateTimeValue(int year, int month, int day, int hour, int minute, int second, int millisecond, int hourOffset, int minuteOffset) {
            try {
                actual = OffsetDateTime.of(year, month, day, hour, minute, second, (int)TimeUnit.MILLISECONDS.toNanos(millisecond), toOffset(hourOffset, minuteOffset));
            }
            catch (DateTimeException dt) {
                throw new InvalidDateTimeException("Cannot create date with provided values.", dt); 
            }
        }

        /**
         * Construct a DateTime object representing the current instant on the date/time
         * scale (in milliseconds, based on the Java epoch).
         * 
         * @param instant The millisecond instant.
         * @param timezoneHours The hour offset for the new object's timezone 
         * @param timezoneMinutes The minute offset for the new object's timezone
         */
        private DateTimeValue(long instant, int timezoneHours, int timezoneMinutes) {
            try {
                actual = Instant.ofEpochMilli(instant).atOffset(toOffset(timezoneHours, timezoneMinutes));
            }
            catch (DateTimeException dt) {
                throw new InvalidDateTimeException("Cannot create date with provided values.", dt); 
            }
        }

        @Override
        public String toString() {
            return defaultToString();
        }

        @Override
        public Type getType() {
            return DATE_TIME_TYPE;
        }

        @Override
        public int compareTo(IDateTime arg0) {
            if (arg0 instanceof DateTimeValue) {
                return actual.compareTo(((DateTimeValue)arg0).actual);
            }
            if (arg0.isDateTime()) {
                return Long.compare(getInstant(), arg0.getInstant());
            }
            throw new UnsupportedOperationException("DateTime and non-DateTime values are not comparable");
        }

        /* (non-Javadoc)
         * @see IDateTime#getInstant()
         */
        @Override
        public long getInstant() {
            return actual.toInstant().toEpochMilli();
        }

        /* (non-Javadoc)
         * @see IDateTime#getCentury()
         */
        @Override
        public int getCentury() {
            return (getYear() - (getYear() % 100)) / 100;
        }

        /* (non-Javadoc)
         * @see IDateTime#getYear()
         */
        @Override
        public int getYear() {
            return actual.getYear();
        }

        /* (non-Javadoc)
         * @see IDateTime#getMonthOfYear()
         */
        @Override
        public int getMonthOfYear() {
            return actual.getMonthValue();
        }

        /* (non-Javadoc)
         * @see IDateTime#getDayOfMonth()
         */
        @Override
        public int getDayOfMonth() {
            return actual.getDayOfMonth();
        }

        /* (non-Javadoc)
         * @see IDateTime#getHourOfDay()
         */
        @Override
        public int getHourOfDay() {
            return actual.getHour();
        }

        /* (non-Javadoc)
         * @see IDateTime#getMinuteOfHour()
         */
        @Override
        public int getMinuteOfHour() {
            return actual.getMinute();
        }

        /* (non-Javadoc)
         * @see IDateTime#getSecondOfMinute()
         */
        @Override
        public int getSecondOfMinute() {
            return actual.getSecond();
        }

        /* (non-Javadoc)
         * @see IDateTime#getMillisecondsOfSecond()
         */
        @Override
        public int getMillisecondsOfSecond() {
            return (int) TimeUnit.MILLISECONDS.convert(actual.getNano(), TimeUnit.NANOSECONDS);
        }

        /* (non-Javadoc)
         * @see IDateTime#getTimezoneOffsetHours()
         */
        @Override
        public int getTimezoneOffsetHours() {
            return (int) TimeUnit.HOURS.convert(actual.getOffset().getTotalSeconds(), TimeUnit.SECONDS);
        }

        /* (non-Javadoc)
         * @see IDateTime#getTimezoneOffsetMinutes()
         */
        @Override
        public int getTimezoneOffsetMinutes() {
            return (int) (TimeUnit.MINUTES.convert(actual.getOffset().getTotalSeconds(), TimeUnit.SECONDS) % 60);
        }

        /* (non-Javadoc)
         * @see IDateTime#isDate()
         */
        @Override
        public boolean isDate() {
            return false;
        }

        /* (non-Javadoc)
         * @see IDateTime#isTime()
         */
        @Override
        public boolean isTime() {
            return false;
        }

        /* (non-Javadoc)
         * @see IDateTime#isDateTime()
         */
        @Override
        public boolean isDateTime() {
            return true;
        }

        @Override
        public int hashCode() {
            return actual.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof DateTimeValue) {
                return actual.equals(((DateTimeValue)obj).actual);
            }
            return false;
        }
    }
}