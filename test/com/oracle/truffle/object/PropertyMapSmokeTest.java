package com.oracle.truffle.object;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

import com.oracle.truffle.api.object.Property;
import static java.lang.System.out;

public class PropertyMapSmokeTest {

	static Property p(int value) {
		return new PropertyMockObject(value);
	}

	static ImmutablePropertyMap createBasicMap() {
		return ImmutablePropertyMap.of().copyAndPut(p(5)).copyAndPut(p(7)).copyAndPut(p(-1))
				.copyAndPut(p(32)).copyAndPut(p(1500));
	}

	static ImmutablePropertyMap createBiggerMap() {
		return ImmutablePropertyMap.of().copyAndPut(p(5)).copyAndPut(p(7)).copyAndPut(p(-1))
				.copyAndPut(p(32)).copyAndPut(p(1500)).copyAndPut(p(34934502)).copyAndPut(p(3344))
				.copyAndPut(p(0)).copyAndPut(p(13)).copyAndPut(p(345)).copyAndPut(p(-15))
				.copyAndPut(p(33)).copyAndPut(p(32));
	}

	@Test
	public void basicContainsKeyValue() {
		ImmutablePropertyMap map = createBasicMap();

		assertTrue(map.containsKey("5"));
		assertTrue(map.containsValue(p(5)));

		assertTrue(map.containsKey("7"));
		assertTrue(map.containsValue(p(7)));

		assertTrue(map.containsKey("-1"));
		assertTrue(map.containsValue(p(-1)));

		assertTrue(map.containsKey("32"));
		assertTrue(map.containsValue(p(32)));

		assertTrue(map.containsKey("1500"));
		assertTrue(map.containsValue(p(1500)));
	}

	@Test
	public void basicOrderedKeyIterator() {
		ImmutablePropertyMap map = createBasicMap();

		Iterator<Object> it = map.orderedKeyIterator();

		assertEquals("5", it.next());
		assertEquals("7", it.next());
		assertEquals("-1", it.next());
		assertEquals("32", it.next());
		assertEquals("1500", it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void basicOrderedKeyIteratorAfterDeleteAndInsert() {
		ImmutablePropertyMap map = createBasicMap().copyAndRemove("-1").copyAndPut(p(-1));

		Iterator<Object> it = map.orderedKeyIterator();

		assertEquals("5", it.next());
		assertEquals("7", it.next());
		assertEquals("32", it.next());
		assertEquals("1500", it.next());
		assertEquals("-1", it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void basicOrderedKeyIteratorAfterReplace() {
		ImmutablePropertyMap map = createBasicMap().copyAndPut(p(-1));

		Iterator<Object> it = map.orderedKeyIterator();

		assertEquals("5", it.next());
		assertEquals("7", it.next());
		assertEquals("-1", it.next());
		assertEquals("32", it.next());
		assertEquals("1500", it.next());
		assertFalse(it.hasNext());
	}

	@Test
	public void basicReplaceRetainsSize() {
		assertEquals(createBasicMap().size(), createBasicMap().copyAndPut(p(-1)).size());
	}
	
	@Test
	public void basicToString() {
		ImmutablePropertyMap map = createBasicMap();
		out.println(map);
	}

}
