/* @author rich
 * Created on 14-Apr-2004
 */
package org.lsmp.djep.rpe;
import org.nfunk.jep.*;
import org.nfunk.jep.function.PostfixMathCommandI;

import java.util.*;

/**
 * A fast evaluation algorithm for equations over Doubles, does not work with vectors or matricies.
 * This is based around reverse polish notation
 * and is optimized for speed at every opportunity.
 * <p>
 * To use do
 * <pre>
 * JEP j = ...;
 * Node node = ...; 
 * RpEval rpe = new RpEval(j);
 * RpCommandList list = rpe.compile(node);
 * double val = rpe.evaluate(list);
 * System.out.println(val);
 * rpe.cleanUp();
 * </pre>
 * <p>
 * Variable values in the evaluator are stored in a array. The array index of a variable can be found using
 * <pre>
 * Varaible v = j.getVar("x");
 * int ref = rpe.getVarRef(v);
 * </pre>
 * and the value of the variable set using
 * <pre>
 * rpe.setVarValue(ref,0.1234);
 * </pre>
 * Variable values can also be set using the standard <tt>Variable.setValue()</tt> or (slower) <tt>JEP.setVarVal(name,vlaue)</tt> methods.
 * Setting the value of a jep variable will automatically update the corresponding rpe value but there will be a performance hit. 
 * Setting the value of the rpe variable does not change the corresponding jep value.
 * <p> 
 * The compile methods converts the expression represented by node
 * into a string of commands. For example the expression "1+2*3" will
 * be converted into the sequence of commands
 * <pre>
 * Constant no 1 (pushes constant onto stack)
 * Constant no 2
 * Constant no 3
 * Multiply scalers (multiplies last two entries on stack)
 * Add scalers (adds last two entries on stack)
 * </pre>
 * The evaluate method executes these methods sequentially
 * using a stack 
 * and returns the last object on the stack. 
 * <p>
 * A few cautionary notes:
 * <ul>
 * <li>It only works over doubles
 * expressions with complex numbers or strings will cause problems.</li>
 * <li>It only works for expressions involving scalers. {@link org.lsmp.djep.mrpe.MRpEval} for a version which works with vectors and matricies.</li>
 * <li>It is safe to use individual RpEval instances in separate threads, 
 * using the same instance in separate threads is like to cause exceptions.
 * The RpCommandList objects created by compile are immutable and are safe to use across threads.
 * The <tt>duplicate()</tt> method creates a copy of the RpEval object which can evaluate the same
 * commandList in separate threads.</li>
 * <p>
 * <b>Implementation notes</b>
 * A lot of things have been done to make it as fast as possible:
 * <ul>
 * <li>Everything is final which maximizes the possibility for in-lining.</li>
 * <li>All object creation happens during compile.</li>
 * <li>All calculations done using double values.</li>
 * <li>Each operator/function is hand coded. To extend functionality you will have to modify the source.</li>
 * </ul>
 *  
 * @author Rich Morris
 * Created on 14-Apr-2004
 */
public final class RpEval implements ParserVisitor {

	private OperatorSet opSet;
	private ScalerStore scalerStore = null;
	/** Contains the constant values **/
	double constVals[] = new double[0];

	/** Temporary holder for command list used during compilation */
	private RpCommandList curCommandList;

	public RpEval(JEP jep) {
		this.opSet = jep.getOperatorSet();
		this.scalerStore = new ScalerStore();
	}

	private RpEval() { /* empty */ }
	
	/** Index for each command */
	public static final short CONST = 0;
	public static final short VAR = 1;

	public static final short ADD = 2;
	public static final short SUB = 3;
	public static final short MUL = 4;
	
	public static final short DIV = 5;
	public static final short MOD = 6;
	public static final short POW = 7;

	public static final short AND = 8;
	public static final short OR  = 9;
	public static final short NOT = 10;

	public static final short LT = 11;
	public static final short LE = 12;
	public static final short GT = 13;
	public static final short GE = 14;
	public static final short NE = 15;
	public static final short EQ = 16;
	
	public static final short LIST = 17;
	public static final short DOT = 18;
	public static final short CROSS = 19;

	public static final short ASSIGN = 20;
	public static final short UMINUS = 21;
	public static final short POWN = 22;
	public static final short RECIP = 23;
	public static final short FUN0 = 24;
	public static final short FUN1 = 25;
	public static final short FUN2 = 26;
	public static final short FUN3 = 27;
	public static final short FUN4 = 28;
	/** Standard functions **/
	
	private static final short SIN = 1;
	private static final short COS = 2;
	private static final short TAN = 3;
	private static final short ASIN = 4;
	private static final short ACOS = 5;
	private static final short ATAN = 6;
	private static final short SINH = 7;
	private static final short COSH = 8;
	private static final short TANH = 9;
	private static final short ASINH = 10;
	private static final short ACOSH = 11;
	private static final short ATANH = 12;
	
	private static final short ABS = 13;
	private static final short EXP = 14;
	private static final short LOG = 15;
	private static final short LN = 16;
	private static final short SQRT = 17;
	
	private static final short SEC = 18;
	private static final short COSEC = 19;
	private static final short COT = 20;
	
	// 2 argument functions
	private static final short ATAN2 = 21;

	// 3 argument functions
	private static final short IF = 22;
	
	// Custom functions
	private static final short CUSTOM = 23;

	// Number of custom functions
	private static short nCustom = 0;
	
	/** Hashtable for function name lookup **/
	
	private static final Hashtable functionHash = new Hashtable();
	{
		functionHash.put("sin",new Short(SIN));
		functionHash.put("cos",new Short(COS));
		functionHash.put("tan",new Short(TAN));
		functionHash.put("asin",new Short(ASIN));
		functionHash.put("acos",new Short(ACOS));
		functionHash.put("atan",new Short(ATAN));
		functionHash.put("sinh",new Short(SINH));
		functionHash.put("cosh",new Short(COSH));
		functionHash.put("tanh",new Short(TANH));
		functionHash.put("asinh",new Short(ASINH));
		functionHash.put("acosh",new Short(ACOSH));
		functionHash.put("atanh",new Short(ATANH));

		functionHash.put("abs",new Short(ABS));
		functionHash.put("exp",new Short(EXP));
		functionHash.put("log",new Short(LOG));
		functionHash.put("ln",new Short(LN));
		functionHash.put("sqrt",new Short(SQRT));

		functionHash.put("sec",new Short(SEC));
		functionHash.put("cosec",new Short(COSEC));
		functionHash.put("cot",new Short(COT));
		
		functionHash.put("atan2",new Short(ATAN2));
		functionHash.put("if",new Short(IF));
	}
	static PostfixMathCommandI[] customFunctionCommands=new PostfixMathCommandI[0];
	
	static synchronized Short getUserFunction(String name,PostfixMathCommandI pfmc) {
		Short val = (Short) functionHash.get(name);
		if(val!=null) return val;
		
		val = new Short((short) (CUSTOM + nCustom));
		functionHash.put(name,val);
		PostfixMathCommandI[] oldCustom = customFunctionCommands;
		customFunctionCommands = new PostfixMathCommandI[nCustom+1];
		for(int k=0;k<oldCustom.length;++k)
			customFunctionCommands[k] = oldCustom[k];
		customFunctionCommands[nCustom] = pfmc;
		++nCustom;
		//throw new ParseException("RpeEval: Sorry unsupported operator/function: "+ node.getName());
		return val;
	}
	/**
	 * Base class for storage for each type of data.
	 * Each subclass should define
	 * <pre>
	 * private double stack[];
	 * private double vars[]= new double[0];
	 * </pre>
	 * and the stack is the current data used for calculations.
	 * Data for Variables is stored in vars and references to the Variables
	 * in varRefs. 
	 */
	abstract static class ObjStore implements Observer {
		/** Contains references to Variables of this type */
		Hashtable varRefs = new Hashtable();
		/** The stack pointer */
		int sp=0;
		/** Maximum size of stack */
		int stackMax=0;
		final void incStack()	{sp++; if(sp > stackMax) stackMax = sp;	}
		final void decStack()	throws ParseException 	{
			--sp; 
			if(sp <0 ) throw new ParseException("RPEval: stack error");
		}
		/** call this to reset pointers as first step in evaluation */
		final void reset() { sp = 0; }
		/** Add a reference to this variable. 
		 * @return the index of variable in table
		 */
		final int addVar(Variable var){
			Object index = varRefs.get(var);
			if(index==null)
			{
				int size = varRefs.size();
				expandVarArray(size+1);
				varRefs.put(var,new Integer(size));
				copyFromVar(var,size);
				var.addObserver(this);
				return size;
			}
			return ((Integer) index).intValue();
		}
		final public void update(Observable obs, Object arg1) 
		{
			Variable var = (Variable) obs;
			Object index = varRefs.get(var);
			copyFromVar(var,((Integer) index).intValue());
		}
		/** allocates space needed */
		abstract void alloc();
		
		final void cleanUp()
		{
			for(Enumeration e=varRefs.keys();e.hasMoreElements();)
			{
				Variable var = (Variable) e.nextElement();
				var.deleteObserver(this);
			}
			varRefs.clear();
		}
		/** Copy variable values into into private storage. 
		 * 
		 * @param var The variable
		 * @param i index of element in array
		 */
		abstract void copyFromVar(Variable var,int i);
		/** expand size of array used to hold variable values. */
		abstract void expandVarArray(int i);
		/** add two objects of same type */
		abstract void add();
		/** subtract two objects of same type */
		abstract void sub();
		/** multiply by a scaler either of left or right */
		abstract void mulS();
		/** assign a variable to stack value
		 * @param i index of variable */
		abstract void assign(int i);
		Variable getVariable(int ref)
		{
			for(Enumeration en=varRefs.keys();en.hasMoreElements();)
			{
				Variable var = (Variable) en.nextElement();
				Integer index = (Integer) varRefs.get(var);
				if(index.intValue()==ref) return var;
			}
			return null;
		}
	}

	static final class ScalerStore extends ObjStore {
		double stack[]=new double[0];
		double vars[]= new double[0];
		final void alloc() { 
			stack = new double[stackMax];
			}
		final void expandVarArray(int size)
		{
			double newvars[] = new double[size];
			System.arraycopy(vars,0,newvars,0,vars.length);
			vars = newvars;
		}
			
		final void copyFromVar(Variable var,int i){
			if(var.hasValidValue())
			{
				Double val = (Double) var.getValue();
				vars[i]=val.doubleValue();
			}
		}
		final void add(){
			double r = stack[--sp];
			stack[sp-1] += r;
		}
		final void sub(){
			double r = stack[--sp];
			stack[sp-1] -= r;
		}
		final void uminus(){
			double r = stack[--sp];
				stack[sp++] = -r;
		}
		final void recroprical(){
			double r = stack[--sp];
				stack[sp++] = 1/r;
		}
		final void mulS(){
			double r = stack[--sp];
			stack[sp-1] *= r;
		} 
		final void div(){
			double r = stack[--sp];
			stack[sp-1] /= r;
		} 
		final void mod(){
			double r = stack[--sp];
			stack[sp-1] %= r;
		} 
		final void pow(){
			double r = stack[--sp];
			short s = (short) r;
			if(r==s) {
				if(r>=0) { powN(s); return; }
				else { powN((short) -s); recroprical(); return; }
			}
			double l = stack[--sp];
			stack[sp++] = Math.pow(l,r);
		} 
		
		/**
		 * Code adapted form http://mindprod.com/jgloss/power.html
		 * @author Patricia Shanahan pats@acm.org
		 * almost identical to the method Knuth gives on page 462 of The Art of Computer Programming Volume 2 Seminumerical Algorithms.
		 */
		
		final void powN(short n){
			double r = stack[--sp];
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
			stack[sp++] = r;
		} 
		
		final void assign(int i) {
			vars[i] = stack[--sp]; ++sp;
		} 
		final void and(){
			double r = stack[--sp];
			double l = stack[--sp];
			if((l != 0.0) && (r != 0.0))
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void or(){
			double r = stack[--sp];
			double l = stack[--sp];
			if((l != 0.0) || (r != 0.0))
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void not(){
			double r = stack[--sp];
			if(r == 0.0)
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void lt(){
			double r = stack[--sp];
			double l = stack[--sp];
			if(l < r)
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void gt(){
			double r = stack[--sp];
			double l = stack[--sp];
			if(l > r)
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void le(){
			double r = stack[--sp];
			double l = stack[--sp];
			if(l <= r)
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void ge(){
			double r = stack[--sp];
			double l = stack[--sp];
			if(l >= r)
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void eq(){
			double r = stack[--sp];
			double l = stack[--sp];
			if(l == r)
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void neq(){
			double r = stack[--sp];
			double l = stack[--sp];
			if(l != r)
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		
		protected ScalerStore duplicate() {
			ScalerStore copy = new ScalerStore();
			copy.sp = this.sp;
			copy.stackMax = this.stackMax;
			copy.varRefs = this.varRefs;
			copy.stack = (double []) this.stack.clone();
			copy.vars = (double []) this.vars.clone();
			return copy;
		}

	}
	
	/**
	 * Compile the expressions to produce a set of commands in reverse Polish notation.
	 */
	public final RpCommandList compile(Node node) throws ParseException
	{
		curCommandList = new RpCommandList();
		node.jjtAccept(this,null);
		scalerStore.alloc();
		return curCommandList;
	}
	
	public final Object visit(ASTStart node, Object data) throws ParseException {
		throw new ParseException("RpeEval: Start node encountered");
	}
	public final Object visit(SimpleNode node, Object data) throws ParseException {
		throw new ParseException("RpeEval: Simple node encountered");
	}

	public final Object visit(ASTConstant node, Object data) throws ParseException {
		Object obj = node.getValue();
		double val;
		if(obj instanceof Double)
			val = ((Double) node.getValue()).doubleValue();
		else
			throw new ParseException("RpeEval: only constants of double type allowed");
		
		scalerStore.incStack();
		for(short i=0;i<constVals.length;++i)
		{
			if(val == constVals[i])
			{
				curCommandList.addCommand(CONST,i);
				return null;
			}
		}
		// create a new const
		double newConst[] = new double[constVals.length+1];
		System.arraycopy(constVals,0,newConst,0,constVals.length);
		newConst[constVals.length] = val;
		curCommandList.addCommand(CONST,(short) constVals.length);
		constVals = newConst;
		return null;
	}

	public final Object visit(ASTVarNode node, Object data) throws ParseException 
	{
		Variable var = node.getVar();
		// find appropriate table
		short vRef = (short) scalerStore.addVar(var);
		scalerStore.incStack();
		curCommandList.addCommand(VAR,vRef);
		return null;
	}

	public final Object visit(ASTFunNode node, Object data) throws ParseException 
	{
		int nChild = node.jjtGetNumChildren();

		if(node.isOperator() && node.getOperator() == opSet.getAssign()) { /* empty */ }
		else if(node.isOperator() && node.getOperator() == opSet.getPower()) { /* empty */ }
		else
			node.childrenAccept(this,null);

		if(node.isOperator())
		{
			Operator op = node.getOperator();

			if(op == opSet.getAdd())
			{
				curCommandList.addCommand(ADD);
				scalerStore.decStack();
				return null;
			}
			else if(op == opSet.getSubtract())
			{
				curCommandList.addCommand(SUB);
				scalerStore.decStack();
				return null;
			}
			else if(op == opSet.getUMinus())
			{
				curCommandList.addCommand(UMINUS);
				return null;
			}
			else if(op == opSet.getMultiply())
			{
				scalerStore.decStack();
				curCommandList.addCommand(MUL);
				return null;
			}
			else if(op == opSet.getAssign())
			{
				Node rightnode = node.jjtGetChild(1);
				rightnode.jjtAccept(this,null);
				Variable var = ((ASTVarNode)node.jjtGetChild(0)).getVar();
				short vRef = (short) scalerStore.addVar(var);
				scalerStore.decStack();
				curCommandList.addCommand(ASSIGN,vRef);
				return null;
			}
			else if(op == opSet.getEQ())
			{
				scalerStore.decStack();
				curCommandList.addCommand(EQ); return null;
			}
			else if(op == opSet.getNE())
			{
				scalerStore.decStack();
				curCommandList.addCommand(NE); return null;
			}
			else if(op == opSet.getLT())
			{
				scalerStore.decStack();
				curCommandList.addCommand(LT); return null;
			}
			else if(op == opSet.getGT())
			{
				scalerStore.decStack();
				curCommandList.addCommand(GT); return null;
			}
			else if(op == opSet.getLE())
			{
				scalerStore.decStack();
				curCommandList.addCommand(LE); return null;
			}
			else if(op == opSet.getGE())
			{
				scalerStore.decStack();
				curCommandList.addCommand(GE); return null;
			}
			else if(op == opSet.getAnd())
			{
				scalerStore.decStack();
				curCommandList.addCommand(AND); return null;
			}
			else if(op == opSet.getOr())
			{
				scalerStore.decStack();
				curCommandList.addCommand(OR); return null;
			}
			else if(op == opSet.getNot())
			{
				//scalerStore.decStack();
				curCommandList.addCommand(NOT); return null;
			}
			else if(op == opSet.getDivide())
			{
				scalerStore.decStack();
				curCommandList.addCommand(DIV); return null;
			}
			else if(op == opSet.getMod())
			{
				scalerStore.decStack();
				curCommandList.addCommand(MOD); return null;
			}
			else if(op == opSet.getPower())
			{
				Node lhs = node.jjtGetChild(0);
				Node rhs = node.jjtGetChild(1);
				lhs.jjtAccept(this,null);	
				if(rhs instanceof ASTConstant) {
					Object val = ((ASTConstant) rhs).getValue();
					if(val instanceof Number) {
						double dval = ((Number) val).doubleValue();
						short sval = ((Number) val).shortValue();
						if(dval>= 0 && dval == sval)
						{
							curCommandList.addCommand(POWN,sval); 
							return null;
						}
						else if(dval == sval)
						{
							curCommandList.addCommand(POWN,(short) (-sval));
							curCommandList.addCommand(RECIP);
							return null;
						}
					}
				}
				rhs.jjtAccept(this,null);
				scalerStore.decStack();
				curCommandList.addCommand(POW); 
				return null;
			}
			throw new ParseException("RpeEval: Sorry unsupported operator/function: "+ node.getName());
		}
		// other functions
		
		Short val = getUserFunction(node.getName(),node.getPFMC());
		curCommandList.addCommand((short)(FUN0+nChild),val.shortValue()); 
		if(nChild==0)
			scalerStore.incStack();
		else if(nChild==1) { /* empty */ }
		else if(nChild==2)
			scalerStore.decStack();
		else 
			for(int k=2;k<nChild;++k)
				scalerStore.decStack();
			
		return null;
		//throw new ParseException("RpeEval: sorry can currently only support single argument functions");
	}

	/***************************** evaluation *****************************/
	
	/** Evaluate the expression.
	 * 
	 * @return the double value of the equation
	 */
	public final double evaluate(RpCommandList comList)
	{
		scalerStore.reset();
		try
		{
		// Now actually process the commands
		int num = comList.getNumCommands();
		for(short commandNum=0;commandNum<num;++commandNum)
		{
			RpCommand command = comList.commands[commandNum];
			short aux1 = command.aux1;
			switch(command.command)
			{
			case CONST:
				scalerStore.stack[scalerStore.sp++]=constVals[aux1]; break;
			case VAR:
				scalerStore.stack[scalerStore.sp++]=scalerStore.vars[aux1]; break;
				
			case ADD: scalerStore.add(); break;
			case SUB: scalerStore.sub(); break; 
			case MUL: scalerStore.mulS(); break;
			case DIV: scalerStore.div(); break;
			case MOD: scalerStore.mod(); break;
			case POW: scalerStore.pow(); break;

			case AND: scalerStore.and(); break;
			case OR:  scalerStore.or(); break;
			case NOT: scalerStore.not(); break;

			case LT: scalerStore.lt(); break;
			case LE: scalerStore.le(); break;
			case GT: scalerStore.gt(); break;
			case GE: scalerStore.ge(); break;
			case NE: scalerStore.neq(); break;
			case EQ: scalerStore.eq(); break;
			case ASSIGN: scalerStore.assign(aux1); break;
			case UMINUS: scalerStore.uminus(); break;
			case POWN: scalerStore.powN(aux1); break;
			case RECIP: scalerStore.recroprical(); break;
			case FUN0: nullaryFunction(aux1); break;
			case FUN1: unitaryFunction(aux1); break;
			case FUN2: binaryFunction(aux1); break;
			case FUN3: trianaryFunction(aux1); break;
			case FUN4: quarteraryFunction(aux1); break;
			default:
				naryFunction(aux1,command.command-FUN0); break;
			}
		}
		}
		catch(ParseException e) {
			return Double.NaN;
		}
		return scalerStore.stack[--scalerStore.sp];
	}

	private final Stack stack = new Stack();
	private static final double LOG10 = Math.log(10.0);

	private final void nullaryFunction(short fun) throws ParseException
	{
		double r;
		int index = fun-CUSTOM;
		PostfixMathCommandI pfmc = customFunctionCommands[index];
		if(pfmc instanceof RealNullaryFunction) {
			r = ((RealNullaryFunction) pfmc).evaluate();
		}
		else if(pfmc instanceof RealNaryFunction) {
			double[] vals = new double[0];
			r = ((RealNaryFunction) pfmc).evaluate(vals);
		}
		else
		{
			pfmc.setCurNumberOfParameters(0);
			pfmc.run(stack);
			r = ((Number) stack.pop()).doubleValue();
		}
		scalerStore.stack[scalerStore.sp++] = r;
	}
	private final void unitaryFunction(short fun) throws ParseException
	{
		double r = scalerStore.stack[--scalerStore.sp];
		switch(fun) {
			case SIN: r = Math.sin(r); break;
			case COS: r = Math.cos(r); break;
			case TAN: r = Math.tan(r); break;

			case ASIN: r = Math.asin(r); break;
			case ACOS: r = Math.acos(r); break;
			case ATAN: r = Math.atan(r); break;

			case SINH: r = (Math.exp(r)-Math.exp(-r))/2; break;
			case COSH: r = (Math.exp(r)+Math.exp(-r))/2; break;
			case TANH: 
				{double ex = Math.exp(r*2);
				 r = (ex-1)/(ex+1); break;
				}

			case ASINH: r = Math.log(r+Math.sqrt(1+r*r)); break;
			case ACOSH: r = Math.log(r+Math.sqrt(r*r-1)); break;
			case ATANH: r = Math.log((1+r)/(1-r))/2.0; break;

			case ABS: r = Math.abs(r); break;
			case EXP: r = Math.exp(r); break;
			case LOG: r = Math.log(r) / LOG10; break;
			case LN:  r = Math.log(r); break;
			case SQRT: r = Math.sqrt(r); break;

			case SEC: r = 1.0/Math.cos(r); break;
			case COSEC:  r = 1.0/Math.sin(r); break;
			case COT: r = 1.0/Math.tan(r); break;
			default:
				int index = fun-CUSTOM;
				PostfixMathCommandI pfmc = customFunctionCommands[index];
				if(pfmc instanceof RealUnaryFunction) {
					r = ((RealUnaryFunction) pfmc).evaluate(r);
				}
				else if(pfmc instanceof RealNaryFunction) {
					double[] vals = new double[]{r};
					r = ((RealNaryFunction) pfmc).evaluate(vals);
				}
				else
				{
					pfmc.setCurNumberOfParameters(1);
					stack.push(new Double(r));
					pfmc.run(stack);
					r = ((Number) stack.pop()).doubleValue();
				}
		}
		scalerStore.stack[scalerStore.sp++] = r;
	}
	
	private final void binaryFunction(short fun) throws ParseException{
		double r = scalerStore.stack[--scalerStore.sp];
		double l = scalerStore.stack[--scalerStore.sp];
		switch(fun) {
		case ATAN2: r = Math.atan2(l,r); break;
		default:
			int index = fun-CUSTOM;
			PostfixMathCommandI pfmc = customFunctionCommands[index];
			if(pfmc instanceof RealBinaryFunction) {
				r = ((RealBinaryFunction) pfmc).evaluate(l,r);
			}
			else if(pfmc instanceof RealNaryFunction) {
				double[] vals = new double[]{l,r};
				r = ((RealNaryFunction) pfmc).evaluate(vals);
			}
			else
			{
				pfmc.setCurNumberOfParameters(2);
				stack.push(new Double(l));
				stack.push(new Double(r));
				pfmc.run(stack);
				r = ((Number) stack.pop()).doubleValue();
			}
		}
		scalerStore.stack[scalerStore.sp++] = r;
	}
	private final void trianaryFunction(short fun) throws ParseException
	{
		double a = scalerStore.stack[--scalerStore.sp];
		double r = scalerStore.stack[--scalerStore.sp];
		double l = scalerStore.stack[--scalerStore.sp];
		switch(fun) {
		case IF: r = (l>0.0?r:a); break;
		default:
			int index = fun-CUSTOM;
			PostfixMathCommandI pfmc = customFunctionCommands[index];
			if(pfmc instanceof RealNaryFunction) {
				double[] args=new double[]{l,r,a};
				r = ((RealNaryFunction) pfmc).evaluate(args);
			}
			else
			{
				pfmc.setCurNumberOfParameters(3);
				stack.push(new Double(l));
				stack.push(new Double(r));
				stack.push(new Double(a));
				pfmc.run(stack);
				r = ((Number) stack.pop()).doubleValue();
			}
		}
		scalerStore.stack[scalerStore.sp++] = r;
		
	}
	private final void quarteraryFunction(short fun) throws ParseException
	{
		double b = scalerStore.stack[--scalerStore.sp];
		double a = scalerStore.stack[--scalerStore.sp];
		double r = scalerStore.stack[--scalerStore.sp];
		double l = scalerStore.stack[--scalerStore.sp];
		switch(fun) {
		case IF: r = (l>0.0?r: (l<0.0?a:b)); break;
		default:
			int index = fun-CUSTOM;
			PostfixMathCommandI pfmc = customFunctionCommands[index];
			if(pfmc instanceof RealNaryFunction) {
				double[] args=new double[]{l,r,a,b};
				r = ((RealNaryFunction) pfmc).evaluate(args);
			}
			else
			{
				pfmc.setCurNumberOfParameters(4);
				stack.push(new Double(l));
				stack.push(new Double(r));
				stack.push(new Double(a));
				stack.push(new Double(b));
				pfmc.run(stack);
				r = ((Number) stack.pop()).doubleValue();
			}
		}
		scalerStore.stack[scalerStore.sp++] = r;
		
	}
	private final void naryFunction(short fun,int nargs) throws ParseException
	{
		int index = fun-CUSTOM;
		PostfixMathCommandI pfmc = customFunctionCommands[index];
		if(pfmc instanceof RealNaryFunction) {
			double[] args=new double[nargs];
			for(int k=nargs-1;k>=0;--k)
				args[k] = scalerStore.stack[--scalerStore.sp];
			double r = ((RealNaryFunction) pfmc).evaluate(args);
			scalerStore.stack[scalerStore.sp++] = r;
		}
		else {
			pfmc.setCurNumberOfParameters(nargs);
			scalerStore.sp -= nargs;
			for(int k=0;k<nargs;++k)
				stack.push(new Double(scalerStore.stack[scalerStore.sp+k]));
			pfmc.run(stack);
			double r = ((Number) stack.pop()).doubleValue();
			scalerStore.stack[scalerStore.sp++] = r;
		}
		
	}

	/**
	 * Removes observers and other cleanup needed when evaluator no longer used.
	 */
	public void cleanUp()
	{
		scalerStore.cleanUp();
	}
	
	/**
	 * Gets the JEP Variable for a give reference number
	 * @param ref reference number for the variable
	 * @return corresponding JEP variable
	 */
	public Variable getVariable(int ref)
	{
		return scalerStore.getVariable(ref);
	}
	/**
	 * Gets the reference number for a given variable
	 * @param var JEP Variable
	 * @return reference number for the variable
	 * @throws ParseException
	 */
	public int getVarRef(Variable var) throws ParseException
	{
		short vRef = (short) scalerStore.addVar(var);
		return vRef;
	}
	
	/**
	 * Sets the value of a variable
	 * @param ref reference number for the variable
	 * @param val the value to set the variable
	 */
	public void setVarValue(int ref, double val)
	{
		scalerStore.vars[ref]=val;
	}
	
	/**
	 * Returns the constant value with a given reference number
	 * @param ref
	 * @return the value of the constant
	 */
	public double getConstantValue(int ref)
	{
		return constVals[ref];
	}

	/**
	 * Returns the name of the function with a given reference number.
	 * @param ref
	 * @return the name of the function
	 */
	public String getFunction(int ref)
	{
			switch(ref) {
			case SIN: return "sin";
			case COS: return "cos";
			case TAN: return "tan";
			case ASIN: return "asin";
			case ACOS: return "acos";
			case ATAN: return "atan";
			case SINH: return "sinh";
			case COSH: return "cosh";
			case TANH: return "tanh";
			case ASINH: return "asinh";
			case ACOSH: return "acosh";
			case ATANH: return "atanh";
			case ABS: return "abs";
			case EXP: return "exp";
			case LOG: return "log";
			case LN: return "ln";
			case SQRT: return "sqrt";
			case SEC: return "sec";
			case COSEC: return "cosec";
			case COT: return "cot";
			case ATAN2: return "atan2";
			case IF: return "if";
			default:
				Enumeration en=functionHash.keys();
				while(en.hasMoreElements()) {
					String s = (String) en.nextElement();
					Short val = (Short) functionHash.get(s);
					if(val.intValue() == ref)
						return s;
				}
			}
			return null;
	}

	/**
	 * Returns a copy of the RpEval object which is safe to use for evaluation in a new thread.
	 * <code>
	 * RpEval rpe1 = new RpEval(jep);
	 * RpCommandList com = rpe1.compile(node);
	 * int varRef = rpe1.getVarRef(jep.getVar("x"));
	 * RpEval rpe2 = rpe1.duplicate();
	 * rpe2.setVarValue(ref,0.5);
	 * double result = rpe2.evaluate(com);
	 * </code>
	 * @return a new instance
	 */
	public RpEval duplicate() {
		
		RpEval copy=null;
		copy = new RpEval();
		copy.opSet = this.opSet;
		copy.constVals = this.constVals;
		copy.scalerStore = this.scalerStore.duplicate();
		return copy;
	}
	
	/** Basic conversion of a command to string representation.
	 * Used when the rpe instance is not available.
	 * @param com an RpCommand to convert
	 * @return string representation
	 */
	static String staticToString(RpCommand com) {
		switch(com.command)
		{
			case RpEval.CONST: return "Constant\tnum "+com.aux1;
			case RpEval.VAR: return "Variable\tnum "+com.aux1;
			case RpEval.ADD: return "ADD";
			case RpEval.SUB: return "SUB";
			case RpEval.MUL: return "MUL";
			case RpEval.DIV: return "DIV";
			case RpEval.MOD: return "MOD";
			case RpEval.POW: return "POW";
			case RpEval.AND: return "AND";
			case RpEval.OR: return "OR";
			case RpEval.NOT: return "NOT";
			case RpEval.LT: return "LT";
			case RpEval.LE: return "LE";
			case RpEval.GT: return "GT";
			case RpEval.GE: return "GE";
			case RpEval.EQ: return "EQ";
			case RpEval.NE: return "NE";
			case RpEval.ASSIGN: return "Assign\t\tnum "+com.aux1;
			case RpEval.UMINUS: return "Unitary minus";
			case RpEval.POWN: return "POWN\t\t"+com.aux1;
			case RpEval.RECIP: return "1/x";
			case RpEval.FUN0: return "Nullary Function\tnum "+com.aux1;
			case RpEval.FUN1: return "Function\tnum "+com.aux1;
			case RpEval.FUN2: return "Binary function\tnum "+com.aux1;
			case RpEval.FUN3: return "Trianary function\tnum "+com.aux1;
			case RpEval.FUN4: return "Nary function\tnum "+com.aux1;
			default: return "Nary function\tnum "+com.aux1;
		}
		//return "WARNING unknown command: "+com.command+" "+com.aux1;
	}

	/**
	 * Enhanced RpCommand to String conversion.
	 * Used when rpe instance is available, prints the values of the constants, variables and functions.
	 * @param com an RpCommand to convert
	 * @return string representation
	 */
	String toString(RpCommand com) {
		switch(com.command)
		{
			case RpEval.CONST: return "Constant\tnum "+com.aux1+"\tval "+getConstantValue(com.getRef());
			case RpEval.VAR: return "Variable\tnum "+com.aux1+"\t"+getVariable(com.aux1).getName();
			case RpEval.ADD: return "ADD";
			case RpEval.SUB: return "SUB";
			case RpEval.MUL: return "MUL";
			case RpEval.DIV: return "DIV";
			case RpEval.MOD: return "MOD";
			case RpEval.POW: return "POW";
			case RpEval.AND: return "AND";
			case RpEval.OR: return "OR";
			case RpEval.NOT: return "NOT";
			case RpEval.LT: return "LT";
			case RpEval.LE: return "LE";
			case RpEval.GT: return "GT";
			case RpEval.GE: return "GE";
			case RpEval.EQ: return "EQ";
			case RpEval.NE: return "NE";
			case RpEval.ASSIGN: return "Assign\t\tnum "+com.aux1+"\t"+getVariable(com.aux1).getName();
			case RpEval.UMINUS: return "Unitary minus";
			case RpEval.POWN: return "POWN\t\t"+com.aux1;
			case RpEval.RECIP: return "1/x";
			case RpEval.FUN0: return "Nullary Function\tnum "+com.aux1+"\t"+getFunction(com.getRef());
			case RpEval.FUN1: return "Function\tnum "+com.aux1+"\t"+getFunction(com.getRef());
			case RpEval.FUN2: return "Binary function\tnum "+com.aux1+"\t"+getFunction(com.getRef());
			case RpEval.FUN3: return "Trianary function\tnum "+com.aux1+"\t"+getFunction(com.getRef());
			case RpEval.FUN4: return "Nary function\tnum "+com.aux1+"\t"+getFunction(com.getRef());
			default: return "Nary function\tnum "+com.aux1+"\t"+getFunction(com.getRef());
		}
		//return "WARNING unknown command: "+com.command+" "+com.aux1;
	}
	
}
