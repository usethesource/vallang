package org.eclipse.imp.pdb.facts;

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeStore;

/**
 * Typed node representation. An IConstructor is a specific kind of INode, namely one
 * that adheres to a specific schema see @link{ConstructorType}.
 *
 */
public interface IConstructor extends INode {

	/**
	 * @return the AbstractDataType of a constructor.
	 */
	public Type getType();
	
	/**
	 * @return the specific ConstructorType of this constructor
	 */
	public Type getConstructorType();
	
	/**
	 * Get a child from a labeled position in the tree.
	 * @param label the name of the child
	 * @return a value at the position indicated by the label.
	 */
	public IValue  get(String label);
	
	/**
	 * Replace a child at a labeled position in the tree. 
	 * @param label    the label of the position
	 * @param newChild the new value of the child
	 * @return a new tree node that is the same as the receiver except for
	 * the fact that at the labeled position the new value has replaced the old value.
	 * All annotations remain equal.
	 * 
	 * @throws FactTypeUseException when this label does not exist for the given tree node, or 
	 *         when the given value has a type that is not a sub-type of the declared type
	 *         of the child with this label.
	 */
	public IConstructor   set(String label, IValue newChild) throws FactTypeUseException;
	
	/**
	 * Replace a child at an indexed position in the tree. 
	 * @param label    the label of the position
	 * @param newChild the new value of the child
	 * @return a new tree node that is the same as the receiver except for
	 * the fact that at the labeled position the new value has replaced the old value.
	 * All annotations remain equal.
	 * 
	 * @throws FactTypeUseException when the index is greater than the arity of this tree node, or 
	 *         when the given value has a type that is not a sub-type of the declared type
	 *         of the child at this index.
	 */
	public IConstructor   set(int index, IValue newChild) throws FactTypeUseException;
	
	/**
	 * @return a tuple type representing the children types of this node/
	 */
	public Type    getChildrenTypes();
	
	/**
	 * Check whether a certain annotation is set.
	 * 
	 * @param label identifies the annotation
	 * @return true iff the annotation has a value on this node
	 * @throws FactTypeUseException when no annotation with this label is defined for this type of node.
	 */
	public boolean hasAnnotation(String label) throws FactTypeUseException;
	
	/**
	 * Check whether a certain annotation label is declared for this type of node.
	 * @param label
	 * @return true iff the given annotation label was declared for this type of node.
	 */
	public boolean declaresAnnotation(TypeStore store, String label);
	
	/**
	 * Get the value of an annotation
	 * 
	 * @param label identifies the annotation
	 * @return a value if the annotation has a value on this node or null otherwise
	 */
	public IValue  getAnnotation(String label) throws FactTypeUseException;
	
	/**
	 * Set the value of an annotation
	 * 
	 * @param label identifies the annotation
	 * @param newValue the new value for the annotation
	 * @return a value if the annotation has a value on this node or null otherwise
	 * @throws FactTypeUseException when the type of the new value is not comparable to the old annotation value
	 */
	public IConstructor   setAnnotation(String label, IValue newValue) throws FactTypeUseException;
}
