package org.eclipse.imp.pdb.facts.util;

public class DefaultTrieSet {

//	public static final <K> ImmutableSet<K> of() {
//		return TrieSet_5Bits.of();
//	}
//
//	@SuppressWarnings("unchecked")
//	public static final <K> ImmutableSet<K> of(K... keys) {
//		return TrieSet_5Bits.of(keys);
//	}
//
//	public static final <K> TransientSet<K> transientOf() {
//		return TrieSet_5Bits.transientOf();
//	}
//
//	@SuppressWarnings("unchecked")
//	public static final <K> TransientSet<K> transientOf(K... keys) {
//		return TrieSet_5Bits.transientOf(keys);
//	}
	
	
	public static final <K> ImmutableSet<K> of() {
		return TrieSet_BleedingEdge.of();
	}

	@SuppressWarnings("unchecked")
	public static final <K> ImmutableSet<K> of(K... keys) {
		return TrieSet_BleedingEdge.of(keys);
	}

	public static final <K> TransientSet<K> transientOf() {
		return TrieSet_BleedingEdge.transientOf();
	}

	@SuppressWarnings("unchecked")
	public static final <K> TransientSet<K> transientOf(K... keys) {
		return TrieSet_BleedingEdge.transientOf(keys);
	}	

}
