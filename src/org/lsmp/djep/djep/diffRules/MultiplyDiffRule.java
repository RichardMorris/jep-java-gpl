/* @author rich
 * Created on 04-Jul-2003
 */
package org.lsmp.djep.djep.diffRules;

import org.lsmp.djep.djep.DJep;
import org.lsmp.djep.djep.DiffRulesI;
import org.lsmp.djep.xjep.NodeFactory;
import org.nfunk.jep.ASTConstant;
import org.nfunk.jep.ASTFunNode;
import org.nfunk.jep.Node;
import org.nfunk.jep.Operator;
import org.nfunk.jep.OperatorSet;
import org.nfunk.jep.ParseException;

/**
 * Diffrentiates a product with respect to var. diff(y*z,x) ->
 * diff(y,x)*z+y*diff(z,x)
 * 
 * @since 28/1/05 now works when multiply has more than two arguments.
 */
public class MultiplyDiffRule implements DiffRulesI {
    private final String name;

    Operator mulOp = null;

    public MultiplyDiffRule(Operator op) {
	// dv = inDv;
	name = op.getName();
	mulOp = op;
    }

    public String toString() {
	return "diff(f" + name + "g,x) -> diff(f,x)" + name + "g+f" + name
		+ "diff(g,x)";
    }

    public String getName() {
	return name;
    }

    public Node differentiate(ASTFunNode node, String var, Node[] children,
	    Node[] dchildren, DJep djep) throws ParseException {
	OperatorSet opset = djep.getOperatorSet();
	NodeFactory nf = djep.getNodeFactory();
	//Operator op = opset.getMultiply();
	//if (mulOp != null)
	    //op = mulOp;

	int nchild = node.jjtGetNumChildren();
	if (nchild == 2) {
	    if (children[0] instanceof ASTConstant) {
		return nf.buildOperatorNode(mulOp, children[0], dchildren[1]);
	    }
	    if (children[1] instanceof ASTConstant) {
		return nf.buildOperatorNode(mulOp, dchildren[0], children[1]);
	    }
	    return nf.buildOperatorNode(opset.getAdd(), nf.buildOperatorNode(
	            mulOp, dchildren[0], djep.deepCopy(children[1])), nf
		    .buildOperatorNode(mulOp, djep.deepCopy(children[0]),
			    dchildren[1]));
	}

	Node sums[] = new Node[nchild];
	for (int i = 0; i < nchild; ++i) {
	    Node terms[] = new Node[nchild];
	    for (int j = 0; j < nchild; ++j)
		terms[j] = children[j];
	    terms[i] = dchildren[i];
	    sums[i] = nf.buildOperatorNode(mulOp, terms);
	}
	Node res = nf.buildOperatorNode(opset.getAdd(), sums);
	return res;

	// throw new ParseException("Too many children "+nchild+" for
	// "+node+"\n");
    }
} /* end MultiplyDiffRule */
