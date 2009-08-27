/*******************************************************************************
 * Copyright (c) CWI 2009
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jurgen Vinju (jurgenv@cwi.nl) - initial API and implementation
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IExternalValue;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.TypeStore;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * This class implements the ATerm readable syntax for {@link IValue}'s.
 * See also {@link ATermReader}
 */
public class ATermWriter implements IValueWriter {
	public void write(IValue value, OutputStream stream) throws IOException {
		try {
			value.accept(new Writer(stream));
		} catch (VisitorException e) {
			throw (IOException) e.getCause();
		}
	}
	
	public void write(IValue value, OutputStream stream, TypeStore typeStore) throws IOException {
		write(value, stream);
	}
	
	private static class Writer implements IValueVisitor<IValue> {
		private OutputStream stream;

		public Writer(OutputStream stream) {
			this.stream = stream;
		}
		
		private void append(String string) throws VisitorException {
			try {
				stream.write(string.getBytes());
			} catch (IOException e) {
				throw new VisitorException(e);
			}
		}
		
		private void append(char c) throws VisitorException {
			try {
				stream.write(c);
			} catch (IOException e) {
				throw new VisitorException(e);
			}
		}
		
		public IValue visitBoolean(IBool boolValue)
				throws VisitorException {
			append(boolValue.getStringRepresentation());
			return boolValue;
		}

		public IValue visitConstructor(IConstructor o) throws VisitorException {
			return visitNode(o);
		}

		public IValue visitReal(IReal o) throws VisitorException {
			append(o.getStringRepresentation());
			return o;
		}

		public IValue visitInteger(IInteger o) throws VisitorException {
			append(o.getStringRepresentation());
			return o;
		}

		public IValue visitList(IList o) throws VisitorException {
			append('[');
			
			Iterator<IValue> listIterator = o.iterator();
			if(listIterator.hasNext()){
				append(listIterator.next().toString());
				
				while(listIterator.hasNext()){
					append(',');
					listIterator.next().accept(this);
				}
			}
			
			append(']');
			
			return o;
		}

		public IValue visitMap(IMap o) throws VisitorException {
			append('[');
		
			Iterator<IValue> mapIterator = o.iterator();
			if(mapIterator.hasNext()){
				append('(');
				IValue key = mapIterator.next();
				key.accept(this);
				append(',');
				o.get(key).accept(this);
				append(')');
				
				while(mapIterator.hasNext()){
					append(',');
					append('(');
					key = mapIterator.next();
					append(',');
					o.get(key).accept(this);
					append(')');
				}
			}
			
			append(']');
			
			return o;
		}

		public IValue visitNode(INode o) throws VisitorException {
			String name = o.getName();
			
			append(name);
			append('(');

			Iterator<IValue> it = o.iterator();
			while (it.hasNext()) {
				it.next().accept(this);
				if (it.hasNext()) {
					append(',');
				}
			}
			append(')');
			
			if (o.hasAnnotations()) {
				append("{[");
				int i = 0;
				Map<String, IValue> annotations = o.getAnnotations();
				for (String key : annotations.keySet()) {
					append("[" + key + ",");
					annotations.get(key).accept(this);
					append("]");
					
					if (++i < annotations.size()) {
						append(",");
					}
				}
				append("]}");
			}
			
			return o;
		}

		public IValue visitRelation(IRelation o) throws VisitorException {
			return visitSet(o);
		}

		public IValue visitSet(ISet o) throws VisitorException {
			append('[');
			
			Iterator<IValue> setIterator = o.iterator();
			if(setIterator.hasNext()){
				setIterator.next().accept(this);
				
				while(setIterator.hasNext()){
					append(",");
					setIterator.next().accept(this);
				}
			}
			
			append(']');
			return o;
		}

		public IValue visitSourceLocation(ISourceLocation o)
				throws VisitorException {
			append("loc(");
			append('\"');
			append(o.getURL().toExternalForm());
			append('\"');
			append("," + o.getOffset());
			append("," + o.getLength());
			append("," + o.getBeginLine());
			append("," + o.getBeginColumn());
			append("," + o.getEndLine());
			append("," + o.getEndColumn());
			append(')');
			return o;
		}

		public IValue visitString(IString o) throws VisitorException {
			// TODO optimize this implementation and finish all escapes
			append('\"');
		    append(o.getValue().replaceAll("\"", "\\\"").replaceAll("\n","\\\\n"));
		    append('\"');
		    return o;
		}

		public IValue visitTuple(ITuple o) throws VisitorException {
			 append('(');
			 
			 Iterator<IValue> it = o.iterator();
			 
			 if (it.hasNext()) {
				 it.next().accept(this);
			 }
			 
			 while (it.hasNext()) {
				 append(',');
				 it.next().accept(this);
			 }
			 append(')');
			 
			 return o;
		}

		public IValue visitExternal(IExternalValue externalValue) {
			// ignore external values
			return externalValue;
		}
	}

}
