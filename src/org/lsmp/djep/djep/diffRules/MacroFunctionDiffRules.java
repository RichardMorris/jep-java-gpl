/* @author rich
 * Created on 04-Jul-2003
 */
package org.lsmp.djep.djep.diffRules;

import org.lsmp.djep.djep.DJep;
import org.lsmp.djep.xjep.*;
import org.nfunk.jep.*;

/**
   * If your really lazy, you don't even need to workout the derivatives
   * of a function defined by a macro yourself.
   * This class will automatically calculate the rules for you.
   */
  public class MacroFunctionDiffRules extends ChainRuleDiffRules
  {
	/**
	 * Calculates the rules for the given function.
	 */
	  public MacroFunctionDiffRules(DJep djep,MacroFunction fun)  throws ParseException
	  {
		  name = fun.getName();
		  pfmc = fun;
		  
		XSymbolTable localSymTab = ((XSymbolTable) djep.getSymbolTable()).newInstance(); //new SymbolTable();
		DJep localJep = (DJep) djep.newInstance(localSymTab);

		  int nargs = fun.getNumberOfParameters();
		  rules = new Node[nargs];
		  this.descriptions = new String[nargs];
		  if(nargs == 1) {
			  rules[0] = localJep.differentiate(fun.getTopNode(),"x");
			  rules[0] = localJep.simplify(rules[0]);
			  descriptions[0] = "diff("+name+",x) -> "+localJep.toString(rules[0]);
		  }
		  else if(nargs == 2)
		  {
			  rules[0] = localJep.differentiate(fun.getTopNode(),"x");
			  rules[1] = localJep.differentiate(fun.getTopNode(),"y");
			  rules[0] = localJep.simplify(rules[0]);
			  rules[1] = localJep.simplify(rules[1]);
			  descriptions[0] = "diff("+name+",x) -> "+localJep.toString(rules[0]);
			  descriptions[1] = "diff("+name+",y) -> "+localJep.toString(rules[1]);
		  }
		  else
		  {
			  for(int i=0;i<nargs;++i) {
				  rules[i] = localJep.differentiate(fun.getTopNode(),"x"+ String.valueOf(i));
				  rules[i] = localJep.simplify(rules[i]);
				  descriptions[i] = "diff("+name+",x"+String.valueOf(i)+" -> " +localJep.toString(rules[i]);
			  }
		  }
		  //fixVarNames();
	  }
  }
