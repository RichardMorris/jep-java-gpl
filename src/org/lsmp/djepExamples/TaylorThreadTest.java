/*
Created 2 Aug 2006 - Richard Morris
*/
package org.lsmp.djepExamples;

import org.lsmp.djep.xjep.XJep;
import org.lsmp.djep.xjep.XSymbolTable;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;

/**
 * Testing thread safety, using Taylor approximation to ln(x).
 * @author Richard Morris
 *
 */
public class TaylorThreadTest {

	class EvaluationThread extends Thread {
		XJep myXj=null;
		Node inExpression;
		Node myExpression;
		double baseValue;
		int itts;
		
		public EvaluationThread(XJep xj,Node inNode,double testValue,int nItts)
		{
			baseValue = testValue;
			itts = nItts;
			//result = testResult;
			XSymbolTable st = ((XSymbolTable)xj.getSymbolTable()).newInstance();
			myXj = xj.newInstance(st);
			inExpression = inNode;
		}
		
		public void run() {
			double maxError=0.0;
			try {
				Node n2 = myXj.importExpression(inExpression);
				myExpression = n2; //myXj.preprocess(n2); Don't need to preprocess.
				
				System.out.println("run "+baseValue);
				for(int j=0;j<itts;++j)
				{
					double value = baseValue+0.01*((double)j)/(2*itts);
					myXj.setVarValue("x", new Double(value));
					Object resObj = myXj.evaluate(myExpression);
					double res = ((Double) resObj).doubleValue();
					double lnres = Math.log(1+value);
					double error = Math.abs(res-lnres);
					if(error>maxError)
						maxError=error;
	//				if(error>1.0E-10)
	//					System.out.println("Result "+res+" does not match expected result "+lnres+" for input value "+value+" error "+error);
				}
				System.out.println("run done "+baseValue+"\tmaxError "+maxError);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
	
	XJep xj;
	
	
	public TaylorThreadTest() {
		xj = new XJep();
		xj.addStandardConstants();
		xj.addStandardFunctions();
		xj.setAllowAssignment(true);
		xj.setAllowUndeclared(true);
		xj.setImplicitMul(true);
		//xj.addStandardDiffRules();
	}
	
	public void go(int nDeriv,int nThreads,int nItts)
	{
		Node globalExpression=null; 
		StringBuffer sb = new StringBuffer();
		for(int i=1;i<=nDeriv;++i)
		{
			if(i%2==0)
				sb.append("-");
			else
				sb.append("+");
			sb.append("x^"+i+"/"+i);
		}
		String expression = sb.toString();
		try
		{
			Node base = xj.parse(expression);
			globalExpression = xj.preprocess(base);
			xj.getPrintVisitor().setMaxLen(80);
			xj.println(globalExpression);
		}
		catch(Exception e) { e.printStackTrace(); }
		
		EvaluationThread et[] = new EvaluationThread[nThreads];
		for(int i=0;i<nThreads;++i)
		{
			double x = ((double) i)/nThreads;
			et[i] = new EvaluationThread(xj,globalExpression,x,nItts);
			et[i].start();
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TaylorThreadTest tt = new TaylorThreadTest();
		tt.go(20,25,100);
	}

}
