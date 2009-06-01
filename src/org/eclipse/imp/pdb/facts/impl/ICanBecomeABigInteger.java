package org.eclipse.imp.pdb.facts.impl;

import java.math.BigInteger;

/**
 * Strangely named class, which defines that the implementor can convert something to a big integer.
 * 
 * @author Arnold Lankamp
 */
public interface ICanBecomeABigInteger{
	
	/**
	 * Returns the big integer.
	 * 
	 * @return The big integer.
	 */
	BigInteger toBigInteger();
}
