/*
Created 2 Aug 2006 - Richard Morris
*/
package org.lsmp.djepExamples;

import org.lsmp.djep.xjep.XJep;
import org.lsmp.djep.xjep.XSymbolTable;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;

public class ThreadTest {

	Node globalExpression; 
	class EvaluationThread extends Thread {
		XJep myXj=null;
		Node myExpression;
		double baseValue;
		public EvaluationThread(XJep xj,Node inNode,double testValue,double testResult)
		{
			baseValue = testValue;
			//result = testResult;
			XSymbolTable st = ((XSymbolTable)xj.getSymbolTable()).newInstance();
			myXj = xj.newInstance(st);
			try
			{
				 Node n2 = myXj.importExpression(inNode);
				 myExpression = myXj.preprocess(n2);
			}
			catch(Exception e) 
			{ e.printStackTrace(); }
		}
		
		public void run() {
			
			try {
				System.out.println("run "+baseValue);
				for(int j=0;j<100000;++j)
				{
					double value = baseValue+j/10000.0;
					myXj.setVarValue("x", new Double(value));
					Object res = myXj.evaluate(myExpression);
					if(((Double) res).doubleValue() != value*value-1.0)
						System.out.println("Result "+res+" does not match expected result "+(value*value-1.0)+" for input value "+value);
				}
				System.out.println("run done "+baseValue);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
	public void go()
	{
		XJep xj = new XJep();
		xj.addStandardConstants();
		xj.addStandardFunctions();
		xj.setAllowAssignment(true);
		xj.setAllowUndeclared(true);
		xj.setImplicitMul(true);
		try
		{
			Node n = xj.parse("x^2-1");
			globalExpression = xj.preprocess(n);
		}
		catch(Exception e) { e.printStackTrace(); }
		
		EvaluationThread et[] = new EvaluationThread[101];
		for(int i=0;i<=100;++i)
		{
			double x = (i-50.0);
			et[i] = new EvaluationThread(xj,globalExpression,x,x*x-1);
		}
		for(int i=100;i>=0;--i)
		{
			et[i].start();
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ThreadTest tt = new ThreadTest();
		tt.go();
	}

}
