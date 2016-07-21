/*
Created 23 Aug 2006 - Richard Morris
*/
package org.lsmp.djep.xjep;

import org.nfunk.jep.function.PostfixMathCommandI;

/**
 * A class representing a ternary operator, for example x?y:z.  
 * @author Richard Morris
 */

public class TernaryOperator extends XOperator {
	String symbol2;
	public TernaryOperator(String name, String lhsSymbol,String rhsSymbol,
			PostfixMathCommandI pfmc, int flags) {
		super(name, lhsSymbol, pfmc, flags);
		symbol2 = rhsSymbol;
	}

	public TernaryOperator(String name, String lhsSymbol,String rhsSymbol,
			PostfixMathCommandI pfmc, int flags, int precedence) {
		super(name, lhsSymbol, pfmc, flags, precedence);
		symbol2 = rhsSymbol;
	}

	public String getRhsSymbol() { return symbol2; }
}
