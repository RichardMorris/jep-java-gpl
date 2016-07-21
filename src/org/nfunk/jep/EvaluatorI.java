/* @author rich
 * Created on 22-Apr-2005
 *
 * See LICENSE.txt for license information.
 */
package org.nfunk.jep;

/**
 * Defines a method which can be used to evaluate a part of a node-tree. 
 * It is passed to classes which use CallbackEvaluatorI 
 * which need greater control over how they are evaluated.
 * 
 * @author Rich Morris
 * Created on 22-Apr-2005
 * @see org.nfunk.function.CallbackEvaluationI
 */
public interface EvaluatorI {
	
	/**
	 * Evaluates a node and returns and object with the value of the node.
	 * 
	 * @throws ParseException if errors occur during evaluation;
	 */
	public abstract Object eval(Node node) throws ParseException;
}
