/* @author rich
 * Created on 13-Feb-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.vectorJep.function;

import java.util.Stack;

import org.lsmp.djep.vectorJep.values.*;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * Creates an identity matrix.
 * id(3) -> [[1,0,0],[0,1,0],[0,0,1]]
 * The two argument version creates a m by n matrix with the diagonals filled with zeros
 * id(2,3) -> [[1,0,0],[0,1,]]
 * id(3,2) -> [[1,0],[0,1],[0,0]]
 * @author Rich Morris
 * Created on 13-Feb-2005
 */
public class Id extends PostfixMathCommand
{
	public Id()	{
		this.numberOfParameters = -1;
	}

	public void run(Stack s) throws ParseException
	{
		int m,n;
		if(this.curNumberOfParameters == 1) {
			Object obj = s.pop();
			m = ((Number) obj).intValue();
			n = m;
		}
		else if(this.curNumberOfParameters == 2) {
			Object obj = s.pop();
			n = ((Number) obj).intValue();
			obj = s.pop();
			m = ((Number) obj).intValue();
		}
		else throw new ParseException("Id should have 1 or 2 parameters");
		Matrix mat = (Matrix) Matrix.getInstance(m,n);
		for(int i=0;i<m;++i)
		{
			for(int j=0;j<n;++j) {
				if(i==j)
					mat.setEle(i,j,new Double(1.0));
				else
					mat.setEle(i,j,new Double(0.0));
			}
		}
		s.push(mat);
	}
}
