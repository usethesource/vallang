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

package io.usethesource.vallang.exceptions;

public abstract class FactTypeUseException extends RuntimeException {
    private static final long serialVersionUID= 2135696551442574010L;

    public FactTypeUseException(String message) {
        super(message);
    }

    public FactTypeUseException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
