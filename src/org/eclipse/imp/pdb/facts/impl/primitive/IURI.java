package org.eclipse.imp.pdb.facts.impl.primitive;

import java.net.URI;

public interface IURI {
	String getScheme();
	String getAuthority();
	String getPath();
	String getFragment();
	String getQuery();
	String getHost();
	String getUserInformation();
	int getPort();
	Boolean hasAuthority();
	Boolean hasPath();
	Boolean hasFragment();
	Boolean hasQuery();
	Boolean hasHost();
	Boolean hasUserInformation();
	Boolean hasPort();
	URI toURI();
}
