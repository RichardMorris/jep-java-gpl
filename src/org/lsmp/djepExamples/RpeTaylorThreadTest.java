/*
Created 2 Aug 2006 - Richard Morris
*/
package org.lsmp.djepExamples;

import org.lsmp.djep.rpe.RpCommandList;
import org.lsmp.djep.rpe.RpEval;
import org.lsmp.djep.xjep.XJep;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;

/**
 * Testing thread safety, using Taylor approximation to ln(1+x).
 * @author Richard Morris
 *
 */
public class RpeTaylorThreadTest {

	XJep xj;
	RpEval rpe;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RpeTaylorThreadTest tt = new RpeTaylorThreadTest();
		tt.go(25,10,100);
	}
	
	public RpeTaylorThreadTest() {
		xj = new XJep();
		xj.addStandardConstants();
		xj.addStandardFunctions();
		xj.setAllowAssignment(true);
		xj.setAllowUndeclared(true);
		xj.setImplicitMul(true);
		this.rpe = new RpEval(xj);
	}

	/**
	 * Builds an expression which is Taylor approximation to ln(1+x), i.e. x-x^2/2+x^3/3+x^4/4+.. 
	 * @param nPower number of terms in sequence, last term is x^nPower/nPower
	 * @return string representation of expression
	 */
	public String buildExpression(int nPower){
		StringBuffer sb = new StringBuffer();
		for(int i=1;i<=nPower;++i)
		{
			if(i%2==0)
				sb.append("-");
			else
				sb.append("+");
			sb.append("x^"+i+"/"+i);
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
		Node globalExpression=null; 
		RpCommandList com=null;
		int ref=-1;
		String expression = buildExpression(nPower);
		try
		{
			Node base = xj.parse(expression);
			globalExpression = xj.preprocess(base);
			xj.getPrintVisitor().setMaxLen(80);
			xj.println(globalExpression);
			com = rpe.compile(globalExpression);
			ref = rpe.getVarRef(xj.getVar("x"));
		}
		catch(ParseException e) { e.printStackTrace(); }
		catch(Exception e) { e.printStackTrace(); }
		
		double range = 1.0/nThreads;
		EvaluationThread et[] = new EvaluationThread[nThreads];
		for(int i=0;i<nThreads;++i)
		{
			double x = ((double) i)/nThreads;
			double val = x+ (nItts-1.0)/nItts * range;
			rpe.setVarValue(ref,val);

			double res = rpe.evaluate(com);
			double lnres = Math.log(1+val);
			double error = Math.abs(res-lnres);
			//System.out.println("Error for "+val+" is "+error);
			
			et[i] = new EvaluationThread(rpe,com,ref,x,range,nItts,Math.max(10*error,5E-16));
			et[i].start();
		}
	}

	/**
	 * A thread which evaluates an expression
	 */
	class EvaluationThread extends Thread {
		RpEval rpe;
		RpCommandList com;
		int ref;
		double baseValue,range,maxError;
		int itts;
		
		/**
		 * Sets up thread for repeated evaluation.
		 * @param rpe The base RpEval object, which will be duplicated.
		 * @param com The command list to be evaluated.
		 * @param ref Reference number for variable x.
		 * @param testValue first value to test over 
		 * @param range the range to evaluate over, evaluation happens over [testValue,tastValue+range]
		 * @param nItts number of iterations to use
		 * @param maxError maximum error allowed
		 */
		public EvaluationThread(RpEval rpe,RpCommandList com,int ref,double testValue,double range,int nItts,double maxError)
		{
			baseValue = testValue;
			this.range = range;
			this.maxError = maxError;
			itts = nItts;
			this.rpe = (RpEval) rpe.duplicate();
			this.com = com;
			this.ref = ref;
		}
		
		public void run() {
			double myMaxError = 0.0;
			double failValue = 0.0;
			System.out.println("run "+baseValue);
			for(int j=0;j<itts;++j)
			{
				double value = baseValue+range*((double)j)/itts;
				rpe.setVarValue(ref,value);
				double res = rpe.evaluate(com);
				double lnres = Math.log(1+value);
				double error = Math.abs(res-lnres);
//				if(error>myMaxError)
//					System.out.println("Result "+res+" does not match expected result "+lnres+" for input value "+value+"\n\terror "+error+" expected error "+maxError);
				if(error>myMaxError) {
					myMaxError = error;
					failValue = value;
				}
			}
			if(myMaxError>maxError)
				System.out.println("Error test failed for "+failValue+" maxError "+myMaxError+" predicted "+maxError);
			else
				System.out.println("Error for "+failValue+" is "+myMaxError);
		}
	}

}
