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

import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class FlyweightCacheTest {
	
	private final class FixedHashEquals {
		private final int hash;
		private final int equals;
		
		public FixedHashEquals(int hash, int equals) {
			this.hash = hash;
			this.equals = equals;
		}
		
		@Override
		public int hashCode() {
			return hash;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof FixedHashEquals) {
				return ((FixedHashEquals)obj).hash == hash && ((FixedHashEquals)obj).equals == equals;
			}
			return false;
		}
		
		@Override
		public String toString() {
			return "" + hash + ":" + equals + "@" + System.identityHashCode(this);
		}
		
		@Override
		protected Object clone() throws CloneNotSupportedException {
			return new FixedHashEquals(hash, equals);
		}
		
	}
	
	private WeakReferenceFlyweightCache<FixedHashEquals> target;

	@Before
	public void constructData() {
		target = new WeakReferenceFlyweightCache<>();
	}
	
	@Test
	public void restoreSameReference() {
		FixedHashEquals a = new FixedHashEquals(1,1);
		assertSame(a, target.getFlyweight(a));
		assertSame(a, target.getFlyweight(a));
	}

	@Test
	public void restoreDifferentReference() {
		FixedHashEquals a = new FixedHashEquals(1,1);
		FixedHashEquals b = new FixedHashEquals(1,2);
		assertSame(a, target.getFlyweight(a));
		assertSame(b, target.getFlyweight(b));

		assertSame(a, target.getFlyweight(a));
	}

	@Test
	public void restoreOldReference() {
		FixedHashEquals a = new FixedHashEquals(1,1);
		FixedHashEquals b = new FixedHashEquals(1,1);
		assertSame(a, target.getFlyweight(a));
		assertSame(a, target.getFlyweight(b));
		assertSame(a, target.getFlyweight(a));
	}
	
	@Test
	public void looseReference() throws InterruptedException {
		FixedHashEquals a = new FixedHashEquals(1,1);
		assertSame(a, target.getFlyweight(a));
		a = null;
		System.gc();
		Thread.sleep(10); // wait for the GC to finish

		// a new reference, the target should not have kept the old reference
		a = new FixedHashEquals(1,1);
		assertSame(a, target.getFlyweight(a));
	}
	
	
	@Test
	public void storeManyObjects() {
		List<FixedHashEquals> objects = new ArrayList<>();
		for (int i = 0; i < 1024*1024; i ++) {
			objects.add(new FixedHashEquals(i, i));
		}
		
		// store them 
		for (FixedHashEquals o : objects) {
			assertSame(o, target.getFlyweight(o));
		}

		// and then again, to see that they are all in there
		for (FixedHashEquals o : objects) {
			assertSame(o, target.getFlyweight(o));
		}
	}
	
	private static int weakenHash(int hash, int maxEntries) {
		return hash % maxEntries;
	}

	@Test
	public void storeManyObjectsAndLooseThem() throws CloneNotSupportedException, InterruptedException {
		FixedHashEquals[] objects = createTestObjects(1024*1024, 64);
		
		// store them 
		for (FixedHashEquals o : objects) {
			assertSame(o, target.getFlyweight(o));
		}
		
		FixedHashEquals a = (FixedHashEquals) objects[0].clone();
		// loose  them
		Arrays.fill(objects, null);
		objects = null;
		System.gc();
		// wait for GC and reference queue to finish
		Thread.sleep(200);
		

		// check if the clone won't return the old reference
		assertSame(a, target.getFlyweight(a));
	}
	

	@Test
	public void storeManyObjectsAndQueryThem() throws InterruptedException, CloneNotSupportedException {
		FixedHashEquals[] objects = createTestObjects(1024*1024, 64);
		
		// store them 
		for (FixedHashEquals o : objects) {
			assertSame(o, target.getFlyweight(o));
		}
		
		// look up
		for (int i = 0; i < objects.length; i ++) {
			assertSame(objects[i],  target.getFlyweight((FixedHashEquals) objects[i].clone()));
		}
	}

	private FixedHashEquals[] createTestObjects(int size, int groups) {
		FixedHashEquals[] objects = new FixedHashEquals[size];
		for (int i = 0; i < size; i ++) {
			objects[i] = new FixedHashEquals(weakenHash(i, size / groups), i);
		}
		return objects;
	}

}
