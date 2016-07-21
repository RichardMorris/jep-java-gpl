/* @author rich
 * Created on 04-Jul-2003
 */
package org.lsmp.djep.djep.diffRules;

import org.lsmp.djep.djep.DJep;
import org.lsmp.djep.djep.DiffRulesI;
import org.lsmp.djep.xjep.NodeFactory;
import org.nfunk.jep.ASTFunNode;
import org.nfunk.jep.Node;
import org.nfunk.jep.Operator;
import org.nfunk.jep.ParseException;

/**
   * Differentiates a division with respect to var.
   * diff(y/z,x) -> (diff(y,x)*z-y*diff(z,x))/(z*z)
   */
  public class DivideDiffRule implements DiffRulesI
  {
	private final String name;
    Operator div;
    Operator sub;
    Operator mul;

    public DivideDiffRule(String name, Operator div, Operator sub, Operator mul) {
        super();
        this.name = name;
        this.div = div;
        this.sub = sub;
        this.mul = mul;
    }

	public String toString()
	{	  return "diff(f/g,x) -> (diff(f,x)*g-f*diff(g,x))/(g*g)";  }
	public String getName() { return name; }
  	
	public Node differentiate(ASTFunNode node,String var,Node [] children,Node [] dchildren,DJep djep) throws ParseException
	{
//	  XOperatorSet opset = (XOperatorSet) djep.getOperatorSet();
	  NodeFactory nf = djep.getNodeFactory();
	  
	  int nchild = node.jjtGetNumChildren();
	  if(nchild==2) {
        return 
        		nf.buildOperatorNode(div,
        		  nf.buildOperatorNode(sub,
        			nf.buildOperatorNode(mul,
        			  dchildren[0],
        			  djep.deepCopy(children[1])),
        			nf.buildOperatorNode(mul,
        			  djep.deepCopy(children[0]),
        			  dchildren[1])),
        		  nf.buildOperatorNode(mul,
        			djep.deepCopy(children[1]),
        			djep.deepCopy(children[1])));
    }
	  
	  throw new ParseException("Too many children "+nchild+" for "+node+"\n");
	}

  } /* end DivideDiffRule */
