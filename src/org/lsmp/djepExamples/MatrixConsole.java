/* @author rich
 * Created on 21-Mar-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djepExamples;

import org.nfunk.jep.Node;
import org.lsmp.djep.matrixJep.*;
/**
 * @author Rich Morris
 * Created on 21-Mar-2005
 */
public class MatrixConsole extends DJepConsole
{

	public static void main(String[] args)
	{
		Console c = new MatrixConsole();
		c.run(args);
	}
	
	public String getPrompt()
	{
		return "MatrixJep > ";
	}

	public void initialise()
	{
		j = new MatrixJep();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		j.setAllowUndeclared(true);
		j.setImplicitMul(true);
		j.setAllowAssignment(true);
		((MatrixJep) j).addStandardDiffRules();
	}

	public void printHelp()
	{
		super.printHelp();
		println("Dot product: [1,2,3].[4,5,6]");
		println("Cross product: [1,2,3]^[4,5,6]");
		println("Matrix Multiplication: [[1,2],[3,4]]*[[1,2],[3,4]]");
	}

	public void printIntroText()
	{
		println("MatrixJep: advanced vector and matrix handeling");
		super.printStdHelp();
	}

	public void processEquation(Node node) throws Exception
	{
		MatrixJep mj = (MatrixJep) j;
		Node matEqn = mj.preprocess(node);
		mj.println(matEqn);
		Object val = mj.evaluate(matEqn);
		String s = mj.getPrintVisitor().formatValue(val);
		println("Value:\t\t"+s);
	}
}
