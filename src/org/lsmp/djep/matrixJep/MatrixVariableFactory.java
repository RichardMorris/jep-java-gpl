/* @author rich
 * Created on 19-Dec-2003
 *
 * This code is covered by a Creative Commons
 * Attribution, Non Commercial, Share Alike license
 * <a href="http://creativecommons.org/licenses/by-nc-sa/1.0">License</a>
 */
package org.lsmp.djep.matrixJep;

import org.lsmp.djep.djep.DVariable;
import org.lsmp.djep.djep.PartialDerivative;
import org.lsmp.djep.djep.PartialVariableFactoryI;
import org.nfunk.jep.Node;
import org.nfunk.jep.Variable;

/**
 * @author Rich Morris
 * Created on 19-Dec-2003
 */
public class MatrixVariableFactory implements PartialVariableFactoryI {

	public PartialDerivative createDerivative(DVariable var,String[] dnames,Node eqn) {
		return null;
	}

	public Variable createVariable(String name, Object value) {
		return new MatrixVariable(name,value);
	}

	/* (non-Javadoc)
	 * @see org.nfunk.jep.VariableFactoryI#createVariable(java.lang.String)
	 */
	public Variable createVariable(String name) {
		return new MatrixVariable(name);
	}

}