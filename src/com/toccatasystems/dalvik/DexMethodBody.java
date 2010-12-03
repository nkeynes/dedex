package com.toccatasystems.dalvik;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class DexMethodBody {
	public static class ExceptionBlock {
		int startInst;
		int instCount;
		int handleInst;
		String type;
		
		public ExceptionBlock(int startInst, int instCount, int handleInst, String type ) {
			this.startInst = startInst;
			this.instCount = instCount;
			this.handleInst = handleInst;
			this.type = type;
		}
	}

	private DexMethod parent;
	private int numRegisters;
	private int inArgWords;
	private int outArgWords;
	private short []code;
	private DexDebug debug;
	private ExceptionBlock []handlers;
	Map<Integer, DexBasicBlock> blocks;
	
	
	public DexMethodBody( int numRegisters, int inArgWords, int outArgWords,
			short[]code, DexDebug debug, ExceptionBlock[]handlers ) {
		this.numRegisters = numRegisters;
		this.inArgWords = inArgWords;
		this.outArgWords = outArgWords;
		this.code = code;
		this.debug = debug;
		this.handlers = handlers;
		computeCFG();
	}
	
	protected void setParent( DexMethod parent ) { this.parent = parent; }
	
	public DexMethod getParent() { return parent; }
	public DexFile getFile() {
		if( parent == null ) {
			return null;
		} else {
			return parent.getFile();
		}
	}
	
	public int getNumRegisters() { return numRegisters; }
	public int getInArgWords() { return inArgWords; }
	public int getOutArgWords() { return outArgWords; }
	public DexDebug getDebugInfo() { return debug; }
	
	public short []getCode() { return code; }
	public short getWord( int idx ) { return code[idx]; }
	
	public ExceptionBlock []getExceptionHandlers() { return handlers; }
	
	public DexBasicBlock getEntryBlock() {
		return blocks.get(0);
	}
	
	public DexBasicBlock getBlockForPC( int pc ) {
		return blocks.get(pc);
	}

	public Iterator<DexBasicBlock> iterator() {
		return blocks.values().iterator();
	}
	
	
	public void disassemble( PrintStream out ) {
		disassemble(out, false);
	}
	
	public void disassemble( PrintStream out, boolean verbose ) {
		Formatter formatter = new Formatter(out);
		out.println( "        Locals: " + getNumRegisters() );
		for( Iterator<DexBasicBlock> bbit = iterator(); bbit.hasNext(); ) {
			DexBasicBlock bb = bbit.next();
			out.print("    " + bb.getName() + ":");
			if( bb.getNumPredecessors() != 0 ) {
				out.print( "    ; preds: " );
				int count = 0;
				for( Iterator<DexBasicBlock> pit = bb.predIterator(); pit.hasNext(); ) {
					if( count != 0 ) {
						out.print( ", " );
					}
					out.print( pit.next().getName() );
					count++;
				}
			}
			out.println();
			for( Iterator<DexInstruction> ii = bb.iterator(); ii.hasNext(); ) {
				DexInstruction inst = ii.next();
				if( verbose ) {
					int pc = inst.getPC();
					formatter.format("        %04X: ", pc);
					/* Print the raw data */
					for( int j=0; j<5; j++ ) {
						if( j < inst.size() ) {
							formatter.format("%04X ", inst.getUShort(j));
						} else {
							out.print( "     " );
						}
					}
				} else {
					out.print( "        " );
				}
				out.println( inst.disassemble() );
				out.print(inst.formatTable("            "));
			}
		}
	}
	
	
	private void computeCFG() {
		/* 1. Build a set of branch targets and associated basic blocks */
		blocks = new TreeMap<Integer, DexBasicBlock>();
		List<Integer> worklist = new LinkedList<Integer>();
		Set<Integer> tryEndInsts = new TreeSet<Integer>();
		worklist.add(new Integer(0));
		
		if( handlers != null ) {
			for( int i=0; i<handlers.length; i++ ) {
				worklist.add(handlers[i].startInst);
				worklist.add(handlers[i].handleInst);
				/* The end of the try block is handled specially - it may not
				 * actually be a real instruction, so just record them and check
				 * during BB construction if we need to split a block purely for
				 * end-of-exception purposes.
				 */
				tryEndInsts.add( handlers[i].startInst + handlers[i].instCount );
			}
		}
		
		do {
			Integer item = worklist.get(0);
			worklist.remove(0);
			if( blocks.containsKey(item) )
				continue;
			blocks.put(item, new DexBasicBlock());
			
			int target = item.intValue();
			while(target < code.length) {
				DexInstruction ins = new DexInstruction(this, target);
				if( ins.isUncondBranch() ) {
					worklist.add(ins.getBranchTarget());
					break;
				} else if( ins.isCondBranch() ) {
					worklist.add(ins.getBranchTarget());
					worklist.add(target + ins.size());
					break;
				} else if( ins.isSwitch() ) {
					int targets[] = ins.getSwitchTargets();
					for( int i=0; i<targets.length; i++ )
						worklist.add( targets[i] );
					worklist.add(target + ins.size());
					break;
				} else if( ins.isReturn() || ins.isThrow() ) {
					break;
				} else {
					target += ins.size();
				}
			}
		} while( !worklist.isEmpty() );
		
		/* 2. Populate and link up the blocks */
		List<DexBasicBlock> added = new ArrayList<DexBasicBlock>();
		int bbcount=0;
		Iterator<Map.Entry<Integer, DexBasicBlock>> it = blocks.entrySet().iterator();
		Map.Entry<Integer, DexBasicBlock> ent = it.next();
		int pc = ent.getKey();
		DexBasicBlock bb = ent.getValue();
		bb.setName("entry");
		while( bb != null ) {
			int nextpc;
			DexBasicBlock nextbb;
			boolean fallthrough;
			if( it.hasNext() ) {
				ent = it.next();
				nextpc = ent.getKey();
				nextbb = ent.getValue();
				nextbb.setName( "bb" + bbcount );
				fallthrough = true;
			} else {
				nextpc = code.length;
				nextbb = null;
				fallthrough = false;
			}
			
			int startpc = pc;
			/* Read instructions up to the end of the block */
			while( pc < nextpc ) {
				if( pc != startpc && tryEndInsts.contains(pc) ) {
					DexBasicBlock split = new DexBasicBlock("bb" + bbcount);
					bb.addSuccessor(split);
					bb = split;
					added.add(split);
					bbcount++;
					nextbb.setName( "bb" + bbcount );
				}
				DexInstruction ins = new DexInstruction(this, pc);
				bb.add(ins);
				if( ins.isUncondBranch() ) {
					DexBasicBlock succ = blocks.get(ins.getBranchTarget());
					bb.addSuccessor(succ);
					fallthrough = false;
					break;
				} else if( ins.isCondBranch() ) {
					DexBasicBlock succ = blocks.get(ins.getBranchTarget());
					bb.addSuccessor(succ);
					break;
				} else if( ins.isSwitch() ) {
					int[] targets = ins.getSwitchTargets();
					for( int i=0; i<targets.length; i++ ) {
						DexBasicBlock succ = blocks.get(targets[i]);
						bb.addSuccessor(succ);
					}
					break;
				}
				pc += ins.size();
			}
			if( fallthrough ) {
				bb.addSuccessor(nextbb);
			}
		
			pc = nextpc;
			bb = nextbb;
			bbcount++;
		}
		
		/* Add any extra BBs at the end to avoid modifying the map while we're
		 * traversing it.
		 */
		for( Iterator<DexBasicBlock> bbit = added.iterator(); bbit.hasNext(); ) {
			bb = bbit.next();
			blocks.put(bb.getPC(), bb);
		}
	}
}
