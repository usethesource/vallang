package org.eclipse.imp.pdb.facts.impl.primitive;

import java.net.URI;
import java.net.URISyntaxException;

public interface IURI {
	String getScheme();
	String getAuthority();
	String getPath();
	String getFragment();
	String getQuery();
	
	IURI setScheme(String scheme) throws URISyntaxException;
	IURI setAuthority(String authority)  throws URISyntaxException;
	IURI setPath(String path)  throws URISyntaxException;
	IURI setFragment(String fragment) throws URISyntaxException;
	IURI setQuery(String query)  throws URISyntaxException;
	
	Boolean hasAuthority();
	Boolean hasPath();
	Boolean hasFragment();
	Boolean hasQuery();
	URI getURI();
}
