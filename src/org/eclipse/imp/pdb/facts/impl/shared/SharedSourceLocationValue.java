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

import java.net.URI;

import org.eclipse.imp.pdb.facts.impl.fast.SourceLocationValues;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;

/**
 * Implementation of shareable source locations.
 * 
 * @author Arnold Lankamp
 */
public class SharedSourceLocationValue extends SourceLocationValues.Largest implements IShareable{
	
	protected SharedSourceLocationValue(URI uri, int offset, int length, int beginLine, int endLine, int beginCol, int endCol){
		super(uri, offset, length, beginLine, endLine, beginCol, endCol);
	}
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
}
