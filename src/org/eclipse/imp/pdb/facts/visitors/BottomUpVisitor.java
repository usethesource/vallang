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
package org.eclipse.imp.pdb.facts.visitors;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IExternalValue;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;

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
