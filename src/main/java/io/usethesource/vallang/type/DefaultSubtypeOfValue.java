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

package io.usethesource.vallang.type;

/* package */abstract class DefaultSubtypeOfValue extends ValueType {

    @Override
    public boolean isTop() {
        return false;
    }

    @Override
    public abstract boolean intersects(Type other);

    protected boolean intersectsWithValue(Type type) {
        // everything intersects with value
        return true;
    }

    protected boolean intersectsWithReal(Type type) {
        return false;
    }

    protected boolean intersectsWithInteger(Type type) {
        return false;
    }

    protected boolean intersectsWithRational(Type type) {
        return false;
    }

    protected boolean intersectsWithList(Type type) {
        return false;
    }

    protected boolean intersectsWithMap(Type type) {
        return false;
    }

    protected boolean intersectsWithNumber(Type type) {
        return false;
    }

    protected boolean intersectsWithRelation(Type type) {
        return false;
    }


    protected boolean intersectsWithSet(Type type) {
        return false;
    }

    protected boolean intersectsWithSourceLocation(Type type) {
        return false;
    }

    protected boolean intersectsWithString(Type type) {
        return false;
    }

    protected boolean intersectsWithNode(Type type) {
        return false;
    }

    protected boolean intersectsWithConstructor(Type type) {
        return false;
    }

    protected boolean intersectsWithAbstractData(Type type) {
        return false;
    }

    protected boolean intersectsWithTuple(Type type) {
        return false;
    }

    protected boolean intersectsWithFunction(Type type) {
        return false;
    }

    protected boolean intersectsWithVoid(Type type) {
        // the intersection of void, even with itself is empty.
        return false;
    }

    protected boolean intersectsWithBool(Type type) {
        return false;
    }

    protected boolean intersectsWithDateTime(Type type) {
        return false;
    }

    @Override
    public abstract Type glb(Type type);

    @Override
    protected Type glbWithValue(Type type) {
        return this; // such that sub-classes do not have to override
    }

    protected Type glbWithReal(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithInteger(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithRational(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithList(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithMap(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithNumber(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithRelation(Type type) {
        return VoidType.getInstance();
    }


    protected Type glbWithSet(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithSourceLocation(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithString(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithNode(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithConstructor(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithAbstractData(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithTuple(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithFunction(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithVoid(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithBool(Type type) {
        return VoidType.getInstance();
    }

    protected Type glbWithDateTime(Type type) {
        return VoidType.getInstance();
    }


}
