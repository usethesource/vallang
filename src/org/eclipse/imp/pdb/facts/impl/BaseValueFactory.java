/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*    Davy Landman - added PI & E constants

*******************************************************************************/

package org.eclipse.imp.pdb.facts.impl;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IDateTime;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IRational;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactParseError;

public abstract class BaseValueFactory implements IValueFactory {
	protected final static int DEFAULT_PRECISION = 10;
	protected AtomicInteger currentPrecision = new AtomicInteger(DEFAULT_PRECISION);
	
    public IInteger integer(int i) {
        return new IntegerValue(i);
    }
    
    public IInteger integer(long l) {
    	return new IntegerValue(l);
    }
    
    public IInteger integer(byte[] a) {
    	return new IntegerValue(a);
    }
    
    public IInteger integer(String s) {
    	return new IntegerValue(new BigInteger(s));
    }
    
	public IRational rational(int a, int b) {
		return rational(integer(a), integer(b));
	}

	public IRational rational(long a, long b) {
		return rational(integer(a), integer(b));
	}

	public IRational rational(IInteger a, IInteger b) {
		return new RationalValue(a, b);
	}

	public IRational rational(String rat) throws NumberFormatException {
		if(rat.contains("r")) {
			String[] parts = rat.split("r");
			if (parts.length == 2) {
				return rational(integer(parts[0]), integer(parts[1]));
			}
			if (parts.length == 1) {
				return rational(integer(parts[0]), integer(1));
			}
			throw new NumberFormatException(rat);
		}
		else {
			return rational(integer(rat), integer(1));
		}
	}

    public IReal real(double d) {
        return new RealValue(d);
    }
    
    public IReal real(double d, int p) {
    	return new RealValue(d, p);
    }
    
    public IReal real(float f) {
    	return new RealValue(f);
    }
    
    public IReal real(float f, int p) {
    	return new RealValue(f, p);
    }
    
    public IReal real(String s) throws NumberFormatException {
    	return new RealValue(s);
    }

    public IReal real(String s, int p) throws NumberFormatException {
    	return new RealValue(s, p);
    }

    public int getPrecision() {
      return currentPrecision.get();
    }

    public int setPrecision(int p) {
    	return currentPrecision.getAndSet(p);
    }

    public IReal pi(int precision) {
    	return RealValue.pi(precision);
    }
    
    public IReal e(int precision) {
    	return RealValue.e(precision);
    }
    
    public IString string(String s) {
    	if (s == null) {
    		throw new NullPointerException();
    	}
        return new StringValue(s);
    }
    
    public ISourceLocation sourceLocation(URI uri, int offset, int length) {
		if (offset < 0) throw new IllegalArgumentException("offset should be positive");
		if (length < 0) throw new IllegalArgumentException("length should be positive");
	
		if (offset < Byte.MAX_VALUE && length < Byte.MAX_VALUE) {
			return new SourceLocationValues.ByteByte(uri, (byte) offset, (byte) length);
		}
		
		if (offset < Character.MAX_VALUE && length < Character.MAX_VALUE) {
			return new SourceLocationValues.CharChar(uri, (char) offset, (char) length);
		}
		
		return new SourceLocationValues.IntInt(uri, offset, length);
	} 
	
	public ISourceLocation sourceLocation(URI uri, int offset, int length, int beginLine, int endLine, int beginCol, int endCol) {
		if (offset < 0) throw new IllegalArgumentException("offset should be positive");
		if (length < 0) throw new IllegalArgumentException("length should be positive");
		if (beginLine < 0) throw new IllegalArgumentException("beginLine should be positive");
		if (beginCol < 0) throw new IllegalArgumentException("beginCol should be positive");
		if (endCol < 0) throw new IllegalArgumentException("endCol should be positive");
		if (endLine < beginLine) throw new IllegalArgumentException("endLine should be larger than or equal to beginLine");
		if (endLine == beginLine && endCol < beginCol) throw new IllegalArgumentException("endCol should be larger than or equal to beginCol, if on the same line");
		
		if (offset < Character.MAX_VALUE 
				&& length < Character.MAX_VALUE 
				&& beginLine < Byte.MAX_VALUE 
				&& endLine < Byte.MAX_VALUE 
				&& beginCol < Byte.MAX_VALUE 
				&& endCol < Byte.MAX_VALUE) {
			return new SourceLocationValues.CharCharByteByteByteByte(uri, (char) offset, (char) length, (byte) beginLine, (byte) endLine, (byte) beginCol, (byte) endCol);
		}
		else if (offset < Character.MAX_VALUE 
				&& length < Character.MAX_VALUE 
				&& beginLine < Character.MAX_VALUE 
				&& endLine < Character.MAX_VALUE 
				&& beginCol < Character.MAX_VALUE 
				&& endCol < Character.MAX_VALUE) {
			return new SourceLocationValues.CharCharCharCharCharChar(uri, (char) offset, (char) length, (char) beginLine, (char) endLine, (char) beginCol, (char) endCol);
		}
		else if (beginLine < Character.MAX_VALUE 
				&& endLine < Character.MAX_VALUE 
				&& beginCol < Byte.MAX_VALUE 
				&& endCol < Byte.MAX_VALUE) {
			return new SourceLocationValues.IntIntCharCharByteByte(uri, offset, length, (char) beginLine, (char) endLine, (byte) beginCol, (byte) endCol);
		} 
		else if (beginCol < Byte.MAX_VALUE 
				&& endCol < Byte.MAX_VALUE) {
			return new SourceLocationValues.IntIntIntIntByteByte(uri,  offset, length, beginLine, endLine, (byte) beginCol, (byte) endCol);
		}
		
		return new SourceLocationValues.IntIntIntIntIntInt(uri, offset, length, beginLine, endLine, beginCol, endCol);
	}
	
	public ISourceLocation sourceLocation(String path, int offset, int length, int beginLine, int endLine, int beginCol, int endCol){
    	try{
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			return sourceLocation(new URI("file", "", path, null), offset, length, beginLine, endLine, beginCol, endCol);
		}catch(URISyntaxException e){
			throw new FactParseError("Illegal path syntax: " + path, e);
		}
    }
	
	public ISourceLocation sourceLocation(URI uri){
		return new SourceLocationValues.OnlyURI(uri); 
	}
	
	public ISourceLocation sourceLocation(String path){
		try {
			if (!path.startsWith("/"))
				path = "/" + path;
			return sourceLocation(new URI("file", "", path, null));
		} catch (URISyntaxException e) {
			throw new FactParseError("Illegal path syntax: " + path, e);
		}
	}
    public IBool bool(boolean value) {
      return BoolValue.getBoolValue(value);
    }
    
	public IDateTime date(int year, int month, int day) {
		return new DateTimeValues.DateValue(year, month, day);
	}

	public IDateTime time(int hour, int minute, int second, int millisecond) {
		return new DateTimeValues.TimeValue(hour,minute,second,millisecond);
	}

	public IDateTime time(int hour, int minute, int second, int millisecond,
			int hourOffset, int minuteOffset) {
		return new DateTimeValues.TimeValue(hour,minute,second,millisecond,hourOffset,minuteOffset);
	}
	
	public IDateTime datetime(int year, int month, int day, int hour,
			int minute, int second, int millisecond) {
		return new DateTimeValues.DateTimeValue(year,month,day,hour,minute,second,millisecond);
	}

	public IDateTime datetime(int year, int month, int day, int hour,
			int minute, int second, int millisecond, int hourOffset,
			int minuteOffset) {
		return new DateTimeValues.DateTimeValue(year,month,day,hour,minute,second,millisecond,hourOffset,minuteOffset);
	}

	public IDateTime datetime(long instant) {
		return new DateTimeValues.DateTimeValue(instant);
	}
		
}
