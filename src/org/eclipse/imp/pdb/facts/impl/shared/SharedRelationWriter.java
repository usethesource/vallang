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

import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.impl.fast.RelationWriter;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesHashSet;
import org.eclipse.imp.pdb.facts.type.Type;

/**
 * Relation writer for shareable relations.
 * 
 * @author Arnold Lankamp
 */
public class SharedRelationWriter extends RelationWriter{
	
	protected SharedRelationWriter(Type tupleType){
		super(tupleType);
	}
	
	protected SharedRelationWriter(){
		super();
	}
	
	protected SharedRelationWriter(Type tupleType, ShareableValuesHashSet data){
		super(tupleType, data);
	}
	
	public IRelation done(){
		if(constructedRelation == null) constructedRelation = SharedValueFactory.getInstance().buildRelation(new SharedRelation(tupleType, data));
		
		return constructedRelation;
	}
}
