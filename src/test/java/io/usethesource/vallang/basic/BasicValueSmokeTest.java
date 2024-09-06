package io.usethesource.vallang.basic;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.PrimitiveIterator.OfInt;
import java.util.Random;
import java.util.regex.Pattern;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.IDateTime;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.INumber;
import io.usethesource.vallang.IReal;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.impl.primitive.StringValue;
import io.usethesource.vallang.random.util.RandomUtil;
import io.usethesource.vallang.type.TypeFactory;

public final class BasicValueSmokeTest {

  protected void assertEqual(IValue l, IValue r) {
    assertTrue(l.equals(r), () -> "Expected " + l + " got " + r);
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testRationalToReal(IValueFactory vf) {
    assertTrue(vf.rational(1, 4).toReal(3).equals(vf.real(0.25)));
  }
  
  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testBrokenGetURI(IValueFactory vf) {
      try {
  
          // PathURI
        ISourceLocation loc1 = vf.sourceLocation("UJ", "", "/pkZ/T5/17152/7/𒉻𒂮𠇯");
        assertEquals("|UJ:///pkZ/T5/17152/7/%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF|", loc1.toString());
        
        // PathAuthorityURI
        ISourceLocation loc2 = vf.sourceLocation("UJ", "UK", "/pkZ/T5/17152/7/𒉻𒂮𠇯");
        assertEquals("|UJ://UK/pkZ/T5/17152/7/%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF|", loc2.toString());
        
        // PathAuthorityURI
        ISourceLocation loc3 = vf.sourceLocation("UJ", "UK", "/pkZ/T5/17152/7/𒉻𒂮𠇯");
        assertEquals("|UJ://UK/pkZ/T5/17152/7/%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF|", loc3.toString());
        // QueryURI
        ISourceLocation loc4 = vf.sourceLocation("UJ", "", "", "bla=𒉻𒂮𠇯", "");
        assertEquals("|UJ:///?bla=%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF|", loc4.toString());
        
        // QueryAuthorityURI
        ISourceLocation loc5 = vf.sourceLocation("UJ", "UK", "", "bla=𒉻𒂮𠇯", "");
        assertEquals("|UJ://UK?bla=%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF|", loc5.toString());
        
        // QueryPathURI
        ISourceLocation loc6 = vf.sourceLocation("UJ", "", "pkZ/T5/17152/7/𒉻𒂮𠇯", "bla=𒉻𒂮𠇯", "");
        assertEquals("|UJ:///pkZ/T5/17152/7/%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF?bla=%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF|", loc6.toString());
        
        // QueryPathAuthorityURI
        ISourceLocation loc7 = vf.sourceLocation("UJ", "UK", "pkZ/T5/17152/7/𒉻𒂮𠇯", "bla=𒉻𒂮𠇯", "");
        assertEquals("|UJ://UK/pkZ/T5/17152/7/%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF?bla=%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF|", loc7.toString());
        
        // FragmentURI
        ISourceLocation loc8 = vf.sourceLocation("UJ", "", "","", "𒉻𒂮𠇯");
        assertEquals("|UJ:///#%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF|", loc8.toString());
        
        // FragmentAuthorityURI
        ISourceLocation loc9 = vf.sourceLocation("UJ", "UK", "","", "𒉻𒂮𠇯");
        assertEquals("|UJ://UK#%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF|", loc9.toString());
        
        // FragmentPathURI
        ISourceLocation loc10 = vf.sourceLocation("UJ", "", "pkZ/T5/17152/7/𒉻𒂮𠇯","", "𒉻𒂮𠇯");
        assertEquals("|UJ:///pkZ/T5/17152/7/%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF#%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF|", loc10.toString());
        
        // FragmentPathAuthorityURI
        ISourceLocation loc11 = vf.sourceLocation("UJ", "UK", "pkZ/T5/17152/7/𒉻𒂮𠇯","", "𒉻𒂮𠇯");
        assertEquals("|UJ://UK/pkZ/T5/17152/7/%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF#%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF|", loc11.toString());
        
        // FragmentQueryURI
        ISourceLocation loc12 = vf.sourceLocation("UJ", "", "","bla=𒉻𒂮𠇯", "𒉻𒂮𠇯");
        assertEquals("|UJ:///?bla=%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF#%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF|", loc12.toString());
        
        // FragmentQueryAuthorityURI
        ISourceLocation loc13 = vf.sourceLocation("UJ", "UK", "","bla=𒉻𒂮𠇯", "𒉻𒂮𠇯");
        assertEquals("|UJ://UK?bla=%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF#%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF|", loc13.toString());
        
        // FragmentQueryPathURI
        ISourceLocation loc14 = vf.sourceLocation("UJ", "", "pkZ/T5/17152/7/𒉻𒂮𠇯","bla=𒉻𒂮𠇯", "𒉻𒂮𠇯");
        assertEquals("|UJ:///pkZ/T5/17152/7/%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF?bla=%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF#%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF|", loc14.toString());

        // FragmentQueryPathAuthorityURI
        ISourceLocation loc15 = vf.sourceLocation("UJ", "UK", "pkZ/T5/17152/7/𒉻𒂮𠇯","bla=𒉻𒂮𠇯", "𒉻𒂮𠇯");
        assertEquals("|UJ://UK/pkZ/T5/17152/7/%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF?bla=%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF#%F0%92%89%BB%F0%92%82%AE%F0%A0%87%AF|", loc15.toString());
    } catch (URISyntaxException e) {
        fail(e.getMessage());
    }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testSourceLocations(IValueFactory vf) {
    assertThrows(URISyntaxException.class, () -> vf.sourceLocation(null, null, null), "empty scheme");
    assertThrows(URISyntaxException.class, () -> vf.sourceLocation("", null, null), "empty scheme");
    assertDoesNotThrow(() -> vf.sourceLocation("valid+sch.em-e", null, null), "valid scheme");
    assertThrows(URISyntaxException.class, () -> vf.sourceLocation("invalid?scheme", null, null), "invalid scheme");
    assertThrows(URISyntaxException.class, () -> vf.sourceLocation("🍝", null, null), "invalid scheme");
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testStringRepresentation(IValueFactory vf) {
    assertTrue(vf.string("\uD83C\uDF5D").equals(vf.string("🍝")));
    assertTrue(vf.string(new String(Character.toChars(0x1F35D))).equals(vf.string("🍝")));
  }
  
  @ParameterizedTest @ArgumentsSource(ValueProvider.class) public void testRascalIssue1192(IValueFactory vf) {
      assertTrue(vf.integer("-2147483648").subtract(vf.integer("2147483648")).equals(vf.integer("-4294967296")));
  }
  
  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testStringLength(IValueFactory vf) {
    assertTrue(vf.string("\uD83C\uDF5D").length() == 1);
    assertTrue(vf.string("\uD83C\uDF5D\uD83C\uDF5D").length() == 2);
    assertTrue(vf.string("🍝").length() == 1);
    assertTrue(vf.string("🍝🍝").length() == 2);
    assertTrue(vf.string("é").length() == 1);
    assertTrue(vf.string("").length() == 0);
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testStringReverse(IValueFactory vf) {
    assertTrue(vf.string("").reverse().equals(vf.string("")));
    assertTrue(vf.string("🍝").reverse().equals(vf.string("🍝")));
    assertTrue(vf.string("🍝🍝").reverse().equals(vf.string("🍝🍝")));
    assertTrue(vf.string("🍝x🍝").reverse().equals(vf.string("🍝x🍝")));
    assertTrue(vf.string("🍝🍞").reverse().getValue().equals("🍞🍝"));
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testStringSubString(IValueFactory vf) {
    assertTrue(vf.string("").substring(0, 0).equals(vf.string("")));
    assertTrue(vf.string("🍝").substring(0, 1).equals(vf.string("🍝")));
    assertTrue(vf.string("🍝🍝").substring(0, 1).equals(vf.string("🍝")));
    assertTrue(vf.string("🍝x🍝").substring(1, 2).equals(vf.string("x")));
    assertTrue(vf.string("🍝x🍝").substring(1, 3).equals(vf.string("x🍝")));
  }
  

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testStringWrite(IValueFactory vf) {
      Random rnd = new Random();
      
      for (int i = 0; i < 1000; i++) {
          IString testString = vf.string(RandomUtil.string(rnd, rnd.nextInt(200)));
          StringWriter w = new StringWriter();
          try {
              testString.write(w);
          } catch (IOException e) {
              fail(e.getMessage());
          }
          
          assertEqual(testString, vf.string(w.toString()));
      }
  }
  
  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testStringEmptyWrite(IValueFactory vf) {
      IString testString = vf.string("");
      StringWriter w = new StringWriter();
      try {
          testString.write(w);
      } catch (IOException e) {
          fail(e.getMessage());
      }

      assertEqual(testString, vf.string(""));
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testStringCharAt(IValueFactory vf) {
    assertTrue(vf.string("🍝").charAt(0) == 0x1F35D);
    assertTrue(vf.string("🍝🍞").charAt(1) == 0x1F35E);
    assertTrue(vf.string("🍝x🍝").charAt(1) == 'x');
    assertTrue(vf.string("🍝x🍞").charAt(2) == 0x1F35E);
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testStringConcat(IValueFactory vf) {
    assertTrue(vf.string("").concat(vf.string("")).equals(vf.string("")));
    assertTrue(vf.string("x").concat(vf.string("y")).equals(vf.string("xy")));
    assertTrue(vf.string("🍝").concat(vf.string("y")).equals(vf.string("🍝y")));
    assertTrue(vf.string("x").concat(vf.string("🍝")).equals(vf.string("x🍝")));
    assertTrue(vf.string("🍝").concat(vf.string("🍝")).equals(vf.string("🍝🍝")));
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testStringReplace(IValueFactory vf) {
    assertTrue(vf.string("").replace(0, 1, 0, vf.string("x")).equals(vf.string("x")));
    assertTrue(vf.string("x").replace(0, 1, 0, vf.string("")).equals(vf.string("x")));
    assertTrue(vf.string("xy").replace(0, 1, 1, vf.string("p")).equals(vf.string("py")));
    assertTrue(vf.string("xy").replace(1, 1, 0, vf.string("p")).equals(vf.string("xp")));
    assertTrue(vf.string("xy").replace(0, 1, 1, vf.string("pq")).equals(vf.string("pqy")));
    assertTrue(vf.string("xy").replace(1, 1, 0, vf.string("pq")).equals(vf.string("xqp")));
    assertTrue(vf.string("xy").replace(0, 1, 0, vf.string("pq")).equals(vf.string("pqxy")));
    assertTrue(vf.string("xy").replace(1, 1, 1, vf.string("pq")).equals(vf.string("xpqy")));

    assertTrue(vf.string("🍝y").replace(0, 1, 1, vf.string("p")).equals(vf.string("py")));
    assertTrue(vf.string("🍝y").replace(1, 1, 0, vf.string("p")).equals(vf.string("🍝p")));
    assertTrue(vf.string("xy").replace(0, 1, 1, vf.string("🍝")).equals(vf.string("🍝y")));
    assertTrue(vf.string("").replace(0, 1, 0, vf.string("🍝")).equals(vf.string("🍝")));
    assertTrue(vf.string("🍝").replace(0, 1, 0, vf.string("")).equals(vf.string("🍝")));
    assertTrue(vf.string("🍝y").replace(0, 1, 1, vf.string("p")).equals(vf.string("py")));
    assertTrue(vf.string("🍝y").replace(1, 1, 0, vf.string("p")).equals(vf.string("🍝p")));
    assertTrue(vf.string("x🍝").replace(0, 1, 1, vf.string("p")).equals(vf.string("p🍝")));
    assertTrue(vf.string("x🍝").replace(1, 1, 0, vf.string("p")).equals(vf.string("xp")));
    assertTrue(vf.string("🍝y").replace(0, 1, 1, vf.string("p🍝")).equals(vf.string("p🍝y")));
    assertTrue(vf.string("🍝y").replace(1, 1, 0, vf.string("p🍝")).equals(vf.string("🍝🍝p")));
    assertTrue(vf.string("🍝y").replace(0, 1, 0, vf.string("🍝q")).equals(vf.string("🍝q🍝y")));
    assertTrue(vf.string("x🍝").replace(1, 1, 1, vf.string("🍝q")).equals(vf.string("x🍝q🍝")));
    assertTrue(vf.string("🍝y🍝").replace(1, 1, 2, vf.string("🍝")).equals(vf.string("🍝🍝🍝")));
  }
  
  private static final String[] commonNewlines = new String[] { "\n"};
  
  private void checkIndent(IValueFactory vf, String indent, String newline, boolean indentFirstLine, String... lines) {
      StringBuilder unindented = new StringBuilder();
      StringBuilder indented = new StringBuilder();
      StringBuilder indentedTwice = new StringBuilder();
      IString concatTree = vf.string("");

      boolean first = true;
      for (String l : lines) {
          unindented.append(l);
          unindented.append(newline);

          concatTree = concatTree.concat(vf.string(l));
          concatTree = concatTree.concat(vf.string(newline));
          
          if (indentFirstLine || !first) {
              indented.append(indent);
          }
          indented.append(l);
          indented.append(newline);

          if (indentFirstLine || !first) {
              indentedTwice.append("first" + indent);
              indentedTwice.append(indent);
          }
          
          indentedTwice.append(l);
          indentedTwice.append(newline);
          
          first = false;
      }
      
      // remove empty line indentations
      String expected = indented.toString();
      String expectedTwice = indentedTwice.toString();
      
      IString indentedDirect = vf.string(unindented.toString()).indent(vf.string(indent), indentFirstLine);
      IString indentedConcatTree = concatTree.indent(vf.string(indent), indentFirstLine);

      IString indentedDirectTwice = indentedDirect.indent(vf.string("first" + indent), indentFirstLine);
      IString indentedConcatTreeTwice = indentedConcatTree.indent(vf.string("first" + indent), indentFirstLine);

      // basic tests showing lazy versus eager indentation should have the same semantics:
      assertEquals(expected, indentedDirect.getValue());
      assertEquals(expected, indentedConcatTree.getValue());
      assertSimilarIteration(vf.string(expected), indentedDirect);
      assertEqualLength(vf.string(expected), indentedDirect);
      assertSimilarIteration(vf.string(expected), indentedConcatTree);
      assertEqualLength(vf.string(expected), indentedConcatTree);
      assertSimilarIteration(indentedDirect, indentedConcatTree);
      assertEqualLength(indentedDirect, indentedConcatTree);
      assertEqual(indentedDirect, indentedConcatTree);
      assertEquals(indentedDirect.hashCode(), indentedConcatTree.hashCode());

      // these modify internal structure as a side-effect, so after this we test the above again!
      assertEqualCharAt(vf.string(expected), indentedDirect);
      assertEqualSubstring(vf.string(expected), indentedDirect);
      assertEqualLength(vf.string(expected), indentedDirect);
      
      // retest after internal structure modifications
      assertEquals(expected, indentedDirect.getValue());
      assertEquals(expected, indentedConcatTree.getValue());
      assertSimilarIteration(vf.string(expected), indentedDirect);
      assertSimilarIteration(vf.string(expected), indentedConcatTree);
      assertSimilarIteration(indentedDirect, indentedConcatTree);
      assertEqual(indentedDirect, indentedConcatTree);
      assertEquals(indentedDirect.hashCode(), indentedConcatTree.hashCode());
      
      // basic tests showing lazy versus eager indentation should have the same semantics:
      assertEquals(expectedTwice, indentedDirectTwice.getValue());
      assertEquals(expectedTwice, indentedConcatTreeTwice.getValue());
      assertSimilarIteration(vf.string(expectedTwice), indentedDirectTwice);
      assertSimilarIteration(vf.string(expectedTwice), indentedConcatTreeTwice);
      assertEqual(indentedDirectTwice, indentedConcatTreeTwice);
      assertSimilarIteration(indentedDirectTwice, indentedConcatTreeTwice);
      assertEquals(indentedDirectTwice.hashCode(), indentedConcatTreeTwice.hashCode());
      
      // these modify internal structure as a side-effect, so after this we test the above again!
      assertEqualCharAt(vf.string(expectedTwice), indentedDirectTwice);
      assertEqualSubstring(vf.string(expectedTwice), indentedDirectTwice);
      assertEqualLength(vf.string(expectedTwice), indentedDirectTwice);
      assertEqualCharAt(vf.string(expectedTwice), indentedConcatTreeTwice);
      assertEqualSubstring(vf.string(expectedTwice), indentedConcatTreeTwice);
      assertEqualLength(vf.string(expectedTwice), indentedConcatTreeTwice);
      assertEqualCharAt(indentedDirectTwice, indentedConcatTreeTwice);
      assertEqualSubstring(indentedDirectTwice, indentedConcatTreeTwice);
      assertEqualLength(indentedDirectTwice, indentedConcatTreeTwice);
      
      // retest after internal structure modifications
      assertEquals(expectedTwice, indentedDirectTwice.getValue());
      assertEquals(expectedTwice, indentedConcatTreeTwice.getValue());
      assertSimilarIteration(vf.string(expectedTwice), indentedDirectTwice);
      assertSimilarIteration(vf.string(expectedTwice), indentedConcatTreeTwice);
      assertEqual(indentedDirectTwice, indentedConcatTreeTwice);
      assertSimilarIteration(indentedDirectTwice, indentedConcatTreeTwice);
      assertEquals(indentedDirectTwice.hashCode(), indentedConcatTreeTwice.hashCode());
  }

  private void assertEqualCharAt(IString one, IString two) {
      assertEquals(one, two);
      
      for (int i = 0; i < one.length(); i++) {
          assertEquals(one.charAt(i), two.charAt(i));
      }
  }
  
  private void assertEqualSubstring(IString one, IString two) {
      assertEqual(one, two);
      
      Random rnd = new Random();
      for (int c = 0; c < 10; c++) {
          int j = rnd.nextInt(one.length());
          
          if (j > 0) {
              int i = rnd.nextInt(j);

              assertEquals(one.substring(i,j), two.substring(i,j));
          }
      }
  }

  private static void assertEqualLength(IString ref, IString target) {
      assertEquals(ref.length(), target.length());
  }
  
  private static void assertSimilarIteration(IString ref, IString target) {
      OfInt refIterator = ref.iterator();
      OfInt targetIterator = target.iterator();

      for (int i = 0; refIterator.hasNext(); i++) {
          assertTrue(targetIterator.hasNext());
          int a = refIterator.nextInt();
          int b = targetIterator.nextInt();
          
          if (a != b) {
              fail("string iterators produce different values at index " + i + " (" + a + " != " + b + ")");
          }
      }
  }
  
  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testStringIndent(IValueFactory vf) {
      for (boolean firstLine : new Boolean[] { true, false}) {
          for (String nl: commonNewlines) {
              checkIndent(vf, " ", nl, firstLine, "a", "b", "c");
              checkIndent(vf, "\t", nl, firstLine, "a", "b", "c");
              checkIndent(vf, "\t", nl, firstLine, "a", "", "c");
              checkIndent(vf, "\t", nl, firstLine, "a", "", "", "c");
              checkIndent(vf, "   ", nl, firstLine, "a", "b", "c");
              checkIndent(vf, " ", nl, firstLine, " abcdef", " bcdefg", " cdefgh");
              checkIndent(vf, " ", nl, firstLine, "🍝", " b", " c");

              // these are some hard tests containing spurious carriage return characters:
              checkIndent(vf, "\t", nl, firstLine, "a", "\r", "", "c");
              checkIndent(vf, "\t", nl, firstLine, "a", "", "\r", "c");
              checkIndent(vf, "\t", nl, firstLine, "a", "", "\r\r\r", "c");
              checkIndent(vf, "\t", nl, firstLine, "a\r", "", "c");
              checkIndent(vf, "\t", nl, firstLine, "a", "", "\rc");
          }
      }
  }
  
  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testStringIndentRandomDefault(IValueFactory vf) {
      Random rnd = new Random();
      for (String nl: commonNewlines) {
          String[] randomLines = new String[10];
          for (int n = 0; n < randomLines.length; n++) {
             String newString = RandomUtil.string(rnd, rnd.nextInt(200));
             newString = newString.replaceAll("[\\r\\n]", "_");
             randomLines[n] = newString;
          }

          StringValue.resetMaxUnbalance();
          StringValue.resetMaxFlatString();

          for (boolean first : new Boolean[] { true, true} ) {
              for (int n = 0; n < 20; n++) {
                  checkIndent(vf, Pattern.quote(RandomUtil.string(rnd, rnd.nextInt(20)).replaceAll("\n",  "_")), nl, first, randomLines);
              }
          }
      }
  }
  
  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testStringIndentRandomShortConcats(IValueFactory vf) {
      Random rnd = new Random();
      for (String nl: commonNewlines) {
          String[] randomLines = new String[10];
          for (int n = 0; n < randomLines.length; n++) {
             String newString = RandomUtil.string(rnd, rnd.nextInt(200));
             for (String newL : commonNewlines) {
                 newString = newString.replaceAll(Pattern.quote(newL), "_");
             }
             randomLines[n] = newString;
          }
          
          try {
              StringValue.setMaxFlatString(5);
              StringValue.setMaxUnbalance(5);
              for (int n = 0; n < 20; n++) {
                  checkIndent(vf, Pattern.quote(RandomUtil.string(rnd, rnd.nextInt(20)).replaceAll("\n",  "_")), nl, true, randomLines);
              }
          }
          finally {
              StringValue.resetMaxUnbalance();
              StringValue.resetMaxFlatString();
          }
      }
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testIntAddition(IValueFactory vf) {
    assertTrue(vf.integer(1).add(vf.integer(1)).equals(vf.integer(2)));
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testReal(IValueFactory vf) {
    assertTrue(vf.real("1.5").floor().equals(vf.real("1")));
    assertTrue(vf.real("1.5").round().equals(vf.real("2")));
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testNumberSubTypes(TypeFactory tf) {
    assertTrue(tf.integerType().isSubtypeOf(tf.numberType()));
    assertFalse(tf.numberType().isSubtypeOf(tf.integerType()));
    assertTrue(tf.realType().isSubtypeOf(tf.numberType()));
    assertFalse(tf.numberType().isSubtypeOf(tf.realType()));
    assertTrue(tf.rationalType().isSubtypeOf(tf.numberType()));
    assertFalse(tf.numberType().isSubtypeOf(tf.rationalType()));

    assertTrue(tf.integerType().lub(tf.realType()).equivalent(tf.numberType()));
    assertTrue(tf.integerType().lub(tf.rationalType()).equivalent(tf.numberType()));
    assertTrue(tf.integerType().lub(tf.numberType()).equivalent(tf.numberType()));
    assertTrue(tf.realType().lub(tf.numberType()).equivalent(tf.numberType()));
    assertTrue(tf.rationalType().lub(tf.integerType()).equivalent(tf.numberType()));
    assertTrue(tf.rationalType().lub(tf.realType()).equivalent(tf.numberType()));
    assertTrue(tf.rationalType().lub(tf.numberType()).equivalent(tf.numberType()));
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testNumberArithmatic(IValueFactory vf) {
    INumber i1 = vf.integer(1);
    INumber i2 = vf.integer(2);
    INumber r1 = vf.real(1.0);
    INumber r2 = vf.real(2.0);
    INumber q1 = vf.rational(1, 1);
    INumber q2 = vf.rational(2, 1);

    assertEqual(i1.add(i2), vf.integer(3));
    assertEqual(i1.add(r2), vf.real(3));
    assertEqual(i1.add(q2), vf.rational(3, 1));
    assertEqual(q1.add(i2), vf.rational(3, 1));
    assertEqual(q1.add(q2), vf.rational(3, 1));
    assertEqual(r1.add(r2), vf.real(3));
    assertEqual(r1.add(i2), vf.real(3));
    assertEqual(r1.add(q2), vf.real(3));

    assertEqual(i1.subtract(i2), vf.integer(-1));
    assertEqual(i1.subtract(r2), vf.real(-1));
    assertEqual(r1.subtract(r2), vf.real(-1));
    assertEqual(r1.subtract(i2), vf.real(-1));
    assertEqual(q1.subtract(q2), vf.rational(-1, 1));
    assertEqual(q1.subtract(r2), vf.real(-1));
    assertEqual(q1.subtract(i2), vf.rational(-1, 1));

    IInteger i5 = vf.integer(5);
    assertEqual(i5.divide(i2, 80 * 80), vf.real(2.5));
    assertEqual(r1.subtract(q2), vf.real(-1));
    assertEqual(i5.divide(i2.toRational()), vf.rational(5, 2));

    assertEqual(vf.integer(0), vf.integer(0).abs());
    assertEqual(vf.rational(0, 1), vf.rational(0, 1).abs());
    assertEqual(vf.real(0), vf.real(0).abs());
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testPreciseRealDivision(IValueFactory vf) {
    IReal e100 = vf.real("1E100");
    IReal maxDiff = vf.real("1E-6300");
    IReal r9 = vf.real("9");
    assertTrue(e100.subtract(e100.divide(r9, 80 * 80).multiply(r9)).lessEqual(maxDiff).getValue());
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testDateTimeLongConversion(IValueFactory vf) {
    long l = 1156521600000L;
    IDateTime dt = vf.datetime(l);
    assertEqual(dt, vf.datetime(dt.getInstant()));
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testDateTimeLongConversionWithTimezone(IValueFactory vf) {
    IDateTime dt = vf.datetime(2014, 10, 13, 10, 7, 50, 1, 7, 0);
    assertEqual(dt,
        vf.datetime(dt.getInstant(), dt.getTimezoneOffsetHours(), dt.getTimezoneOffsetMinutes()));
  }

  @ParameterizedTest @ArgumentsSource(ValueProvider.class)
  public void testLocationTop(IValueFactory vf) throws URISyntaxException {
    ISourceLocation l = vf.sourceLocation("tmp", "", "/file.txt");
    assertTrue(l.top() == l);

    ISourceLocation m = vf.sourceLocation(l, 10, 20);
    assertEquals(m.top(), l);
  }
}
