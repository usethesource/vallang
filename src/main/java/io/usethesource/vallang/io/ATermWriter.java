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
package io.usethesource.vallang.io;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import io.usethesource.vallang.IBool;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IDateTime;
import io.usethesource.vallang.IExternalValue;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IRational;
import io.usethesource.vallang.IReal;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.type.TypeStore;
import io.usethesource.vallang.visitors.IValueVisitor;

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
        
        @Override
        public IValue visitBoolean(IBool boolValue) throws IOException {
            append(boolValue.getStringRepresentation());
            return boolValue;
        }

        @Override
        public IValue visitConstructor(IConstructor o) throws IOException {
            return visitNode(o);
        }

        @Override
        public IValue visitReal(IReal o) throws IOException {
            append(o.getStringRepresentation());
            return o;
        }

        @Override
        public IValue visitInteger(IInteger o) throws IOException {
            append(o.getStringRepresentation());
            return o;
        }

        // TODO: There probably isn't a good ATerm repr of rationals,
        // what should we do here?
        @Override
        public IValue visitRational(IRational o) throws IOException {
            append("rat");
            append('(');
            append(o.numerator().getStringRepresentation());
            append(',');
            append(o.denominator().getStringRepresentation());
            append(')');
            return o;
        }
        
        @Override
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

        @Override
        public IValue visitMap(IMap o) throws IOException {
            append('[');
        
            Iterator<Entry<IValue,IValue>> mapIterator = o.entryIterator();
            if (mapIterator.hasNext()) {
                Entry<IValue, IValue> entry = mapIterator.next();
                append('(');
                IValue key = entry.getKey();
                key.accept(this);
                append(',');
                entry.getValue().accept(this);
                append(')');
                
                while(mapIterator.hasNext()) {
                    entry = mapIterator.next();
                    append(',');
                    append('(');
                    key = entry.getKey();
                    append(',');
                    entry.getValue().accept(this);
                    append(')');
                }
            }
            
            append(']');
            
            return o;
        }

        @Override
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
            
            if (o.asWithKeywordParameters().hasParameters()) {
                append("{[");
                int i = 0;
                Map<String, IValue> kwFields = o.asWithKeywordParameters().getParameters();
                for (Entry<String, IValue> entry : kwFields.entrySet()) {
                    append("[" + entry.getKey() + ",");
                    entry.getValue().accept(this);
                    append("]");
                    
                    if (++i < kwFields.size()) {
                        append(",");
                    }
                }
                append("]}");
            }
            
            return o;
        }

        @Override
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

        @Override
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

        @Override
        public IValue visitString(IString o) throws IOException {
            // TODO optimize this implementation and finish all escapes
            append('\"');
            append(o.getValue().replaceAll("\"", "\\\"").replaceAll("\n","\\\\n"));
            append('\"');
            return o;
        }

        @Override
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

        @Override
        public IValue visitExternal(IExternalValue externalValue) {
            // ignore external values
            return externalValue;
        }

        @Override
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
    }
}
