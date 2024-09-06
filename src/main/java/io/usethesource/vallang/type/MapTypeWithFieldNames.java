/*******************************************************************************
* Copyright (c) 2008, 2012, 2016 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*    Anya Helene Bagge - labels
*    Jurgen Vinju - reification
*******************************************************************************/

package io.usethesource.vallang.type;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.exceptions.UndeclaredFieldException;

/*package*/ final class MapTypeWithFieldNames extends MapType {
    private final String fKeyLabel;
    private final String fValueLabel;

    /*package*/ MapTypeWithFieldNames(Type keyType, String keyLabel, Type valueType, String valueLabel) {
        super(keyType, valueType);
        fKeyLabel = keyLabel;
        fValueLabel = valueLabel;
    }

    @Override
    public String getValueLabel() {
        return fValueLabel;
    }

    @Override
    public String getKeyLabel() {
        return fKeyLabel;
    }

    @Override
    public boolean hasFieldNames() {
        return true;
    }

    @Override
    public Type getFieldType(String fieldName) throws FactTypeUseException {
        if (fKeyLabel.equals(fieldName)) {
            return fKeyType;
        }

        if (fValueLabel.equals(fieldName)) {
            return fValueType;
        }

        throw new UndeclaredFieldException(this, fieldName);
    }

    @Override
    public boolean hasField(String fieldName) {
        if (fieldName.equals(fKeyLabel)) {
            return true;
        }
        else if (fieldName.equals(fValueLabel)) {
            return true;
        }

        return false;
    }

    @Override
    public String getFieldName(int i) {
        switch (i) {
        case 0: return fKeyLabel;
        case 1: return fValueLabel;
        default:
            throw new IndexOutOfBoundsException();
        }

    }

    @Override
    public Type select(String... names) {
        return TypeFactory.getInstance().setType(getFieldTypes().select(names));
    }

    @Override
    public int getFieldIndex(String fieldName) {
        if (fKeyLabel.equals(fieldName)) {
            return 0;
        }
        if (fValueLabel.equals(fieldName)) {
            return 1;
        }
        throw new UndeclaredFieldException(this, fieldName);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Type getFieldTypes() {
        return TypeFactory.getInstance().tupleType(fKeyType, fKeyLabel, fValueType, fValueLabel);
    }

    @Override
    public int hashCode() {
      return 56509 + 3511 * fKeyType.hashCode() + 1171 * fValueType.hashCode() + 13 * fKeyLabel.hashCode() + 1331 * fValueLabel.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }

        if (!obj.getClass().equals(getClass())) {
            return false;
        }

        MapTypeWithFieldNames other= (MapTypeWithFieldNames) obj;

        // N.B.: The element type must have been created and canonicalized before any
        // attempt to manipulate the outer type (i.e. SetType), so we can use object
        // identity here for the fEltType.
        return fKeyType == other.fKeyType
                && fValueType == other.fValueType
                && fKeyLabel.equals(other.fKeyLabel)
                && fValueLabel.equals(other.fValueLabel)
                ;
    }

    @Override
    public String toString() {
        return "map["
        + fKeyType + " " + fKeyLabel + ", "
        + fValueType + " " + fValueLabel
        + "]";
    }

    @Override
    public Type instantiate(Map<Type, Type> bindings) {
        return TypeFactory.getInstance().mapType(getKeyType().instantiate(bindings), fKeyLabel, getValueType().instantiate(bindings), fValueLabel);
    }
}
