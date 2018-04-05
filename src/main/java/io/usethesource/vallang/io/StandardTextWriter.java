/*******************************************************************************
 * Copyright (c) CWI 2009-2017
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
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfInt;

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
import io.usethesource.vallang.IWithKeywordParameters;
import io.usethesource.vallang.impl.primitive.StringValue;
import io.usethesource.vallang.type.ITypeVisitor;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeStore;
import io.usethesource.vallang.visitors.IValueVisitor;

/**
 * This class implements the standard readable syntax for {@link IValue}'s.
 * See also {@link StandardTextReader}
 */
public class StandardTextWriter implements IValueTextWriter {
    protected final boolean indent;
    protected final int tabSize;

    public StandardTextWriter() {
        this(false);
    }

    public StandardTextWriter(boolean indent) {
        this(indent, 2);
    }

    public StandardTextWriter(boolean indent, int tabSize) {
        this.indent = indent;
        this.tabSize = tabSize;
    }

    public static String valueToString(IValue value) {
        try(StringWriter stream = new StringWriter()) {
            new StandardTextWriter().write(value, stream);
            return stream.toString();
        } catch (IOException ioex) {
            throw new RuntimeException("Should have never happened.", ioex);
        }
    }

    public void write(IValue value, java.io.Writer stream) throws IOException {
        try {
            value.accept(new Writer(stream, indent, tabSize));
        } 
        finally {
            stream.flush();
        }
    }

    public void write(IValue value, java.io.Writer stream, TypeStore typeStore) throws IOException {
        write(value, stream);
    }

    protected static class Writer implements IValueVisitor<IValue, IOException> {
        private final java.io.Writer stream;
        private final int tabSize;
        private final boolean indent;
        private int tab = 0;

        public Writer(java.io.Writer stream, boolean indent, int tabSize) {
            this.stream = stream;
            this.indent = indent;
            this.tabSize = tabSize;
        }

        private void append(String string) throws IOException {
            stream.write(string);
        }

        private void append(int cp) throws IOException {
            if (Character.isBmpCodePoint(cp)) {
                stream.write(cp);
            }
            else {
                stream.write(Character.highSurrogate(cp));
                stream.write(Character.lowSurrogate(cp));
            }
        }

        private void append(char c) throws IOException {
            stream.write(c);
        }

        private void tab() {
            this.tab++;
        }

        private void untab() {
            this.tab--;
        }

        public IValue visitBoolean(IBool boolValue)
                throws IOException {
            append(boolValue.getValue() ? "true" : "false");
            return boolValue;
        }

        public IValue visitReal(IReal o) throws IOException {
            append(o.getStringRepresentation());
            return o;
        }

        public IValue visitInteger(IInteger o) throws IOException {
            append(o.getStringRepresentation());
            return o;
        }

        public IValue visitRational(IRational o) throws IOException {
            append(o.getStringRepresentation());
            return o;
        }

        public IValue visitList(IList o) throws IOException {
            append('[');

            boolean indent = checkIndent(o);
            Iterator<IValue> listIterator = o.iterator();
            tab();
            indent(indent);
            if(listIterator.hasNext()){
                listIterator.next().accept(this);

                while(listIterator.hasNext()){
                    append(',');
                    if (indent) indent();
                    listIterator.next().accept(this);
                }
            }
            untab();
            indent(indent);
            append(']');

            return o;
        }

        public IValue visitMap(IMap o) throws IOException {
            append('(');
            tab();
            boolean indent = checkIndent(o);
            indent(indent);
            Iterator<IValue> mapIterator = o.iterator();
            if(mapIterator.hasNext()){
                IValue key = mapIterator.next();
                key.accept(this);
                append(':');
                o.get(key).accept(this);

                while(mapIterator.hasNext()){
                    append(',');
                    indent(indent);
                    key = mapIterator.next();
                    key.accept(this);
                    append(':');
                    o.get(key).accept(this);
                }
            }
            untab();
            indent(indent);
            append(')');

            return o;
        }

        public IValue visitConstructor(IConstructor o) throws IOException {
            String name = o.getName();

            if (name.equals("loc")) {
                append('\\');
            }

            if (name.indexOf('-') != -1) {
                append('\\');
            }
            append(name);

            boolean indent = checkIndent(o);

            append('(');
            tab();
            indent(indent);
            Iterator<IValue> it = o.iterator();
            int k = 0;
            while (it.hasNext()) {
                it.next().accept(this);
                if (it.hasNext()) {
                    append(',');
                    indent(indent);
                }
                k++;
            }

            if (o.mayHaveKeywordParameters()) {
                IWithKeywordParameters<? extends IConstructor> wkw = o.asWithKeywordParameters();
                if (wkw.hasParameters()) {
                    if (k > 0) {
                        append(',');
                        indent(indent);
                    }

                    Iterator<Entry<String, IValue>> iterator = wkw.getParameters().entrySet().iterator();
                    while (iterator.hasNext()) {
                        Entry<String,IValue> e = iterator.next();

                        append(e.getKey());
                        append('=');
                        e.getValue().accept(this);

                        if (iterator.hasNext()) {
                            append(',');
                            indent(indent);
                        }
                    }
                }
            }

            append(')');
            untab();
            if (o.isAnnotatable() && o.asAnnotatable().hasAnnotations()) {
                append('[');
                tab();
                indent();
                int i = 0;
                Map<String, IValue> annotations = o.asAnnotatable().getAnnotations();
                for (Entry<String, IValue> entry : annotations.entrySet()) {
                    append("@" + entry.getKey() + "=");
                    entry.getValue().accept(this);

                    if (++i < annotations.size()) {
                        append(",");
                        indent();
                    }
                }
                untab();
                indent();
                append(']');
            }
            try {
                stream.flush();
            }
            catch (IOException e) {
                // flushing is just to make sure we get some intermediate output
            }

            return o;
        }

        private void indent() throws IOException {
            indent(indent);
        }

        private void indent(boolean indent) throws IOException {
            if (indent) {
                append('\n');
                for (int i = 0; i < tabSize * tab; i++) {
                    append(' ');
                }
            }
        }

        public IValue visitRelation(ISet o) throws IOException {
            return visitSet(o);
        }

        public IValue visitSet(ISet o) throws IOException {
            append('{');

            boolean indent = checkIndent(o);
            tab();
            indent(indent);
            Iterator<IValue> setIterator = o.iterator();
            if(setIterator.hasNext()){
                setIterator.next().accept(this);

                while(setIterator.hasNext()){
                    append(",");
                    indent(indent);
                    setIterator.next().accept(this);
                }
            }
            untab(); 
            indent(indent);
            append('}');
            return o;
        }

        private boolean checkIndent(ISet o) {
            if (indent && o.size() > 1) {
                for (IValue x : o) {
                    Type type = x.getType();
                    return indented(type);
                }
            }
            return false;
        }

        private boolean indented(Type type) {
            return type.accept(new ITypeVisitor<Boolean,RuntimeException>() {
                @Override
                public Boolean visitReal(Type type) {
                    return false;
                }

                @Override
                public Boolean visitInteger(Type type) {
                    return false;
                }

                @Override
                public Boolean visitRational(Type type) {
                    return false;
                }

                @Override
                public Boolean visitList(Type type) {
                    return true;
                }

                @Override
                public Boolean visitMap(Type type) {
                    return true;
                }

                @Override
                public Boolean visitNumber(Type type) {
                    return false;
                }

                @Override
                public Boolean visitAlias(Type type) {
                    return type.getAliased().accept(this);
                }

                @Override
                public Boolean visitSet(Type type) {
                    return true;
                }

                @Override
                public Boolean visitSourceLocation(Type type) {
                    return true;
                }

                @Override
                public Boolean visitString(Type type) {
                    return false;
                }

                @Override
                public Boolean visitNode(Type type) {
                    return true;
                }

                @Override
                public Boolean visitConstructor(Type type) {
                    return true;
                }

                @Override
                public Boolean visitAbstractData(Type type) {
                    return true;
                }

                @Override
                public Boolean visitTuple(Type type) {
                    return true;
                }

                @Override
                public Boolean visitValue(Type type) {
                    return false;
                }

                @Override
                public Boolean visitVoid(Type type) {
                    return false;
                }

                @Override
                public Boolean visitBool(Type type) {
                    return false;
                }

                @Override
                public Boolean visitParameter(Type type) {
                    return type.getBound().accept(this);
                }

                @Override
                public Boolean visitExternal(Type type) {
                    return false;
                }

                @Override
                public Boolean visitDateTime(Type type) {
                    return false;
                }
            });
        }

        private boolean checkIndent(IList o) {
            if (indent && o.length() > 1) {
                for (IValue x : o) {
                    Type type = x.getType();
                    if (indented(type)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean checkIndent(INode o) {
            int kwArity = o.mayHaveKeywordParameters() ? o.asWithKeywordParameters().getParameters().size() : 0;
            if (indent && (o.arity() + kwArity) > 1) {
                for (IValue x : o) {
                    Type type = x.getType();
                    if (indented(type)) {
                        return true;
                    }
                }
                if (o.mayHaveKeywordParameters()) {
                    for (IValue x : o.asWithKeywordParameters().getParameters().values()) {
                        Type type = x.getType();
                        if (indented(type)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private boolean checkIndent(IMap o) {
            if (indent && o.size() > 1) {
                for (IValue x : o) {
                    Type type = x.getType();
                    Type vType = o.get(x).getType();
                    if (indented(type)) {
                        return true;
                    }
                    if (indented(vType)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public IValue visitSourceLocation(ISourceLocation o)
                throws IOException {
            append('|');
            append(o.getURI().toString());
            append('|');

            if (o.hasOffsetLength()) {
                append('(');
                append(Integer.toString(o.getOffset()));
                append(',');
                append(Integer.toString(o.getLength()));

                if (o.hasLineColumn()) {
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
                }
                append(')');
            }
            return o;
        }

        public IValue visitString(IString o) throws IOException {
            append('\"');
            OfInt it = o.iterator();
            while (it.hasNext()) {
                int ch = it.nextInt();
                
                switch (ch) {
                    case '\"':
                        append('\\');
                        append('\"');
                        break;
                    case '>':
                        append('\\');
                        append('>');
                        break;
                    case '<':
                        append('\\');
                        append('<');
                        break;
                    case '\'':
                        append('\\');
                        append('\'');
                        break;
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
                    case ' ':
                        // needed because other space chars will be escaped in the default branch
                        append(' ');
                        break;
                    default:
                        if (Character.isSpaceChar(ch)
                                || Character.isISOControl(ch)
                                || Character.UnicodeBlock.SPECIALS.equals(Character.UnicodeBlock.of(ch))) {
                            // these characters are invisible or otherwise unreadable and we escape them here
                            // for clarity of the serialized string

                            if (ch <= Byte.MAX_VALUE) {
                                append("\\a" + String.format("%02x", ch));
                            }
                            else if (ch <= Character.MAX_VALUE) {
                                append("\\u" + String.format("%04x", ch));
                            }
                            else {
                                append("\\U" + String.format("%06x", ch));
                            }
                        }
                        else {
                            append(ch);
                        }
                }
            }
            append('\"');
            return o;
        }

        public IValue visitTuple(ITuple o) throws IOException {
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

        public IValue visitExternal(IExternalValue externalValue) throws IOException {
            return visitConstructor(externalValue.encodeAsConstructor());
        }

        public IValue visitDateTime(IDateTime o) throws IOException {
            append("$");
            if (o.isDate()) {
                append(String.format("%04d", o.getYear()));
                append("-");
                append(String.format("%02d", o.getMonthOfYear()));
                append("-");
                append(String.format("%02d", o.getDayOfMonth()));
            } else if (o.isTime()) {
                append("T");
                append(String.format("%02d", o.getHourOfDay()));
                append(":");
                append(String.format("%02d", o.getMinuteOfHour()));
                append(":");
                append(String.format("%02d", o.getSecondOfMinute()));
                append(".");
                append(String.format("%03d", o.getMillisecondsOfSecond()));
                if (o.getTimezoneOffsetHours() < 0 || (o.getTimezoneOffsetHours() == 0 && o.getTimezoneOffsetMinutes() < 0))
                    append("-");
                else
                    append("+");
                append(String.format("%02d", Math.abs(o.getTimezoneOffsetHours())));
                append(":");
                append(String.format("%02d", Math.abs(o.getTimezoneOffsetMinutes())));
            } else {
                append(String.format("%04d", o.getYear()));
                append("-");
                append(String.format("%02d", o.getMonthOfYear()));
                append("-");
                append(String.format("%02d", o.getDayOfMonth()));

                append("T");
                append(String.format("%02d", o.getHourOfDay()));
                append(":");
                append(String.format("%02d", o.getMinuteOfHour()));
                append(":");
                append(String.format("%02d", o.getSecondOfMinute()));
                append(".");
                append(String.format("%03d", o.getMillisecondsOfSecond()));
                if (o.getTimezoneOffsetHours() < 0 || (o.getTimezoneOffsetHours() == 0 && o.getTimezoneOffsetMinutes() < 0))
                    append("-");
                else
                    append("+");
                append(String.format("%02d", Math.abs(o.getTimezoneOffsetHours())));
                append(":");
                append(String.format("%02d", Math.abs(o.getTimezoneOffsetMinutes())));
            }
            append("$");
            return o;
        }

        public IValue visitListRelation(IList o)
                throws IOException {
            visitList(o);
            return o;
        }

        public IValue visitNode(INode o) throws IOException {
            visitString(StringValue.newString(o.getName()));

            boolean indent = checkIndent(o);

            append('(');
            tab();
            indent(indent);
            Iterator<IValue> it = o.iterator();
            int k = 0;
            while (it.hasNext()) {
                it.next().accept(this);
                if (it.hasNext()) {
                    append(',');
                    indent(indent);
                }
                k++;
            }


            if (o.mayHaveKeywordParameters()) {
                IWithKeywordParameters<? extends INode> wkw = o.asWithKeywordParameters();
                if (wkw.hasParameters()) {
                    if (k > 0) {
                        append(',');
                    }

                    Iterator<Entry<String,IValue>> kwIt = wkw.getParameters().entrySet().iterator();
                    while (kwIt.hasNext()) {
                        Entry<String, IValue> e = kwIt.next();
                        indent();
                        append(e.getKey());
                        append('=');
                        e.getValue().accept(this);

                        if (kwIt.hasNext()) {
                            append(',');
                        }

                    }
                }
            }
            append(')');
            untab();
            if (o.isAnnotatable() && o.asAnnotatable().hasAnnotations()) {
                append('[');
                tab();
                indent();
                int i = 0;
                Map<String, IValue> annotations = o.asAnnotatable().getAnnotations();
                for (Entry<String, IValue> entry : annotations.entrySet()) {
                    append("@" + entry.getKey() + "=");
                    entry.getValue().accept(this);

                    if (++i < annotations.size()) {
                        append(",");
                        indent();
                    }
                }
                untab();
                indent();
                append(']');
            }

            return o;
        }
    }

}
