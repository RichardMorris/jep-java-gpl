/*****************************************************************************

@header@
@date@
@copyright@
@license@

*****************************************************************************/

package org.nfunk.jep;

import java.io.*;
import java.util.*;
import org.nfunk.jep.function.*;
import org.nfunk.jep.type.*;

/**
 * The JEP class is the main interface with which the user should
 * interact. It contains all neccessary methods to parse and evaluate
 * expressions.
 * <p>
 * The most important methods are parseExpression(String), for parsing the
 * mathematical expression, and getValue() for obtaining the value of the
 * expression.
 * <p>
 * Visit <a href="http://www.singularsys.com/jep">http://www.singularsys.com/jep</a>
 * for the newest version of JEP, and complete documentation.
 *
 * @author Nathan Funk
 */
public class JEP {

	/** Debug flag for extra command line output */
	private static final boolean debug = false;
	
	/** Traverse option */
	private boolean traverse;
	
	/** Allow undeclared variables option */
	protected boolean allowUndeclared;
	
	/** Allow undeclared variables option */
	protected boolean allowAssignment;
	
	/** Implicit multiplication option */
	protected boolean implicitMul;
	
	/** Symbol Table */
	protected SymbolTable symTab;

	/** Function Table */
	protected FunctionTable funTab;
	
	/** Error List */
	protected Vector errorList;
	
	/** The parser object */
	protected ParserI parser;
	
	/** Node at the top of the parse tree */
	private Node topNode;

	/** Evaluator */
	protected EvaluatorVisitor ev;
	
	/** Number factory */
	protected NumberFactory numberFactory;

	/**
	 * Creates a new JEP instance with the default settings.
	 * <p>
	 * Traverse = false<br>
	 * Allow undeclared variables = false<br>
	 * Implicit multiplication = false<br>
	 * Number Factory = DoubleNumberFactory
	 */
	public JEP() {
		topNode = null;
		traverse = false;
		allowUndeclared = false;
		allowAssignment  = false;
		implicitMul = false;
		numberFactory = new DoubleNumberFactory();
		initSymTab();
		initFunTab();
		errorList = new Vector();
		ev = new EvaluatorVisitor();
		parser = new Parser(new StringReader(""));

		//Ensure errors are reported for the initial expression
		//e.g. No expression entered
		//parseExpression("");
	}

	/**
	 * Creates a new JEP instance with custom settings. If the
	 * numberFactory_in is null, the default number factory is used.
	 * @param traverse_in The traverse option.
	 * @param allowUndeclared_in The "allow undeclared variables" option.
	 * @param implicitMul_in The implicit multiplication option.
	 * @param numberFactory_in The number factory to be used.
	 */
	public JEP(boolean traverse_in,
			   boolean allowUndeclared_in,
			   boolean implicitMul_in,
			   NumberFactory numberFactory_in) {
		topNode = null;
		traverse = traverse_in;
		allowUndeclared = allowUndeclared_in;
		implicitMul = implicitMul_in;
		if (numberFactory_in == null) {
			numberFactory = new DoubleNumberFactory();
		} else {
			numberFactory = numberFactory_in;
		}
		initSymTab();
		initFunTab();
		errorList = new Vector();
		ev = new EvaluatorVisitor();
		parser = new Parser(new StringReader(""));

		//Ensure errors are reported for the initial expression
		//e.g. No expression entered
		parseExpression("");		
	}

	/** This constructor suppresses the construction
	 * of the default componants. Sub classes can call this 
	 * protected constructor and set the individual components
	 * themselves.
	 * @param use_defaults
	 */
	protected JEP(JEP j)
	{
		topNode = null;
		traverse = j.traverse;
		allowUndeclared = j.allowUndeclared;
		allowAssignment  = j.allowAssignment;
		implicitMul = j.implicitMul;
		this.ev =j.ev;
		this.funTab=j.funTab;
		this.numberFactory=j.numberFactory;
		this.parser=j.parser;
		this.symTab=j.symTab;
		this.traverse=j.traverse;
	}

	/**
	 * Creates a new SymbolTable object as symTab.
	 */
	public void initSymTab() {
		//Init SymbolTable
		symTab = new SymbolTable(new VariableFactory());
	}

	/**
	 * Creates a new FunctionTable object as funTab.
	 */
	public void initFunTab() {
		//Init FunctionTable
		funTab = new FunctionTable();
	}

	/**
	 * Adds the standard functions to the parser. If this function is not called
	 * before parsing an expression, functions such as sin() or cos() would
	 * produce an "Unrecognized function..." error.
	 * In most cases, this method should be called immediately after the JEP
	 * object is created.
	 */
	public void addStandardFunctions() {
		//add functions to Function Table
		funTab.put("sin", new Sine());
		funTab.put("cos", new Cosine());
		funTab.put("tan", new Tangent());
		funTab.put("asin", new ArcSine());
		funTab.put("acos", new ArcCosine());
		funTab.put("atan", new ArcTangent());

		funTab.put("sinh", new SineH());
		funTab.put("cosh", new CosineH());
		funTab.put("tanh", new TanH());
		funTab.put("asinh", new ArcSineH());
		funTab.put("acosh", new ArcCosineH());
		funTab.put("atanh", new ArcTanH());

		funTab.put("log", new Logarithm());
		funTab.put("ln", new NaturalLogarithm());
		funTab.put("exp", new Exp());

		funTab.put("sqrt",new SquareRoot());
		funTab.put("angle", new Angle());
		funTab.put("abs", new Abs());
		funTab.put("mod", new Modulus());
		funTab.put("sum", new Sum());

		funTab.put("rand", new org.nfunk.jep.function.Random());
		
		// rjm additions
		funTab.put("if", new org.nfunk.jep.function.If());
	}

	/**
	 * Adds the constants pi and e to the parser. As addStandardFunctions(),
	 * this method should be called immediatly after the JEP object is
	 * created.
	 */
	public void addStandardConstants() {
		//add constants to Symbol Table
		symTab.makeConstant("pi", new Double(Math.PI));
		symTab.makeConstant("e", new Double(Math.E));
	}
	
	/**
	 * Call this function if you want to parse expressions which involve
	 * complex numbers. This method specifies "i" as the imaginary unit
	 * (0,1). Two functions re() and im() are also added for extracting the
	 * real or imaginary components of a complex number respectively.
	 *<p>
	 *RJM addition The functions cmod and arg are added to get the modulus and argument. 
	 */
	public void addComplex() {
		//add constants to Symbol Table
		symTab.makeConstant("i", new Complex(0,1));
		funTab.put("re", new Real());
		funTab.put("im", new Imaginary());
		funTab.put("arg", new Arg());
		funTab.put("cmod", new Abs());
	}

	/**
	 * Adds a new function to the parser. This must be done before parsing
	 * an expression so the parser is aware that the new function may be
	 * contained in the expression.
	 * @param functionName The name of the function
	 * @param function The function object that is used for evaluating the
	 * function
	 */
	public void addFunction(String functionName,
							PostfixMathCommandI function) {
		funTab.put(functionName, function);
	}

	/**
	 * Adds a new variable to the parser, or updates the value of an
	 * existing variable. This must be done before parsing
	 * an expression so the parser is aware that the new variable may be
	 * contained in the expression.
	 * @param name Name of the variable to be added
	 * @param value Initial value or new value for the variable
	 * @return Double object of the variable
	 */
	public Double addVariable(String name, double value) {
		Double object = new Double(value);
		symTab.makeVarIfNeeded(name, object);
		return object;
	}

	/**
	 * Adds a new complex variable to the parser, or updates the value of an
	 * existing variable. This must be done before parsing
	 * an expression so the parser is aware that the new variable may be
	 * contained in the expression.
	 * @param name Name of the variable to be added
	 * @param re Initial real value or new real value for the variable
	 * @param re Initial imaginary value or new imaginary value for the variable
	 * @return Complex object of the variable
	 */
	public Complex addVariable(String name, double re, double im) {
		Complex object = new Complex(re,im);
		symTab.makeVarIfNeeded(name, object);
		return object;
	}
		
	/**
	 * Adds a new variable to the parser as an object, or updates the value of an
	 * existing variable. This must be done before parsing
	 * an expression so the parser is aware that the new variable may be
	 * contained in the expression.
	 * @param name Name of the variable to be added
	 * @param object Initial value or new value for the variable
	 */
	public void addVariable(String name, Object object) {
		symTab.makeVarIfNeeded(name, object);
	}
	
	/**
	 * Removes a variable from the parser. For example after calling
	 * addStandardConstants(), removeVariable("e") might be called to remove
	 * the euler constant from the set of variables.
	 *
	 * @return The value of the variable if it was added earlier. If
	 * the variable is not in the table of variables, <code>null</code> is
	 * returned.
	 */
	public Object removeVariable(String name) {
		return symTab.remove(name);
	}
	/** returns the value of the varible with given name. 
	 * Added RJM Feb 04 */
	public Object getVarValue(String name) {
		return symTab.getVar(name).getValue();
	}
	/** set the value of a variable 
	 * Added RJM Feb 04 */
	public Object setVarValue(String name,Object val) {
		return symTab.setVarValue(name,val);
	}
	/** get the object represeing the variable 
	 * Added RJM Feb 04 */
	public Variable getVar(String name) {
		return symTab.getVar(name);
	}
	
	/**
	 * Removes a function from the parser.
	 *
	 * @return If the function was added earlier, the function class instance
	 * is returned. If the function was not present, <code>null</code>
	 * is returned.
	 */
	public Object removeFunction(String name) {
		return funTab.remove(name);
	}

	/**
	 * Sets the value of the traverse option. setTraverse is useful for
	 * debugging purposes. When traverse is set to true, the parse-tree
	 * will be dumped to the standard ouput device.
	 * <p>
	 * The default value is false.
	 * @param value The boolean traversal option.
	 */
	public void setTraverse(boolean value) {
		traverse = value;
	}

	/**
	 * Sets the value of the implicit multiplication option.
	 * If this option is set to true before parsing, implicit multiplication
	 * will be allowed. That means that an expression such as
	 * <pre>"1 2"</pre> is valid and is interpreted as <pre>"1*2"</pre>.
	 * <p>
	 * The default value is false.
	 * @param value The boolean implicit multiplication option.
	 */
	public void setImplicitMul(boolean value) {
		implicitMul = value;
	}
	
	public boolean getImplicitMul() { return implicitMul; }
	
	/**
	 * Sets the value for the undeclared variables option. If this option
	 * is set to true, expressions containing variables that were not
	 * previously added to JEP will not produce an "Unrecognized Symbol"
	 * error. The new variables will automatically be added while parsing,
	 * and initialized to 0.
	 * <p>
	 * If this option is set to false, variables that were not previously
	 * added to JEP will produce an error while parsing.
	 * <p>
	 * The default value is false.
	 * @param value The boolean option for allowing undeclared variables.
	 */
	public void setAllowUndeclared(boolean value) {
		allowUndeclared = value;
	}

	public boolean getAllowUndeclared() { return allowUndeclared; }
	
	/**
	 * Whether assignment equation <tt>y=x+1</tt> equations are allowed.
	 * Added by RJM Nov 03.
	 */
	public boolean getAllowAssignment() { return allowAssignment; }

	public void setAllowAssignment(boolean value) {
		allowAssignment = value;
	}

	/**
	 * Parses the expression. If there are errors in the expression,
	 * they are added to the <code>errorList</code> member.
	 *
	 * @param expression_in The input expression string
	 */
	public void parseExpression(String expression_in) {
		Reader reader = new StringReader(expression_in);
		
		try {
			// try parsing
			errorList.removeAllElements();
			topNode = parser.parseStream(reader, this);
		} 
		catch (Throwable e) 
		{
			// an exception was thrown, so there is no parse tree
			topNode = null;
			
			// check the type of error
			if (e instanceof ParseException) {
				// the ParseException object contains additional error
				// information
				errorList.addElement(((ParseException)e).getMessage());
				//getErrorInfo());
			} else {
				// if the exception was not a ParseException, it was most
				// likely a syntax error
				if (debug) {
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				errorList.addElement("Syntax error");
			}
		}
		
				
		// If traversing is enabled, print a dump of the tree to
		// standard output
		if (traverse && !hasError()) {
			ParserVisitor v = new ParserDumpVisitor();
			try
			{
				topNode.jjtAccept(v, null);
			}
			catch(ParseException e) 
			{
				errorList.addElement(e.getMessage());
			}
		}
	}

	/**
	 * parses an expression. 
	 * Returns a object of type Node, does not catch errors.
	 * Does not set the topNode variable of the JEP instance.
	 * This method should generally be used with the {@link #evaluate evaluate}
	 * method rather than getValueAsObject.
	 * Added by RJM Oct 03.
	 * @param expression represeded as a string.
	 * @return The top node of an tree representing the parsed expression.
	 * @throws ParseException
	 */
	public Node parse(String expression) throws ParseException
	{
		java.io.StringReader sr = new java.io.StringReader(expression);
		Node node = parser.parseStream(sr,this);
		return node;
	}

	/**
	 * Evaluate an expression. This method evaluates the argument
	 * rather than the topNode of the JEP instance.
	 * It should be used in conjunction with {@link #parse parse}
	 * rather than {@link #parseExpression parseExpression}.
	 * Added by RJM Feb 04.
	 * @param node the top node of the tree representing the expression.
	 * @return The value of the expression
	 * @throws Exception if for some reason the expression could not be evaluated
	 */
	public Object evaluate(Node node) throws Exception
	{
		return ev.getValue(node,new Vector(),this.symTab);
	}

	/**
	 * Evaluates and returns the value of the expression as a double number.
	 *
	 * @return The calculated value of the expression as a double number.
	 * If the type of the value does not implement the Number interface
	 * (e.g. Complex), NaN is returned. If an error occurs during evaluation,
	 * NaN is returned and hasError() will return true.
	 *
	 * @see #getComplexValue()
	 */
	public double getValue() {
		Object value = getValueAsObject();
		
		if (value != null && value instanceof Number) {
			return ((Number)value).doubleValue();
		}
		
		return Double.NaN;
	}


	/**
	 * Evaluates and returns the value of the expression as a complex number.
	 * @return The calculated value of the expression as a complex number if
	 * no errors occur. Returns null otherwise.
	 */
	public Complex getComplexValue() {
		Object value = getValueAsObject();
		
		if (value == null) {
			return null;
		} else if (value instanceof Number) {
			return new Complex(((Number)value).doubleValue(), 0);
		} else if (value instanceof Complex) {
			return (Complex)value;
		} else {
			return null;
		}
	}



	/**
	 * Evaluates and returns the value of the expression as an object.
	 * The EvaluatorVisitor member ev is used to do the evaluation procedure.
	 * This method is useful when the type of the value is unknown, or
	 * not important.
	 * @return The calculated value of the expression if no errors occur.
	 * Returns null otherwise.
	 */
	public Object getValueAsObject() {
		Object result;
		
		if (topNode != null && !hasError()) {
			// evaluate the expression
			try {
				result = ev.getValue(topNode,errorList,symTab);
			} catch (Exception e) {
				if (debug) System.out.println(e);
				errorList.addElement("Error during evaluation");
				return null;
			}
			
			return result;
		} else {
			return null;
		}
	}

	/**
	 * Returns true if an error occured during the most recent
	 * action (parsing or evaluation).
	 * @return Returns <code>true</code> if an error occured during the most
	 * recent action (parsing or evaluation).
	 */
	public boolean hasError() {
		return !errorList.isEmpty();
	}

	/**
	 * Reports information on the errors that occured during the most recent
	 * action.
	 * @return A string containing information on the errors, each separated
	 * by a newline character; null if no error has occured
	 */
	public String getErrorInfo() {
		if (hasError()) {
			String str = "";
			
			// iterate through all errors and add them to the return string
			for (int i=0; i<errorList.size(); i++) {
				str += errorList.elementAt(i) + "\n";
			}
			
			return str;
		} else {
			return null;
		}
	}

	/**
	 * Returns the top node of the expression tree. Because all nodes are
	 * pointed to either directly or indirectly, the entire expression tree
	 * can be accessed through this node. It may be used to manipulate the
	 * expression, and subsequently evaluate it manually.
	 * @return The top node of the expression tree
	 */
	public Node getTopNode() {
		return topNode;
	}

	/**
	 * Returns the symbol table (the list of all variables that the parser
	 * recognises).
	 * @return The symbol table
	 */
	public SymbolTable getSymbolTable() {
		return symTab;
	}

	/**
	 * Returns the function table (the list of all functions that the parser
	 * recognises).
	 * @return The function table
	 */
	public FunctionTable getFunctionTable() {
			return funTab;
	}


	/**
	 * Returns the number factory.
	 */
	public NumberFactory getNumberFactory() {
		return numberFactory;
	}
//------------------------------------------------------------------------
// Old code


/*
	/**
	* Returns the position (vertical) at which the last error occured.
	/
	public int getErrorColumn() {
		if (hasError && parseException != null)
			return parseException.getColumn();
		else
			return 0;
	}

	/**
	* Returns the line in which the last error occured.
	/
	public int getErrorLine() {
		if (hasError && parseException != null)
			return parseException.getLine();
		else
			return 0;
	}
*/

}

