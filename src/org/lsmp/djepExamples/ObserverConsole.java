/* @author rich
 * Created on 28-Mar-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djepExamples;

import java.util.Observable;
import java.util.Observer;
import java.util.Enumeration;

import org.lsmp.djep.djep.DPrintVisitor;
import org.lsmp.djep.xjep.PrintVisitor;
import org.lsmp.djep.xjep.XJep;
import org.lsmp.djep.xjep.XVariable;
import org.nfunk.jep.SymbolTable;
import org.nfunk.jep.Variable;
import org.nfunk.jep.Node;
/**
 * A console applet which illustrates how variables can be watched.
 * @author Rich Morris
 * Created on 28-Mar-2005
 */
public class ObserverConsole extends DJepConsole implements Observer
{
	private static final long serialVersionUID = 5393968786564920519L;

	public void update(Observable arg0, Object arg1)
	{
		PrintVisitor pv = ((XJep) j).getPrintVisitor();
		
		if(arg0 instanceof XVariable)
		{
			XVariable var = (XVariable) arg0;
			if(arg1 instanceof Node)
				println("Variables equation changed: "+var.toString(pv));
			else
				println("Var changed: "+var.toString(pv));
		}
		else if(arg0 instanceof SymbolTable.StObservable)
		{
			println("New var: "+((XVariable) arg1).toString(pv));
			((Variable) arg1).addObserver(this);
		}
	}

	public void initialise()
	{
		super.initialise();
		SymbolTable st = j.getSymbolTable();
		st.addObserver(this);
		st.addObserverToExistingVariables(this);

		for(Enumeration en = st.elements();en.hasMoreElements();) {
			Variable var = (Variable) en.nextElement();
			println("Existing variable "+var);
		}
	}

	public static void main(String args[]) {
		Console c = new ObserverConsole();
		c.run(args);
	}
}
