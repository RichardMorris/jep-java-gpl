/* @author rich
 * Created on 19-Dec-2003
 */
package org.lsmp.djep.djep;

import org.nfunk.jep.*;
import org.lsmp.djep.xjep.*;
/**
 * A VariableFactory which can work with PartialDerivatives.
 * @author Rich Morris
 * Created on 19-Dec-2003
 */
public class DVariableFactory extends XVariableFactory {

	public Variable createVariable(String name, Object value) {
		return new DVariable(name,value);
	}

	public Variable createVariable(String name) {
		if(defaultValue!=null)
			return new DVariable(name,defaultValue);
		else
			return new DVariable(name);
	}

}
