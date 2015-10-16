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
package io.usethesource.treasure.visitors;

import io.usethesource.treasure.IConstructor;
import io.usethesource.treasure.IExternalValue;
import io.usethesource.treasure.IList;
import io.usethesource.treasure.IListWriter;
import io.usethesource.treasure.IMap;
import io.usethesource.treasure.IMapWriter;
import io.usethesource.treasure.INode;
import io.usethesource.treasure.ISet;
import io.usethesource.treasure.ISetWriter;
import io.usethesource.treasure.ITuple;
import io.usethesource.treasure.IValue;
import io.usethesource.treasure.IValueFactory;
import io.usethesource.treasure.visitors.IValueVisitor;

/**
 * This visitor will apply another visitor in a bottom-up fashion to an IValue 
 *
 */
public class BottomUpVisitor<T, E extends Throwable> extends VisitorAdapter<T, E> {
	protected IValueFactory fFactory;

	public BottomUpVisitor(IValueVisitor<T, E> visitor, IValueFactory factory) {
		super(visitor);
		this.fFactory = factory;
	}
	
	@Override
	public T visitNode(INode o) throws E {
		for (int i = 0; i < o.arity(); i++) {
			o.get(i).accept(this);
		}
		
		return fVisitor.visitNode(o);
	}
	
	public T visitConstructor(IConstructor o) throws E {
		for (int i = 0; i < o.arity(); i++) {
			o.get(i).accept(this);
		}
		
		return fVisitor.visitConstructor(o);
	}
	
	@Override
	public T visitList(IList o) throws E {
		IListWriter w = fFactory.listWriter();
		for (IValue elem : o) {
			elem.accept(this);
		}
		
		return fVisitor.visitList(w.done());
	}
	
	@Override
	public T visitSet(ISet o) throws E {
		ISetWriter w = fFactory.setWriter();
		for (IValue elem : o) {
			elem.accept(this);
		}
		
		return fVisitor.visitSet(w.done());
	}
	
	@Override
	public T visitMap(IMap o) throws E {
		IMapWriter w = fFactory.mapWriter();
		for (IValue elem : o) {
			elem.accept(this);
			o.get(elem).accept(this);
		}
		
		return fVisitor.visitMap(w.done());
	}

	@Override
	public T visitRelation(ISet o) throws E {
		ISetWriter w = fFactory.setWriter();
		
		for (IValue tuple : o) {
			tuple.accept(this);
		}
		
		return fVisitor.visitRelation(w.done());
	}
	
	@Override
	public T visitTuple(ITuple o) throws E {
		for (int i = 0; i < o.arity(); i++) {
			o.get(i).accept(this);
		}
		
		return fVisitor.visitTuple(o);
	}

	public T visitExternal(IExternalValue externalValue) throws E {
		return fVisitor.visitExternal(externalValue);
	}

	public T visitListRelation(IList o) throws E {
		IListWriter w = fFactory.listWriter();
		
		for (IValue tuple : o) {
			tuple.accept(this);
		}
		
		return fVisitor.visitListRelation(w.done());
	}
}
