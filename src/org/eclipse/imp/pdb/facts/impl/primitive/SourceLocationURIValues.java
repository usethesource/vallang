package org.eclipse.imp.pdb.facts.impl.primitive;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/*
 * Not supported: in URI class, scheme is case insensitive, but this is already kinda broken, since on windows & osx, so should path's be.
 */
/*package*/ class SourceLocationURIValues {
	static IURI newURI(URI base) throws URISyntaxException  {
		return newURI(base.getScheme(), base.getAuthority(),base.getPath(), base.getQuery(), base.getFragment());
	}

	private static final Pattern schemePattern = Pattern.compile("[A-Za-z][A-Za-z0-9+\\-.]*");
	private static final Pattern doubleSlashes = Pattern.compile("//+");
	static IURI newURI(String scheme, String authority, String path, String query, String fragment) throws URISyntaxException  {
		if (path != null) {
			if (path.isEmpty()) {
				path = null;
			}
			else if (!path.startsWith("/")) {
				path = "/" + path;
			}
			if (path != null) {
				// normalize double or longer slashes
				path = doubleSlashes.matcher(path).replaceAll("/");
			}
		}
		if (scheme == null || scheme.equals("")) {
			throw new URISyntaxException(scheme, "scheme cannot be empty or null");
		}
		if (!schemePattern.matcher(scheme).matches()) {
			throw new URISyntaxException(scheme, "Scheme is not a valid scheme");
		}
		if (authority == null || authority.equals("")) {
			if (path == null || path.equals("/")) {
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
					return new SourceLocationURIValues.AuthorityURI(scheme, authority);
				}
				return new SourceLocationURIValues.FragmentAuthorityURI(scheme, authority, fragment);
			}
			if (fragment == null) {
				return new SourceLocationURIValues.QueryAuthorityURI(scheme, authority, query);
			}
			return new SourceLocationURIValues.FragmentQueryAuthorityURI(scheme, authority, query, fragment);
		}
		if (query == null) {
			if (fragment == null) {
				return new SourceLocationURIValues.PathAuthorityURI(scheme, authority, path);
			}
			return new SourceLocationURIValues.FragmentPathAuthorityURI(scheme, authority, path, fragment);
		}
		if (fragment == null) {
			return new SourceLocationURIValues.QueryPathAuthorityURI(scheme, authority, path, query);
		}
		return new SourceLocationURIValues.FragmentQueryPathAuthorityURI(scheme, authority, path, query, fragment);
	}
	
	
	private static class BaseURI implements IURI {
		protected final String scheme;
		
		
		public BaseURI(String scheme)  {
			this.scheme = scheme.intern();
		}
		

		public URI getURI() {
			try {
				return new URI(scheme,"","/",null,null);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if(obj.getClass() == getClass()){
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
			throw new UnsupportedOperationException();
		}

		@Override
		public String getPath() {
			return "/";
		}

		@Override
		public String getFragment() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getQuery() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Boolean hasAuthority() {
			return false;
		}

		@Override
		public Boolean hasPath() {
			return true;
		}

		@Override
		public Boolean hasFragment() {
			return false;
		}

		@Override
		public Boolean hasQuery() {
			return false;
		}

	}
	
	private static class AuthorityURI extends BaseURI {
		protected final String authority;
		
		public AuthorityURI(String scheme, String authority)  {
			super(scheme);
			this.authority = authority.intern();
		}
		
		@Override
		public URI getURI() {
			try {
				return new URI(scheme, authority, null, null, null);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Internal state corrupted?", e);
			}
		}
		
		@Override
		public Boolean hasPath() {
			return false;
		}
		@Override
		public String getPath() {
			throw new UnsupportedOperationException();
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
			if(obj.getClass() == getClass()){
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
		
		public PathURI(String scheme, String path)  {
			super(scheme);
			this.path = path;
		}
		
		@Override
		public URI getURI() {
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
			if(obj.getClass() == getClass()){
				PathURI u = (PathURI)obj;
				return scheme == u.scheme
					&& path.equals(u.path);
			}
			return false;
		}
	}
	
	private static class PathAuthorityURI extends AuthorityURI {
		protected final String path;
		
		public PathAuthorityURI(String scheme, String authority, String path)  {
			super(scheme, authority);
			this.path = path;
		}
		
		@Override
		public URI getURI() {
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
			if(obj.getClass() == getClass()){
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
		
		public QueryURI(String scheme, String query)  {
			super(scheme);
			this.query = query;
		}
		
		@Override
		public URI getURI() {
			try {
				return new URI(scheme, "", "/", query, null);
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
			if(obj.getClass() == getClass()){
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
		
		public QueryAuthorityURI(String scheme, String authority, String query)  {
			super(scheme, authority);
			this.query = query;
		}
		
		@Override
		public URI getURI() {
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
			if(obj.getClass() == getClass()){
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
		
		public QueryPathURI(String scheme, String path, String query)  {
			super(scheme, path);
			this.query = query;
		}
		
		@Override
		public URI getURI() {
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
			if(obj.getClass() == getClass()){
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
		
		public QueryPathAuthorityURI(String scheme, String authority, String path, String query)  {
			super(scheme, authority, path);
			this.query = query;
		}
		
		@Override
		public URI getURI() {
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
			if(obj.getClass() == getClass()){
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
		
		public FragmentURI(String scheme, String fragment)  {
			super(scheme);
			this.fragment = fragment;
		}
		
		@Override
		public URI getURI() {
			try {
				return new URI(scheme, "", "/", null, fragment);
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
			if(obj.getClass() == getClass()){
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
		
		public FragmentAuthorityURI(String scheme, String authority, String fragment)  {
			super(scheme, authority);
			this.fragment = fragment;
		}
		
		@Override
		public URI getURI() {
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
			if(obj.getClass() == getClass()){
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
		
		public FragmentPathURI(String scheme, String path, String fragment)  {
			super(scheme, path);
			this.fragment = fragment;
		}
		
		@Override
		public URI getURI() {
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
			if(obj.getClass() == getClass()){
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
		
		public FragmentPathAuthorityURI(String scheme, String authority, String path, String fragment)  {
			super(scheme, authority, path);
			this.fragment = fragment;
		}
		
		@Override
		public URI getURI() {
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
			if(obj.getClass() == getClass()){
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
		
		public FragmentQueryURI(String scheme, String query, String fragment)  {
			super(scheme, query);
			this.fragment = fragment;
		}
		
		@Override
		public URI getURI() {
			try {
				return new URI(scheme, "", "/", query, fragment);
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
			if(obj.getClass() == getClass()){
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
		
		public FragmentQueryAuthorityURI(String scheme, String authority, String query, String fragment)  {
			super(scheme, authority, query);
			this.fragment = fragment;
		}
		
		@Override
		public URI getURI() {
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
			if(obj.getClass() == getClass()){
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
		
		public FragmentQueryPathURI(String scheme, String path, String query, String fragment)  {
			super(scheme, path, query);
			this.fragment = fragment;
		}
		
		@Override
		public URI getURI() {
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
			if(obj.getClass() == getClass()){
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
		
		public FragmentQueryPathAuthorityURI(String scheme, String authority, String path, String query, String fragment)  {
			super(scheme, authority, path, query);
			this.fragment = fragment;
		}
		
		@Override
		public URI getURI() {
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
			if(obj.getClass() == getClass()){
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