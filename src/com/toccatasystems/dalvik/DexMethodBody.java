package com.toccatasystems.dalvik;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class DexMethodBody {
	
	/**
	 * Instruction iterator (note this assumes that the method has at least one
	 * basic block, and that basic blocks cannot be empty).
	 * @author nkeynes
	 *
	 */
	private class InstIterator implements Iterator<DexInstruction> {
		Iterator<DexBasicBlock> bbit;
		Iterator<DexInstruction> ii;
		
		protected InstIterator() {
			bbit = blocks.values().iterator();
			ii = bbit.next().iterator();
		}

		public boolean hasNext() {
			return ii.hasNext() || bbit.hasNext();
		}

		public DexInstruction next() {
			if( !ii.hasNext() ) {
				ii = bbit.next().iterator();
			}
			return ii.next();
		}

		public void remove() {
			ii.remove();
		}
	}
	
	
	private DexMethod parent;
	private int numRegisters;
	private int inArgWords;
	private int outArgWords;
	private short []code;
	private DexDebug debug;
	private List<DexTryCatch> handlers;
	private Map<Integer, DexBasicBlock> blocks;
	private List<DexBasicBlock> exitBlocks;
	
	
	public DexMethodBody( int numRegisters, int inArgWords, int outArgWords,
			short[]code, DexDebug debug, List<DexTryCatch> handlers ) {
		this.numRegisters = numRegisters;
		this.inArgWords = inArgWords;
		this.outArgWords = outArgWords;
		this.code = code;
		this.debug = debug;
		this.handlers = handlers;
		this.exitBlocks = new ArrayList<DexBasicBlock>();
		if( handlers == null ) {
			this.handlers = new ArrayList<DexTryCatch>();
		} else {
			for( Iterator<DexTryCatch> it = handlers.iterator(); it.hasNext(); ) {
				it.next().setParent(this);
			}
		}

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
	
	public List<DexTryCatch> getExceptionHandlers() { return handlers; }
	
	public Iterator<DexTryCatch> handlerIterator() {
		return handlers.iterator();
	}
		
	public DexBasicBlock getEntryBlock() {
		return blocks.get(0);
	}
	
	public List<DexBasicBlock> getExitBlocks() {
		return exitBlocks;
	}
	
	public Set<DexBasicBlock> getHandlerBlocks() {
		Set<DexBasicBlock> handlerBlocks = new TreeSet<DexBasicBlock>();
		for( Iterator<DexTryCatch> it = handlers.iterator(); it.hasNext(); ) {
			DexTryCatch ent = it.next();
			handlerBlocks.add( ent.getHandlerBlock() );
		}
		return handlerBlocks;
	}
	
	public DexBasicBlock getBlockForPC( int pc ) {
		return blocks.get(pc);
	}

	public Iterator<DexBasicBlock> iterator() {
		return blocks.values().iterator();
	}
	
	public Iterator<DexInstruction> instIterator() {
		return new InstIterator();
	}
	
	public Iterator<DexBasicBlock> iterator( int pc ) {
		if( pc == 0 ) {
			return blocks.values().iterator();
		} else {
			int count = 0;
			Iterator<DexBasicBlock> it = blocks.values().iterator();
			while( it.hasNext() ) {
				DexBasicBlock bb = it.next();
				if( bb.getEndPC() == pc ) {
					break;
				}
				if( bb.getPC() <= pc && bb.getEndPC() > pc ) {
					/* Went too far, start again with count */
					it = blocks.values().iterator();
					for( int i=0; i<count; i++ ) {
						it.next();
					}
					break;
				}
				count++;
			}
			return it;
		}
	}
	
	public Iterator<DexBasicBlock> iterator( DexBasicBlock bb ) {
		return iterator(bb.getPC());
	}
	
	public DexBasicBlock getPrevious( DexBasicBlock before ) {
		DexBasicBlock last = null;
		for( Iterator<DexBasicBlock> it = iterator(); it.hasNext(); ) {
			DexBasicBlock bb = it.next();
			if( bb == before ) {
				return last;
			} else {
				last = bb;
			}
		}
		return null;	
	}
	
	public DexBasicBlock getNext( DexBasicBlock after ) {
		for( Iterator<DexBasicBlock> it = blocks.values().iterator(); it.hasNext(); ) {
			if( it.next() == after ) {
				if( it.hasNext() )
					return it.next();
				else
					return null;
			}
			
		}
		return null;
	}
	

	public void disassemble( PrintStream out ) {
		disassemble(out, false);
	}
	
	public void disassemble( PrintStream out, boolean verbose ) {
		out.println( "        Locals: " + getNumRegisters() );
		for( Iterator<DexBasicBlock> bbit = iterator(); bbit.hasNext(); ) {
			DexBasicBlock bb = bbit.next();
			bb.disassemble(out, verbose);
		}
		
		if( !handlers.isEmpty() ) {
			out.println( "Exceptions:" );
			for( Iterator<DexTryCatch> ebit = handlers.iterator(); ebit.hasNext(); ) {
				DexTryCatch eb = ebit.next();
				out.println("    " + eb.getStartBlock().getName() + " .. " + eb.getEndBlock().getName() + ": " +
						(eb.getType() == null ? "*" : DexType.format(eb.getType())) + " => " + eb.getHandlerBlock().getName() );
			}
		}
	}
	
	
	private void computeCFG() {
		/* 1. Build a set of branch targets and associated basic blocks */
		blocks = new TreeMap<Integer, DexBasicBlock>();
		List<Integer> worklist = new LinkedList<Integer>();
		Set<Integer> tryEndInsts = new TreeSet<Integer>();
		worklist.add(new Integer(0));
		
		for( Iterator<DexTryCatch> ebit = handlers.iterator(); ebit.hasNext(); ) {
			DexTryCatch handler = ebit.next();
			worklist.add(handler.getStartPC());
			worklist.add(handler.getHandlerPC());
			/* The end of the try block is handled specially - it may not
			 * actually be a real instruction, so just record them and check
			 * during BB construction if we need to split a block purely for
			 * end-of-exception purposes.
			 */
			tryEndInsts.add( handler.getEndPC() );
		}
		
		do {
			Integer item = worklist.get(0);
			worklist.remove(0);
			if( blocks.containsKey(item) )
				continue;
			blocks.put(item, new DexBasicBlock(this));
			
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
					DexBasicBlock split = new DexBasicBlock(this, "bb" + bbcount);
					bb.addFallthroughSuccessor(split);
					bb = split;
					added.add(split);
					bbcount++;
					if( nextbb != null )
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
				} else if( ins.isReturn() || ins.isThrow() ) {
					exitBlocks.add(bb);
					fallthrough = false;
					break;
				}
				pc += ins.size();
			}
			if( fallthrough ) {
				bb.addFallthroughSuccessor(nextbb);
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
		
		/* Check exception handlers for end labels that may not actually belong
		 * to any basic block (i.e. they point past the end of code), and create
		 * empty blocks for them.
		 */
		for( Iterator<DexTryCatch> ebit = handlers.iterator(); ebit.hasNext(); ) {
			DexTryCatch handler = ebit.next();
			if( getBlockForPC(handler.getEndPC()) == null ) {
				blocks.put(handler.getEndPC(), new DexBasicBlock(this));
			}
		}
		
		/* Add exception blocks to the final block they're protecting, as well
		 * as any exit blocks - this isn't strictly sound, but it should be 
		 * sufficient for our purposes
		 */
		for( Iterator<DexTryCatch> ebit = handlers.iterator(); ebit.hasNext(); ) {
			DexTryCatch handler = ebit.next();
			DexBasicBlock handlerbb = null;
			for( Iterator<DexBasicBlock> bbit = iterator(); bbit.hasNext(); ) {
				bb = bbit.next();
				if( handler.getEndPC() == bb.getEndPC() ) {
					handlerbb = handler.getHandlerBlock();
					bb.addExceptionSuccessor(handlerbb);
					break;
				}
			}

			if( handlerbb != null ) {
				for( Iterator<DexBasicBlock> exit = exitBlocks.iterator(); exit.hasNext(); ) {
					DexBasicBlock exitbb = exit.next();
					if( exitbb != handlerbb &&
							exitbb.getPC() >= handler.getStartPC() &&
							exitbb.getPC() < handler.getEndPC() ) {
						exitbb.addExceptionSuccessor(handlerbb);
						break;
					}
				}
			}
		}
	}
}
