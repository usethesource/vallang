/**
 * This contains an abstract IValueFactory implementation, AbstractPrimitiveValueFactory, which offers the
 * implementation for the atomic data-types of Vallang, to be re-used by factories
 * which specialize on the implementation of the container types.
 *
 * <ul>
 *   <li>IBool: see BoolValue</li>
 *   <li>IInteger: see AbstractNumberValue, BigIntegerValue and IntegerValue, ICanBecomeABigInteger</li>
 *   <li>IReal: see AbstractNumberValue, BigDecimalValue</li>
 *   <li>IRational: see AbstractNumberValue, RationalValue</li>
 *   <li>ISourceLocation: see SourceLocationURIValues and SourceLocationValues</li>
 *   <li>IString: see StringValue</li>
 *   <li>IDateTime: see DateTimeValues</li>
 *
 * </ul>
 */
package io.usethesource.vallang.impl.primitive;