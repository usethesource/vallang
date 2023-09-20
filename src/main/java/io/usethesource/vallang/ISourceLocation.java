/*******************************************************************************
* Copyright (c) 2007 IBM Corporation, 2023 NWO-I Centrum Wiskunde & Informatica
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*    Jurgen Vinju (Jurgen.Vinju@cwi.nl) - extensions and maintenance
*******************************************************************************/

package io.usethesource.vallang;

import java.net.URI;
import java.net.URISyntaxException;

import io.usethesource.vallang.visitors.IValueVisitor;

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
    public URI getURI();

    /**
     * @return the scheme of the URI
     */
    public String getScheme();
    
    /**
     * @return the authority of the URI or "" if it does not exist
     */
	public String getAuthority();
	
	/**
	 * @return the path of the URI or "" if it does not exist
	 */
	public String getPath();
	
	/**
	 * @return the fragment of the URI or "" if it does not exist
	 */
	public String getFragment();
	
	/**
	 * @return the query part of the URI or "" if it does not exist
	 */
	public String getQuery();
	
	/**
	 * @return true iff the URI has an authority part
	 */
	public boolean hasAuthority();
	
	/**
     * @return true iff the URI has an path part
     */
	public boolean hasPath();
	
	/**
     * @return true iff the URI has an fragment part
     */
	public boolean hasFragment();
	
	/**
     * @return true iff the URI has a query part
     */
	public boolean hasQuery();
    
    
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
     * @throws UnsupportedOperationException
     */
    public int getOffset();
    
    /**
     * @return the character length of the location (the amount characters).
     * @throws UnsupportedOperationException
     */
    public int getLength();

    /**
     * @return the (inclusive) line number where the location begins. The first
     * line is always line number 1.
     * @throws UnsupportedOperationException
     */
    public int getBeginLine();
    
    /**
     * @return the (exclusive) line where the location ends
     * @throws UnsupportedOperationException
     */
    public int getEndLine();

    /**
     * @return the (inclusive) column number where the location begins. The
     * first column is always column number 0 (zero).
     * @throws UnsupportedOperationException
     */
    public int getBeginColumn();
    
    /**
     * @return the (exclusive) column number where the location ends.
     * @throws UnsupportedOperationException
     */
    public int getEndColumn();

    /**
     * @return the source location without any offset & length information.
     */
    public ISourceLocation top();

    public String getFileName();

    public boolean hasFileName();

    public ISourceLocation changeScheme(String newScheme) throws URISyntaxException;

    public ISourceLocation changeAuthority(String newAuthority) throws URISyntaxException;

    public ISourceLocation changePath(String newPath) throws URISyntaxException;

    public ISourceLocation changeFile(String newFile) throws URISyntaxException;

    public ISourceLocation changeExtension(String newExtension) throws URISyntaxException;

    public ISourceLocation changeFragment(String newFragment) throws URISyntaxException;

    public ISourceLocation changeQuery(String newQuery) throws URISyntaxException;

    public ISourceLocation changeFileName(String newFileName) throws URISyntaxException;

    public ISourceLocation makeChildLocation(String childPath) throws URISyntaxException;

    public ISourceLocation getParentLocation();
    
    @Override
    default <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
        return v.visitSourceLocation(this);
    }
}
