/*******************************************************************************
* Copyright (c) 2009 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   Jurgen Vinju (jurgen@vinju.org) - initial API and implementation
*******************************************************************************/
package io.usethesource.vallang;

import io.usethesource.vallang.visitors.IValueVisitor;

public interface IBool extends IValue {
    @Override
    default int getMatchFingerprint() {
        if (getValue()) {
            return 3569038; /* "true".hashCode() */
        }
        else {
            return 97196323; /* "false".hashCode() */
        }
    }

    boolean getValue();
    String getStringRepresentation();
    IBool and(IBool other);
    IBool or(IBool other);
    IBool xor(IBool other);
    IBool not();
    IBool implies(IBool other);
    IBool equivalent(IBool other);

    @Override
    default <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
        return v.visitBoolean(this);
    }
}
