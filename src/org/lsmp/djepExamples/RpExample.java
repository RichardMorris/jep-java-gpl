/* @author rich
 * Created on 26-Feb-2004
 */

package org.lsmp.djepExamples;
import org.nfunk.jep.*;
import org.lsmp.djep.rpe.*;
/**
 * Examples using vectors and matrices
 */
public class RpExample {
	static JEP j;
	
	public static void main(String args[])	{
		j = new JEP();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		j.setAllowUndeclared(true);
		j.setImplicitMul(true);
		j.setAllowAssignment(true);

		// parse and evaluate each equation in turn
		
		doAll(new String[]{"1*2*3+4*5*6+7*8*9"});
		doAll(new String[]{"x1=1","x2=2","x3=3","x4=4","x5=5","x6=6","x7=7","x8=8","x9=9",
			"x1*x2*x3+x4*x5*x6+x7*x8*x9"});
		doAll(new String[]{"x=0.7","cos(x)^2+sin(x)^2"});
	}


	public static void doAll(String str[])	{
		try	{
			RpEval rpe = new RpEval(j);

			for(int i=0;i<str.length;++i)
			{
				Node node = j.parse(str[i]);
				RpCommandList list = rpe.compile(node);
				double res = rpe.evaluate(list);

				// conversion to String
				System.out.println("Expression "+i+":\t"+str[i]+"\nresult\t" + res+"\n");
			
				// List of commands
				System.out.println("Commands:");
				System.out.println(list.toString(rpe));
				System.out.println();
			}

		}
		catch(ParseException e) { System.out.println("Parse error "+e.getMessage()); }		
		catch(Exception e) { System.out.println("evaluation error "+e.getMessage()); e.printStackTrace(); }		
	}
}
