package io.usethesource.vallang.tree;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringWriter;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.Setup;
import io.usethesource.vallang.impl.primitive.StringValue;
import io.usethesource.vallang.random.util.RandomUtil;
import io.usethesource.vallang.type.TypeFactory;

@RunWith(Parameterized.class)
public final class TreeStringTest {
	

	@Parameterized.Parameters
	public static Iterable<? extends Object> data() {
		return Setup.valueFactories();
	}
	
	private IString example;

	private final IValueFactory vf;

	private final Random rnd = new Random();
	
	public TreeStringTest(final IValueFactory vf) {
		this.vf = vf;
		this.example =  vf.string("ab").concat(vf.string("cd")).concat(vf.string("ef")).concat(vf.string("gh"));
	}

	private IString genString(int max) {
	    return vf.string(RandomUtil.string(rnd, rnd.nextInt(max)));
	}
	
	@Test 
	public void testRandomHashcodeEquals() {
	    for (int count = 0; count < 50; count++) {
	        int loops = 100 + rnd.nextInt(250);

	        try {
	            StringValue.setMaxFlatString(3);
	            StringValue.setMaxUnbalance(5);
	            StringBuilder b = new StringBuilder();
	            IString concat = genString(25);
	            b.append(concat.getValue());

	            for (int i = 0; i < loops; i++) {
	                IString next = genString(25);
	                concat = concat.concat(next);
	                b.append(next.getValue());
	            }

	            IString single = vf.string(b.toString());

	            assertTrue(single.hashCode() == concat.hashCode());
	            assertTrue(single.equals(concat));
	            assertTrue(concat.equals(single));
	        } 
	        finally {
	            StringValue.resetMaxFlatString();
	            StringValue.resetMaxUnbalance();
	        }
	    }
	}
	
	protected TypeFactory tf = TypeFactory.getInstance();

	protected void assertEqual(IValue l, IValue r) {
		assertTrue("Expected " + l + " got " + r, l.isEqual(r));
	}

	@Test
	public void testStringLength() {
	    assertTrue(example.substring(0, 0).length() == 0);
		assertTrue(example.substring(0, 1).length() == 1);
		assertTrue(example.substring(0, 2).length() == 2);
		assertTrue(example.substring(0, 3).length() == 3);
		assertTrue(example.substring(0, 4).length() == 4);
		assertTrue(example.substring(0, 5).length() == 5);
		assertTrue(example.substring(0, 6).length() == 6);
	}

	@Test
	public void testEquals() {
	    try {
	        StringValue.setMaxFlatString(1);
	        StringValue.setMaxUnbalance(1);
	        
	        IString x = vf.string("ab").concat(vf.string("cd")).concat(vf.string("ef")).concat(vf.string("gh"));
	        IString y = vf.string("abcdefgh");
	        IString z = vf.string("abcdefgi");

	        assertTrue(x.hashCode() == y.hashCode());
	        assertTrue(x.equals(y));
	        assertTrue(y.equals(x));
	        assertTrue(!z.equals(x));
	        assertTrue(x.substring(0,0).equals(vf.string("")));
	    }
	    finally {
	        StringValue.resetMaxFlatString();
	        StringValue.resetMaxUnbalance();
	    }
	}
	
	@Test
    public void testEqualsUnicode() {
        try {
            StringValue.setMaxFlatString(1);
            StringValue.setMaxUnbalance(1);

            IString x = vf.string("a🍕b").concat(vf.string("c🍕d")).concat(vf.string("e🍕f")).concat(vf.string("g🍕h"));
            IString y = vf.string("a🍕bc🍕de🍕fg🍕h");

            assertTrue(x.hashCode() == y.hashCode());
            assertTrue(x.equals(y));
            assertTrue(y.equals(x));
            assertTrue(x.substring(0,0).equals(vf.string("")));
        }
        finally {
            StringValue.resetMaxFlatString();
            StringValue.resetMaxUnbalance();
        }
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
		int n = 10;
        StringValue.setMaxFlatString(1);
        StringValue.setMaxUnbalance(0);
        String[] s = {"a", "b", "c", "d", "e", "f","g", "h"};
        IString str = vf.string(s[0]);
        for (int i=1;i<n; i++) {
              str = str.concat(vf.string(s[i%8]));
                   // result = vf.string(s[i%8]).concat(result);
               }
		// System.out.println(example.replace(4, 1, 4, vf.string("x")).getValue());
		assertEqual(example.replace(0, 1, 0, vf.string("x")), vf.string("xabcdefgh"));
		assertEqual(example.replace(1, 1, 1, vf.string("x")), vf.string("axbcdefgh"));
		assertEqual(example.replace(2, 1, 2, vf.string("x")), vf.string("abxcdefgh"));
		assertEqual(example.replace(3, 1, 3, vf.string("x")), vf.string("abcxdefgh"));
		assertEqual(example.replace(4, 1, 4, vf.string("x")), vf.string("abcdxefgh"));
		assertEqual(example.replace(5, 1, 5, vf.string("x")), vf.string("abcdexfgh"));
		assertEqual(example.replace(6, 1, 6, vf.string("x")), vf.string("abcdefxgh"));
		assertEqual(str.replace(6, 1, 6, vf.string("x").concat(vf.string("y"))), vf.string("abcdefxygh").concat(str.substring(8)));
	}
    
	@Test
	public void neverRunOutOfStack() {
	    int outofStack = 200000;

	    // first we have to know for sure that we would run out of stack with @see outOfStack iterations:
	    try {
	        StringValue.setMaxFlatString(1);
	        StringValue.setMaxUnbalance(Integer.MAX_VALUE);
	        
	        IString v = vf.string("x");
	        for (int i = 0; i < outofStack; i++) {
	            v = v.concat(vf.string("-" + i));
	        }
	        
	        try {
	            new StringWriter().write(v.toString());
	            fail("this should run out of stack");
	        }
	        catch (StackOverflowError e) {
	            // yes, that is what is expected
	        }
	    }
	    finally {
	        StringValue.resetMaxFlatString();
	        StringValue.resetMaxUnbalance();
	    }
	    
	    // then, with the maxFlatString and Unbalance parameters reset, we should _not_ run out of stack anymore:
	    IString v = vf.string("x");
        for (int i = 0; i < outofStack; i++) {
            v = v.concat(vf.string("-" + i));
        }
        
	    try {
	        new StringWriter().write(v.toString());  // do not remove this, this is the test
	        assertTrue(true);
	    }
	    catch (StackOverflowError e) {
	        fail("the tree balancer should have avoided a stack overflow");
	    }
	}
	
/** 
     * This method is for tuning and benchmarking purposes only.
     * It uses internal implementation details of IString values.
     * 
*/
	
	
	public void testBalanceFactor(int maxFlatString) {
		int n = 10000;
		for (int i = 0; i < 2; i++) {
			long startTime, estimatedTime;
	        try {
	        	System.out.println("Fully balanced maxFlatString:"+maxFlatString);
	        	StringValue.setMaxFlatString(maxFlatString);
	        	StringValue.setMaxUnbalance(0);
	        	startTime = System.nanoTime();
	        	IString example1 = (IString) genFixedString1(n);
	        	estimatedTime = (System.nanoTime() - startTime)/1000000;
	        	System.out.println("Fully Balanced "  + example1.length() + " " + estimatedTime + "ms");
	        }
	        finally {
	        	StringValue.resetMaxFlatString();
	        	StringValue.resetMaxUnbalance();
	        }
	        try {
	        	System.out.println("Partly balanced (1500) maxFlatString:"+maxFlatString);
	        	StringValue.setMaxFlatString(maxFlatString);
	        	StringValue.setMaxUnbalance(1500);
	        	startTime = System.nanoTime();
	        	IString example2 = genFixedString1(n);
	        	estimatedTime = (System.nanoTime() - startTime)/1000000;
	        	System.out.println("Partly balanced: " + " " + example2.length() + " " + estimatedTime + "ms");

	        }
	        finally {
	        	StringValue.resetMaxFlatString();
	        	StringValue.resetMaxUnbalance();
	        }
	        try {
	        	System.out.println("Simple maxFlatString:"+maxFlatString);
	        	StringValue.setMaxFlatString(10000000);
	        	StringValue.setMaxUnbalance(Integer.MAX_VALUE);
	        	startTime = System.nanoTime();
	        	IString example3 = genFixedString1(n);
	        	estimatedTime = (System.nanoTime() - startTime)/1000000;
	        	System.out.println("Simple" + " " + example3.length() + " " + estimatedTime + "ms");
	        }
	        finally {
	        	StringValue.resetMaxFlatString();
	        	StringValue.resetMaxUnbalance();
	        }
	        
	        try {
	        	System.out.println("Native maxFlatString:"+maxFlatString);
	        	startTime = System.nanoTime();
	        	IString example3 = genFlatString(n);
	        	estimatedTime = (System.nanoTime() - startTime)/1000000;
	        	System.out.println("Native" + " " + example3.length() + " " + estimatedTime + "ms");
	        }
	        finally {
	        	StringValue.resetMaxFlatString();
	        	StringValue.resetMaxUnbalance();
	        }
	        
	        // System.out.println(""+example3+" "+example4);
	        try {
	        	StringValue.setMaxFlatString(1);
	        	StringValue.setMaxUnbalance(0);
	        	IString ex1 = genFixedString1(n);
	        	IString ex2 = genFlatString(n);
	        	
	        	startTime = System.nanoTime();
	        	assert ex1.equals(ex2);
	        	estimatedTime = (System.nanoTime() - startTime)/1000000;
	        	System.out.println("Equals "  + estimatedTime + "ms");
	        	
	        	assert ex2.equals(ex1);
	        	assert !ex1.equals(ex2.replace(1, 1, 1, vf.string("x")));
	        	assertEqual(ex1, ex2);
	        }
	        finally {
	        	StringValue.resetMaxFlatString();
	        	StringValue.resetMaxUnbalance();
	        }
		}
	}
	
	
@Test
public void testBalanceFactor() {
	    testBalanceFactor(1);
	    testBalanceFactor(512);
        }
	
	
private IString genFixedString1(int n) {
		String[] s = {"a", "b", "c", "d", "e", "f","g", "h"};
        IString str = vf.string(s[0]);
        for (int i=1;i<n; i++) {
              str = str.concat(vf.string(s[i%8]));
              }
       return str;
       }

private IString genFlatString(int n) {
	String[] s = {"a", "b", "c", "d", "e", "f","g", "h"};
	StringBuffer str = new StringBuffer(n);
	for (int i=0;i<n; i++) {
        str = str.append(s[i%8]);
        }
	return vf.string(str.toString());
}

private IString genFixedString2(int n) {
	String[] s = {"a", "b", "c", "d", "e", "f","g", "h"};
    IString str = vf.string(s[0]);
    for (int i=1;i<n; i++) {
           str = vf.string(s[i%8]).concat(str);
           }
   
   return str;
   }

private int work(IString str) {
	int r = 0;
    for (Integer c : str) {
       	// System.out.print(Character.toChars(c));
    	    r+=c;
     }
    System.out.println("Sum:"+r);  
    return r;
}
		
@Test
public void testStringIterator1() {
    int  n =1000000;
	IString flatStr = genFlatString(n);
    for (int i=0;i<2;i++) {
		 long startTime, estimatedTime;
	        System.out.println("Fully balanced:"+n);
	        try {
	        	StringValue.setMaxFlatString(512);
	        	StringValue.setMaxUnbalance(0);
	        	IString str = genFixedString1(n);
	        	startTime = System.nanoTime();
	        	work(str);
	        	estimatedTime = (System.nanoTime() - startTime)/1000000;
	        	System.out.println("Fully Balanced:"+ estimatedTime + "ms");
	        }
	        finally {
	        	StringValue.resetMaxFlatString();
	        	StringValue.resetMaxUnbalance();
	        }
	        System.out.println("Partly balanced:"+n);
	        try {
	        	StringValue.setMaxFlatString(512);
	        	StringValue.setMaxUnbalance(512);
	        	IString str = genFixedString1(n);
	        	startTime = System.nanoTime();
	        	work(str);
	        	estimatedTime = (System.nanoTime() - startTime)/1000000;
	        	System.out.println("Partly Balanced:"+ estimatedTime + "ms");
	        }
	        finally {
	        	StringValue.resetMaxFlatString();
	        	StringValue.resetMaxUnbalance();
	        }
	        /*
	        System.out.println("Unbalanced:"+n);
	        try {
	        	StringValue.setMaxFlatString(512);
	        	StringValue.setMaxUnbalance(1000000);
	        	IString str = genFixedString1(n);
	        	startTime = System.nanoTime();
	        	work(str);
	        	estimatedTime = (System.nanoTime() - startTime)/1000000;
	        	System.out.println("Unbalanced:"+ estimatedTime + "ms");
	        }
	        finally {
	        	StringValue.resetMaxFlatString();
	        	StringValue.resetMaxUnbalance();
	        }
	        */	  
	        System.out.println("Simple :"+n);
	        try {
	        	
	        	startTime = System.nanoTime();
	        	work(flatStr);
	        	estimatedTime = (System.nanoTime() - startTime)/1000000;
	        	System.out.println("Simple:"+ estimatedTime + "ms");
	        }
	        finally {
	        	StringValue.resetMaxFlatString();
	        	StringValue.resetMaxUnbalance();
	        }
    }
}
	        
@Test
public void testStringIterator2() {
	    int  n =100000;
		IString flatStr = genFlatString(n);
	    for (int i=0;i<2;i++) {
	    		 long startTime, estimatedTime;
	    	        System.out.println("Fully balanced:"+n);
	    	        try {
	    	        	StringValue.setMaxFlatString(512);
	    	        	StringValue.setMaxUnbalance(0);
	    	        	IString str = genFixedString2(n);
	    	        	startTime = System.nanoTime();	
	    	        	work(str);
	    	        	estimatedTime = (System.nanoTime() - startTime)/1000000;
	    	        	System.out.println("Fully Balanced:"+ estimatedTime + "ms");
	    	        }
	    	        finally {
	    	        	StringValue.resetMaxFlatString();
	    	        	StringValue.resetMaxUnbalance();
	    	        }
	    	        System.out.println("Partly balanced:"+n);
	    	        try {
	    	        	StringValue.setMaxFlatString(512);
	    	        	StringValue.setMaxUnbalance(512);
	    	        	IString str = genFixedString2(n);
	    	        	startTime = System.nanoTime();
	    	        	work(str);
	    	        	estimatedTime = (System.nanoTime() - startTime)/1000000;
	    	        	System.out.println("Partly Balanced:"+ estimatedTime + "ms");
	    	        }
	    	        finally {
	    	        	StringValue.resetMaxFlatString();
	    	        	StringValue.resetMaxUnbalance();
	    	        }
	    	        System.out.println("Simple :"+n);
	    	        try {
	    	        	startTime = System.nanoTime();
	    	        	work(flatStr);
	    	        	estimatedTime = (System.nanoTime() - startTime)/1000000;
	    	        	System.out.println("Simple:"+ estimatedTime + "ms");
	    	        }
	    	        finally {
	    	        	StringValue.resetMaxFlatString();
	    	        	StringValue.resetMaxUnbalance();
	    	        }
	     }
} 




@Test
public void testIndent() {
	 IString s = vf.string("start\naap").indent(vf.string("123")).concat((vf.string("\nnoot\nteun").indent(vf.string("456")))).concat(vf.string("\nmies"));
	 System.out.println(s.getValue()+" "+s.length());
	  for (int i=0;i<10;i++)
	      System.out.print(Character.toChars(s.charAt(i)));
	 System.out.println(""+vf.string("\naap").indent(vf.string("123"))+"=="+vf.string("\n123aap"));
	 assertEqual(vf.string("\naap").indent(vf.string("123")), vf.string("\n123aap"));
     }

String simulateOld(String string, String indent) {
	StringBuffer buf  = new StringBuffer();
	String[] strings = string.split("\n");
	buf.append(strings[0]);
	for (int i=1;i<strings.length;i++) {
		buf.append("\n");
	    buf.append(indent);
	    buf.append(strings[i]);
	}
	return buf.toString();
}


@Test
public void compareIndent()  {
	int n  = 1000000;
	String indent = "123123";
	// String start = "start"+"a🍕🍕🍕🍕b";
	String start = "start";
	String stepc = "abc";
	String stepd = "abcd";
	IString header = vf.string(start);
	IString nextc = vf.string("\n"+stepc);
	IString nextd = vf.string("\n"+stepd);
	for (int i=0;i<2;i++) {
		System.out.println("Round:"+i);
		IString text = header;
		long startTime, estimatedTime;
		startTime = System.nanoTime();
		for (int j=0;j<n;j++) {
		   text = text.concat(j%2==0?nextc:nextd);
		   }
		estimatedTime = (System.nanoTime() - startTime)/1000000;
		System.out.println("Basis creation:"+ estimatedTime + "ms");
		
		startTime = System.nanoTime();
		IString oldString = vf.string(simulateOld(text.getValue(), indent));
		estimatedTime = (System.nanoTime() - startTime)/1000000;
		System.out.println("Old indentation:"+ estimatedTime + "ms");
		
		startTime = System.nanoTime();
		IString newString = text.indent(vf.string(indent));
		estimatedTime = (System.nanoTime() - startTime)/1000000;
		System.out.println("New indentation:"+ estimatedTime + "ms");
	  
	  startTime = System.nanoTime();
	  int oldWork = work(oldString);
	  estimatedTime = (System.nanoTime() - startTime)/1000000;
	  System.out.println("work old:"+ estimatedTime + "ms");
	  
	  startTime = System.nanoTime();
	  int newWork = work(newString);
	  estimatedTime = (System.nanoTime() - startTime)/1000000;
	  System.out.println("work new:"+ estimatedTime + "ms");
	  
	  
	  startTime = System.nanoTime();
	  
	  assertEqual(oldString, newString);
	  estimatedTime = (System.nanoTime() - startTime)/1000000;
	  System.out.println("Equals:"+ estimatedTime + "ms");
	  
	  startTime = System.nanoTime();
	  assertEqual(newString, oldString);
	  estimatedTime = (System.nanoTime() - startTime)/1000000;
	  System.out.println("Equals 2:"+ estimatedTime + "ms");
	  
	  assertEqual(vf.integer(oldWork), vf.integer(newWork));
	  
      }
}
}

