/*
Created 2 Aug 2006 - Richard Morris
*/
package org.lsmp.djepExamples;

import org.lsmp.djep.rpe.RpCommandList;
import org.lsmp.djep.rpe.RpEval;
import org.nfunk.jep.JEP;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;

/**
 * Testing thread safety, using Taylor approximation to ln(1+x).
 * @author Richard Morris
 *
 */
public class RpeThreadTest {

	JEP jep;
	RpEval rpe;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RpeThreadTest tt = new RpeThreadTest();
		tt.go(15,25,10000);
	}
	
	public RpeThreadTest() {
		jep = new JEP();
		jep.addStandardConstants();
		jep.addStandardFunctions();
		jep.setAllowAssignment(true);
		jep.setAllowUndeclared(true);
		jep.setImplicitMul(true);
		this.rpe = new RpEval(jep);
	}

	/**
	 * Builds the expression 1+x+x^2+...+x^n 
	 * @param nPower number of terms in sequence, last term is x^(nPower-1)
	 * @return string representation of expression
	 */
	public String buildExpression(int nPower){
		StringBuffer sb = new StringBuffer("1");
		for(int i=1;i<nPower;++i)
		{
			sb.append("+x^"+i);
		}
		return sb.toString();
		
	}
	
	/**
	 * Main execution routine, creates a number of threads evaluation the same expression and executes them.
	 * 
	 * @param nPower
	 * @param nThreads
	 * @param nItts
	 */
	public void go(int nPower,int nThreads,int nItts)
	{
		RpCommandList com=null;
		int ref=-1;
		String expression = buildExpression(nPower);
		System.out.println(expression);
		try
		{
			Node base = jep.parse(expression);
			com = rpe.compile(base);
			ref = rpe.getVarRef(jep.getVar("x"));
		}
		catch(ParseException e) { e.printStackTrace(); return; }
		catch(Exception e) { e.printStackTrace(); return; }
		
		EvaluationThread et[] = new EvaluationThread[nThreads];
		for(int i=0;i<nThreads;++i)
		{
			et[i] = new EvaluationThread(rpe,com,ref,nPower,nItts,i);
			et[i].start();
		}
	}

	/**
	 * A thread which evaluates an expression
	 */
	class EvaluationThread extends Thread {
		RpEval baseRpe;
		RpCommandList com;
		int ref;
		int itts;
		int power;
		int index;
		
		/**
		 * Sets up thread for repeated evaluation.
		 * @param rpe The base RpEval object, which will be duplicated.
		 * @param com The command list to be evaluated.
		 * @param ref Reference number for variable x.
		 * @param nPower max power of geometric progression
		 * @param nItts number of iterations to use
		 * @param index index number of thread
		 */
		public EvaluationThread(RpEval rpe,RpCommandList com,int ref,int nPower,int nItts,int index)
		{
			this.baseRpe = rpe;
			this.com = com;
			this.ref = ref;

			this.itts = nItts;
			this.power = nPower;
			this.index = index;
			System.out.println("Constructed thread "+index);
		}
		
		public void run() {
			RpEval rpe = baseRpe.duplicate();
			double maxError=0.0;
			System.out.println("Running "+itts+" evaluations in thread "+index);
			for(int j=0;j<itts;++j)
			{
				double value = Math.random()-0.5;
				rpe.setVarValue(ref,value);
				double res = rpe.evaluate(com);
				double expected = (1-Math.pow(value,power))/(1-value);
				double error = Math.abs(res-expected);
				if(error>1E-15)
					System.out.println("Result "+res+" does not match expected result "+expected+" for input value "+value+"\terror "+error);
				if(error>maxError)
					maxError = error;
			}
			System.out.println("Finished thread "+index+" max error "+maxError);
		}
	}

}
