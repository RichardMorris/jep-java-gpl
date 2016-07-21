/*
Created 2 Aug 2006 - Richard Morris
*/
package org.lsmp.djep.xjep;

import java.util.Hashtable;

import org.nfunk.jep.ASTFunNode;
import org.nfunk.jep.ASTVarNode;
import org.nfunk.jep.FunctionTable;
import org.nfunk.jep.Node;
import org.nfunk.jep.Operator;
import org.nfunk.jep.OperatorSet;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommandI;

/**
 * @author Richard Morris
 * 
 *
 */
public class ImportationVisitor extends DeepCopyVisitor {
    	Hashtable ht = new Hashtable();
    	NodeFactory nf;
    	FunctionTable ft;
	public ImportationVisitor(XJep xj) {
		super(xj);
		this.nf = xj.getNodeFactory();
		this.ft = xj.getFunctionTable();
		OperatorSet os = xj.getOperatorSet();
		Operator ops[] = os.getOperators();
		for(int i=0;i<ops.length;++i) {
		    Operator op = ops[i];
		    ht.put(op.getName(),op);
		}
	}

	public Node importExpression(Node node) throws ParseException {
		Node res = (Node) node.jjtAccept(this,null);
		return res;
	}

	public Object visit(ASTVarNode node, Object data) throws ParseException {
		Node res = nf.buildVariableNode(node.getName(),node.getVar().getValue());
		return res;
	}

	public Object visit(ASTFunNode node, Object data) throws ParseException {
	    Node children[] = this.acceptChildrenAsArray(node, data);
	    Node res;
	    if(node.isOperator()) {
		Operator oldOp = node.getOperator();
		String oldName = oldOp.getName();
		Operator newOp = (Operator) ht.get(oldName);
		res = nf.buildOperatorNode(newOp, children);
	    }
	    else {
		String oldName = node.getName();
		PostfixMathCommandI pfmc = ft.get(oldName);
		res = nf.buildFunctionNode(oldName, pfmc, children);
	    }
		
	    return res;
	}

	
}
