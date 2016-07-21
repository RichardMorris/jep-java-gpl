/*****************************************************************************

@header@
@date@
@copyright@
@license@

*****************************************************************************/
package org.nfunk.jep.function;

import java.lang.Math;
import java.util.*;
import org.nfunk.jep.*;

/**
 * A PostfixMathCommandI which find the smallest integer below the number
 * ceil(pi) give 3
 * ceil(-i) give -4
 * 
 * @author Richard Morris
 * @see Math.ceil
 */

public class Floor extends PostfixMathCommand
{
	public Floor()
	{
		numberOfParameters = 1;
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(floor(param));//push the result on the inStack
		return;
	}


	public Object floor(Object param)
		throws ParseException
	{
		if (param instanceof Number)
		{
			return new Double(Math.floor(((Number)param).doubleValue()));
		}

		throw new ParseException("Invalid parameter type");
	}

}
