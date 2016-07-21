/*
Created 9 Sep 2006 - Richard Morris
*/
package org.lsmp.djep.matrixJep;

import org.lsmp.djep.xjep.MacroFunction;
import org.lsmp.djep.xjep.XJep;
import org.nfunk.jep.ParseException;

public class MatrixMacroFunction extends MacroFunction {

	public MatrixMacroFunction(String inName, int nargs, String expression,
			XJep jep) throws IllegalArgumentException, ParseException {
		super(inName, nargs, expression, jep);
		this.topNode = ((MatrixJep) this.localJep).preprocess(this.topNode);
	}

}
