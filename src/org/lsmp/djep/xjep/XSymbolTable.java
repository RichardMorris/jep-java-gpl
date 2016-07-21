/*****************************************************************************

@header@
@date@
@copyright@
@license@

*****************************************************************************/
package org.lsmp.djep.xjep;
import java.util.*;
import org.nfunk.jep.*;

/**
 * An extension of the symbol table with a few new features.
 * 
 * @author Rich Morris
 * Created on 18-Mar-2004
 */
public class XSymbolTable extends SymbolTable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 741560154912130566L;

	/**
	 * Create a new XSymbolTable with the given variable factory.
	 */
	public XSymbolTable(VariableFactory varFac)
	{
		super(varFac);
	}
	
	/** 
	 * Creates a new SymbolTable with the same variable factory as this, and the same set of constants. 
	 *
	 **/
	public XSymbolTable newInstance()
	{
		XSymbolTable st = new XSymbolTable(this.getVariableFactory());
		st.copyConstants(this);
		return st;
	}

	/** Prints the contents of the symbol table displaying its equations and value. */	
	public void print(PrintVisitor pv)
	{
		for(Enumeration e = this.elements(); e.hasMoreElements(); ) 
		{
			XVariable var = (XVariable) e.nextElement();
			pv.append(var.toString(pv)+"\n");
			// TODO watch out for possible conflict with overriding pv's string buffer
		}
	}	
	
	/** Copy the values of all constants from the supplied symbol table into this symbol table. 
	 * 
	 * @param symTab the Symbol table with existing constants.
	 */
	public void copyConstants(SymbolTable symTab)
	{
		for(Enumeration e = symTab.elements(); e.hasMoreElements(); ) 
		{
			Variable var = (Variable) e.nextElement();
			if(var.isConstant())
				this.addConstant(var.getName(),var.getValue());
		}
	}
}
