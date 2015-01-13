package org.eclipse.imp.pdb.facts.impl.primitive;

import java.net.URI;

public interface IURI {
	String getScheme();
	String getAuthority() throws UnsupportedOperationException;
	String getPath() throws UnsupportedOperationException;
	String getFragment() throws UnsupportedOperationException;
	String getQuery() throws UnsupportedOperationException;
	Boolean hasAuthority();
	Boolean hasPath();
	Boolean hasFragment();
	Boolean hasQuery();
	URI getURI();
}
