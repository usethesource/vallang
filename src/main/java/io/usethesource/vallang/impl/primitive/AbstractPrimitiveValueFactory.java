/*******************************************************************************
 * Copyright (c) 2009, 2012-2013 Centrum Wiskunde en Informatica (CWI)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jurgen Vinju - interface and implementation
 *    Arnold Lankamp - implementation
 *    Anya Helene Bagge - rational support, labeled maps and tuples
 *    Davy Landman - added PI & E constants
 *    Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.vallang.impl.primitive;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

import io.usethesource.vallang.IBool;
import io.usethesource.vallang.IDateTime;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IRational;
import io.usethesource.vallang.IReal;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValueFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base value factory with optimized representations of primitive values.
 */
public abstract class AbstractPrimitiveValueFactory implements IValueFactory {

	private final static int DEFAULT_PRECISION = 10;
	private final AtomicInteger currentPrecision = new AtomicInteger(DEFAULT_PRECISION);

	@Override
	public IInteger integer(String integerValue) {
		return IntegerValue.newInteger(integerValue);
	}

	@Override
	public IInteger integer(int value) {
		return IntegerValue.newInteger(value);
	}

	@Override
	public IInteger integer(long value) {
		return IntegerValue.newInteger(value);
	}

	@Override
	public IInteger integer(byte[] integerData) {
		return IntegerValue.newInteger(integerData);
	}

	@Override
	public IRational rational(int a, int b) {
		return rational(integer(a), integer(b));
	}

	@Override
	public IRational rational(long a, long b) {
		return rational(integer(a), integer(b));
	}

	@Override
	public IRational rational(IInteger a, IInteger b) {
		return RationalValue.newRational(a, b);
	}

	@Override
	public IRational rational(String rat) throws NumberFormatException {
		if (rat.contains("r")) {
			String[] parts = rat.split("r");
			if (parts.length == 2) {
				return rational(integer(parts[0]), integer(parts[1]));
			}
			if (parts.length == 1) {
				return rational(integer(parts[0]), integer(1));
			}
			throw new NumberFormatException(rat);
		} else {
			return rational(integer(rat), integer(1));
		}
	}

	@Override
	public IReal real(String value) {
		return BigDecimalValue.newReal(value);
	}

	@Override
	public IReal real(String value, int precision) throws NumberFormatException {
		return BigDecimalValue.newReal(value, precision);
	}

	@Override
	public IReal real(double value) {
		return BigDecimalValue.newReal(value);
	}

	@Override
	public IReal real(double value, int precision) {
		return BigDecimalValue.newReal(value, precision);
	}

	@Override
	public int getPrecision() {
		return currentPrecision.get();
	}

	@Override
	public int setPrecision(int p) {
		return currentPrecision.getAndSet(p);
	}

	@Override
	public IReal pi(int precision) {
		return BigDecimalValue.pi(precision);
	}

	@Override
	public IReal e(int precision) {
		return BigDecimalValue.e(precision);
	}

	@Override
	public IString string(String value) {
		return StringValue.newString(value);
	}

	@Override
	public IString string(int[] chars) {
		StringBuilder b = new StringBuilder(chars.length);
		for (int ch : chars) {
			b.appendCodePoint(ch);
		}
		return string(b.toString());
	}

	@Override
	public IString string(int ch) {
		StringBuilder b = new StringBuilder(1);
		b.appendCodePoint(ch);
		return string(b.toString());
	}

	@Override
	public IBool bool(boolean value) {
		return BoolValue.getBoolValue(value);
	}

	@Override
	public IDateTime date(int year, int month, int day) {
		return DateTimeValues.newDate(year, month, day);
	}

	@Override
	public IDateTime time(int hour, int minute, int second, int millisecond) {
		return DateTimeValues.newTime(hour, minute, second, millisecond);
	}

	@Override
	public IDateTime time(int hour, int minute, int second, int millisecond,
						  int hourOffset, int minuteOffset) {
		return DateTimeValues.newTime(hour, minute, second, millisecond, hourOffset, minuteOffset);
	}

	@Override
	public IDateTime datetime(int year, int month, int day, int hour,
							  int minute, int second, int millisecond) {
		return DateTimeValues.newDateTime(year, month, day, hour, minute, second, millisecond);
	}

	@Override
	public IDateTime datetime(int year, int month, int day, int hour,
							  int minute, int second, int millisecond, int hourOffset,
							  int minuteOffset) {
		return DateTimeValues.newDateTime(year, month, day, hour, minute, second, millisecond, hourOffset, minuteOffset);
	}

	@Override
	public IDateTime datetime(long instant) {
		return DateTimeValues.newDateTime(instant);
	}
	
	@Override
	public IDateTime datetime(long instant, int timezoneHours, int timezoneMinutes) {
		return DateTimeValues.newDateTime(instant, timezoneHours, timezoneMinutes);
	}

	@Override
	public ISourceLocation sourceLocation(URI uri, int offset, int length) {
		return sourceLocation(sourceLocation(uri), offset, length);
	}
	
	@Override
	public ISourceLocation sourceLocation(ISourceLocation loc, int offset, int length) {
		return SourceLocationValues.newSourceLocation(loc, offset, length);	
	}

	@Override
	public ISourceLocation sourceLocation(URI uri, int offset, int length, int beginLine, int endLine, int beginCol, int endCol) {
		return sourceLocation(sourceLocation(uri), offset, length, beginLine, endLine, beginCol, endCol);
	}
	@Override
	public ISourceLocation sourceLocation(ISourceLocation loc, int offset, int length, int beginLine, int endLine, int beginCol, int endCol) {
		return SourceLocationValues.newSourceLocation(loc, offset, length, beginLine, endLine, beginCol, endCol);
	}

	@Override
	public ISourceLocation sourceLocation(String path, int offset, int length, int beginLine, int endLine, int beginCol, int endCol) {
		return sourceLocation(sourceLocation(path), offset, length, beginLine, endLine, beginCol, endCol);
	}

	@Override
	public ISourceLocation sourceLocation(URI uri) {
		try {
			return SourceLocationValues.newSourceLocation(uri);
		} 
		catch (URISyntaxException e) {
			throw new RuntimeException("An URI should always be a correct URI", e);
		}
	}

	@Override
	public ISourceLocation sourceLocation(String path) {
		if (!path.startsWith("/"))
			path = "/" + path;
		try {
			return sourceLocation("file", "", path);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Paths should not cause a incorrect syntax exception", e);
		}
	}
	
	@Override
	public ISourceLocation sourceLocation(String scheme, String authority, String path) throws URISyntaxException {
		return sourceLocation(scheme, authority, path, null, null);
	}
	
	@Override
	public ISourceLocation sourceLocation(String scheme, String authority,
			String path, @Nullable String query, @Nullable String fragment) throws URISyntaxException {
		return SourceLocationValues.newSourceLocation(scheme, authority, path, query, fragment);
	}
}
