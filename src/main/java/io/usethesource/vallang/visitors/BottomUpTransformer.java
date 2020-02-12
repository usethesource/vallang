/*******************************************************************************
* Copyright (c) 2008 CWI.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    jurgen@vinju.org

*******************************************************************************/
package io.usethesource.vallang.visitors;

import java.util.Map.Entry;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IExternalValue;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;

/**
 * This visitor will apply another visitor in a bottom-up fashion to an IValue 
 *
 */
public class BottomUpTransformer<E extends Throwable> extends VisitorAdapter<IValue,E> {
	protected IValueFactory fFactory;

	public BottomUpTransformer(IValueVisitor<IValue,E> visitor, IValueFactory factory) {
		super(visitor);
		this.fFactory = factory;
	}
	
	@Override
	public IValue visitNode(INode o) throws E {
		for (int i = 0; i < o.arity(); i++) {
			o = o.set(i, o.get(i).accept(this));
		}
		
		return fVisitor.visitNode(o);
	}
	
	public IValue visitConstructor(IConstructor o) throws E {
		for (int i = 0; i < o.arity(); i++) {
			o = o.set(i, o.get(i).accept(this));
		}
		
		return fVisitor.visitConstructor(o);
	}
	
	@Override
	public IValue visitList(IList o) throws E {
		IListWriter w = fFactory.listWriter();
		for (IValue elem : o) {
			w.append(elem.accept(this));
		}
		
		return fVisitor.visitList(w.done());
	}
	
	@Override
	public IValue visitSet(ISet o) throws E {
		ISetWriter w = fFactory.setWriter();
		for (IValue elem : o) {
			w.insert(elem.accept(this));
		}
		
		return fVisitor.visitSet(w.done());
	}
	
	@Override
	public IValue visitMap(IMap o) throws E {
		IMapWriter w = fFactory.mapWriter();
		for (Entry<IValue, IValue> entry : (Iterable<Entry<IValue, IValue>>) () -> o.entryIterator()) {
		    w.put(entry.getKey().accept(this), entry.getValue().accept(this));
		}
		
		return fVisitor.visitMap(w.done());
	}

	@Override
	public IValue visitRelation(ISet o) throws E {
		ISetWriter w = fFactory.setWriter();
		
		for (IValue tuple : o) {
			w.insert((ITuple) tuple.accept(this));
		}
		
		return fVisitor.visitRelation(w.done());
	}
	
	@Override
	public IValue visitTuple(ITuple o) throws E {
		for (int i = 0; i < o.arity(); i++) {
			o.set(i, o.get(i).accept(this));
		}
		
		return fVisitor.visitTuple(o);
	}

	public IValue visitExternal(IExternalValue externalValue) throws E {
		return fVisitor.visitExternal(externalValue);
	}

	public IValue visitListRelation(IList o) throws E {
		IListWriter w = fFactory.listWriter();
		
		for (IValue tuple : o) {
			w.insert((ITuple) tuple.accept(this));
		}
		
		return fVisitor.visitListRelation(w.done());
	}
}
