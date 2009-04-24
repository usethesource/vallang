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

import java.net.URL;

import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/ class SourceLocationValue extends Value implements ISourceLocation {
    private final URL fPath;
    private final int fStartOffset;

	private final int fLength;

	private final int fStartLine;

	private final int fEndLine;

	private final int fStartCol;

	private final int fEndCol;

    /*package*/ SourceLocationValue(URL path, int startOffset, int length, int startLine,
			int endLine, int startCol, int endCol) {
        super(TypeFactory.getInstance().sourceLocationType());
        fPath= path;
        fStartOffset = startOffset;
		fLength = length;
		fStartLine = startLine;
		fEndLine = endLine;
		fStartCol = startCol;
		fEndCol = endCol;
    }
    
	public URL getURL() {
        return fPath;
    }

	public int getEndColumn() {
		return fEndCol;
	}

	public int getEndLine() {
		return fEndLine;
	}

	public int getLength() {
		return fLength;
	}

	public int getBeginColumn() {
		return fStartCol;
	}

	public int getBeginLine() {
		return fStartLine;
	}

	public int getOffset() {
		return fStartOffset;
	}

	@Override
	public boolean equals(Object o) {
		if (getClass() == o.getClass()) {
			SourceLocationValue other = (SourceLocationValue) o;
			return fPath.toString().equals(other.fPath.toString()) 
			        && other.fStartOffset == fStartOffset
					&& other.fLength == fLength
					&& other.fStartLine == fStartLine
					&& other.fEndLine == fEndLine
					&& other.fStartCol == fStartCol && other.fEndCol == fEndCol;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 1923 * fPath.hashCode() + 24551 + 
		(fStartOffset << 5)+ 
		(fLength << 10) + 
		(fStartLine << 15) + 
		(fEndLine << 20) + 
		(fStartCol << 25) + 
		(fEndCol << 30);
	}
	
    public <T> T accept(IValueVisitor<T> v) throws VisitorException {
    	return v.visitSourceLocation(this);
    }
}
