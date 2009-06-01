/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.impl.fast.BigDecimalValue;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;

/**
 * Implementation of shareable reals.
 * 
 * @author Arnold Lankamp
 */
public class SharedBigDecimalValue extends BigDecimalValue implements IShareable{
	
	public SharedBigDecimalValue(BigDecimal bigDecimal){
		super(bigDecimal);
	}
	
	public IInteger toInteger(){
		return SharedValueFactory.getInstance().integer(value.toBigInteger());
	}
	
	public IReal floor(){
		return SharedValueFactory.getInstance().real(value.setScale(0, RoundingMode.FLOOR));
	}
	
	public IReal round(){
		return SharedValueFactory.getInstance().real(value.setScale(0, RoundingMode.HALF_UP));
	}
	
	public IReal add(IReal other){
		return SharedValueFactory.getInstance().real(value.add(((SharedBigDecimalValue) other).value));
	}
	
	public IReal subtract(IReal other){
		return SharedValueFactory.getInstance().real(value.subtract(((SharedBigDecimalValue) other).value));
	}
	
	public IReal multiply(IReal other){
		return SharedValueFactory.getInstance().real(value.multiply(((SharedBigDecimalValue) other).value));
	}
	
	public IReal divide(IReal other, int precision){
		return SharedValueFactory.getInstance().real(value.divide(((SharedBigDecimalValue) other).value));
	}
	
	public IReal negate(){
		return SharedValueFactory.getInstance().real(value.negate());
	}
	
	public IInteger unscaled(){
		return SharedValueFactory.getInstance().integer(value.unscaledValue());
	}
	
	public IBool greater(IReal other){
		return SharedValueFactory.getInstance().bool(compare(other) > 0);
	}
	
	public IBool greaterEqual(IReal other){
		return SharedValueFactory.getInstance().bool(compare(other) >= 0);
	}
	
	public IBool less(IReal other){
		return SharedValueFactory.getInstance().bool(compare(other) < 0);
	}
	
	public IBool lessEqual(IReal other){
		return SharedValueFactory.getInstance().bool(compare(other) <= 0);
	}
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
}
