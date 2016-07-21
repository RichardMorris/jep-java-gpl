/* @author rich
 * Created on 04-May-2004
 */
package org.lsmp.djep.rpe;

/** A list of commands 
 * @see RpEval
 * */
public final class RpCommandList {
	
	/** Incremental size for list of commands **/
	private static final int STACK_INC=10;
	/** List of commands **/
	RpCommand commands[] = new RpCommand[STACK_INC];
	/** Current position in the command Stack. **/
	private short commandPos;
	/** Package private constructor */
	RpCommandList() {}
	/** Adds a command to the list */
	final void addCommand(short command,short aux)
	{
		if(commandPos == commands.length)
		{
			RpCommand newCommands[] = new RpCommand[commands.length+STACK_INC];
			System.arraycopy(commands,0,newCommands,0,commands.length);
			commands = newCommands;
		}
		commands[commandPos]=new RpCommand(command,aux);
		++commandPos;
//		++maxCommands;
	}
	final void addCommand(short command)
	{
		if(commandPos == commands.length)
		{
			RpCommand newCommands[] = new RpCommand[commands.length+STACK_INC];
			System.arraycopy(commands,0,newCommands,0,commands.length);
			commands = newCommands;
		}
		commands[commandPos]=new RpCommand(command);
		++commandPos;
//		++maxCommands;
	}

	public int getNumCommands() { return commandPos;}
	public RpCommand getCommand(int i) { return commands[i]; }

	/**
	 * Basic toString method.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<commandPos;++i) {
			sb.append(commands[i].toString());
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Enhanced RpCommand to String conversion.
	 * Used when rpe instance is available, prints the values of the constants, variables and functions.
	 * 
	 * @param rpe an RpEval instance to use 
	 * @return String representation, one command per line
	 */
	public String toString(RpEval rpe) {
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<commandPos;++i) {
			sb.append(commands[i].toString(rpe));
			sb.append("\n");
		}
		return sb.toString();
	}

}
