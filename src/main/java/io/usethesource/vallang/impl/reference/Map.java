/*******************************************************************************
 * Copyright (c) 2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
 *
 * Based on code by:
 *
 *   * Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *******************************************************************************/
package io.usethesource.vallang.impl.reference;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IMap;
import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.IRelation;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.util.ValueEqualsWrapper;

/*package*/ class Map implements IMap {
	final Type type;
	final java.util.Map<ValueEqualsWrapper, IValue> content;

	/*package*/ Map(Type candidateMapType, java.util.Map<ValueEqualsWrapper, IValue> content) {
		super();
		this.content = content;
		this.type = candidateMapType;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public int size() {
		return content.size();
	}

	@Override
	public boolean isEmpty() {
		return content.isEmpty();
	}

	@Override
	public @Nullable IValue get(IValue key) {
	    // see how we can't use the hash tabel due to the semantics of isEqual
	    return content.get(new ValueEqualsWrapper(key));
	}

	@Override
	public Iterator<IValue> iterator() {
	    return new Iterator<IValue>() {
            Iterator<ValueEqualsWrapper> it = content.keySet().iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public IValue next() {
                return it.next().getValue();
            }
        };
	}

	@Override
	public Iterator<IValue> valueIterator() {
		return content.values().iterator();
	}

	@Override
	public Iterator<Entry<IValue, IValue>> entryIterator() {
		return new Iterator<Entry<IValue, IValue>>() {
            Iterator<Entry<ValueEqualsWrapper, IValue>> it = content.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Entry<IValue, IValue> next() {
                Entry<ValueEqualsWrapper, IValue> entry = it.next();
                return new AbstractMap.SimpleImmutableEntry<>(entry.getKey().getValue(), entry.getValue());
            }
        };
	}

    @Override
    public Type getElementType() {
        // the iterator iterates over the keys
        return type.getKeyType();
    }

    @Override
    public IMap empty() {
        return writer().done();
    }

    @Override
    public IMapWriter writer() {
        return new MapWriter();
    }

    @Override
    public IRelation<IMap> asRelation() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public String toString() {
        return defaultToString();
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }
    
    @Override
    public boolean equals(@Nullable Object obj) {
        return defaultEquals(obj);
    }
    
    @Override
    public Stream<IValue> stream() {
        Iterable<Entry<IValue, IValue>> it = () -> entryIterator();
        return StreamSupport.stream(it.spliterator(), false).map(entry -> new Tuple(entry.getKey(), entry.getValue()));
    }
}
