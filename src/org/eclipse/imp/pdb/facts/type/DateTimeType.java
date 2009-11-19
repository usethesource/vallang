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
package org.eclipse.imp.pdb.facts.type;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;

/**
 * @author mhills
 *
 */
public class DateTimeType extends Type {

    private final static DateTimeType sInstance= new DateTimeType();

    /*package*/ static DateTimeType getInstance() {
        return sInstance;
    }
    
	private DateTimeType() {
		super();
	}

	@Override
	public boolean isDateTimeType() {
		return true;
	}

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof DateTimeType);
    }

    @Override
    public int hashCode() {
        return 63097;
    }

	@Override
	public String toString() {
		return "datetime";
	}
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitDateTime(this);
	}

	@Override
	public IValue make(IValueFactory f, int year, int month, int day) {
		return f.date(year, month, day);
	}

	@Override
	public IValue make(IValueFactory f, int hour, int minute, int second,
			int millisecond) {
		return f.time(hour, minute, second, millisecond);
	}
	
	@Override
	public IValue make(IValueFactory f, int hour, int minute, int second,
			int millisecond, int hourOffset, int minuteOffset) {
		return f.time(hour, minute, second, millisecond, hourOffset, minuteOffset);
	}
	
	@Override
	public IValue make(IValueFactory f, int year, int month, int day, int hour,
			int minute, int second, int millisecond) {
		return f.datetime(year, month, day, hour, minute, second, millisecond);
	}
	
	@Override
	public IValue make(IValueFactory f, int year, int month, int day, int hour,
			int minute, int second, int millisecond, int hourOffset, int minuteOffset) {
		return f.datetime(year, month, day, hour, minute, second, millisecond, hourOffset, minuteOffset);
	}
	
	@Override
	public IValue make(IValueFactory f, long instant) {
		return f.datetime(instant);
	}

	@Override
	public IValue make(IValueFactory f, int instant) {
		// NOTE: We override this as well, since longs in the int range
		// are also possible datetime values.
		return f.datetime(instant);
	}
	
	
}
