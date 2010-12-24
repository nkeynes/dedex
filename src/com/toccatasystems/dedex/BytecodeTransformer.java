package com.toccatasystems.dedex;

import java.util.Iterator;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.toccatasystems.dalvik.DexBasicBlock;
import com.toccatasystems.dalvik.DexField;
import com.toccatasystems.dalvik.DexInstruction;
import com.toccatasystems.dalvik.DexMethod;
import com.toccatasystems.dalvik.DexMethodBody;
import com.toccatasystems.dalvik.DexTryCatch;
import com.toccatasystems.dalvik.DexType;

import static com.toccatasystems.dalvik.DexOpcodes.*;

/**
 * This class does the low-level work of translating dalvik bytecode
 * back into jvm bytecode. 
 * 
 * @author nkeynes
 */
public class BytecodeTransformer {

	private Map<DexBasicBlock, Label> labelMap;
	private MethodVisitor out;
	int argWords, localWords;
	int maxStackSize;
	
	public BytecodeTransformer( ) {
		labelMap = new HashMap<DexBasicBlock,Label>();
	}
	
	public void transform( DexMethodBody body, MethodVisitor out ) {
		/* Construct labels for each basic block */
		labelMap.clear();
		for( Iterator<DexBasicBlock> it = body.iterator(); it.hasNext(); ) {
			labelMap.put(it.next(), new Label());
		}
		this.argWords = body.getInArgWords();
		this.localWords = body.getNumRegisters() - this.argWords;
		this.out = out;
		this.maxStackSize = 3;
		out.visitCode();
		
		/* Visit the exception handlers - ASM requires that this is done before
		 * reaching the labels in question, so it's simplest to do it first
		 */
		for( Iterator<DexTryCatch> it = body.handlerIterator(); it.hasNext(); ) {
			DexTryCatch e = it.next();
			DexBasicBlock start = e.getStartBlock();
			DexBasicBlock end = e.getEndBlock();
			DexBasicBlock handler = e.getHandlerBlock();
			out.visitTryCatchBlock(labelMap.get(start), labelMap.get(end), labelMap.get(handler), e.getInternalType() );
		}
		
		for( Iterator<DexBasicBlock> bbit = body.iterator(); bbit.hasNext(); ) {
			DexBasicBlock bb = bbit.next();
			out.visitLabel(labelMap.get(bb));
			for(Iterator<DexInstruction> ii = bb.iterator(); ii.hasNext(); ) {
				DexInstruction inst = ii.next();

				switch( inst.getOpcode() ) {
				case NOP:
					out.visitInsn(Opcodes.NOP);
					break;
				case MOVE: case MOVE_FROM16: case MOVE_16: case MOVE_WIDE: case MOVE_WIDE_FROM16: case MOVE_WIDE16:
				case MOVE_OBJECT: case MOVE_OBJECT_FROM16: case MOVE_OBJECT16:
					push( inst, 1 );
					pop( inst, 0 );
					break;
				case MOVE_RESULT: case MOVE_RESULT_WIDE: case MOVE_RESULT_OBJECT: case MOVE_EXCEPTION:
					/* Result should have been left on the stack by the previous instruction. */
					pop( inst, 0 );
					break;
				case RETURN_VOID:
					out.visitInsn(Opcodes.RETURN);
					break;
				case RETURN: case RETURN_WIDE: case RETURN_OBJECT:
					push( inst, 0 );
					ret(inst.getRegisterType(0));
					break;
				case CONST4: case CONST16: case CONST: case CONST_HIGH16: case CONST_WIDE: case CONST_WIDE32:
				case CONST_WIDE_HIGH16: case CONST_WIDE16:
					ldc(inst);
					pop( inst, 0 );
					break;
				case CONST_STRING: case CONST_STRING_JUMBO:
					out.visitLdcInsn(inst.getStringOperand());
					pop( inst, 0 );
					break;
				case CONST_CLASS:
					out.visitLdcInsn(Type.getType(inst.getTypeOperand().getName()));
					pop( inst, 0 );
					break;
				case MONITOR_ENTER:
					push( inst, 0 );
					out.visitInsn(Opcodes.MONITORENTER);
					break;
				case MONITOR_EXIT:
					push( inst, 0 );
					out.visitInsn(Opcodes.MONITOREXIT);
					break;
				case CHECK_CAST:
					push( inst, 0 );
					out.visitTypeInsn(Opcodes.CHECKCAST, inst.getTypeOperand().getInternalName());
					pop( inst, 0 );
					break;
				case INSTANCE_OF:
					push( inst, 1 );
					out.visitTypeInsn(Opcodes.INSTANCEOF, inst.getTypeOperand().getInternalName());
					pop( inst, 0 );
					break;
				case ARRAY_LENGTH:
					push( inst, 1 );
					out.visitInsn(Opcodes.ARRAYLENGTH);
					pop( inst, 0 );
					break;
				case NEW_INSTANCE:
					out.visitTypeInsn(Opcodes.NEW, inst.getTypeOperand().getInternalName());
					pop( inst, 0 );
					break;
				case NEW_ARRAY:
					push( inst, 1 );
					newarray(inst.getTypeOperand().getElementType());
					pop( inst, 0 );
					break;
				case FILLED_NEW_ARRAY: case FILLED_NEW_ARRAY_RANGE:
					ldc(inst.getNumRegisters());
					newarray(inst.getTypeOperand().getElementType());
					for( int i=0; i<inst.getNumRegisters(); i++ ) {
						dup();
						ldc(i);
						push(inst, i);
						storearray(inst.getRegisterType(i));
					}
					/* Result left on stack for a move-result */
					break;
				case FILL_ARRAY_DATA:
					for( int i=0; i<inst.getNumFillElements(); i++ ) {
						push(inst, 0);
						ldc(i);
						out.visitLdcInsn(inst.getFillElement(i));
						storearray(inst.getRegisterType(0).getElementType());
					}
					break;
				case THROW:
					push( inst, 0 );
					out.visitInsn(Opcodes.ATHROW);
					break;
				case GOTO: case GOTO16: case GOTO32:
					out.visitJumpInsn(Opcodes.GOTO, labelMap.get(inst.getBranchBlock()));
					break;
				case PACKED_SWITCH:
					push(inst, 0);
					out.visitTableSwitchInsn(inst.getMinSwitchKey(), inst.getMaxSwitchKey(),
							labelMap.get(bb.getFallthroughSuccessor()), 
							getLabels(inst.getSwitchBlocks()) );
					break;
				case SPARSE_SWITCH:
					push(inst, 0);
					out.visitLookupSwitchInsn(labelMap.get(bb.getFallthroughSuccessor()), 
							inst.getSwitchKeys(), getLabels(inst.getSwitchBlocks()));
					break;
				case CMPL_FLOAT:
					arith3(inst, Opcodes.FCMPL);
					break;
				case CMPG_FLOAT:
					arith3(inst, Opcodes.FCMPG);
					break;
				case CMPL_DOUBLE:
					arith3(inst, Opcodes.DCMPL);
					break;
				case CMPG_DOUBLE:
					arith3(inst, Opcodes.DCMPG);
					break;
				case CMP_LONG:
					arith3(inst, Opcodes.LCMP);
					break;
				case IF_EQ:
					if( inst.getRegisterType(0).isObject() ) {
						cond2(inst, Opcodes.IF_ACMPEQ);
					} else {
						cond2(inst, Opcodes.IF_ICMPEQ);
					}
					break;
				case IF_NE:
					if( inst.getRegisterType(0).isObject() ) {
						cond2(inst, Opcodes.IF_ACMPNE);
					} else {
						cond2(inst, Opcodes.IF_ICMPNE);
					}
					break;
				case IF_LT:
					cond2(inst, Opcodes.IF_ICMPLT);
					break;
				case IF_GE:
					cond2(inst, Opcodes.IF_ICMPGE);
					break;
				case IF_GT:
					cond2(inst, Opcodes.IF_ICMPGT);
					break;
				case IF_LE:
					cond2(inst, Opcodes.IF_ICMPLE);
					break;
				case IF_EQZ:
					if( inst.getRegisterType(0).isObject() ) {
						cond1(inst, Opcodes.IFNULL);
					} else {
						cond1(inst, Opcodes.IFEQ);
					}
					break;
				case IF_NEZ:
					if( inst.getRegisterType(0).isObject() ) {
						cond1(inst, Opcodes.IFNONNULL);
					} else {
						cond1(inst, Opcodes.IFNE);
					}
					break;
				case IF_LTZ:
					cond1(inst, Opcodes.IFLT);
					break;
				case IF_GEZ:
					cond1(inst, Opcodes.IFGE);
					break;
				case IF_GTZ:
					cond1(inst, Opcodes.IFGT);
					break;
				case IF_LEZ:
					cond1(inst, Opcodes.IFLE);
					break;

				case AGET: case AGET_WIDE: case AGET_OBJECT: case AGET_BOOLEAN:
				case AGET_BYTE: case AGET_CHAR: case AGET_SHORT:
					push( inst, 1 );
					push( inst, 2 );
					loadarray( inst.getRegisterType(0) );
					pop( inst, 0 );
					break;
				case APUT: case APUT_WIDE: case APUT_OBJECT: case APUT_BOOLEAN:
				case APUT_BYTE: case APUT_CHAR: case APUT_SHORT:
					push( inst, 1 );
					push( inst, 2 );
					push( inst, 0 );
					storearray( inst.getRegisterType(0) );
					break;

				case IGET: case IGET_WIDE: case IGET_OBJECT: case IGET_BOOLEAN:
				case IGET_BYTE: case IGET_CHAR: case IGET_SHORT:
					push( inst, 1 );
					field( inst.getFieldOperand(), Opcodes.GETFIELD );
					pop( inst, 0 );
					break;
				case IPUT: case IPUT_WIDE: case IPUT_OBJECT: case IPUT_BOOLEAN:
				case IPUT_BYTE: case IPUT_CHAR: case IPUT_SHORT:
					push( inst, 1 );
					push( inst, 0 );
					field( inst.getFieldOperand(), Opcodes.PUTFIELD );
					break;
				
				case SGET: case SGET_WIDE: case SGET_OBJECT: case SGET_BOOLEAN:
				case SGET_BYTE: case SGET_CHAR: case SGET_SHORT:
					field( inst.getFieldOperand(), Opcodes.GETSTATIC );
					pop( inst, 0 );
					break;
				case SPUT: case SPUT_WIDE: case SPUT_OBJECT: case SPUT_BOOLEAN:
				case SPUT_BYTE: case SPUT_CHAR: case SPUT_SHORT:
					push( inst, 0 );
					field( inst.getFieldOperand(), Opcodes.PUTSTATIC );
					break;
					
				case INVOKE_VIRTUAL: case INVOKE_VIRTUAL_RANGE:
					pushall( inst );
					invoke( inst, Opcodes.INVOKEVIRTUAL );
					break;
				case INVOKE_SUPER: case INVOKE_SUPER_RANGE:
				case INVOKE_DIRECT: case INVOKE_DIRECT_RANGE:
					pushall( inst );
					invoke( inst, Opcodes.INVOKESPECIAL );
					break;
				case INVOKE_STATIC: case INVOKE_STATIC_RANGE:
					pushall( inst );
					invoke( inst, Opcodes.INVOKESTATIC );
					break;
				case INVOKE_INTERFACE: case INVOKE_INTERFACE_RANGE:
					pushall( inst ) ;
					invoke( inst, Opcodes.INVOKEINTERFACE );
					break;
	
					/* 'not' is not a jvm bytecode, so emit val xor -1 instead */
				case NOT_INT:
					push(inst, 1);
					ldc(-1);
					out.visitInsn(Opcodes.IXOR);
					pop(inst, 0);
					break;
				case NOT_LONG: /* ??? */ 
					push(inst, 1);
					ldc(-1);
					out.visitInsn(Opcodes.I2L);
					out.visitInsn(Opcodes.LXOR);
					pop(inst, 0);
					break;
					/* Core arithmetic */
				case NEG_INT:         unary( inst, Opcodes.INEG ); break;
				case NEG_LONG:        unary( inst, Opcodes.LNEG ); break;
				case NEG_FLOAT:       unary( inst, Opcodes.FNEG ); break;
				case NEG_DOUBLE:      unary( inst, Opcodes.DNEG ); break;
				case INT_TO_LONG:     unary( inst, Opcodes.I2L ); break;
				case INT_TO_FLOAT:    unary( inst, Opcodes.I2F ); break;
				case INT_TO_DOUBLE:   unary( inst, Opcodes.I2D ); break;
				case LONG_TO_INT:     unary( inst, Opcodes.L2I ); break;
				case LONG_TO_FLOAT:   unary( inst, Opcodes.L2F ); break;
				case LONG_TO_DOUBLE:  unary( inst, Opcodes.L2D ); break;
				case FLOAT_TO_INT:    unary( inst, Opcodes.F2I ); break;
				case FLOAT_TO_LONG:   unary( inst, Opcodes.F2L ); break;
				case FLOAT_TO_DOUBLE: unary( inst, Opcodes.F2D ); break;
				case DOUBLE_TO_INT:   unary( inst, Opcodes.D2I ); break;
				case DOUBLE_TO_LONG:  unary( inst, Opcodes.D2L ); break;
				case DOUBLE_TO_FLOAT: unary( inst, Opcodes.D2F ); break;
				case INT_TO_BYTE:     unary( inst, Opcodes.I2B ); break;
				case INT_TO_CHAR:     unary( inst, Opcodes.I2C ); break;
				case INT_TO_SHORT:    unary( inst, Opcodes.I2S ); break;
				case ADD_INT:        arith3( inst, Opcodes.IADD ); break;
				case SUB_INT:        arith3( inst, Opcodes.ISUB ); break;
				case MUL_INT:        arith3( inst, Opcodes.IMUL ); break;
				case DIV_INT:        arith3( inst, Opcodes.IDIV ); break;
				case REM_INT:        arith3( inst, Opcodes.IREM ); break;
				case AND_INT:        arith3( inst, Opcodes.IAND ); break;
				case OR_INT:         arith3( inst, Opcodes.IOR ); break;
				case XOR_INT:        arith3( inst, Opcodes.IXOR ); break;
				case SHL_INT:        arith3( inst, Opcodes.ISHL ); break;
				case SHR_INT:        arith3( inst, Opcodes.ISHR ); break;
				case USHR_INT:       arith3( inst, Opcodes.IUSHR ); break;
				case ADD_LONG:       arith3( inst, Opcodes.LADD ); break;
				case SUB_LONG:       arith3( inst, Opcodes.LSUB ); break;
				case MUL_LONG:       arith3( inst, Opcodes.LMUL ); break;
				case DIV_LONG:       arith3( inst, Opcodes.LDIV ); break;
				case REM_LONG:       arith3( inst, Opcodes.LREM ); break;
				case AND_LONG:       arith3( inst, Opcodes.LAND ); break;
				case OR_LONG:        arith3( inst, Opcodes.LOR ); break;
				case XOR_LONG:       arith3( inst, Opcodes.LXOR ); break;
				case SHL_LONG:       arith3( inst, Opcodes.LSHL ); break;
				case SHR_LONG:       arith3( inst, Opcodes.LSHR ); break;
				case USHR_LONG:      arith3( inst, Opcodes.LUSHR ); break;
				case ADD_FLOAT:      arith3( inst, Opcodes.FADD ); break;
				case SUB_FLOAT:      arith3( inst, Opcodes.FSUB ); break;
				case MUL_FLOAT:      arith3( inst, Opcodes.FMUL ); break;
				case DIV_FLOAT:      arith3( inst, Opcodes.FDIV ); break;
				case REM_FLOAT:      arith3( inst, Opcodes.FREM ); break;
				case ADD_DOUBLE:     arith3( inst, Opcodes.DADD ); break;
				case SUB_DOUBLE:     arith3( inst, Opcodes.DSUB ); break;
				case MUL_DOUBLE:     arith3( inst, Opcodes.DMUL ); break;
				case DIV_DOUBLE:     arith3( inst, Opcodes.DDIV ); break;
				case REM_DOUBLE:     arith3( inst, Opcodes.DREM ); break;
				case ADD_INT_2ADDR:    arith2( inst, Opcodes.IADD ); break;
				case SUB_INT_2ADDR:    arith2( inst, Opcodes.ISUB ); break;
				case MUL_INT_2ADDR:    arith2( inst, Opcodes.IMUL ); break;
				case DIV_INT_2ADDR:    arith2( inst, Opcodes.IDIV ); break;
				case REM_INT_2ADDR:    arith2( inst, Opcodes.IREM ); break;
				case AND_INT_2ADDR:    arith2( inst, Opcodes.IAND ); break;
				case OR_INT_2ADDR:     arith2( inst, Opcodes.IOR ); break;
				case XOR_INT_2ADDR:    arith2( inst, Opcodes.IXOR ); break;
				case SHL_INT_2ADDR:    arith2( inst, Opcodes.ISHL ); break;
				case SHR_INT_2ADDR:    arith2( inst, Opcodes.ISHR ); break;
				case USHR_INT_2ADDR:   arith2( inst, Opcodes.IUSHR ); break;
				case ADD_LONG_2ADDR:   arith2( inst, Opcodes.LADD ); break;
				case SUB_LONG_2ADDR:   arith2( inst, Opcodes.LSUB ); break;
				case MUL_LONG_2ADDR:   arith2( inst, Opcodes.LMUL ); break;
				case DIV_LONG_2ADDR:   arith2( inst, Opcodes.LDIV ); break;
				case REM_LONG_2ADDR:   arith2( inst, Opcodes.LREM ); break;
				case AND_LONG_2ADDR:   arith2( inst, Opcodes.LAND ); break;
				case OR_LONG_2ADDR:    arith2( inst, Opcodes.LOR ); break;
				case XOR_LONG_2ADDR:   arith2( inst, Opcodes.LXOR ); break;
				case SHL_LONG_2ADDR:   arith2( inst, Opcodes.LSHL ); break;
				case SHR_LONG_2ADDR:   arith2( inst, Opcodes.LSHR ); break;
				case USHR_LONG_2ADDR:  arith2( inst, Opcodes.LUSHR ); break;
				case ADD_FLOAT_2ADDR:  arith2( inst, Opcodes.FADD ); break;
				case SUB_FLOAT_2ADDR:  arith2( inst, Opcodes.FSUB ); break;
				case MUL_FLOAT_2ADDR:  arith2( inst, Opcodes.FMUL ); break;
				case DIV_FLOAT_2ADDR:  arith2( inst, Opcodes.FDIV ); break;
				case REM_FLOAT_2ADDR:  arith2( inst, Opcodes.FREM ); break;
				case ADD_DOUBLE_2ADDR: arith2( inst, Opcodes.DADD ); break;
				case SUB_DOUBLE_2ADDR: arith2( inst, Opcodes.DSUB ); break;
				case MUL_DOUBLE_2ADDR: arith2( inst, Opcodes.DMUL ); break;
				case DIV_DOUBLE_2ADDR: arith2( inst, Opcodes.DDIV ); break;
				case REM_DOUBLE_2ADDR: arith2( inst, Opcodes.DREM ); break;

				case ADD_INT_LIT16:   arith2s( inst, Opcodes.IADD ); break;
				case RSUB_INT_LIT16:  arith2s( inst, Opcodes.ISUB ); break;
				case MUL_INT_LIT16:   arith2s( inst, Opcodes.IMUL ); break;
				case DIV_INT_LIT16:   arith2s( inst, Opcodes.IDIV ); break;
				case REM_INT_LIT16:   arith2s( inst, Opcodes.IREM ); break;
				case AND_INT_LIT16:   arith2s( inst, Opcodes.IAND ); break;
				case OR_INT_LIT16:    arith2s( inst, Opcodes.IOR ); break;
				case XOR_INT_LIT16:   arith2s( inst, Opcodes.IXOR ); break;
				case ADD_INT_LIT8:    arith2b( inst, Opcodes.IADD ); break;
				case RSUB_INT_LIT8:   arith2b( inst, Opcodes.ISUB ); break;
				case MUL_INT_LIT8:    arith2b( inst, Opcodes.IMUL ); break;
				case DIV_INT_LIT8:    arith2b( inst, Opcodes.IDIV ); break;
				case REM_INT_LIT8:    arith2b( inst, Opcodes.IREM ); break;
				case AND_INT_LIT8:    arith2b( inst, Opcodes.IAND ); break;
				case OR_INT_LIT8:     arith2b( inst, Opcodes.IOR ); break;
				case XOR_INT_LIT8:    arith2b( inst, Opcodes.IXOR ); break;
				case SHL_INT_LIT8:    arith2b( inst, Opcodes.ISHL ); break;
				case SHR_INT_LIT8:    arith2b( inst, Opcodes.ISHR ); break;
				case USHR_INT_LIT8:   arith2b( inst, Opcodes.IUSHR ); break;

				default:
					throw new RuntimeException( "Unhandled opcode: " + inst.disassemble() );
				}
			}
		}
		
		try { 
			out.visitMaxs(maxStackSize, body.getNumRegisters());
		} catch( Exception e ) {
			e.printStackTrace();
			System.err.println( "at " + body.getParent().getDisplaySignature() );
		}
	}

	/**
	 * Remap register indexes - JVM maps parameters from 0,
	 * Dalvik puts them at the end.
	 * @param reg
	 * @return
	 */
	private int mapReg( int reg ) {
		if( reg < localWords ) {
			return reg + argWords;
		} else {
			return reg - localWords;
		}
	}
	
	/**
	 * Push a local variable on the stack
	 * @param type
	 * @param reg
	 * @param out
	 */
	private void push( DexType type, int reg ) {
		int opcode;
		int mappedReg = mapReg(reg);
		if( type.equals(DexType.INT) || type.equals(DexType.SHORT) || 
			type.equals(DexType.BYTE) || type.equals(DexType.CHAR) ||
			type.equals(DexType.BOOLEAN) ) {
			opcode = Opcodes.ILOAD;
		} else if( type.equals(DexType.LONG) ) {
			opcode = Opcodes.LLOAD;
		} else if( type.equals(DexType.FLOAT) ) {
			opcode = Opcodes.FLOAD;
		} else if( type.equals(DexType.DOUBLE) ) {
			opcode = Opcodes.DLOAD;
		} else {
			opcode = Opcodes.ALOAD;
		}
		out.visitVarInsn(opcode, mappedReg);
	}
	
	/**
	 * Pop a local variable off the stack
	 * @param type
	 * @param reg
	 * @param out
	 */
	private void pop( DexType type, int reg ) {
		int opcode;
		int mappedReg = mapReg(reg);
		if( type.equals(DexType.INT) || type.equals(DexType.SHORT) || 
				type.equals(DexType.BYTE) || type.equals(DexType.CHAR) ||
				type.equals(DexType.BOOLEAN) ) {
			opcode = Opcodes.ISTORE;
		} else if( type.equals(DexType.LONG) ) {
			opcode = Opcodes.LSTORE;
		} else if( type.equals(DexType.FLOAT) ) {
			opcode = Opcodes.FSTORE;
		} else if( type.equals(DexType.DOUBLE) ) {
			opcode = Opcodes.DSTORE;
		} else {
			opcode = Opcodes.ASTORE;
		}
		out.visitVarInsn(opcode, mappedReg);
	}
	
	private void push( DexInstruction inst, int reg ) {
		push( inst.getRegisterType(reg), inst.getRegister(reg) );
	}
	
	private void pop( DexInstruction inst, int reg ) {
		pop( inst.getRegisterType(reg), inst.getRegister(reg) );
	}
	
	private void pushall( DexInstruction inst ) {
		if( inst.getNumRegisters() > maxStackSize ) {
			maxStackSize = inst.getNumRegisters();
		}
		for( int i=0; i<inst.getNumRegisters(); i++ ) {
			push( inst.getRegisterType(i), inst.getRegister(i) );
		}
	}
	
	/**
	 * Discard the top-of-stack value
	 * @param type
	 */
	private void discard( DexType type ) {
		if( type.equals(DexType.LONG) || type.equals(DexType.DOUBLE) ) {
			out.visitInsn(Opcodes.POP2);
		} else if( !type.equals(DexType.VOID) ) {
			out.visitInsn(Opcodes.POP);
		}
	}
	
	private void dup() {
		out.visitInsn(Opcodes.DUP);
	}
	
	private void newarray( DexType type ) {
		if( type.equals(DexType.INT) ) {
			out.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
		} else if( type.equals(DexType.SHORT) ) {
			out.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_SHORT);
		} else if( type.equals(DexType.BYTE) ) {
			out.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
		} else if( type.equals(DexType.BOOLEAN) ) {
			out.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);
		} else if( type.equals(DexType.CHAR) ) {
			out.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_CHAR);
		} else if( type.equals(DexType.LONG) ) {
			out.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG);
		} else if( type.equals(DexType.FLOAT) ) {
			out.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_FLOAT);
		} else if( type.equals(DexType.DOUBLE) ) {
			out.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_DOUBLE);
		} else {
			out.visitTypeInsn(Opcodes.ANEWARRAY, type.getInternalName());
		}
	}		
	
	private void storearray( DexType type ) {
		if( type.equals(DexType.INT) ) {
			out.visitInsn(Opcodes.IASTORE);
		} else if( type.equals(DexType.SHORT) ) {
			out.visitInsn(Opcodes.SASTORE);
		} else if( type.equals(DexType.BYTE) || type.equals(DexType.BOOLEAN) ) {
			out.visitInsn(Opcodes.BASTORE);
		} else if( type.equals(DexType.CHAR) ) {
			out.visitInsn(Opcodes.CASTORE);
		} else if( type.equals(DexType.LONG) ) {
			out.visitInsn(Opcodes.LASTORE);
		} else if( type.equals(DexType.FLOAT) ) {
			out.visitInsn(Opcodes.FASTORE);
		} else if( type.equals(DexType.DOUBLE) ) {
			out.visitInsn(Opcodes.DASTORE);
		} else {
			out.visitInsn(Opcodes.AASTORE);
		}
	}

	private void loadarray( DexType type ) {
		if( type.equals(DexType.INT) ) {
			out.visitInsn(Opcodes.IALOAD);
		} else if( type.equals(DexType.SHORT) ) {
			out.visitInsn(Opcodes.SALOAD);
		} else if( type.equals(DexType.BYTE) || type.equals(DexType.BOOLEAN) ) {
			out.visitInsn(Opcodes.BALOAD);
		} else if( type.equals(DexType.CHAR) ) {
			out.visitInsn(Opcodes.CALOAD);
		} else if( type.equals(DexType.LONG) ) {
			out.visitInsn(Opcodes.LALOAD);
		} else if( type.equals(DexType.FLOAT) ) {
			out.visitInsn(Opcodes.FALOAD);
		} else if( type.equals(DexType.DOUBLE) ) {
			out.visitInsn(Opcodes.DALOAD);
		} else {
			out.visitInsn(Opcodes.AALOAD);
		}
	}

	private void ldc( int value ) {
		switch( value ) {
		case -1: out.visitInsn(Opcodes.ICONST_M1); break;
		case 0: out.visitInsn(Opcodes.ICONST_0); break;
		case 1: out.visitInsn(Opcodes.ICONST_1); break;
		case 2: out.visitInsn(Opcodes.ICONST_2); break;
		case 3: out.visitInsn(Opcodes.ICONST_3); break;
		case 4: out.visitInsn(Opcodes.ICONST_4); break;
		case 5: out.visitInsn(Opcodes.ICONST_5); break;
		default:
			if( value <= Byte.MAX_VALUE && value >= Byte.MIN_VALUE ) {
				out.visitIntInsn(Opcodes.BIPUSH, value);
			} else if( value <= Short.MAX_VALUE && value >= Short.MIN_VALUE ) {
				out.visitIntInsn(Opcodes.SIPUSH, value);
			} else {
				out.visitLdcInsn(value);
			}
		}
	}		
	
	/**
	 * Load a constant onto the stack
	 * @param inst
	 * @param out
	 */
	private void ldc( DexInstruction inst ) {
		DexType type = inst.getRegisterType(0);
		if( type.equals(DexType.INT) || type.equals(DexType.SHORT) || 
				type.equals(DexType.BYTE) || type.equals(DexType.CHAR) ||
				type.equals(DexType.BOOLEAN) ) {
			ldc(inst.getIntOperand());
		} else if( type.equals(DexType.LONG) ) {
			Long l = inst.getLongOperand();
			if( l == 0 ) {
				out.visitInsn(Opcodes.LCONST_0);
			} else if( l == 1 ) {
				out.visitInsn(Opcodes.LCONST_1);
			} else {
				out.visitLdcInsn(l);
			}
		} else if( type.equals(DexType.FLOAT) ) {
			Float f = inst.getFloatOperand();
			if( f == 0.0F ) {
				out.visitInsn(Opcodes.FCONST_0);
			} else if( f == 1.0F ) {
				out.visitInsn(Opcodes.FCONST_1);
			} else if( f == 2.0F ) {
				out.visitInsn(Opcodes.FCONST_2);
			} else {
				out.visitLdcInsn(f);
			}
		} else if( type.equals(DexType.DOUBLE) ) {
			Double d =inst.getDoubleOperand();
			if( d == 0.0 ) {
				out.visitInsn(Opcodes.DCONST_0);
			} else if( d == 1.0 ) {
				out.visitInsn(Opcodes.DCONST_1);
			} else {
				out.visitLdcInsn(d);
			}
		} else {
			if( inst.getIntOperand() == 0 ) {
				out.visitInsn(Opcodes.ACONST_NULL);
			} else {
				throw new RuntimeException("Invalid type for constant: " + type );
			}
		}
	}
	
	/**
	 * Return the value on the top of the stack
	 * @param type
	 * @param out
	 */
	private void ret( DexType type ) {
		if( type.equals(DexType.INT) || type.equals(DexType.SHORT) || 
				type.equals(DexType.BYTE) || type.equals(DexType.CHAR) ||
				type.equals(DexType.BOOLEAN) ) {
			out.visitInsn(Opcodes.IRETURN);
		} else if( type.equals(DexType.LONG) ) {
			out.visitInsn(Opcodes.LRETURN);
		} else if( type.equals(DexType.FLOAT) ) {
			out.visitInsn(Opcodes.FRETURN);
		} else if( type.equals(DexType.DOUBLE) ) {
			out.visitInsn(Opcodes.DRETURN);
		} else {
			out.visitInsn(Opcodes.ARETURN);
		}
	}
	
	private void field( DexField field, int opcode ) {
		out.visitFieldInsn( opcode, field.getInternalClassType(),
				field.getName(), field.getType() );
	}
	
	/**
	 * Visit an invoke* operator. 
	 * @param method
	 * @param opcode
	 * @return the result type of the invoke
	 */
	private void invoke( DexInstruction inst, int opcode ) {
		DexMethod method = inst.getMethodOperand();
		out.visitMethodInsn(opcode, method.getInternalClassType(), 
				method.getName(), method.getDescriptor());

		/* Check if the result is going to be used immediately. If not, emit
		 * a pop to get it off the stack */
		DexType resultType = new DexType(method.getReturnType());
		if( !resultType.equals(DexType.VOID) ) {
			DexInstruction next = inst.getParent().getNext(inst);
			if( next == null ) {
				next = inst.getParent().getFallthroughSuccessor().first();
			}
			if( next.getOpcode() != MOVE_RESULT && next.getOpcode() != MOVE_RESULT_WIDE &&
					next.getOpcode() != MOVE_RESULT_OBJECT ) {
				discard(resultType);
			}
		}
	}
		
	/**
	 * Convert a 3-op arithmetic instruction form.
	 * @param inst
	 * @param opcode
	 */
	private void arith3( DexInstruction inst, int opcode ) {
		push( inst, 1 );
		push( inst, 2 );
		out.visitInsn(opcode);
		pop( inst, 0 );
	}
	
	/**
	 * Convert a 2-op arithmetic instruction form.
	 * @param inst
	 * @param opcode
	 */
	private void arith2( DexInstruction inst, int opcode ) {
		push( inst, 0 );
		push( inst, 1 );
		out.visitInsn(opcode);
		pop( inst, 0 );
	}

	/**
	 * Convert a 1-op + literal byte arithmetic instruction form.
	 * @param inst
	 * @param opcode
	 */
	private void arith2b( DexInstruction inst, int opcode ) {
		if( opcode == Opcodes.ISHR || opcode == Opcodes.ISHL || opcode == Opcodes.IUSHR ) {
			push( inst, 1 );
			out.visitIntInsn(Opcodes.BIPUSH, inst.getIntOperand());
		} else {
			out.visitIntInsn(Opcodes.BIPUSH, inst.getIntOperand());
			push( inst, 1 );
		}
		out.visitInsn(opcode);
		pop( inst, 0 );
	}
	
	/**
	 * Convert a 1-op + literal short arithmetic instruction form.
	 * @param inst
	 * @param opcode
	 */
	private void arith2s( DexInstruction inst, int opcode ) {
		out.visitIntInsn(Opcodes.SIPUSH, inst.getIntOperand());
		push( inst, 1 );
		out.visitInsn(opcode);
		pop( inst, 0 );
	}
	
	private void unary( DexInstruction inst, int opcode ) {
		push( inst, 1 );
		out.visitInsn(opcode);
		pop( inst, 0 );
	}
	
	private void cond1( DexInstruction inst, int opcode ) {
		push( inst, 0 );
		out.visitJumpInsn(opcode, labelMap.get(inst.getBranchBlock()));
	}

	private void cond2( DexInstruction inst, int opcode ) {
		push( inst, 0 );
		push( inst, 1 );
		out.visitJumpInsn(opcode, labelMap.get(inst.getBranchBlock()));
	}
	

	private Label[] getLabels( DexBasicBlock []arr ) {
		Label[] result = new Label[arr.length];
		for( int i=0; i<arr.length; i++ ) {
			result[i] = labelMap.get(arr[i]);
		}
		return result;
	}
	
}
