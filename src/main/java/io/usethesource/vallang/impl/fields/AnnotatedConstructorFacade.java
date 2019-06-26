/*******************************************************************************
 * Copyright (c) 2013-2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI  
 *******************************************************************************/
package io.usethesource.vallang.impl.fields;

import java.util.Iterator;

import io.usethesource.capsule.Map;
import io.usethesource.vallang.IAnnotatable;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IWithKeywordParameters;
import io.usethesource.vallang.io.StandardTextWriter;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeStore;
import io.usethesource.vallang.visitors.IValueVisitor;

public class AnnotatedConstructorFacade implements IConstructor {

	protected final IConstructor content;
	protected final Map.Immutable<String, IValue> annotations;
	
	public AnnotatedConstructorFacade(final IConstructor content, final Map.Immutable<String, IValue> annotations) {
		this.content = content;
		this.annotations = annotations;
	}
	
	@SuppressWarnings("deprecation")
    @Override
	public INode setChildren(IValue[] childArray) {
	    return content.setChildren(childArray).asAnnotatable().setAnnotations(annotations);
	}
	
	@Override
	public <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
		return v.visitConstructor(this);
	}

	@Override
	public Type getType() {
		return content.getType();
	}

	@Override
	public IValue get(int i) throws IndexOutOfBoundsException {
		return content.get(i);
	}

	@Override
	public Type getConstructorType() {
		return content.getConstructorType();
	}
	
	@Override
	public Type getUninstantiatedConstructorType() {
		return content.getUninstantiatedConstructorType();
	}

	@Override
	public IValue get(String label) {
		return content.get(label);
	}

	@Override
	public IConstructor set(String label, IValue newChild) {
		IConstructor newContent = content.set(label, newChild);
		return new AnnotatedConstructorFacade(newContent, annotations);	// TODO: introduce wrap() here as well			
	}

	@Override
	public int arity() {
		return content.arity();
	}

	@Override
	public boolean has(String label) {
		return content.has(label);
	}

	@Override
	public String toString() {
		return StandardTextWriter.valueToString(this);
	}

	@Override
	public IConstructor set(int index, IValue newChild) {
		IConstructor newContent = content.set(index, newChild);
		return new AnnotatedConstructorFacade(newContent, annotations);	// TODO: introduce wrap() here as well		
	}

	@Override
	public String getName() {
		return content.getName();
	}

	@Override
	public Iterable<IValue> getChildren() {
		return content.getChildren();
	}

	@Override
	public Iterator<IValue> iterator() {
		return content.iterator();
	}

	@Override
	public INode replace(int first, int second, int end, IList repl) {
		return content.replace(first, second, end, repl);
	}

	@Override
	public Type getChildrenTypes() {
		return content.getChildrenTypes();
	}

	@Override
	public boolean declaresAnnotation(TypeStore store, String label) {
		return content.declaresAnnotation(store, label);
	}

	@Override
	public boolean equals(Object o) {
		if(o == this) return true;
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			AnnotatedConstructorFacade other = (AnnotatedConstructorFacade) o;
		
			return content.equals(other.content) &&
					annotations.equals(other.annotations);
		}
		
		return false;
	}

	@Override
	public boolean isEqual(IValue other) {
		return content.isEqual(other);
	}
	
	@Override
	public boolean match(IValue other) {
        return content.match(other);
    }
	
	@Override
	public int hashCode() {
		return content.hashCode();
	}

	@Override
	@Deprecated
	public boolean isAnnotatable() {
		return true;
	}
	
	@Override
	public IAnnotatable<? extends IConstructor> asAnnotatable() {
		return new AbstractDefaultAnnotatable<IConstructor>(content, annotations) {

			@Override
			protected IConstructor wrap(IConstructor content,
					Map.Immutable<String, IValue> annotations) {
				return annotations.isEmpty() ? content : new AnnotatedConstructorFacade(content, annotations);
			}
		};
	}
	
	@Override
	public boolean mayHaveKeywordParameters() {
	  return false;
	}

	@Override
	public IWithKeywordParameters<IConstructor> asWithKeywordParameters() {
	  throw new UnsupportedOperationException("can not add keyword parameters to a constructor (" + content.getType().toString() + ") which already has annotations");
	}
}
