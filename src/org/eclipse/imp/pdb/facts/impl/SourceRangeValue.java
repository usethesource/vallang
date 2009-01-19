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

import org.eclipse.imp.pdb.facts.ISourceRange;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/class SourceRangeValue extends Value implements ISourceRange {
	private final int fStartOffset;

	private final int fLength;

	private final int fStartLine;

	private final int fEndLine;

	private final int fStartCol;

	private final int fEndCol;

	/* package */SourceRangeValue(int startOffset, int length, int startLine,
			int endLine, int startCol, int endCol) {
		super(TypeFactory.getInstance().sourceRangeType());
		fStartOffset = startOffset;
		fLength = length;
		fStartLine = startLine;
		fEndLine = endLine;
		fStartCol = startCol;
		fEndCol = endCol;

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

	public int getStartColumn() {
		return fStartCol;
	}

	public int getStartLine() {
		return fStartLine;
	}

	public int getStartOffset() {
		return fStartOffset;
	}

	@Override
	public String toString() {
		return "<srcRange: " + fStartOffset + "," + fLength + ",L" + fStartLine
				+ ":" + fEndLine + ",C" + fStartCol + ":" + fEndCol + ">";
	}

	@Override
	public boolean equals(Object o) {
		if (getClass() == o.getClass()) {
			SourceRangeValue other = (SourceRangeValue) o;
			return other.fStartOffset == fStartOffset
					&& other.fLength == fLength
					&& other.fStartLine == fStartLine
					&& other.fEndLine == fEndLine
					&& other.fStartCol == fStartCol && other.fEndCol == fEndCol;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 24551 + 
		(fStartOffset << 5)+ 
		(fLength << 10) + 
		(fStartLine << 15) + 
		(fEndLine << 20) + 
		(fStartCol << 25) + 
		(fEndCol << 30);
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitSourceRange(this);
	}
}
