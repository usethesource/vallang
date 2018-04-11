package io.usethesource.vallang.basic;

import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.Setup;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

@RunWith(Parameterized.class)
public class RegressionTest {
    @Parameterized.Parameters
    public static Iterable<? extends Object> data() {
          return Setup.valueFactories();
            }

    private final IValueFactory vf;

    public RegressionTest(final IValueFactory vf) {
    this.vf = vf;
    }

    private TypeFactory tf = TypeFactory.getInstance();

    @Test
    public void testConstructorEquality() throws Exception {
    final TypeStore ts = new TypeStore();
    final Type adtType = tf.abstractDataType(ts, "n");
    final Type c0Type = tf.constructor (ts, adtType, "c");
    final Type c1Type = tf.constructor (ts, adtType, "c1", tf.integerType());

    final IConstructor c0Normal = vf.constructor(c0Type);      final IConstructor c0WithKWParams = vf.constructor(c0Type).asWithKeywordParameters().setParameters(Collections.emptyMap());

    final IConstructor c1Normal = vf.constructor(c1Type, vf.integer(1));
        final IConstructor c1WithKWParams = vf.constructor(c1Type, vf.integer(1)).asWithKeywordParameters().setParameters(Collections.emptyMap());

        assertTrue(c0WithKWParams.isEqual(c0Normal));
        assertTrue(c0WithKWParams.equals(c0Normal));
        assertTrue(c0Normal.isEqual(c0WithKWParams));
        assertTrue(c0Normal.equals(c0WithKWParams));
        assertTrue(c1WithKWParams.isEqual(c1Normal));
        assertTrue(c1WithKWParams.equals(c1Normal));
        assertTrue(c1Normal.isEqual(c1WithKWParams));
        assertTrue(c1Normal.equals(c1WithKWParams));
    }
}
