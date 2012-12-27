/*******************************************************************************
* Copyright (c) 2009, 2012 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*    Anya Helene Bagge - labels
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.shared;

import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.impl.fast.MapWriter;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesHashMap;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

/**
 * Map writer for shareable maps.
 * 
 * @author Arnold Lankamp
 */
public class SharedMapWriter extends MapWriter{
	
	protected SharedMapWriter(Type mapType){
		super(mapType);
	}
	
	protected SharedMapWriter(){
		super();
	}
	
	protected SharedMapWriter(Type mapType, ShareableValuesHashMap data){
		super(mapType, data);
	}
	
	public IMap done(){
		if(constructedMap == null) {
			if (mapType == null) {
				mapType = TypeFactory.getInstance().mapType(keyType, valueType);
			}

			if (!data.isEmpty()) {
				constructedMap = SharedValueFactory.getInstance().buildMap(new SharedMap(mapType, data));
			}
			else {
				Type voidType = TypeFactory.getInstance().voidType();
				Type voidMapType = TypeFactory.getInstance().mapType(voidType, mapType.getKeyLabel(), voidType, mapType.getValueLabel());
				constructedMap = SharedValueFactory.getInstance().buildMap(new SharedMap(voidMapType, data));
			}
		}

		return constructedMap;
	}
}
