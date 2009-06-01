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

import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.impl.fast.StringValue;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;

/**
 * Implementation of shareable strings.
 * 
 * @author Arnold Lankamp
 */
public class SharedStringValue extends StringValue implements IShareable{
	
	protected SharedStringValue(String value){
		super(value);
	}
	
	public IString concat(IString other){
		StringBuilder buffer = new StringBuilder();
		buffer.append(value);
		buffer.append(other.getValue());
		
		return SharedValueFactory.getInstance().string(buffer.toString());
	}
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
}
