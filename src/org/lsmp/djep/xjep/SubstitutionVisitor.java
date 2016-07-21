/* @author rich
 * Created on 16-Nov-2003
 */
package org.lsmp.djep.xjep;
import org.nfunk.jep.*;
/**
 * Allows substitution of a given variable with an expression tree.
 * Substitution is best done using the 
 * {@link XJep#substitute(Node,String,Node) XJep.substitute} method. 
 * For example
 * <pre>
 * XJepI xjep = ...;
 * Node node = xjep.parse("x^2+x");
 * Node sub = xjep.parse("sin(y)");
 * Node res = xjep.substitute(node,"x",sub,xjep);
 * </pre>
 * Will give the expression "(sin(y))^2+sin(y)".
 * 
 * @author Rich Morris
 * Created on 16-Nov-2003
 */
public class SubstitutionVisitor extends DoNothingVisitor {

	private String names[];
	private Node replacements[];
	private XJep xjep;
	public SubstitutionVisitor() {}

	/**
	 * Substitutes all occurrences of variable var with replacement.
	 * Does not do a DeepCopy.
	 * @param orig	the expression we wish to perform the substitution on
	 * @param name	the name of the variable
	 * @param replacement	the expression var is substituted for
	 * @return the tree with variable replace (does not do a DeepCopy)
	 * @throws ParseException
	 */
	public Node substitute(Node orig,String name,Node replacement,XJep xj) throws ParseException
	{
		this.names = new String[]{name};
		this.replacements = new Node[]{replacement};
		this.xjep=xj;
		Node res = (Node) orig.jjtAccept(this,null);
		return res;
	}

	/**
	 * Substitutes into orig the equation given by sub 
	 * @param orig the equation to substitute into
	 * @param sub and equation of the form x=....
	 * @param xj
	 * @return orig after substitution
	 * @throws ParseException if sub is of the wrong form
	 */
	public Node substitute(Node orig,Node sub,XJep xj) throws ParseException
	{
		if(sub.jjtGetNumChildren()!=2
				|| !(sub instanceof ASTFunNode)
				|| ((ASTFunNode) sub).getOperator() !=
					xj.getOperatorSet().getAssign()
				) 
			throw new ParseException("substitute: substitution equation should be of the form x=....");
		Node var = sub.jjtGetChild(0);
		if(!(var instanceof ASTVarNode) )
			throw new ParseException("substitute: substitution equation should be of the form x=....");
		return this.substitute(orig, ((ASTVarNode)var).getName(), sub.jjtGetChild(1), xj);
	}

	public Node substitute(Node orig,Node[] subs,XJep xj) throws ParseException
	{
		String[] names = new String[subs.length]; 
		Node[] reps = new Node[subs.length];
		for(int i=0;i<subs.length;++i)
		{
			Node sub = subs[i];
			if(sub.jjtGetNumChildren()!=2
				|| !(sub instanceof ASTFunNode)
				|| ((ASTFunNode) sub).getOperator() !=
					xj.getOperatorSet().getAssign()
				) 
			throw new ParseException("substitute: substitution equation should be of the form x=....");
			Node var = sub.jjtGetChild(0);
			if(!(var instanceof ASTVarNode) )
				throw new ParseException("substitute: substitution equation should be of the form x=....");
			names[i] = ((ASTVarNode)var).getName();
			reps[i] =  sub.jjtGetChild(1);
		}
		return this.substitute(orig, names, reps, xj);
	}

	/**
	 * Substitutes all occurrences of a set of variable var with a set of replacements.
	 * Does not do a DeepCopy.
	 * @param orig	the expression we wish to perform the substitution on
	 * @param names	the names of the variable
	 * @param replacements	the expression var is substituted for
	 * @return the tree with variable replace (does not do a DeepCopy)
	 * @throws ParseException
	 */
	public Node substitute(Node orig,String names[],Node replacements[],XJep xj) throws ParseException
	{
		this.names = names;
		this.replacements = replacements;
		this.xjep=xj;
		Node res = (Node) orig.jjtAccept(this,null);
		return res;
	}
	
	/**
	 * Substitute a set of names with a set of values.
	 * @param orig
	 * @param names
	 * @param values
	 * @param xj
	 * @return node with the substitution performed
	 * @throws ParseException
	 */
	public Node substitute(Node orig,String names[],Object values[],XJep xj) throws ParseException
	{
		Node[] replacements = new Node[values.length];
		for(int i=0;i<values.length;++i)
			replacements[i] = xj.getNodeFactory().buildConstantNode(values[i]);
		return substitute(orig,names,replacements,xj);
	}
	

	public Object visit(ASTVarNode node, Object data) throws ParseException
	{
		for(int i=0;i<names.length;++i)
		{
			if(names[i].equals(node.getName()))
				return xjep.deepCopy(replacements[i]);
		}
		return node;
		//if(node.getVar().isConstant())
		//	return xjep.getNodeFactory().buildVariableNode(xjep.getSymbolTable().getVar(node.getName()));
			
		//throw new ParseException("No substitution specified for variable "+node.getName());
	}
}
