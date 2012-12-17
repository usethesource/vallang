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

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.impl.fast.ListWriter;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesList;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

/**
 * List writer for shareable lists.
 * 
 * @author Arnold Lankamp
 */
public class SharedListWriter extends ListWriter{
	
	protected SharedListWriter(Type elementType){
		super(elementType);
	}
	
	protected SharedListWriter(){
		super();
	}
	
	protected SharedListWriter(Type elementType, ShareableValuesList data){
		super(elementType, data);
	}
	
	public IList done(){
		if(constructedList == null) {
		  constructedList = SharedValueFactory.getInstance().buildList(new SharedList(data.isEmpty() ? TypeFactory.getInstance().voidType() : elementType, data));
		}
		
		return constructedList;
	}
}
