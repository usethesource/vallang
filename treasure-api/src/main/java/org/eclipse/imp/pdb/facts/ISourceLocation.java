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

package org.eclipse.imp.pdb.facts;

import java.net.URI;

/**
 * Source locations point to (parts of) files that contain source code.
 * Technically, the contents of the file does not have to be source code per se.
 * <br>
 * The part of the file is indicated by a character offset and length, as well as a
 * line and column region. The reason for the redundancy is that each representation
 * can not be computed from the other without processing the entire file. 
 * The goal of this representation is to allow different kinds and implementations
 * of tools, such as editors, to easily jump to source locations. 
 */
public interface ISourceLocation extends IValue {
	/**
	 * The {@link #top() top} method is preferred.
	 * @return exact url where the source is located. The particular encoding of
	 * the URL is not specified.
	 */
    URI getURI();

    String getScheme();
	String getAuthority();
	String getPath();
	String getFragment();
	String getQuery();
	Boolean hasAuthority();
	Boolean hasPath();
	Boolean hasFragment();
	Boolean hasQuery();
    
    
    /**
     * @return true iff the source location has offset/length information stored with it.
     */
    public boolean hasOffsetLength();
    
    /**
     * @return true iff the source location has start/end line/column info. true implies that hasOffsetLength() will also 
     * return true.
     */
    public boolean hasLineColumn();
    
    /**
     * @return the character offset starting from the beginning of the file located 
     * at the given url. Offsets start at 0 (zero).
     */
    int getOffset() throws UnsupportedOperationException;
    
    /**
     * @return the character length of the location (the amount characters).
     */
    int getLength() throws UnsupportedOperationException;;

    /**
     * @return the (inclusive) line number where the location begins. The first
     * line is always line number 1.
     */
    int getBeginLine() throws UnsupportedOperationException;;
    
    /**
     * @return the (exclusive) line where the location ends
     */
    int getEndLine() throws UnsupportedOperationException;;

    /**
     * @return the (inclusive) column number where the location begins. The
     * first column is always column number 0 (zero).
     */
    int getBeginColumn() throws UnsupportedOperationException;;
    
    /**
     * @return the (exclusive) column number where the location ends.
     */
    int getEndColumn() throws UnsupportedOperationException;

    /**
     * @return the source location without any offset & length information.
     */
	ISourceLocation top();
}
