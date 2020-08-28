/* @author rich
 * Created on 26-Feb-2004
 */

package org.nfunk.jepexamples.diagnostics;

import static java.lang.System.out;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.nfunk.jep.JEP;
import org.nfunk.jep.Node;
import org.nfunk.jep.Variable;


/**
 * Compares the speed of evaluation between different evaluation schemes.
 * The standard class compares BigDecimal, Jep (with default Fast evaluator), 
 * the old StandardEvaluator, and RealEvaluator.
 * <p>
 * If you have some nice complicated examples, I'd love to
 * hear about them to see if we can tune things up. - rich
 */
public class SpeedTest {
    public int num_itts = 100000; // for normal use
    public int num_warmup = 100000; // number of iterations for warm up 
    //	static int num_itts = 100;	  // for use with profiler
    public int num_vals = 1000; // number of random numbers selected
    public int nDeriv = 20;
    static MathContext MC = MathContext.DECIMAL64;

    long seed; // seed for random number generator
    Random generator;


    protected final List<EvaluationConfig> configs = new ArrayList<>();
    protected Outputter outputter;
    protected long[] totalTimes;

    /**
     * @param outputter
     */
    public SpeedTest(Outputter outputter) {
        this.outputter = outputter;
        seed = System.currentTimeMillis();
    }

    public void init() {
        generator = new Random(seed);
        totalTimes = new long[configs.size()];
        outputter.printHeader(this);
    }

    public void fini() {
        outputter.printFooter(this, totalTimes);
    }

    /*** Different output methods ****************/

    public static class Outputter {
        JEP globalJep = new JEP();
        {
        globalJep.setAllowUndeclared(true);
        globalJep.setImplicitMul(true);
        globalJep.setAllowAssignment(true);
        globalJep.addStandardFunctions();
        }
        public void printHeader(SpeedTest st) {
            out.println("Performing "+st.num_itts+" iterations, "+st.num_warmup+" warm up iterations.");
            for(EvaluationConfig c :st.configs) {
                out.println(c.name + "\t" + c.description());
            }
        }
        
        /**
		 * @param st  
         * @param varNames 
		 */
        public void printOutputHeader(SpeedTest st,String eqn, String varNames[]) {
            out.println("\nTesting speed for \"" + eqn + "\"");
            try {
                Node node = globalJep.parse(eqn);
            } catch (Exception e) {
                out.println(e.getMessage());
            }

        }

        /**
		 * @param eqn 
         * @param varNames  
		 */
        public void printOutputTimes(SpeedTest st,String eqn, String varNames[],long[] times) {
            for(int i=0;i<st.configs.size();++i) {
                out.println(st.configs.get(i).name+"\t"+times[i]);
            }
        }

		public void printOutputHeader(SpeedTest st,String eqns[], String varNames[]) {
            out.print("\nTesting speed for ");
            for(int i=0;i<eqns.length;++i) {
                if(i>0) System.out.print(", ");
                out.print("\""+eqns[i] + "\"");
            }
            out.println(".");
//            try {
//                TreeAnalyzer ta = new TreeAnalyzer();
//                for(int i=0;i<eqns.length;++i) {
//                    Node node = globalJep.parse(eqns[i]);
//                    ta.analyze(node);
//                }
//                out.println(ta.summary());
//            } catch (Exception e) {
//                out.println(e.getMessage());
//            }
        }

        /**
		 * @param eqns  
         * @param varNames 
		 */
        public void printOutputTimes(SpeedTest st,String eqns[], String varNames[],long[] times) {

        	for(int i=0;i<st.configs.size();++i) {
                out.println(st.configs.get(i).name+"\t"+times[i]);
            }
        }

        public void printFooter(SpeedTest st,long[] totalTimes) {
            out.println();
            out.println("======= Totals =======");
            for(int i=0;i<st.configs.size();++i) {
                EvaluationConfig c = st.configs.get(i);
				out.println(c.name+"\t"+totalTimes[i]+"\t"+c.description());
            }
            this.printRatios(st, totalTimes);
        }
        
        public void printRatios(SpeedTest st, long[] time) {
            out.println();
            out.println("======= Ratios =======");
            out.print("\t");
            for(int i=0;i<time.length;++i) 
                out.print(st.configs.get(i).name+"\t");
            out.println();

            for(int i=0;i<time.length;++i) {
                out.print(st.configs.get(i).name+"\t");
                for(int j=0;j<time.length;++j) {
                    long t1 = time[i];
                    long t2 = time[j];
                    if(t2 != 0) {
                        Double ratio = ((double) t1) / t2;
                        out.printf("%.2f\t", new Object[] { ratio });
                    } else
                        out.print("" + t1 + "/0\t");
                }
                out.println();
            }
        }

    }
    /**
     * Print detailed ratios for each run.
     */
    public static class RatioOutputter extends Outputter {
        @Override
        public void printOutputTimes(SpeedTest st,String eqn, String varNames[],long[] times) {
            super.printOutputTimes(st,eqn,varNames,times);
            printRatios(st,times);
        }
        @Override
        public void printOutputTimes(SpeedTest st,String eqns[], String varNames[],long[] times) {
            super.printOutputTimes(st, eqns, varNames, times);
            printRatios(st,times);
        }
        @Override
        public void printFooter(SpeedTest st, long[] totalTimes) {
            super.printFooter(st, totalTimes);
        }
    }
    /**
     * Print output tab separated.
     * One line of headers
     * One line per test
     * One line of totals
     */
    public static class TabOutputter extends Outputter {
        @Override
        public void printOutputTimes(SpeedTest st,String eqn, String varNames[],long[] times) {
            out.print(eqn);

            for(int i=0;i<st.configs.size();++i) {
                out.print("\t");
                out.print(times[i]);
            }
            out.println();
        }
        @Override
        public void printOutputTimes(SpeedTest st,String eqns[], String varNames[],long[] times) {
            for(int i=0;i<eqns.length;++i) {
                out.print(eqns[i] + ";");
            }

            for(int i=0;i<st.configs.size();++i) {
                out.print("\t");
                out.print(times[i]);
            }
            out.println();
        }
        @Override
        public void printFooter(SpeedTest st, long[] totalTimes) {
            out.print("Total");
            for(int i=0;i<st.configs.size();++i) {
                out.print("\t");
                out.print(totalTimes[i]);
            }
            out.println();
        }
        @Override
        public void printHeader(SpeedTest st) {
            for(int i=0;i<st.configs.size();++i) {
                out.print("\t");
                out.print(st.configs.get(i).name);
            }
            out.println();
        }


    }

    /**
     * Run speed comparison for a single equation.
     * 
     * @param eqn
     *            The equation to test
     * @param varNames
     *            an array of variable names which will be set to random values.
     */
    public void doAll(String eqn, String varNames[]) {

        outputter.printOutputHeader(this,eqn, varNames);

        double varVals[][] = new double[varNames.length][num_vals];

        for (int i = 0; i < varNames.length; ++i) {
            for (int j = 0; j < num_vals; ++j)
                varVals[i][j] = generator.nextDouble();
        }

        long times[] = new long[configs.size()];
        for (int i=0; i < configs.size(); ++i) {
            EvaluationConfig c = configs.get(i);
            times[i] = c.doEval(eqn, varNames, varVals);
            totalTimes[i] += times[i];
        }
        outputter.printOutputTimes(this,eqn, varNames, times);
        //		Runtime rt = Runtime.getRuntime();
        //		out.format("free %d max %d total %d%n",rt.freeMemory(),rt.maxMemory(),rt.totalMemory());
    }

    /**
     * Run speed comparison for a set of equations.
     * @param eqns
     * @param varNames
     */
    public void doAll(String eqns[], String varNames[]) {

        outputter.printOutputHeader(this,eqns, varNames);
        double varVals[][] = new double[varNames.length][num_vals];

        for (int i = 0; i < varNames.length; ++i) {
            for (int j = 0; j < num_vals; ++j)
                varVals[i][j] = generator.nextDouble();
        }

        long times[] = new long[configs.size()];
        for(int i=0;i<configs.size();++i) {
            EvaluationConfig c = configs.get(i);
            times[i] = c.doEval(eqns, varNames, varVals);
            totalTimes[i] += times[i];
        }
        outputter.printOutputTimes(this,eqns, varNames, times);
        //		Runtime rt = Runtime.getRuntime();
        //		out.format("free %d max %d total %d%n",rt.freeMemory(),rt.maxMemory(),rt.totalMemory());
    }
    /** Basic class to set the evaluation context **/


    public static abstract class EvaluationConfig {
        protected String name;
        protected JEP jep;
        /** Factor to reduce the number of iterations for this evaluator. Useful for very slow evaluators. */ 
        protected int div=1;
        protected SpeedTest st;
        /**
         * @param name
         */
        public EvaluationConfig(String name) {
            this.name = name;
        }

        public Object getValue(double d) {
            return d;
        }

        public abstract String description();

        public long doEval(String eqn, String varNames[], double vals[][])
        {
            return doEval(new String[]{eqn}, varNames, vals);
        }

        public long doEval(String eqns[], String varNames[], double vals[][])
        {
            long tdiff = 0;
            try {
            	// add all variables listed in the varNames array
                Variable vars[] = new Variable[varNames.length];
                for (int i=0; i < varNames.length; ++i) {
                    jep.addVariable(varNames[i], 0.0);
                    vars[i] = jep.getVar(varNames[i]);
                }
                
                // create a 2d array of values to set the variables to
                Object bdvals[][] = new Object[vals.length][st.num_vals];
                for (int i=0; i < vals.length; ++i)
                    for (int j=0; j < st.num_vals; ++j)
                        bdvals[i][j] = getValue(vals[i][j]);

                // parse all equations in the eqns array and store the root nodes in an array
                Node nodes[] = new Node[eqns.length];
                for (int i=0; i < eqns.length; ++i)
                    nodes[i] = jep.parse(eqns[i]);

                // Warm up run
                for (int i = 0; i < st.num_warmup/div; ++i) {
                    // set each variable value
                    for (int j = 0; j < vars.length; ++j)
                        vars[j].setValue(bdvals[j][i % st.num_vals]);
                    for (int j=0; j < eqns.length; ++j)
                        jep.evaluate(nodes[j]);
                }
                
                
                // get current time
                long t1 = System.currentTimeMillis();
                // perform iterations
                for (int i = 0; i < st.num_itts/div; ++i) {
                    // set each variable value
                    for (int j = 0; j < vars.length; ++j)
                        vars[j].setValue(bdvals[j][i % st.num_vals]);
                    for (int j=0; j < eqns.length; ++j)
                        jep.evaluate(nodes[j]);
                }
                // get current time
                long t2 = System.currentTimeMillis();
                // calc time elapsed
                tdiff = t2 - t1;
            } catch (Exception e) {
                out.println("Error: " + this.name + "\t"+  e.toString());
                //e.printStackTrace();
                tdiff = -1;
            }
            return tdiff * div;
        }
    }

    /**
     * Standard Jep configuration (with FastEvaluator)
     */
    public static class JepConfig extends EvaluationConfig {

        public JepConfig(String name) {
            super(name);
            jep = new JEP();
            jep.setImplicitMul(true);
            jep.setAllowUndeclared(true);
            jep.setAllowAssignment(true);
            jep.addStandardFunctions();
            jep.addStandardConstants();
        }

        @Override
        public String description() {
            return "Standard JEP config";
        }
    }



    
    /** 
     * Adds a new EvaluationConfig to be be run for comparison.
     * @param config
     */
    public void addConfig(EvaluationConfig config) {
        this.configs.add(config);
        config.st = this;
    }




    static void doCos(SpeedTest st)
    {
        Double[] varVals = new Double[st.num_vals];
        for(int j=0;j<st.num_vals;++j) {
            varVals[j] = Double.valueOf(st.generator.nextDouble());
        }

        long t1 = System.currentTimeMillis();
        double x; 
        for(int i=0;i<st.num_itts;++i)
        {
            x = varVals[i%st.num_vals].doubleValue();
            @SuppressWarnings("unused")
            double c = Math.cos(x);
            //double s = Math.sin(x);
            //y = c*c+s*s;
        }
        long t2 = System.currentTimeMillis();
        out.println("Using Java:\t"+(t2-t1));
    }

    static void doHorner(SpeedTest st)
    {
        Double[] varVals = new Double[st.num_vals];
        for(int j=0;j<st.num_vals;++j)
            varVals[j] = Double.valueOf(st.generator.nextDouble());

        long t1 = System.currentTimeMillis();
        double x; 
        @SuppressWarnings("unused")
        double y;
        for(int i=0;i<st.num_itts;++i)
        {
            x = varVals[i%st.num_vals].doubleValue();
            y = 1+x*(1+x*(1+x*(1+x*(1+x))));
        }
        long t2 = System.currentTimeMillis();
        out.println("Using Java:\t"+(t2-t1));
    }

    static final double powN(double rIn,short n){
        double r = rIn;
        switch(n){
        case 0: r = 1.0; break;
        case 1: break;
        case 2: r *= r; break;
        case 3: r *= r*r; break;
        case 4: r *= r*r*r; break;
        case 5: r *= r*r*r*r; break;
        case 6: r *= r*r*r*r*r; break;
        case 7: r *= r*r*r*r*r*r; break;
        case 8: r *= r*r*r*r*r*r*r; break;
        default:
        {
            short bitMask = n;
            double evenPower = r;
            double result;
            if ( (bitMask & 1) != 0 )
                result = r;
            else
                result = 1;
            bitMask >>>= 1;
        while ( bitMask != 0 ) {
            evenPower *= evenPower;
            if ( (bitMask & 1) != 0 )
                result *= evenPower;
            bitMask >>>= 1;
        } // end while
        r = result;
        }
        }
        return r;
    } 

    /** Evaluate approximation to log in Java "x^1/1-x^2/2+x^3/3-..." */
    static void doLn(SpeedTest st)
    {
        Double[] varVals = new Double[st.num_vals];
        for(int j=0;j<st.num_vals;++j) {
            varVals[j] = Double.valueOf(st.generator.nextDouble());
        }

        long t1 = System.currentTimeMillis();
        double x; 
        for(int i=0;i<st.num_itts;++i)
        {
            x = varVals[i%st.num_vals].doubleValue();
            @SuppressWarnings("unused")
			double res = 0;
            for(int j=1;j<st.nDeriv;++j)
            {
                double val = powN(x,(short) j)/j;
                if(j%2==0) {
                    res -= val;
                } else {
                    res += val;
                }
            }
        }
        long t2 = System.currentTimeMillis();
        out.println("Using Java:\t"+(t2-t1));
    }

    /** Evaluate approximation to log using Horner form in Java "x*(1/1-x*(1/2-x*(1/3-x*(1/4 ...))))" */
    static void doLnHorner(SpeedTest st)
    {
        Double[] varVals = new Double[st.num_vals];

        for(int j=0;j<st.num_vals;++j) {
            varVals[j] = Double.valueOf(st.generator.nextDouble());
        }

        long t1 = System.currentTimeMillis();
        double x; 
        for(int i=0;i<st.num_itts;++i)
        {
            x = varVals[i%st.num_vals].doubleValue();
            double res = 1.0 / st.nDeriv;
            for(int j=st.nDeriv;j>=1;--j)
            {
                res = 1.0 /j - x * res;
            }
            res *= x;
        }
        long t2 = System.currentTimeMillis();
        out.println("Using Java:\t"+(t2-t1));
    }


    /**
     * Main method, executes all speed tests.
     * @param args
     */
    public static void main(String args[])	{
        SpeedTest st = new SpeedTest(new Outputter());
        if(args.length == 1)
            st.num_itts = Integer.parseInt(args[0]);

        st.addConfig(new JepConfig("Jep"));
        st.init();
        
        runTests(st);
        st.fini();
    }

	/**
	 * Generate string giving approximation to log 
	 */
	public static String lnExpression(String varName,int nDeriv) {
	    StringBuffer sb = new StringBuffer();
	    for(int i=1;i<=nDeriv ;++i)
	    {
	        if(i%2==0)
	            sb.append("-");
	        else if(i>1)
	            sb.append("+");
	        sb.append(varName+"^"+i+"/"+i);
	    }
	    String expression = sb.toString();
	    return expression;
	}

	/**
	 *  Generate a string giving an approximation to log using Horner expression of the form "x*(1/1-x*(1/2-x*(1/3-x*(1/4 ...))))" 
	 *
	 * @param varName
	 * @param nTerms
	 * @return a string containing the expression
	 */
	public static String hornerExpression(String varName,int nTerms) {
	    StringBuffer sb2 = new StringBuffer();
	    for(int i=1;i<nTerms ;++i)
	    {
	        sb2.append(varName);
	        sb2.append("*(");
	        sb2.append("1/"+i);
	        sb2.append("-");
	    }
	    sb2.append(varName);
	    sb2.append("/");
	    sb2.append(nTerms);
	    for(int i=1;i<nTerms ;++i)
	        sb2.append(")");
	
	    String expression2 = sb2.toString();
	    return expression2;
	}


    public static void runTests2(SpeedTest st) {
        String expression = lnExpression("x", st.nDeriv);
        st.doAll(expression, new String[]{"x"});
    }
    
    /** 
     * A standard set of tests.
     * @param st
     */
    public static void runTests(SpeedTest st) {

    	
    	st.doAll(new String[]{}, new String[]{});
        st.doAll(new String[]{}, new String[]{"x"});
        st.doAll(new String[]{}, new String[]{"x","y","z","w"});
        st.doAll("5", new String[]{});
        st.doAll("x", new String[]{"x"});
        st.doAll("1+x", new String[]{"x"});
        st.doAll("5*x", new String[]{"x"});
        st.doAll("5/x", new String[]{"x"});
        st.doAll("x^2", new String[]{"x"});
        st.doAll("x*x", new String[]{"x"});
        st.doAll("1+x+x^2", new String[]{"x"});
        st.doAll("1+x+x^2+x^3", new String[]{"x"});
        st.doAll("1+x+x^2+x^3+x^4", new String[]{"x"});
        st.doAll("1+x+x^2+x^3+x^4+x^5", new String[]{"x"});
        st.doAll("1+x(1+x(1+x(1+x(1+x))))", new String[]{"x"});
        //doHorner();
        st.doAll("1*2*3+4*5*6+7*8*9", new String[]{});
        st.doAll("x1*x2*x3+x4*x5*x6+x7*x8*x9", new String[]{"x1","x2","x3","x4","x5","x6","x7","x8","x9"});
        
        // Big decimal components do not include trig functions
        st.doAll("cos(x)", new String[]{"x"});
        doCos(st);
        st.doAll("cos(x)^2+sin(x)^2", new String[]{"x"});
        st.doAll(new String[]{"c=cos(x)","s=sin(x)","c*c+s*s"}, new String[]{"x"});
        
        st.doAll("if(x-0.5, 1, 0)", new String[]{"x"});
        st.doAll(new String[]{"y=x*x", "z=y*y", "w=z*z"}, new String[]{"x"});

        // Expression like Boys surface
        st.doAll(new String[] {"w=(2 x -1.5 x^2 + 0.5 x^3)",
        		"rt5 = 2.2360679774997896964091736687313",
        		"denom = w^6 + rt5 w^3 - 1",
        		"g3 = (( 1 + w^6)/ denom ) - 0.5",
        		"g2 = -3/2 *(w*(1 + w^4))/denom",
        		"g1 = -3/2 *(w*(1 - w^4))/denom",
        		"d = g1^2 + g2^2 + g3^2",
        		"g1/d"},new String[]{"x"});

        // The same but breaking down parts using more variables
        st.doAll(new String[] {"w=(2 x -1.5 x^2 + 0.5 x^3)",
        		"w2=w*w","w3=w2*w","w4=w2*w2","w6=w4*w2","rt5 = 2.2360679774997896964091736687313",
        		"denom = w6 + rt5 w3 - 1",
        		"g3 = (( 1 + w6)/ denom ) - 0.5",
        		"g2 = -3/2 *(w*(1 + w4))/denom",
        		"g1 = -3/2 *(w*(1 - w4))/denom",
        		"d = g1^2 + g2^2 + g3^2",
        		"g1/d"},new String[]{"x"});

        String expression = lnExpression("x", st.nDeriv);
        st.doAll(expression, new String[]{"x"});
        //doLn();

        String expression2 = hornerExpression("x", st.nDeriv);
        st.doAll(expression2, new String[]{"x"});
        //		doLnHorner();
    }
}
