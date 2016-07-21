/*
Created 15-Jul-2006 - Richard Morris
*/
package org.lsmp.djepExamples;

import java.util.Stack;

import org.lsmp.djep.rpe.*;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/** A Console application illustrating the use of the RPE evaluator.
 * The org.lsmp.djep.rpe package is intended to speed up multiple evaluation of the same equation
 * with different values for the variable. 
 * As each equation is only evaluated once this will not show a speed improvement. 
 * @author Richard Morris
 */
public class RpeConsole extends Console {
	private static final long serialVersionUID = 2604208990249603097L;
	RpEval rpe;

	public static void main(String[] args) {
		Console c = new RpeConsole();
		c.run(args);
	}

	public static class f0 extends PostfixMathCommand {
		public f0() {
			this.numberOfParameters=0;
		}
//		@Override
		public void run(Stack s) throws ParseException {
			s.push(new Double(123));
		}
	}
	public static class f1 extends PostfixMathCommand {
		public f1() {
			this.numberOfParameters=1;
		}
//		@Override
		public void run(Stack s) throws ParseException {
			double val = ((Double)s.pop()).doubleValue();
			s.push(new Double(val*val));
		}
	}
	public static class f2 extends PostfixMathCommand {
		public f2() {
			this.numberOfParameters=2;
		}
//		@Override
		public void run(Stack s) throws ParseException {
			double r = ((Double)s.pop()).doubleValue();
			double l = ((Double)s.pop()).doubleValue();
			s.push(new Double(l+r*10));
		}
	}
	public static class fn extends PostfixMathCommand {
		public fn() {
			this.numberOfParameters=-1;
		}
//		@Override
		public void run(Stack s) throws ParseException {
			int n=this.curNumberOfParameters;
			double val=0.0;
			for(int i=0;i<n;++i)
			val = val*10+((Double)s.pop()).doubleValue();
			s.push(new Double(val));
		}
	}
	public static class s0 extends PostfixMathCommand implements RealNullaryFunction {
		public s0() {
			this.numberOfParameters=0;
		}
//		@Override
		public void run(Stack s) throws ParseException {
			s.push(new Double(-1));
		}
		public double evaluate() {
			return 123;
		}
	}
	public static class s1 extends PostfixMathCommand implements RealUnaryFunction {
		public s1() {
			this.numberOfParameters=1;
		}
//		@Override
		public void run(Stack s) throws ParseException {
			double val = ((Double)s.pop()).doubleValue();
			s.push(new Double(-1));
		}
		public double evaluate(double l) {
			return l*l;
		}
	}
	public static class s2 extends PostfixMathCommand implements RealBinaryFunction {
		public s2() {
			this.numberOfParameters=2;
		}
//		@Override
		public void run(Stack s) throws ParseException {
			double r = ((Double)s.pop()).doubleValue();
			double l = ((Double)s.pop()).doubleValue();
			s.push(new Double(-1));
		}
		public double evaluate(double l, double r) {
			return l+10*r;
		}
	}
	public static class sn extends PostfixMathCommand implements RealNaryFunction {
		public sn() {
			this.numberOfParameters=-1;
		}
//		@Override
		public void run(Stack s) throws ParseException {
			int n=this.curNumberOfParameters;
			double val=0.0;
			for(int i=0;i<n;++i)
			val = val*10+((Double)s.pop()).doubleValue();
			s.push(new Double(-1));
		}
		public double evaluate(double[] parameters) {
			double val=0;
			for(int i=0;i<parameters.length;++i)
				val = val*10+parameters[i];
			return val;
		}
	}
	public void initialise() {
		super.initialise();
		j.addFunction("f0",new f0());
		j.addFunction("f1",new f1());
		j.addFunction("f2",new f2());
		j.addFunction("fn",new fn());
		j.addFunction("s0",new s0());
		j.addFunction("s1",new s1());
		j.addFunction("s2",new s2());
		j.addFunction("sn",new sn());
		rpe = new RpEval(j);
	}

	public void processEquation(Node node) throws ParseException {
		RpCommandList list = rpe.compile(node);
		double val = rpe.evaluate(list);
		println(new Double(val));
	}

	public String getPrompt() {
		return "RPE > ";
	}

	public void printIntroText() {
		println("RPE Console.");
		printStdHelp();
	}

}
