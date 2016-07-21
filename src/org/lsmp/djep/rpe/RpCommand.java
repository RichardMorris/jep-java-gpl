package org.lsmp.djep.rpe;

/** Data type for the command string 
 * @see RpEval
 **/
public final class RpCommand {
	short command;
	short aux1; 
	private RpCommand() {};
	RpCommand(short command){
		this.command = command; this.aux1 = -1;
	}
	RpCommand(short command,short aux){
		this.command = command; this.aux1 = aux;
	}
	public String toString() {
		return RpEval.staticToString(this);
	}
	
	/**
	 * Enhanced RpCommand to String conversion.
	 * Used when rpe instance is available, prints the values of the constants, variables and functions.
	 * 
	 * @param rpe an RpEval instance to use 
	 * @return String representation
	 */

	public String toString(RpEval rpe) {
		return rpe.toString(this);
	}
	/**
	 * Returns the type of an individual command. The return value will be one of the constants defined in RpEval. 
	 * These include RpEval.CONST - constants, RpEval.VAR - variables, RpEval.ASSIGN assignments x=..., RpEval.FUN functions. 
	 * Other indices correspond to unary and binary operators,  RpEval.ADD.
	 * @return an integer representing the type
	 */
	public int getType() { return command; }
	public int getRef() { return aux1; }
}
