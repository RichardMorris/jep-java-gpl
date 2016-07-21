/* @author rich
 * Created on 04-Jul-2003
 */
package org.lsmp.djep.djep.diffRules;

import org.lsmp.djep.djep.DJep;
import org.lsmp.djep.djep.DiffRulesI;
import org.nfunk.jep.ASTFunNode;
import org.nfunk.jep.Node;
import org.nfunk.jep.Operator;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommandI;


/**
   * Rules like Sum where diff(sum(a,b,c),x) -> sum(da/dx,db/dx,dc/dx) are instance of this class.
   **/
  public class PassThroughDiffRule implements DiffRulesI
  {
	private final String name;
	private final PostfixMathCommandI pfmc;
	private Operator op=null;

	public PassThroughDiffRule(DJep djep,String inName)
	{	  
	  name = inName;
	  pfmc = djep.getFunctionTable().get(name);
	}
	public PassThroughDiffRule(String inName,PostfixMathCommandI inPfmc)
	{
		name = inName;
		pfmc = inPfmc; 
	}
	public PassThroughDiffRule(Operator op) {
	    this(op.getName(),op.getPFMC());
	    this.op = op;
	}
	public String toString()
	{
		if(pfmc==null)
		{
			return name +" Passthrough but no math command!"; 
		}
		switch(pfmc.getNumberOfParameters())
		{
		case 0:
			return "diff("+name+",x) -> "+name;
		case 1:
			return "diff("+name+"a,x) -> "+name+"diff(a,x)";
		case 2:
			return "diff(a"+name+"b,x) -> diff(a,x)"+name+"diff(b,x)";
		default:
			return "diff(a"+name+"b"+name+"...,x) -> diff(a,x)"+name+"diff(b,x)"+name+"...";
		}
	}
	public String getName() { return name; }
  	  	
	public Node differentiate(ASTFunNode node,String var,Node [] children,Node [] dchildren,DJep djep) throws ParseException
	{
	    if(op!=null)
		return djep.getNodeFactory().buildOperatorNode(op,dchildren);
	    else
		return djep.getNodeFactory().buildFunctionNode(node,dchildren);
	}
  }
