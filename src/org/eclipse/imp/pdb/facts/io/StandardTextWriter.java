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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IDateTime;
import org.eclipse.imp.pdb.facts.IExternalValue;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.INode;
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
 * This class implements the standard readable syntax for {@link IValue}'s.
 * See also {@link StandardTextReader}
 */
public class StandardTextWriter implements IValueWriter {
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
			append(boolValue.getValue() ? "true" : "false");
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
				listIterator.next().accept(this);
				
				while(listIterator.hasNext()){
					append(',');
					listIterator.next().accept(this);
				}
			}
			
			append(']');
			
			return o;
		}

		public IValue visitMap(IMap o) throws VisitorException {
			append('(');
			
			Iterator<IValue> mapIterator = o.iterator();
			if(mapIterator.hasNext()){
				IValue key = mapIterator.next();
				key.accept(this);
				append(':');
				o.get(key).accept(this);
				
				while(mapIterator.hasNext()){
					append(',');
					key = mapIterator.next();
					key.accept(this);
					append(':');
					o.get(key).accept(this);
				}
			}
			
			append(')');
			
			return o;
		}

		public IValue visitNode(INode o) throws VisitorException {
			String name = o.getName();
			
			if (name.indexOf('-') != -1) {
				append('\\');
			}
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
				append('[');
				int i = 0;
				Map<String, IValue> annotations = o.getAnnotations();
				for (String key : annotations.keySet()) {
					append("@" + key + "=");
					annotations.get(key).accept(this);
					
					if (++i < annotations.size()) {
						append(",");
					}
				}
				append(']');
			}
			
			return o;
		}

		public IValue visitRelation(IRelation o) throws VisitorException {
			return visitSet(o);
		}

		public IValue visitSet(ISet o) throws VisitorException {
			append('{');
			
			Iterator<IValue> setIterator = o.iterator();
			if(setIterator.hasNext()){
				setIterator.next().accept(this);
				
				while(setIterator.hasNext()){
					append(",");
					setIterator.next().accept(this);
				}
			}
			
			append('}');
			return o;
		}

		public IValue visitSourceLocation(ISourceLocation o)
				throws VisitorException {
			append('|');
			append(o.getURI().toString());
			append('|');
			
			if (o.getOffset() != -1) {
				append('(');
				append(Integer.toString(o.getOffset()));
				append(',');
				append(Integer.toString(o.getLength()));
				append(',');
				append('<');
				append(Integer.toString(o.getBeginLine()));
				append(',');
				append(Integer.toString(o.getBeginColumn()));
				append('>');
				append(',');
				append('<');
				append(Integer.toString(o.getEndLine()));
				append(',');
				append(Integer.toString(o.getEndColumn()));
				append('>');
				append(')');
			}
			return o;
		}

		public IValue visitString(IString o) throws VisitorException {
			append('\"');
		    for (byte ch : o.getValue().getBytes()) {
		    	switch (ch) {
		    	case '\"':
		    		append('\\');
		    		append('\"');
		    		break;
		    	case '\'':
		    		append('\\');
		    		append('\'');
		    	case '\\':
		    		append('\\');
		    		append('\\');
		    		break;
		    	case '\n':
		    		append('\\');
		    		append('n');
		    		break;
		    	case '\r':
		    		append('\\');
		    		append('r');
		    		break;
		    	case '\t':
		    		append('\\');
		    		append('t');
		    		break;
		    	default:
		    		append((char) ch);
		    	}
		    }
 		    append('\"');
		    return o;
		}

		public IValue visitTuple(ITuple o) throws VisitorException {
			 append('<');
			 
			 Iterator<IValue> it = o.iterator();
			 
			 if (it.hasNext()) {
				 it.next().accept(this);
			 }
			 
			 while (it.hasNext()) {
				 append(',');
				 it.next().accept(this);
			 }
			 append('>');
			 
			 return o;
		}

		public IValue visitExternal(IExternalValue externalValue) throws VisitorException {
			append(externalValue.toString());
			return externalValue;
		}

		public IValue visitDateTime(IDateTime o) throws VisitorException {
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
	}

}
