package com.oracle.truffle.object;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;

public class PropertyMockObject extends Property {

	private int value;

	PropertyMockObject(int value) {
		this.value = value;
	}
	
	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (other == this) {
			return true;
		}

		if (other instanceof PropertyMockObject) {
			int otherValue = ((PropertyMockObject) other).value;

			return value == otherValue;
		}
		return false;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public Object getKey() {
		return String.valueOf(value);
	}

	@Override
	public int getFlags() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Property relocate(Location newLocation) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object get(DynamicObject store, Shape shape) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object get(DynamicObject store, boolean condition) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void set(DynamicObject store, Object value, Shape shape)
			throws IncompatibleLocationException, FinalLocationException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setGeneric(DynamicObject store, Object value, Shape shape) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSafe(DynamicObject store, Object value, Shape shape) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setInternal(DynamicObject store, Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void set(DynamicObject store, Object value, Shape oldShape, Shape newShape)
			throws IncompatibleLocationException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setGeneric(DynamicObject store, Object value, Shape oldShape, Shape newShape) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSafe(DynamicObject store, Object value, Shape oldShape, Shape newShape) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSame(Property other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Location getLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isHidden() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isShadow() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Property copyWithFlags(int newFlags) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Property copyWithRelocatable(boolean newRelocatable) {
		// TODO Auto-generated method stub
		return null;
	}

}