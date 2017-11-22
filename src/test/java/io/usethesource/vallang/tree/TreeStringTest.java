package io.usethesource.vallang.tree;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.Setup;
import io.usethesource.vallang.impl.primitive.StringValue;
import io.usethesource.vallang.type.TypeFactory;

@RunWith(Parameterized.class)
public final class TreeStringTest {

	@Parameterized.Parameters
	public static Iterable<? extends Object> data() {
		return Setup.valueFactories();
	}
	
	private IString example, example1, example2;

	private final IValueFactory vf;

	public TreeStringTest(final IValueFactory vf) {
		this.vf = vf;
		this.example =  vf.string("ab").concat(vf.string("cd")).concat(vf.string("ef")).concat(vf.string("gh"));
		this.example1 = vf.string("ab").concat(vf.string("cd").concat(vf.string("ef").concat(vf.string("gh"))));
		this.example2 = example.concat(example1);
		
	}

	protected TypeFactory tf = TypeFactory.getInstance();

	protected void assertEqual(IValue l, IValue r) {
		assertTrue("Expected " + l + " got " + r, l.isEqual(r));
	}

	@Test
	public void testStringLength() {
		assertTrue(example.substring(0, 1).length() == 1);
		assertTrue(example.substring(0, 2).length() == 2);
		assertTrue(example.substring(0, 3).length() == 3);
		assertTrue(example.substring(0, 4).length() == 4);
		assertTrue(example.substring(0, 5).length() == 5);
		assertTrue(example.substring(0, 6).length() == 6);
	}

	@Test
	public void testConcat() {
		assertTrue(example.isEqual(vf.string("ab").concat(vf.string("cd")).concat(vf.string("ef")).concat(vf.string("gh"))));  
		assertTrue(example.
				isEqual(vf.string("ab").concat(vf.string("cd")).concat(vf.string("ef").concat(vf.string("gh")))));
	}

	@Test
	public void testStringCharAt() {
		assertTrue(example.charAt(0) == 'a');
		assertTrue(example.charAt(1) == 'b');
		assertTrue(example.charAt(2) == 'c');
		assertTrue(example.charAt(3) == 'd');
		assertTrue(example.charAt(4) == 'e');
		assertTrue(example.charAt(5) == 'f');
	}

	@Test
	public void testStringSubString() {
		assertEqual(example.substring(0, 1), vf.string("a"));
		assertEqual(example.substring(0, 2), vf.string("ab"));
		assertEqual(example.substring(0, 3), vf.string("abc"));
		assertEqual(example.substring(0, 4), vf.string("abcd"));
		assertEqual(example.substring(0, 5), vf.string("abcde"));
		assertEqual(example.substring(0, 6), vf.string("abcdef"));
	}

	@Test
	public void testStringReplace() {
		// System.out.println(example.replace(4, 1, 4, vf.string("x")).getValue());
		assertEqual(example.replace(0, 1, 0, vf.string("x")), vf.string("xabcdefgh"));
		assertEqual(example.replace(1, 1, 1, vf.string("x")), vf.string("axbcdefgh"));
		assertEqual(example.replace(2, 1, 2, vf.string("x")), vf.string("abxcdefgh"));
		assertEqual(example.replace(3, 1, 3, vf.string("x")), vf.string("abcxdefgh"));
		assertEqual(example.replace(4, 1, 4, vf.string("x")), vf.string("abcdxefgh"));
		assertEqual(example.replace(5, 1, 5, vf.string("x")), vf.string("abcdexfgh"));
		assertEqual(example.replace(6, 1, 6, vf.string("x")), vf.string("abcdefxgh"));
	}
    
	@Test
	public void testEquals() {
		assertTrue(vf.string("abc").concat(vf.string("de")).isEqual(vf.string("ab").concat(vf.string("cd")).concat(vf.string("e"))));
	}
	
	@Test
	public void testBalanceFactor() {
	    assertTrue(StringValue.tuneBalancedTreeParameters());
	}
}