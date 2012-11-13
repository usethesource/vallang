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
package org.eclipse.imp.pdb.facts.impl.shared;

import org.eclipse.imp.pdb.facts.impl.DateTimeValues.DateTimeValue;
import org.eclipse.imp.pdb.facts.impl.DateTimeValues.DateValue;
import org.eclipse.imp.pdb.facts.impl.DateTimeValues.TimeValue;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;

public class SharedDateTimeValues {

	public static class SharedDateValue extends DateValue {

		public SharedDateValue(int year, int month, int day) {
			super(year, month, day);
		}

		public boolean equivalent(IShareable shareable){
			return super.equals(shareable);
		}
		
		public boolean equals(Object o){
			return (this == o);
		}
	}
	
	public static class SharedTimeValue extends TimeValue {
		
		public SharedTimeValue(int hour, int minute, int second, int millisecond) {
			super(hour, minute, second, millisecond);
		}

		public SharedTimeValue(int hour, int minute, int second, int millisecond, int hourOffset, int minuteOffset) {
			super(hour, minute, second, millisecond, hourOffset, minuteOffset);
		}

		public boolean equivalent(IShareable shareable){
			return super.equals(shareable);
		}
		
		public boolean equals(Object o){
			return (this == o);
		}
	}

	public static class SharedDateTimeValue extends DateTimeValue {
		
		public SharedDateTimeValue(int year, int month, int day, int hour, int minute, int second, int millisecond) {
			super(year, month, day, hour, minute, second, millisecond);
		}

		public SharedDateTimeValue(int year, int month, int day, int hour, int minute, int second, int millisecond, int hourOffset, int minuteOffset) {
			super(year, month, day, hour, minute, second, millisecond, hourOffset, minuteOffset);
		}

		public SharedDateTimeValue(long instant) {
			super(instant);
		}

		public boolean equivalent(IShareable shareable){
			return super.equals(shareable);
		}
		
		public boolean equals(Object o){
			return (this == o);
		}
	}
	
}
