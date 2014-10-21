package org.eclipse.imp.pdb.facts.util;

public class DefaultTrieMap {
	
//	public static final <K, V> ImmutableMap<K, V> of() {
//		return TrieMap_5Bits.of();
//	}
//
//	public static final <K, V> ImmutableMap<K, V> of(Object... keyValuePairs) {
//		return TrieMap_5Bits.of(keyValuePairs);
//	}
//
//	public static final <K, V> TransientMap<K, V> transientOf() {
//		return TrieMap_5Bits.transientOf();
//	}
//
//	public static final <K, V> TransientMap<K, V> transientOf(Object... keyValuePairs) {
//		return TrieMap_5Bits.transientOf(keyValuePairs);
//	}

	
	public static final <K, V> ImmutableMap<K, V> of() {
		return TrieMap_BleedingEdge.of();
	}

	public static final <K, V> ImmutableMap<K, V> of(Object... keyValuePairs) {
		return TrieMap_BleedingEdge.of(keyValuePairs);
	}

	public static final <K, V> TransientMap<K, V> transientOf() {
		return TrieMap_BleedingEdge.transientOf();
	}

	public static final <K, V> TransientMap<K, V> transientOf(Object... keyValuePairs) {
		return TrieMap_BleedingEdge.transientOf(keyValuePairs);
	}
	
}
