package org.eclipse.imp.pdb.facts.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DefaultTrieMap {

	private static Class<?> target = TrieMap_5Bits.class;

	private static Method persistentMapOfEmpty;
	private static Method persistentMapOfKeyValuePairs;

	private static Method transientMapOfEmpty;
	private static Method transientMapOfKeyValuePairs;

	static {
		try {
			persistentMapOfEmpty = target.getMethod("of");
			persistentMapOfKeyValuePairs = target.getMethod("of", Object[].class);

			transientMapOfEmpty = target.getMethod("transientOf");
			transientMapOfKeyValuePairs = target.getMethod("transientOf", Object[].class);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> ImmutableMap<K, V> of() {
		try {
			return (ImmutableMap<K, V>) persistentMapOfEmpty.invoke(null);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> ImmutableMap<K, V> of(Object... keyValuePairs) {
		try {
			return (ImmutableMap<K, V>) persistentMapOfKeyValuePairs.invoke(null,
							(Object) keyValuePairs);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> TransientMap<K, V> transientOf() {
		try {
			return (TransientMap<K, V>) transientMapOfEmpty.invoke(null);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static final <K, V> TransientMap<K, V> transientOf(Object... keyValuePairs) {
		try {
			return (TransientMap<K, V>) transientMapOfKeyValuePairs.invoke(null,
							(Object) keyValuePairs);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
