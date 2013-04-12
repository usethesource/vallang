/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts.impl;

import java.nio.CharBuffer;

import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.impl.fast.ValueFactory;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/ class StringValue extends Value implements IString {
    private final String fValue;

    /*package*/ StringValue(String value) {
        super(TypeFactory.getInstance().stringType());
        if (value == null) {
            throw new IllegalArgumentException("Null string value");
        }
        fValue= value;
    }

	public String getValue() {
        return fValue;
    }

    public IString concat(IString other) {
    	return new StringValue(fValue.concat(other.getValue()));
    }
    
    public int compare(IString other) {
    	int result = fValue.compareTo(other.getValue());
    	return result < 0 ? -1 : (result > 0) ? 1 : 0;
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o == null) {
    		return false;
    	}
    	if (this == o) {
    		return true;
    	}
    	if (getClass() == o.getClass()) {
    		return ((StringValue) o).fValue.equals(fValue);
    	}
    	return false;
    }
    
    @Override
    public int hashCode() {
    	return fValue.hashCode();
    }
    
    public <T> T accept(IValueVisitor<T> v) throws VisitorException {
    	return v.visitString(this);
    }

	public IString reverse() {
		StringBuilder b = new StringBuilder(fValue);
		return new StringValue(b.reverse().toString());
	}

	public int length() {
		return fValue.codePointCount(0, fValue.length());
	}

	private int codePointAt(java.lang.String str, int i) {
		return str.codePointAt(str.offsetByCodePoints(0,i));
	}
	
	public IString substring(int start, int end) {
		 return new StringValue(fValue.substring(fValue.offsetByCodePoints(0, start),fValue.offsetByCodePoints(0, end)));
	}
	
	public IString substring(int start) {
		 return new StringValue(fValue.substring(fValue.offsetByCodePoints(0, start)));
	}

	public int charAt(int index) {
		return codePointAt(fValue, index);
	}
	
	private int nextCP(CharBuffer cbuf){
		int cp = Character.codePointAt(cbuf, 0); 
		if(cbuf.position() < cbuf.capacity()){
			cbuf.position(cbuf.position() + Character.charCount(cp));
		}
		return cp;
	}
	
	private void skipCP(CharBuffer cbuf){
		if(cbuf.hasRemaining()){
			int cp = Character.codePointAt(cbuf, 0); 
			cbuf.position(cbuf.position() + Character.charCount(cp));
		}
	}
	
	// REFERENCE
	public IString replace(int first, int second, int end, IString repl) {
		StringBuilder buffer = new StringBuilder();
	
		int valueLen = fValue.codePointCount(0, fValue.length());
		CharBuffer valueBuf;
		
		int replLen = repl.length();
		CharBuffer replBuf = CharBuffer.wrap(repl.getValue());
		
		int increment = Math.abs(second - first);
		if(first <= end){ 
			valueBuf = CharBuffer.wrap(fValue);
			int valueIndex = 0;
			// Before begin (from left to right)
			while(valueIndex < first){
				buffer.appendCodePoint(nextCP(valueBuf)); valueIndex++;
			}
			int replIndex = 0;
			boolean wrapped = false;
			// Between begin and end
			while(valueIndex < end){
				buffer.appendCodePoint(nextCP(replBuf)); replIndex++;
				if(replIndex == replLen){
					replBuf.position(0); replIndex = 0;
					wrapped = true;
				}
				skipCP(valueBuf); valueIndex++; //skip the replaced element
				for(int j = 1; j < increment && valueIndex < end; j++){
					buffer.appendCodePoint(nextCP(valueBuf)); valueIndex++;
				}
			}
			if(!wrapped){
				while(replIndex < replLen){
					buffer.appendCodePoint(nextCP(replBuf)); replIndex++;
				}
			}
			// After end
			
			while( valueIndex < valueLen){
				buffer.appendCodePoint(nextCP(valueBuf)); valueIndex++;
			}
		} else { 
			// Before begin (from right to left)
			
			// Place reversed value of fValue in valueBuffer for better sequential code point access
			// Also add code points to buffer in reverse order and reverse again at the end
			valueBuf = CharBuffer.wrap(new StringBuilder(fValue).reverse().toString());
			
			int valueIndex = valueLen - 1;
			while(valueIndex > first){
				buffer.appendCodePoint(nextCP(valueBuf));
				valueIndex--;
			}
			// Between begin (right) and end (left)
			int replIndex = 0;
			boolean wrapped = false;
			while(valueIndex > end){
				buffer.appendCodePoint(nextCP(replBuf)); replIndex++;
				if(replIndex == repl.length()){
					replBuf.position(0); replIndex = 0;
					wrapped = true;
				}
				skipCP(valueBuf); valueIndex--; //skip the replaced element
				for(int j = 1; j < increment && valueIndex > end; j++){
					buffer.appendCodePoint(nextCP(valueBuf)); valueIndex--;
				}
			}
			if(!wrapped){
				while(replIndex < replLen){
					buffer.appendCodePoint(nextCP(replBuf)); replIndex++;
				}
			}
			// Left of end
			while(valueIndex >= 0){
				buffer.appendCodePoint(nextCP(valueBuf)); valueIndex--;
			}
			buffer.reverse();
		}
		
		String res = buffer.toString();
		return ValueFactory.getInstance().string(res);
	}
}
