/** 
 * Copyright (c) 2017, Davy Landman, SWAT.engineering
 * All rights reserved. 
 *  
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: 
 *  
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 *  
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */ 
package io.usethesource.vallang.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class WeakReferenceFlyweightCache<T> {
	private WeakEntry<T>[] table;
	private int count;
	private final ReferenceQueue<T> cleared;

	private static final int INITIAL_CAPACITY = 32;
	private static final int MAX_CAPACITY = 1 << 30;

	@SuppressWarnings("unchecked")
	public WeakReferenceFlyweightCache() {
		table = new WeakEntry[INITIAL_CAPACITY];
		count = 0;
		cleared = new ReferenceQueue<>();
	}
	
	private int bucket(int hash) {
		return hash % table.length;
	}
	
	public T getFlyweight(T key) {
		cleanup();
		int hash = key.hashCode();
		int bucket = bucket(hash);
		WeakEntry<T>[] table = this.table;
		WeakEntry<T> cur = table[bucket];
		while (cur != null) {
			if (cur.hash == hash) {
				T result = cur.get();
				if (result != null && result.equals(key)) {
					return result;
				}
			}
			cur = cur.next;
		}
		// end of the chain reached, so inserting
		if (resize()) {
			// in this case, we have to re-find our insertion spot:
			bucket = bucket(hash);
			table = this.table;
		}
		// put them in front of the chain
		table[bucket] = new WeakEntry<>(key, hash, table[bucket], cleared);
		count++;
		return key;
	}
	
	@SuppressWarnings("unchecked")
	private void cleanup() {
		WeakEntry<T> entry;
		int clearCount = 0;
		while ((entry = (WeakEntry<T>) cleared.poll()) != null) {
			int bucket = bucket(entry.hash);
			WeakEntry<T> prev = null;
			WeakEntry<T> cur = table[bucket];
			while (cur != entry) {
				prev = cur;
				cur = cur.next;
				assert cur != null; // we have to find entry in this bucket
			}
			if (prev == null) {
				table[bucket] = entry.next;
			}
			else {
				prev.next = entry.next;
			}
			count--;
			clearCount++;
		}
		if (clearCount > 0) {
			System.err.println("Cleared: " + clearCount);
			System.err.flush();
		}
	}

	private boolean resize() {
		int newSize = table.length;
		int count = this.count;
		// improve performance of the capacity calculation
		if ((count + 1) > (newSize * 0.8)) {
			newSize = newSize * 2;
		}
		else if (newSize > INITIAL_CAPACITY && ((count + 1) < ((newSize / 2) * 0.7))) {
			// we can clear space
			newSize = INITIAL_CAPACITY;
			while ((count + 1) > (newSize * 0.8)) {
				newSize *= 2;
			}
		}

		if (0 >= newSize || newSize > MAX_CAPACITY) {
			newSize = MAX_CAPACITY;
		}
		if (newSize != table.length) {
			WeakEntry<T>[] oldTable = table;
			@SuppressWarnings("unchecked")
			WeakEntry<T>[] newTable = new WeakEntry[newSize];
			int moved = 0;
			for (WeakEntry<T> rootEntry : oldTable) {
				if (moved == count) {
					// don't have to iterate through rest of the table
					break;
				}
				WeakEntry<T> cur = rootEntry;
				while (cur != null) {
					WeakEntry<T> oldNext = cur.next; // store the current next part of the chain

					// put the entry at the front of the bucket
					int newBucket = cur.hash % newSize;
					cur.next = newTable[newBucket];
					newTable[newBucket] = cur;
					moved++;

					cur = oldNext; // move to the next part of the old chain
				}
			}
			table = newTable;
			return true;
		}
		return false;
	}

	private static final class WeakEntry<T> extends WeakReference<T> {
		private final int hash;
		private WeakEntry<T> next;

		public WeakEntry(T referent, int hash, WeakEntry<T> next, ReferenceQueue<? super T> q) {
			super(referent, q);
			this.hash = hash;
			this.next = next;
		}
	}
}
