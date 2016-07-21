/* @author rich
 * Created on 19-Dec-2003
 */
package org.lsmp.djep.matrixJep;

import org.lsmp.djep.djep.*;
import org.lsmp.djep.xjep.XVariable;
import org.nfunk.jep.Node;
import org.nfunk.jep.Variable;

/**
 * Allows creation of matrix aware variables.
 * 
 * @author Rich Morris
 * Created on 19-Dec-2003
 */
public class MatrixVariableFactory extends DVariableFactory {

	/** Create a variable with a given value. */
	//@Override
	public Variable createVariable(String name, Object value) {
		if(defaultValue!=null)
			return new MatrixVariable(name,defaultValue);
		else
			return new MatrixVariable(name,value);
	}

	/** Create a variable with a given value. */
	//@Override
	public Variable createVariable(String name) {
		return new MatrixVariable(name);
	}

}
