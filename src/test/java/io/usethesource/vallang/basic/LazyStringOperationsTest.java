package io.usethesource.vallang.basic;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Random;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.impl.primitive.StringValue;
import io.usethesource.vallang.random.util.RandomUtil;
import io.usethesource.vallang.type.TypeFactory;

public final class LazyStringOperationsTest {

    private final Random rnd = new Random();

    private IString example2(final IValueFactory vf) {
        return vf.string("abcdef\nxyz").indent(vf.string("123"), true);
    }

    private IString example1(final IValueFactory vf) {
        return vf.string("ab").concat(vf.string("cd")).concat(vf.string("ef")).concat(vf.string("gh"));
    }

    private IString genString(IValueFactory vf, int max) {
        return vf.string(RandomUtil.string(rnd, rnd.nextInt(max)));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRandomHashcodeEquals(IValueFactory vf) {
        for (int count = 0; count < 50; count++) {
            int loops = 100 + rnd.nextInt(250);

            try {
                StringValue.setMaxFlatString(3);
                StringValue.setMaxUnbalance(5);
                StringBuilder b = new StringBuilder();
                IString concat = genString(vf, 25);
                b.append(concat.getValue());

                for (int i = 0; i < loops; i++) {
                    IString next = genString(vf, 25);
                    concat = concat.concat(next);
                    b.append(next.getValue());
                }

                IString single = vf.string(b.toString());

                assertTrue(single.hashCode() == concat.hashCode());
                assertTrue(single.equals(concat));
                assertTrue(concat.equals(single));
            } finally {
                StringValue.resetMaxFlatString();
                StringValue.resetMaxUnbalance();
            }
        }
    }

    protected TypeFactory tf = TypeFactory.getInstance();

    protected void assertEqual(IValue l, IValue r) {
        assertTrue(l.equals(r), () -> "Expected " + l + " got " + r);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testStringLength(IValueFactory vf) {
        assertTrue(example1(vf).substring(0, 0).length() == 0);
        assertTrue(example1(vf).substring(0, 1).length() == 1);
        assertTrue(example1(vf).substring(0, 2).length() == 2);
        assertTrue(example1(vf).substring(0, 3).length() == 3);
        assertTrue(example1(vf).substring(0, 4).length() == 4);
        assertTrue(example1(vf).substring(0, 5).length() == 5);
        assertTrue(example1(vf).substring(0, 6).length() == 6);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testEquals(IValueFactory vf) {
        try {
            StringValue.setMaxFlatString(1);
            StringValue.setMaxUnbalance(1);

            IString x = example1(vf);
            IString y = vf.string("abcdefgh");
            IString z = vf.string("abcdefgi");

            assertTrue(x.hashCode() == y.hashCode());
            assertTrue(x.equals(y));
            assertTrue(y.equals(x));
            assertTrue(!z.equals(x));
            assertTrue(x.substring(0, 0).equals(vf.string("")));
        } finally {
            StringValue.resetMaxFlatString();
            StringValue.resetMaxUnbalance();
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testEqualsUnicode(IValueFactory vf) {
        try {
            StringValue.setMaxFlatString(1);
            StringValue.setMaxUnbalance(1);

            IString x = vf.string("a🍕b").concat(vf.string("c🍕d")).concat(vf.string("e🍕f")).concat(vf.string("g🍕h"));
            IString y = vf.string("a🍕bc🍕de🍕fg🍕h");

            assertTrue(x.hashCode() == y.hashCode());
            assertTrue(x.equals(y));
            assertTrue(y.equals(x));
            assertTrue(x.substring(0, 0).equals(vf.string("")));
        } finally {
            StringValue.resetMaxFlatString();
            StringValue.resetMaxUnbalance();
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testConcat(IValueFactory vf) {
        assertTrue(example1(vf)
                .equals(example1(vf)));
        assertTrue(example1(vf)
                .equals(vf.string("ab").concat(vf.string("cd")).concat(vf.string("ef").concat(vf.string("gh")))));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testStringCharAt(IValueFactory vf) {
        assertTrue(example2(vf).charAt(0) == '1');
        assertTrue(example2(vf).charAt(1) == '2');
        assertTrue(example2(vf).charAt(2) == '3');
        assertTrue(example2(vf).charAt(3) == 'a');
        assertTrue(example2(vf).charAt(4) == 'b');
        assertTrue(example2(vf).charAt(5) == 'c');
        assertTrue(example2(vf).charAt(6) == 'd');
        assertTrue(example2(vf).charAt(7) == 'e');
        assertTrue(example2(vf).charAt(8) == 'f');
        assertTrue(example2(vf).charAt(9) == '\n');
        assertTrue(example2(vf).charAt(10) == '1');
        assertTrue(example2(vf).charAt(11) == '2');
        assertTrue(example2(vf).charAt(12) == '3');
        assertTrue(example2(vf).charAt(13) == 'x');
        assertTrue(example2(vf).charAt(14) == 'y');
        assertTrue(example2(vf).charAt(15) == 'z');
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testStringSubString(IValueFactory vf) {
        assertEqual(example2(vf).substring(0, 1), vf.string("1"));
        assertEqual(example2(vf).substring(0, 2), vf.string("12"));
        assertEqual(example2(vf).substring(0, 3), vf.string("123"));
        assertEqual(example2(vf).substring(0, 4), vf.string("123a"));
        assertEqual(example2(vf).substring(0, 5), vf.string("123ab"));
        assertEqual(example2(vf).substring(0, 6), vf.string("123abc"));
        assertEqual(example2(vf).substring(0, 7), vf.string("123abcd"));
        assertEqual(example2(vf).substring(0, 8), vf.string("123abcde"));
        assertEqual(example2(vf).substring(0, 9), vf.string("123abcdef"));
        assertEqual(example2(vf).substring(0, 10), vf.string("123abcdef\n"));
        assertEqual(example2(vf).substring(0, 11), vf.string("123abcdef\n1"));
        assertEqual(example2(vf).substring(0, 12), vf.string("123abcdef\n12"));
        assertEqual(example2(vf).substring(0, 13), vf.string("123abcdef\n123"));
        assertEqual(example2(vf).substring(0, 14), vf.string("123abcdef\n123x"));
        assertEqual(example2(vf).substring(0, 15), vf.string("123abcdef\n123xy"));
        assertEqual(example2(vf).substring(0, 16), vf.string("123abcdef\n123xyz"));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testStringReplace(IValueFactory vf) {
        int n = 10;
        StringValue.setMaxFlatString(1);
        StringValue.setMaxUnbalance(0);
        String[] s = { "a", "b", "c", "d", "e", "f", "g", "h" };
        IString str = vf.string(s[0]);
        for (int i = 1; i < n; i++) {
            str = str.concat(vf.string(s[i % 8]));
            // result = vf.string(s[i%8]).concat(result);
        }
        // System.out.println(example.replace(4, 1, 4, vf.string("x")).getValue());
        assertEqual(example1(vf).replace(0, 1, 0, vf.string("x")), vf.string("xabcdefgh"));
        assertEqual(example1(vf).replace(1, 1, 1, vf.string("x")), vf.string("axbcdefgh"));
        assertEqual(example1(vf).replace(2, 1, 2, vf.string("x")), vf.string("abxcdefgh"));
        assertEqual(example1(vf).replace(3, 1, 3, vf.string("x")), vf.string("abcxdefgh"));
        assertEqual(example1(vf).replace(4, 1, 4, vf.string("x")), vf.string("abcdxefgh"));
        assertEqual(example1(vf).replace(5, 1, 5, vf.string("x")), vf.string("abcdexfgh"));
        assertEqual(example1(vf).replace(6, 1, 6, vf.string("x")), vf.string("abcdefxgh"));
        assertEqual(str.replace(6, 1, 6, vf.string("x").concat(vf.string("y"))),
                vf.string("abcdefxygh").concat(str.substring(8)));
    }

    private String fromReader(Reader r) throws IOException {
        try {
            var result = new StringBuilder();
            char[] buffer = new char[8 * 1024];
            int read = 0;
            while ((read = r.read(buffer)) > 0) {
                result.append(buffer, 0, read);
            }
            return result.toString();
        } finally {
            r.close();
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void neverRunOutOfStack(IValueFactory vf) throws IOException {
        int outofStack = 100000;

        // first we have to know for sure that we would run out of stack with @see
        // outOfStack iterations:
        try {
            StringValue.setMaxFlatString(1);
            StringValue.setMaxUnbalance(Integer.MAX_VALUE);

            IString v = vf.string("x");
            for (int i = 0; i < outofStack; i++) {
                v = v.concat(vf.string("-" + i));
            }

            try {
                v.write(new StringWriter());
                fail("this should run out of stack");
            } catch (StackOverflowError e) {
                // yes, that is what is expected
            } catch (IOException e) {
                // TODO Auto-generated catch block
                fail("unexpected IO:" + e);
            }
        } finally {
            StringValue.resetMaxFlatString();
            StringValue.resetMaxUnbalance();
        }

        // then, with the maxFlatString and Unbalance parameters reset, we should _not_
        // run out of stack anymore:
        IString v = vf.string("x");
        for (int i = 0; i < outofStack; i++) {
            v = v.concat(vf.string("-" + i));
        }

        try {
            new StringWriter().write(v.toString()); // do not remove this, this is the test
            assertTrue(true);
            fromReader(v.asReader());
            assertTrue(true);
        } catch (StackOverflowError e) {
            fail("the tree balancer should have avoided a stack overflow");
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testIndent(IValueFactory vf) {
        assertEqual(vf.string("\naap").indent(vf.string("123"), true), vf.string("123\n123aap"));
    }

    IString simulateOld(IValueFactory vf, String string, String indent) {
        StringBuffer buf = new StringBuffer();
        String[] strings = string.split("\n");
        for (int i = 0; i < strings.length; i++) {
            buf.append(indent);
            buf.append(strings[i]);

            if (i != strings.length - 1 || string.charAt(string.length() - 1) == '\n') {
                buf.append('\n');
            }
        }
        return vf.string(buf.toString());
    }
}
