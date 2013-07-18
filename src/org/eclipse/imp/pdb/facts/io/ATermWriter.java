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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IDateTime;
import org.eclipse.imp.pdb.facts.IExternalValue;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IRational;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.TypeStore;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;

/**
 * This class implements the ATerm readable syntax for {@link IValue}'s.
 * See also {@link ATermReader}
 */
public class ATermWriter implements IValueTextWriter {
	public void write(IValue value, java.io.Writer stream) throws IOException {
	  value.accept(new Writer(stream));
	}
	
	public void write(IValue value, java.io.Writer stream, TypeStore typeStore) throws IOException {
		write(value, stream);
	}
	
	private static class Writer implements IValueVisitor<IValue, IOException> {
		private java.io.Writer stream;

		public Writer(java.io.Writer stream) {
			this.stream = stream;
		}
		
		private void append(String string) throws IOException {
		  stream.write(string);
		}
		
		private void append(char c) throws IOException {
		  stream.write(c);
		}
		
		public IValue visitBoolean(IBool boolValue) throws IOException {
			append(boolValue.getStringRepresentation());
			return boolValue;
		}

		public IValue visitConstructor(IConstructor o) throws IOException {
			return visitNode(o);
		}

		public IValue visitReal(IReal o) throws IOException {
			append(o.getStringRepresentation());
			return o;
		}

		public IValue visitInteger(IInteger o) throws IOException {
			append(o.getStringRepresentation());
			return o;
		}

		// TODO: There probably isn't a good ATerm repr of rationals,
		// what should we do here?
		public IValue visitRational(IRational o) throws IOException {
			append("rat");
			append('(');
			append(o.numerator().getStringRepresentation());
			append(',');
			append(o.denominator().getStringRepresentation());
			append(')');
			return o;
		}
		
		public IValue visitList(IList o) throws IOException {
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

		public IValue visitMap(IMap o) throws IOException {
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

		public IValue visitNode(INode o) throws IOException {
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
			
			if (o.asAnnotatable().hasAnnotations()) {
				append("{[");
				int i = 0;
				Map<String, IValue> annotations = o.asAnnotatable().getAnnotations();
				for (Entry<String, IValue> entry : annotations.entrySet()) {
					append("[" + entry.getKey() + ",");
					entry.getValue().accept(this);
					append("]");
					
					if (++i < annotations.size()) {
						append(",");
					}
				}
				append("]}");
			}
			
			return o;
		}

		public IValue visitRelation(ISet o) throws IOException {
			return visitSet(o);
		}

		public IValue visitSet(ISet o) throws IOException {
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
				throws IOException {
			append("loc(");
			append('\"');
			append(o.getURI().toString());
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

		public IValue visitString(IString o) throws IOException {
			// TODO optimize this implementation and finish all escapes
			append('\"');
		    append(o.getValue().replaceAll("\"", "\\\"").replaceAll("\n","\\\\n"));
		    append('\"');
		    return o;
		}

		public IValue visitTuple(ITuple o) throws IOException {
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

		public IValue visitDateTime(IDateTime o) throws IOException {
			append("$");
			if (o.isDate()) {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				append(df.format(new Date(o.getInstant())));
			} else if (o.isTime()) {
				SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss.SSSZZZ");
				append("T");
				append(df.format(new Date(o.getInstant())));
			} else {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZ");
				append(df.format(new Date(o.getInstant())));
			}
			return o;
		}

		public IValue visitListRelation(IList o)
				throws IOException {
			visitList(o);
			return o;
		}
	}

}
