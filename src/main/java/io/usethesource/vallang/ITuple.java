/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package io.usethesource.vallang;

import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.visitors.IValueVisitor;

public interface ITuple extends Iterable<IValue>, IValue {
    @Override
    default int getPatternMatchFingerprint() {
        return 442900256 /* "tuple".hashCode() << 2 */ + arity();
    }

    /**
     * Retrieve the given field at the given index.
     * 
     * @param i the index of the field, starting at 0
     * @return the value at the given label in the tuple
     * @throws IndexOutOfBoundsException
     */
    public IValue get(int i);
    
    @Deprecated
    /**
     * Retrieve the given field at the given label.
     * 
     * @param label the name of the field
     * @return the value at the given label in the tuple
     * 
     * TODO: this method will dissappear when field names will no longer be recorded 
     * the vallang library. This is necessary to be able to provide canonical types
     * and use reference equality for type equality; a major factor in CPU performance.
     */
    public IValue get(String label);
    
    /**
     * Replace the given field by a new value.
     * 
     * @param i the index of the field
     * @param arg   the new value
     * @return
     * @throws IndexOutOfBoundsException
     */
    public ITuple set(int i, IValue arg);
    
    @Deprecated
    /**
     * Replace the given field by a new value.
     * 
     * @param label the name of the field
     * @param arg   the new value
     * @return
     * @throws FactTypeUseException
     * 
     * TODO: this method will dissappear when field names will no longer be recorded 
     * the vallang library. This is necessary to be able to provide canonical types
     * and use reference equality for type equality; a major factor in CPU performance.
     */
    public ITuple set(String label, IValue arg);

    /**
     * @return the number of columns/fields of the tuple
     */
    public int arity();

    /**
     * Reduces an n-ary tuple to fewer fields, given by the field indexes to select.
     * @param fields to select from the tuple, index starts at 0
     * @return a new tuple with only the fields selected by the fields parameter
     */
    public IValue select(int... fields) throws IndexOutOfBoundsException;
    
    @Deprecated
    /**
     * Reduces an n-ary tuple to fewer fields, given by the field names to select.
     * @param fields to select from the tuple
     * @return a new tuple with only the fields selected by the fields parameter
     * 
     * TODO: this method will dissappear when field names will no longer be recorded 
     * the vallang library. This is necessary to be able to provide canonical types
     * and use reference equality for type equality; a major factor in CPU performance.
     */
    public IValue selectByFieldNames(String... fields) throws FactTypeUseException;
    
    @Override
    public default boolean match(IValue o) {
        if (this == o) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof ITuple) {
            ITuple peer = (ITuple) o;

            if (getType() != peer.getType()) {
                return false;
            }

            int arity = arity();
            if (arity != peer.arity()) {
                return false;
            }
            for (int i = 0; i < arity; i++) {
                if (!get(i).match(peer.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    @Override
    default <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
        return v.visitTuple(this);
    }
}
