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
package org.eclipse.imp.pdb.facts.impl.fast;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;

/**
 * Specialized implementation for constructors with annotations.
 * 
 * @author Arnold Lankamp
 */
/*package*/ class AnnotatedConstructor extends AnnotatedConstructorBase {

	public static Constructor createAnnotatedConstructor(Type constructorType, IValue[] children, ShareableHashMap<String, IValue> annotations) {
		int aSize = annotations.size();
		if (1 == aSize) {
			String keyOne = (String)(annotations.keySet().toArray())[0];
			return new AnnotatedConstructorOne(constructorType, children, keyOne, annotations.get(keyOne));
		} else if (2 == aSize) {
			Object[] anns = annotations.keySet().toArray();
			return new AnnotatedConstructorTwo(constructorType, children, (String)anns[0], annotations.get((String)anns[0]), (String)anns[1], annotations.get((String)anns[1]));
		} else if (3 == aSize) {
			Object[] anns = annotations.keySet().toArray();
			return new AnnotatedConstructorThree(constructorType, children, (String)anns[0], annotations.get((String)anns[0]), (String)anns[1], annotations.get((String)anns[1]), (String)anns[2], annotations.get((String)anns[2]));
		} else {
			return new AnnotatedConstructor(constructorType, children, annotations);
		}
	}

	protected final ShareableHashMap<String, IValue> annotations;
	
	/*package*/ AnnotatedConstructor(Type constructorType, IValue[] children, ShareableHashMap<String, IValue> annotations){
		super(constructorType, children);
		this.annotations = annotations;
	}

	public IConstructor set(int i, IValue arg){
		IValue[] newChildren = children.clone();
		newChildren[i] = arg;
		return new AnnotatedConstructor(constructorType, newChildren, annotations);
	}
	
	public boolean hasAnnotation(String label){
		return annotations.containsKey(label);
	}
	
	public IValue getAnnotation(String label){
		return annotations.get(label);
	}
	
	public Map<String, IValue> getAnnotations(){
		return new ShareableHashMap<>(annotations);
	}
	
	@Override
	public IConstructor set(String label, IValue newChild) {
		IValue[] newChildren = children.clone();
		newChildren[constructorType.getFieldIndex(label)] = newChild;
		return new AnnotatedConstructor(constructorType, newChildren, annotations);
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label, IValue value){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>(annotations);
		newAnnotations.put(label, value);
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>(annotations);
		newAnnotations.remove(label);
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(Map<String, IValue> newAnnos){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>(annotations);
		
		Iterator<Map.Entry<String, IValue>> newAnnosIterator = newAnnos.entrySet().iterator();
		while(newAnnosIterator.hasNext()){
			Map.Entry<String, IValue> entry = newAnnosIterator.next();
			String key = entry.getKey();
			IValue value = entry.getValue();
			
			newAnnotations.put(key, value);
		}
		
		return newAnnotations;
	}

	public boolean equals(Object o){
		if (super.equals(o)) {
			AnnotatedConstructor other = (AnnotatedConstructor) o;
			return annotations.equals(other.annotations);
		}
		return false;
	}
	
	// we specialize once more, for cases of 1, 2 and 3 constructors we have specialized sub-classes.
	// this saves quite a bit of memory
	private static class AnnotatedConstructorOne extends AnnotatedConstructorBase {
		
	    private String annotationLabelOne;
	    private IValue annotationValueOne;
	    
		protected AnnotatedConstructorOne(Type constructorType, IValue[] children, String annLabelOne, IValue annValueOne){
			super(constructorType, children);
			annotationLabelOne = annLabelOne;
			annotationValueOne = annValueOne;
		}

		public IConstructor set(int i, IValue arg){
			IValue[] newChildren = children.clone();
			newChildren[i] = arg;
			return new AnnotatedConstructorOne(constructorType, newChildren, annotationLabelOne, annotationValueOne);
		}
		
		@Override
		public IConstructor set(String label, IValue newChild) {
			IValue[] newChildren = children.clone();
			newChildren[constructorType.getFieldIndex(label)] = newChild;
			return new AnnotatedConstructorOne(constructorType, newChildren, annotationLabelOne, annotationValueOne);
	
		}
		
		public boolean hasAnnotation(String label){
			return annotationLabelOne.equals(label);
		}
		
		public IValue getAnnotation(String label){
			return annotationLabelOne.equals(label) ? annotationValueOne : null;
		}
		
		public Map<String, IValue> getAnnotations(){
			ShareableHashMap<String, IValue> shm = new ShareableHashMap<>();
			shm.put(annotationLabelOne, annotationValueOne);
			return shm;
		}
		
		protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label, IValue value){
			ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
			if (! annotationLabelOne.equals(label))
				newAnnotations.put(annotationLabelOne, annotationValueOne);
			newAnnotations.put(label, value);
			return newAnnotations;
		}
		
		protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label){
			ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
			if (! annotationLabelOne.equals(label))
				newAnnotations.put(annotationLabelOne, annotationValueOne);
			return newAnnotations;
		}
		
		protected ShareableHashMap<String, IValue> getUpdatedAnnotations(Map<String, IValue> newAnnos){
			ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
			boolean foundAnnotationLabelOne = false;
			
			Iterator<Map.Entry<String, IValue>> newAnnosIterator = newAnnos.entrySet().iterator();
			while(newAnnosIterator.hasNext()){
				Map.Entry<String, IValue> entry = newAnnosIterator.next();
				String key = entry.getKey();
				IValue value = entry.getValue();
				
				if (key.equals(annotationLabelOne)) foundAnnotationLabelOne = true;
				newAnnotations.put(key, value);
			}
			
			if (!foundAnnotationLabelOne) newAnnotations.put(annotationLabelOne, annotationValueOne);
			
			return newAnnotations;
		}

		public boolean equals(Object o){
			if (super.equals(o)) {
				AnnotatedConstructorOne other = (AnnotatedConstructorOne) o;
				return annotationLabelOne.equals(other.annotationLabelOne) && annotationValueOne.equals(other.annotationValueOne); 
			}
			return false;
		}
	}

	private static class AnnotatedConstructorTwo extends AnnotatedConstructorBase {
		
	    private String annotationLabelOne;
	    private IValue annotationValueOne;
	    private String annotationLabelTwo;
	    private IValue annotationValueTwo;
	    
		protected AnnotatedConstructorTwo(Type constructorType, IValue[] children, String annLabelOne, IValue annValueOne, String annLabelTwo, IValue annValueTwo){
			super(constructorType, children);
			annotationLabelOne = annLabelOne;
			annotationValueOne = annValueOne;
			annotationLabelTwo = annLabelTwo;
			annotationValueTwo = annValueTwo;
		}

		public IConstructor set(int i, IValue arg){
			IValue[] newChildren = children.clone();
			newChildren[i] = arg;
			return new AnnotatedConstructorTwo(constructorType, newChildren, annotationLabelOne, annotationValueOne, annotationLabelTwo, annotationValueTwo);
		}
		
		@Override
		public IConstructor set(String label, IValue newChild) {
			IValue[] newChildren = children.clone();
			newChildren[constructorType.getFieldIndex(label)] = newChild;
			return new AnnotatedConstructorTwo(constructorType, newChildren, annotationLabelOne, annotationValueOne, annotationLabelTwo, annotationValueTwo);
	
		}
		
		public boolean hasAnnotation(String label){
			return annotationLabelOne.equals(label) || annotationLabelTwo.equals(label);
		}
		
		public IValue getAnnotation(String label){
			return annotationLabelOne.equals(label) ? annotationValueOne : 
				( annotationLabelTwo.equals(label) ? annotationValueTwo : null);
		}
		
		public Map<String, IValue> getAnnotations(){
			ShareableHashMap<String, IValue> shm = new ShareableHashMap<>();
			shm.put(annotationLabelOne, annotationValueOne);
			shm.put(annotationLabelTwo, annotationValueTwo);
			return shm;
		}
		
		protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label, IValue value){
			ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
			if (! annotationLabelOne.equals(label))
				newAnnotations.put(annotationLabelOne, annotationValueOne);
			if (! annotationLabelTwo.equals(label))
				newAnnotations.put(annotationLabelTwo, annotationValueTwo);
			newAnnotations.put(label, value);
			return newAnnotations;
		}
		
		protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label){
			ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
			if (! annotationLabelOne.equals(label))
				newAnnotations.put(annotationLabelOne, annotationValueOne);
			if (! annotationLabelTwo.equals(label))
				newAnnotations.put(annotationLabelTwo, annotationValueTwo);
			return newAnnotations;
		}
		
		protected ShareableHashMap<String, IValue> getUpdatedAnnotations(Map<String, IValue> newAnnos){
			ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
			boolean foundAnnotationLabelOne = false;
			boolean foundAnnotationLabelTwo = false;
			
			Iterator<Map.Entry<String, IValue>> newAnnosIterator = newAnnos.entrySet().iterator();
			while(newAnnosIterator.hasNext()){
				Map.Entry<String, IValue> entry = newAnnosIterator.next();
				String key = entry.getKey();
				IValue value = entry.getValue();
				
				if (key.equals(annotationLabelOne)) foundAnnotationLabelOne = true;
				if (key.equals(annotationLabelTwo)) foundAnnotationLabelTwo = true;
				newAnnotations.put(key, value);
			}
			
			if (!foundAnnotationLabelOne) newAnnotations.put(annotationLabelOne, annotationValueOne);
			if (!foundAnnotationLabelTwo) newAnnotations.put(annotationLabelTwo, annotationValueTwo);
			
			return newAnnotations;
		}

		public boolean equals(Object o){
			if (super.equals(o)) {
				AnnotatedConstructorTwo other = (AnnotatedConstructorTwo) o;
				return annotationLabelOne.equals(other.annotationLabelOne) && annotationValueOne.equals(other.annotationValueOne)
						&& annotationLabelTwo.equals(other.annotationLabelTwo) && annotationValueTwo.equals(other.annotationValueTwo);
			}
			return false;
		}
	}

	private static class AnnotatedConstructorThree extends AnnotatedConstructorBase {
		
	    private String annotationLabelOne;
	    private IValue annotationValueOne;
	    private String annotationLabelTwo;
	    private IValue annotationValueTwo;
	    private String annotationLabelThree;
	    private IValue annotationValueThree;
	    
		protected AnnotatedConstructorThree(Type constructorType, IValue[] children, String annLabelOne, IValue annValueOne, String annLabelTwo, IValue annValueTwo, String annLabelThree, IValue annValueThree){
			super(constructorType, children);
			annotationLabelOne = annLabelOne;
			annotationValueOne = annValueOne;
			annotationLabelTwo = annLabelTwo;
			annotationValueTwo = annValueTwo;
			annotationLabelThree = annLabelThree;
			annotationValueThree = annValueThree;
		}

		public IConstructor set(int i, IValue arg){
			IValue[] newChildren = children.clone();
			newChildren[i] = arg;
			return new AnnotatedConstructorThree(constructorType, newChildren, annotationLabelOne, annotationValueOne, annotationLabelTwo, annotationValueTwo, annotationLabelThree, annotationValueThree);
		}
		
		@Override
		public IConstructor set(String label, IValue newChild) {
			IValue[] newChildren = children.clone();
			newChildren[constructorType.getFieldIndex(label)] = newChild;
			return new AnnotatedConstructorThree(constructorType, newChildren, annotationLabelOne, annotationValueOne, annotationLabelTwo, annotationValueTwo, annotationLabelThree, annotationValueThree);
		}
		
		public boolean hasAnnotation(String label){
			return annotationLabelOne.equals(label) || annotationLabelTwo.equals(label) || annotationLabelThree.equals(label);
		}
		
		public IValue getAnnotation(String label){
			return annotationLabelOne.equals(label) ? annotationValueOne : 
				( annotationLabelTwo.equals(label) ? annotationValueTwo : 
					( annotationLabelThree.equals(label) ? annotationValueThree : null));
		}
		
		public Map<String, IValue> getAnnotations(){
			ShareableHashMap<String, IValue> shm = new ShareableHashMap<>();
			shm.put(annotationLabelOne, annotationValueOne);
			shm.put(annotationLabelTwo, annotationValueTwo);
			shm.put(annotationLabelThree, annotationValueThree);
			return shm;
		}
		
		protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label, IValue value){
			ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
			if (! annotationLabelOne.equals(label))
				newAnnotations.put(annotationLabelOne, annotationValueOne);
			if (! annotationLabelTwo.equals(label))
				newAnnotations.put(annotationLabelTwo, annotationValueTwo);
			if (! annotationLabelThree.equals(label))
				newAnnotations.put(annotationLabelThree, annotationValueThree);
			newAnnotations.put(label, value);
			return newAnnotations;
		}
		
		protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label){
			ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
			if (! annotationLabelOne.equals(label))
				newAnnotations.put(annotationLabelOne, annotationValueOne);
			if (! annotationLabelTwo.equals(label))
				newAnnotations.put(annotationLabelTwo, annotationValueTwo);
			if (! annotationLabelThree.equals(label))
				newAnnotations.put(annotationLabelThree, annotationValueThree);
			return newAnnotations;
		}
		
		protected ShareableHashMap<String, IValue> getUpdatedAnnotations(Map<String, IValue> newAnnos){
			ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
			boolean foundAnnotationLabelOne = false;
			boolean foundAnnotationLabelTwo = false;
			boolean foundAnnotationLabelThree = false;
			
			Iterator<Map.Entry<String, IValue>> newAnnosIterator = newAnnos.entrySet().iterator();
			while(newAnnosIterator.hasNext()){
				Map.Entry<String, IValue> entry = newAnnosIterator.next();
				String key = entry.getKey();
				IValue value = entry.getValue();
				
				if (key.equals(annotationLabelOne)) foundAnnotationLabelOne = true;
				if (key.equals(annotationLabelTwo)) foundAnnotationLabelTwo = true;
				if (key.equals(annotationLabelThree)) foundAnnotationLabelThree = true;
				newAnnotations.put(key, value);
			}
			
			if (!foundAnnotationLabelOne) newAnnotations.put(annotationLabelOne, annotationValueOne);
			if (!foundAnnotationLabelTwo) newAnnotations.put(annotationLabelTwo, annotationValueTwo);
			if (!foundAnnotationLabelThree) newAnnotations.put(annotationLabelThree, annotationValueThree);
			
			return newAnnotations;
		}

		public boolean equals(Object o){
			if (super.equals(o)) {
				AnnotatedConstructorThree other = (AnnotatedConstructorThree) o;
				return annotationLabelOne.equals(other.annotationLabelOne) && annotationValueOne.equals(other.annotationValueOne)
						&& annotationLabelTwo.equals(other.annotationLabelTwo) && annotationValueTwo.equals(other.annotationValueTwo)
						&& annotationLabelThree.equals(other.annotationLabelThree) && annotationValueThree.equals(other.annotationValueThree);
			}
			return false;
		}
	}
}
