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

import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

public interface ISetWriter extends IWriter<ISet> {
    @Override
    public default Type computeType() {
        Type eltType = TypeFactory.getInstance().voidType();
        
        for (IValue el : this) {
            eltType = eltType.lub(el.getType());
        }
        
        return IValue.TF.setType(eltType);
    }
}
