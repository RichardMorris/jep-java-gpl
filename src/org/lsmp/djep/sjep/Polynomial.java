/* @author rich
 * Created on 14-Dec-2004
 */
package org.lsmp.djep.sjep;
import org.nfunk.jep.*;
/**
 * Represents a polynomial.
 * i.e. a sum of terms which are typically 
 * {@link Monomial}s, but can be any {@link AbstractPNode}. The order of the terms is specified by the total ordering.
 * 
 * @author Rich Morris
 * Created on 14-Dec-2004
 */
public class Polynomial extends AbstractPNode {

	PNodeI terms[];

	/**
	 * 
	 */
	Polynomial(PolynomialCreator pc,PNodeI terms[]) {
		super(pc);
		this.terms = terms;
	}

	MutiablePolynomial toMutiablePolynomial()
	{
		PNodeI newTerms[] = new PNodeI[terms.length];
		for(int i=0;i<terms.length;++i)
			newTerms[i] = terms[i];
		return new MutiablePolynomial(pc,newTerms);
	}
	
	public PNodeI add(PNodeI node) throws ParseException
	{
		if(node instanceof Polynomial)
			return this.add((Polynomial) node);
			
		MutiablePolynomial mp = this.toMutiablePolynomial();
		mp.add(node);
		return mp.toPNode();
	}
	
	public PNodeI sub(PNodeI node) throws ParseException
	{
		if(node instanceof Polynomial)
			return this.sub((Polynomial) node);
			
		MutiablePolynomial mp = this.toMutiablePolynomial();
		mp.add(node.negate());
		return mp.toPNode();
	}

	public PNodeI add(Polynomial p) throws ParseException
	{
		MutiablePolynomial mp = this.toMutiablePolynomial();
		for(int i=0;i<p.terms.length;++i)
			mp.add(p.terms[i]);
		return mp.toPNode();
	}

	public PNodeI sub(Polynomial p) throws ParseException
	{
		MutiablePolynomial mp = this.toMutiablePolynomial();
		for(int i=0;i<p.terms.length;++i)
			mp.add(p.terms[i].negate());
		return mp.toPNode();
	}

	public PNodeI negate() throws ParseException
	{
		PNodeI newTerms[] = new PNodeI[terms.length];
		for(int i=0;i<terms.length;++i)
			newTerms[i] = terms[i].negate();
		return new Polynomial(pc,newTerms);				
	}
	
	public PNodeI mul(PNodeI node)  throws ParseException
	{
		if(node instanceof PConstant)
		{
			PConstant c = (PConstant) node;
			if(c.isZero()) return pc.zeroConstant;
			if(c.isOne()) return this;
			//if(c.isInfinity()) return pc.infConstant;
			
//			PNodeI newTerms[] = new PNodeI[terms.length];
//			for(int i=0;i<terms.length;++i)
//				newTerms[i] = terms[i].mul(c);
//			return new Polynomial(pc,newTerms);				
		}
		return super.mul(node);
	}
	
	public PNodeI div(PNodeI node)  throws ParseException
	{
		if(node instanceof PConstant)
		{
			PConstant c = (PConstant) node;
			if(c.isZero()) return pc.infConstant;
			if(c.isOne()) return this;
			PNodeI newTerms[] = new PNodeI[terms.length];
			for(int i=0;i<terms.length;++i)
				newTerms[i] = terms[i].div(c);
			return new Polynomial(pc,newTerms);				
		}
		return super.div(node);
	}
	
	public boolean equalsPNode(PNodeI node)
	{
		if(!(node instanceof Polynomial)) return false;
		Polynomial p = (Polynomial) node;
		if(terms.length != (p.terms.length)) return false;
		for(int i=0;i<terms.length;++i)
			if(!terms[i].equalsPNode(p.terms[i])) return false;
		return true; 
	}
	/**
	this < arg ---> -1
	this > arg ---> 1
	*/

	public int compareTo(PNodeI node)
	{
		if(node instanceof Polynomial)
			return this.compareTo((Polynomial) node);
		int res = terms[0].compareTo(node);
		if(res != 0) return res;
		if(terms.length == 1) return 0;
		return 1;
	}

	public int compareTo(Polynomial p)
	{
		for(int i=0;i<terms.length;++i)
		{
			if(i >= p.terms.length) return 1;
			int res = terms[i].compareTo(p.terms[i]);
			if(res != 0) return res;
		}
		if(terms.length < p.terms.length) return -1;
		return 0;
	}
	
	private boolean isNegative(PNodeI node)
	{
		if(node instanceof PConstant)
			return ((PConstant) node).isNegative();
		if(node instanceof Monomial)
			return ((Monomial) node).negativeCoefficient();
		return false;
	}
	public String toString() {
		if(terms.length==0)
			return "0";
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<terms.length;++i)
		{
			if(i>0 && !isNegative(terms[i])) 
				sb.append('+');
			sb.append(terms[i].toString());
		}
		return sb.toString();
	}
	
	public Node toNode() throws ParseException {
		if(terms.length==0)
			return pc.nf.buildConstantNode(pc.zero);
		Node args[] = new Node[terms.length];
		int signs[] = new int[terms.length];
		int nNeg = 0;
		for(int i=0;i<terms.length;++i) {
		    signs[i]=1;
		    if(terms[i] instanceof Monomial) {
			if(((Monomial) terms[i]).getCoeff().isNegative()) {
			    signs[i] = -1;
			    ++nNeg;
			    args[i] = terms[i].negate().toNode();
			}
			else
			    args[i] = terms[i].toNode();
			    
		    }
		    else
			args[i] = terms[i].toNode();
		}
		if(nNeg==0) {
			if(terms.length ==1) 
			    if(nNeg==0) return args[0];
			return pc.nf.buildOperatorNode(pc.os.getAdd(),args);	
		    
		}
		if(terms.length ==1) 
		    return pc.nf.buildOperatorNode(pc.os.getUMinus(),args[0]);

		if(terms.length==nNeg) {
		    Node n1 = pc.nf.buildOperatorNode(pc.os.getUMinus(),args[0]);
		    for(int i=1;i<terms.length;++i) {
			n1 = pc.nf.buildOperatorNode(pc.os.getSubtract(),n1,args[i]);
		    }
		    return n1;
		}

		Node posargs[] = new Node[terms.length-nNeg];
		Node negargs[] = new Node[nNeg];
		int posCount=0;
		int negCount=0;
		for(int i=0;i<terms.length;++i) {
		    if(signs[i]>0)
			posargs[posCount++] = args[i];
		    else
			negargs[negCount++]=args[i];
		}
		Node n1;
		if(posCount==1) 
		    n1 = posargs[0];
		else
		    n1 = pc.nf.buildOperatorNode(pc.os.getAdd(),posargs);
		
		for(int i=0;i<negCount;++i)
		    n1 = pc.nf.buildOperatorNode(pc.os.getSubtract(),n1,negargs[i]);	    
		return n1;	
	}
	
	public PNodeI expand() throws ParseException {
		MutiablePolynomial mp = new MutiablePolynomial(pc,new PNodeI[]{pc.zeroConstant});
		for(int i=0;i<terms.length;++i)
		{
			PNodeI exp = terms[i].expand();
			mp.add(exp);
		}
		 return mp.toPNode();
	}
	
	/**
	 * The number of terms which make up the polynomial.
	 * @return the number of terms
	 */
	public int getNTerms() { return terms.length; }
	/**
	 * Get the n-th term
	 * @param i index of terms
	 * @return PNodeI for that term.
	 */
	public PNodeI getTerm(int i) { return terms[i]; }
}
