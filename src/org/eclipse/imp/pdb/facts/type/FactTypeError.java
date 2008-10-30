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

package org.eclipse.imp.pdb.facts.type;

public class FactTypeError extends RuntimeException {
    private static final long serialVersionUID= 2135696551442574010L;

    public FactTypeError() {
        super("Fact type error");
    }
    
    public FactTypeError(String reason) {
    	super(reason);
    }
    
    public FactTypeError(String reason, Throwable cause) {
    	super(reason, cause);
    }
}
