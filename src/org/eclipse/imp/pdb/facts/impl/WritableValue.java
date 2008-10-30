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

package org.eclipse.imp.pdb.facts.impl;

import org.eclipse.imp.pdb.facts.type.Type;

public abstract class WritableValue<WriterT> extends Value {
	/*package*/ MutabilityState fState = MutabilityState.Mutable;

	private WriterT fWriter;

	public WritableValue(Type type) {
		super(type);
	}

	protected abstract WriterT createWriter();
	
	/*package*/ void setImmutable() {
		fState = MutabilityState.Immutable;
		fWriter = null;
	}
	
	public final boolean isMutable() {
		return fState == MutabilityState.Mutable;
	}

	public final WriterT getWriter() {
		if (fState == MutabilityState.Mutable) {
			if (fWriter == null) {
				fWriter = createWriter();
			}
			return fWriter;
		} else {
			throw new IllegalStateException(
					"Can only obtain writer on a mutable value.");
		}
	}
}
