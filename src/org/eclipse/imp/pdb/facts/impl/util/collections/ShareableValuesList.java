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
package org.eclipse.imp.pdb.facts.impl.util.collections;

import java.util.Iterator;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;
import org.eclipse.imp.pdb.facts.util.ShareableList;

/**
 * A specialized version of the ShareableList, specifically meant for storing values.
 * 
 * @author Arnold Lankamp
 */
public class ShareableValuesList extends ShareableList<IValue>{

	public static ShareableValuesList newShareableValuesList(){
		return new ShareableValuesList().intern();
	}		
	
	private ShareableValuesList(){
		super();
	}

	public static ShareableValuesList newShareableValuesList(ShareableValuesList shareableValuesList){
		return new ShareableValuesList(shareableValuesList).intern();
	}	
	
	private ShareableValuesList(ShareableValuesList shareableValuesList){
		super(shareableValuesList);
	}
	
	public static ShareableValuesList newShareableValuesList(ShareableValuesList shareableValuesList, int offset, int length){
		return new ShareableValuesList(shareableValuesList, offset, length).intern();
	}	
	
	private ShareableValuesList(ShareableValuesList shareableValuesList, int offset, int length){
		super(shareableValuesList, offset, length);
	}
	
	public ShareableValuesList intern() {
		return (ShareableValuesList) IShareable.intern(this);
	}		
	
	public boolean isEqual(ShareableValuesList otherShareableValuesList){
		if(otherShareableValuesList == this) return true;
		if(otherShareableValuesList == null) return false;
		if(otherShareableValuesList.size() != size()) return false;
		
		if(otherShareableValuesList.isEmpty()) return true;
		
		Iterator<IValue> thisListIterator = iterator();
		Iterator<IValue> otherListIterator = otherShareableValuesList.iterator();
		while(thisListIterator.hasNext()){
			IValue thisValue = thisListIterator.next();
			IValue otherValue = otherListIterator.next();
			if(!thisValue.isEqual(otherValue)){
				return false;
			}
		}
		
		return true;
	}
	
	public boolean contains(IValue value){
		Iterator<IValue> valuesIterator = iterator();
		while(valuesIterator.hasNext()){
			IValue next = valuesIterator.next();
			if(next.isEqual(value)) return true;
		}
		
		return false;
	}
	
	public boolean remove(IValue value){
		int index = 0;
		Iterator<IValue> valuesIterator = iterator();
		while(valuesIterator.hasNext()){
			IValue next = valuesIterator.next();
			if(next.isEqual(value)) break;
			
			index++;
		}
		
		if(index < size()){
			remove(index);
			return true;
		}
		
		return false;
	}
	
	public ShareableValuesList subList(int offset, int length){
		if(offset < 0) throw new IndexOutOfBoundsException("Offset may not be smaller then 0.");
		if(length < 0) throw new IndexOutOfBoundsException("Length may not be smaller then 0.");
		if((offset + length) > size()) throw new IndexOutOfBoundsException("'offset + length' may not be larger then 'list.size()'");
		
		return newShareableValuesList(this, offset, length);
	}
}
