package com.oracle.truffle.object;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

import com.oracle.truffle.api.object.Property;

public class PropertyMapSmokeTest {

	static Property p(int value) {
		return new PropertyMockObject(value);
	}

	@Test
	public void basicContainsKey() {
		ImmutablePropertyMap map = ImmutablePropertyMap.of().copyAndPut(p(5)).copyAndPut(p(7))
				.copyAndPut(p(32)).copyAndPut(p(1500));

		assertTrue(map.containsKey("5"));
		assertTrue(map.containsValue(p(5)));

		assertTrue(map.containsKey("7"));
		assertTrue(map.containsValue(p(7)));

		assertTrue(map.containsKey("32"));
		assertTrue(map.containsValue(p(32)));

		assertTrue(map.containsKey("1500"));
		assertTrue(map.containsValue(p(1500)));
	}

	@Test
	public void basicOrderedKeyIterator() {
		ImmutablePropertyMap map = ImmutablePropertyMap.of().copyAndPut(p(5)).copyAndPut(p(7))
				.copyAndPut(p(32)).copyAndPut(p(1500));

		Iterator<Object> it = map.orderedKeyIterator();

		assertEquals("5", it.next());
		assertEquals("7", it.next());
		assertEquals("32", it.next());
		assertEquals("1500", it.next());
	}

}
