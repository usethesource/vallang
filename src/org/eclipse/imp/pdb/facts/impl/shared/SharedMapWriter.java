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

import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.impl.fast.MapWriter;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesHashMap;
import org.eclipse.imp.pdb.facts.type.Type;

/**
 * Map writer for shareable maps.
 * 
 * @author Arnold Lankamp
 */
public class SharedMapWriter extends MapWriter{
	
	protected SharedMapWriter(Type keyType, Type valueType){
		super(keyType, valueType);
	}
	
	protected SharedMapWriter(Type keyType, Type valueType, ShareableValuesHashMap data){
		super(keyType, valueType, data);
	}
	
	public IMap done(){
		if(constructedMap == null) constructedMap = SharedValueFactory.getInstance().buildMap(new SharedMap(keyType, valueType, data));
		
		return constructedMap;
	}
}
