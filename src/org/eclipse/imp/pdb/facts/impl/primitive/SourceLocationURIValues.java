package org.eclipse.imp.pdb.facts.impl.primitive;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.imp.pdb.facts.impl.primitive.IURI;

/*
 * Not supported: in URI class, scheme and host are case insensitive, but this is already kinda broken, since on windows & osx, so should path's be.
 */
/*package*/ class SourceLocationURIValues {
	static IURI newURI(URI base) {
		Boolean authorityIsHost = base.getAuthority().equals(base.getHost());
		return newURI(base.getScheme(), base.getAuthority(), authorityIsHost, base.getPath(), base.getQuery(), base.getFragment());
	}
	static IURI newURI(String scheme, String authority, boolean authorityIsHost, String path, String query, String fragment) {
		if (scheme == null || scheme.equals(""))
			throw new IllegalArgumentException("scheme cannot be empty or null");
		if (authority == null || authority.equals("")) {
			if (path == null) {
				if (query == null) {
					if (fragment == null) {
						return new SourceLocationURIValues.BaseURI(scheme);
					}
					return new SourceLocationURIValues.FragmentURI(scheme, fragment);
				}
				if (fragment == null) {
					return new SourceLocationURIValues.QueryURI(scheme, query);
				}
				return new SourceLocationURIValues.FragmentQueryURI(scheme, query, fragment);
			}
			if (query == null) {
				if (fragment == null) {
					return new SourceLocationURIValues.PathURI(scheme, path);
				}
				return new SourceLocationURIValues.FragmentPathURI(scheme, path, fragment);
			}
			if (fragment == null) {
				return new SourceLocationURIValues.QueryPathURI(scheme, path, query);
			}
			return new SourceLocationURIValues.FragmentQueryPathURI(scheme, path, query, fragment);
		}
		if (path == null) {
			if (query == null) {
				if (fragment == null) {
					return new SourceLocationURIValues.AuthorityURI(scheme, authority, authorityIsHost);
				}
				return new SourceLocationURIValues.FragmentAuthorityURI(scheme, authority, authorityIsHost, fragment);
			}
			if (fragment == null) {
				return new SourceLocationURIValues.QueryAuthorityURI(scheme, authority, authorityIsHost, query);
			}
			return new SourceLocationURIValues.FragmentQueryAuthorityURI(scheme, authority, authorityIsHost, query, fragment);
		}
		if (query == null) {
			if (fragment == null) {
				return new SourceLocationURIValues.PathAuthorityURI(scheme, authority, authorityIsHost, path);
			}
			return new SourceLocationURIValues.FragmentPathAuthorityURI(scheme, authority, authorityIsHost, path, fragment);
		}
		if (fragment == null) {
			return new SourceLocationURIValues.QueryPathAuthorityURI(scheme, authority, authorityIsHost, path, query);
		}
		return new SourceLocationURIValues.FragmentQueryPathAuthorityURI(scheme, authority, authorityIsHost, path, query, fragment);
	}
	
	
	private static class BaseURI implements IURI {
		protected final String scheme;
		
		public BaseURI(String scheme) {
			this.scheme = scheme.intern();
		}
		
		public URI toURI() {
			try {
				return new URI(scheme,"",null,null,null);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof BaseURI) {
				return scheme == ((BaseURI)obj).scheme;
			}
			return false;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode(); 
		}

		@Override
		public String getScheme() {
			return scheme;
		}

		@Override
		public String getAuthority() {
			return null;
		}

		@Override
		public String getPath() {
			return null;
		}

		@Override
		public String getFragment() {
			return null;
		}

		@Override
		public String getQuery() {
			return null;
		}

		@Override
		public Boolean hasAuthority() {
			return false;
		}

		@Override
		public Boolean hasPath() {
			return false;
		}

		@Override
		public Boolean hasFragment() {
			return false;
		}

		@Override
		public Boolean hasQuery() {
			return false;
		}

		@Override
		public String getHost() {
			return null;
		}

		@Override
		public String getUserInformation() {
			return null;
		}

		@Override
		public int getPort() {
			return -1;
		}

		@Override
		public Boolean hasHost() {
			return false;
		}

		@Override
		public Boolean hasUserInformation() {
			return false;
		}

		@Override
		public Boolean hasPort() {
			return false;
		}
	}
	// support for authority, userInformation and port is slow to avoid even bigger classs mess
	private static class AuthorityURI extends BaseURI {
		protected final String authority;
		protected final boolean authorityIsHost;
		
		public AuthorityURI(String scheme, String authority, boolean authorityIsHost) {
			super(scheme);
			this.authority = authority.intern();
			this.authorityIsHost = authorityIsHost;
		}
		
		@Override
		public URI toURI() {
			try {
				return new URI(scheme, authority, null, null, null);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasPort() {
			return toURI().getPort() != -1;
		}
		@Override
		public int getPort() {
			return toURI().getPort();
		}
		@Override
		public Boolean hasUserInformation() {
			return toURI().getUserInfo() != null;
		}
		
		@Override
		public String getUserInformation() {
			return toURI().getUserInfo();
		}
		
		@Override
		public Boolean hasHost() {
			return authorityIsHost || toURI().getHost() != null;
		}
		@Override
		public String getHost() {
			if (authorityIsHost)  {
				return authority;
			}
			else {
				return toURI().getHost();
			}
		}
		
		@Override
		public Boolean hasAuthority() {
			return true;
		}
		@Override
		public String getAuthority() {
			return authority;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode() + authority.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof AuthorityURI){
				AuthorityURI u = (AuthorityURI)obj;
				return scheme == u.scheme
					&& authority == u.authority
					;
			}
			return false;
		}
	}
	
	private static class PathURI extends BaseURI {
		protected final String path;
		
		public PathURI(String scheme, String path) {
			super(scheme);
			this.path = path;
		}
		
		@Override
		public URI toURI() {
			try {
				return new URI(scheme, "", path, null, null);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasPath() {
			return true;
		}
		@Override
		public String getPath() {
			return path;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode() + path.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof PathURI){
				PathURI u = (PathURI)obj;
				return scheme == u.scheme
					&& path.equals(u.path);
			}
			return false;
		}
	}
	
	private static class PathAuthorityURI extends AuthorityURI {
		protected final String path;
		
		public PathAuthorityURI(String scheme, String authority, boolean authorityIsHost, String path) {
			super(scheme, authority, authorityIsHost);
			this.path = path;
		}
		
		@Override
		public URI toURI() {
			try {
				return new URI(scheme, authority, path, null, null);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasPath() {
			return true;
		}
		@Override
		public String getPath() {
			return path;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode() + authority.hashCode() + path.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof PathAuthorityURI){
				PathAuthorityURI u = (PathAuthorityURI)obj;
				return scheme == u.scheme
					&& authority == u.authority
					&& path.equals(u.path);
			}
			return false;
		}
	}
	
	private static class QueryURI extends BaseURI {
		protected final String query;
		
		public QueryURI(String scheme, String query) {
			super(scheme);
			this.query = query;
		}
		
		@Override
		public URI toURI() {
			try {
				return new URI(scheme, "", null, query, null);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasQuery() {
			return true;
		}
		@Override
		public String getQuery() {
			return query;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode() + query.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof QueryURI){
				QueryURI u = (QueryURI)obj;
				return scheme == u.scheme
					&& query.equals(u.query)
					;
			}
			return false;
		}
	}
	
	private static class QueryAuthorityURI extends AuthorityURI {
		protected final String query;
		
		public QueryAuthorityURI(String scheme, String authority, boolean authorityIsHost, String query) {
			super(scheme, authority, authorityIsHost);
			this.query = query;
		}
		
		@Override
		public URI toURI() {
			try {
				return new URI(scheme, authority, null, query, null);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasQuery() {
			return true;
		}
		@Override
		public String getQuery() {
			return query;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode() + authority.hashCode() + query.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof QueryAuthorityURI){
				QueryAuthorityURI u = (QueryAuthorityURI)obj;
				return scheme == u.scheme
					&& authority == u.authority
					&& query.equals(u.query)
					;
			}
			return false;
		}
	}
	
	private static class QueryPathURI extends PathURI {
		protected final String query;
		
		public QueryPathURI(String scheme, String path, String query) {
			super(scheme, path);
			this.query = query;
		}
		
		@Override
		public URI toURI() {
			try {
				return new URI(scheme, "", path, query, null);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasQuery() {
			return true;
		}
		@Override
		public String getQuery() {
			return query;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode() + path.hashCode() + query.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof QueryPathURI){
				QueryPathURI u = (QueryPathURI)obj;
				return scheme == u.scheme
					&& path.equals(u.path)
					&& query.equals(u.query)
					;
			}
			return false;
		}
	}
	
	private static class QueryPathAuthorityURI extends PathAuthorityURI {
		protected final String query;
		
		public QueryPathAuthorityURI(String scheme, String authority, boolean authorityIsHost, String path, String query) {
			super(scheme, authority, authorityIsHost, path);
			this.query = query;
		}
		
		@Override
		public URI toURI() {
			try {
				return new URI(scheme, authority, path, query, null);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasQuery() {
			return true;
		}
		@Override
		public String getQuery() {
			return query;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode() + authority.hashCode() + path.hashCode() + query.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof QueryPathAuthorityURI){
				QueryPathAuthorityURI u = (QueryPathAuthorityURI)obj;
				return scheme == u.scheme
					&& authority == u.authority
					&& path.equals(u.path)
					&& query.equals(u.query)
					;
			}
			return false;
		}
	}
	
	private static class FragmentURI extends BaseURI {
		protected final String fragment;
		
		public FragmentURI(String scheme, String fragment) {
			super(scheme);
			this.fragment = fragment;
		}
		
		@Override
		public URI toURI() {
			try {
				return new URI(scheme, "", null, null, fragment);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasFragment() {
			return true;
		}
		@Override
		public String getFragment() {
			return fragment;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode() + fragment.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof FragmentURI){
				FragmentURI u = (FragmentURI)obj;
				return scheme == u.scheme
					&& fragment.equals(u.fragment)
					;
			}
			return false;
		}
	}
	
	private static class FragmentAuthorityURI extends AuthorityURI {
		protected final String fragment;
		
		public FragmentAuthorityURI(String scheme, String authority, boolean authorityIsHost, String fragment) {
			super(scheme, authority, authorityIsHost);
			this.fragment = fragment;
		}
		
		@Override
		public URI toURI() {
			try {
				return new URI(scheme, authority, null, null, fragment);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasFragment() {
			return true;
		}
		@Override
		public String getFragment() {
			return fragment;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode() + authority.hashCode() + fragment.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof FragmentAuthorityURI){
				FragmentAuthorityURI u = (FragmentAuthorityURI)obj;
				return scheme == u.scheme
					&& authority == u.authority
					&& fragment.equals(u.fragment)
					;
			}
			return false;
		}
	}
	
	private static class FragmentPathURI extends PathURI {
		protected final String fragment;
		
		public FragmentPathURI(String scheme, String path, String fragment) {
			super(scheme, path);
			this.fragment = fragment;
		}
		
		@Override
		public URI toURI() {
			try {
				return new URI(scheme, "", path, null, fragment);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasFragment() {
			return true;
		}
		@Override
		public String getFragment() {
			return fragment;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode() + path.hashCode() + fragment.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof FragmentPathURI){
				FragmentPathURI u = (FragmentPathURI)obj;
				return scheme == u.scheme
					&& path.equals(u.path)
					&& fragment.equals(u.fragment)
					;
			}
			return false;
		}
	}
	
	private static class FragmentPathAuthorityURI extends PathAuthorityURI {
		protected final String fragment;
		
		public FragmentPathAuthorityURI(String scheme, String authority, boolean authorityIsHost, String path, String fragment) {
			super(scheme, authority, authorityIsHost, path);
			this.fragment = fragment;
		}
		
		@Override
		public URI toURI() {
			try {
				return new URI(scheme, authority, path, null, fragment);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasFragment() {
			return true;
		}
		@Override
		public String getFragment() {
			return fragment;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode() + authority.hashCode() + path.hashCode() + fragment.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof FragmentPathAuthorityURI){
				FragmentPathAuthorityURI u = (FragmentPathAuthorityURI)obj;
				return scheme == u.scheme
					&& authority == u.authority
					&& path.equals(u.path)
					&& fragment.equals(u.fragment)
					;
			}
			return false;
		}
	}
	private static class FragmentQueryURI extends QueryURI {
		protected final String fragment;
		
		public FragmentQueryURI(String scheme, String query, String fragment) {
			super(scheme, query);
			this.fragment = fragment;
		}
		
		@Override
		public URI toURI() {
			try {
				return new URI(scheme, "", null, query, fragment);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasFragment() {
			return true;
		}
		@Override
		public String getFragment() {
			return fragment;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode() + query.hashCode() + fragment.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof FragmentQueryURI){
				FragmentQueryURI u = (FragmentQueryURI)obj;
				return scheme == u.scheme
					&& query.equals(u.query)
					&& fragment.equals(u.fragment)
					;
			}
			return false;
		}
	}
	
	private static class FragmentQueryAuthorityURI extends QueryAuthorityURI {
		protected final String fragment;
		
		public FragmentQueryAuthorityURI(String scheme, String authority, boolean authorityIsHost, String query, String fragment) {
			super(scheme, authority, authorityIsHost, query);
			this.fragment = fragment;
		}
		
		@Override
		public URI toURI() {
			try {
				return new URI(scheme, authority, null, query, fragment);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasFragment() {
			return true;
		}
		@Override
		public String getFragment() {
			return fragment;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode() + authority.hashCode() + query.hashCode() + fragment.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof FragmentQueryAuthorityURI){
				FragmentQueryAuthorityURI u = (FragmentQueryAuthorityURI)obj;
				return scheme == u.scheme
					&& authority == u.authority
					&& query.equals(u.query)
					&& fragment.equals(u.fragment)
					;
			}
			return false;
		}
	}
	
	private static class FragmentQueryPathURI extends QueryPathURI {
		protected final String fragment;
		
		public FragmentQueryPathURI(String scheme, String path, String query, String fragment) {
			super(scheme, path, query);
			this.fragment = fragment;
		}
		
		@Override
		public URI toURI() {
			try {
				return new URI(scheme, "", path, query, fragment);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasFragment() {
			return true;
		}
		@Override
		public String getFragment() {
			return fragment;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode() + path.hashCode() + query.hashCode() + fragment.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof FragmentQueryPathURI){
				FragmentQueryPathURI u = (FragmentQueryPathURI)obj;
				return scheme == u.scheme
					&& path.equals(u.path)
					&& query.equals(u.query)
					&& fragment.equals(u.fragment)
					;
			}
			return false;
		}
	}
	
	private static class FragmentQueryPathAuthorityURI extends QueryPathAuthorityURI {
		protected final String fragment;
		
		public FragmentQueryPathAuthorityURI(String scheme, String authority, boolean authorityIsHost, String path, String query, String fragment) {
			super(scheme, authority, authorityIsHost, path, query);
			this.fragment = fragment;
		}
		
		@Override
		public URI toURI() {
			try {
				return new URI(scheme, authority, path, query, fragment);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasFragment() {
			return true;
		}
		@Override
		public String getFragment() {
			return fragment;
		}
		@Override
		public int hashCode() {
			return scheme.hashCode() + authority.hashCode() + path.hashCode() + query.hashCode() + fragment.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof FragmentQueryPathAuthorityURI){
				FragmentQueryPathAuthorityURI u = (FragmentQueryPathAuthorityURI)obj;
				return scheme == u.scheme
					&& authority == u.authority
					&& path.equals(u.path)
					&& query.equals(u.query)
					&& fragment.equals(u.fragment)
					;
			}
			return false;
		}
	}
	
}