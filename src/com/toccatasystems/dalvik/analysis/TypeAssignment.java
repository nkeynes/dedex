package com.toccatasystems.dalvik.analysis;

import java.util.Iterator;

import com.toccatasystems.dalvik.DexArgument;
import com.toccatasystems.dalvik.DexInstruction;
import com.toccatasystems.dalvik.DexMethodBody;
import com.toccatasystems.dalvik.DexType;

import static com.toccatasystems.dalvik.DexOpcodes.*;


/**
 * Assign types to all instruction operands. Note at this point we mainly care
 * about primitive vs object type in order to deal with the fact that many of
 * the dalvik opcodes are polymorphic in different ways to JVM bytecode.
 * 
 * To keep things interesting, primitive constants are essentially untyped bits,
 * which we need to resolve down to specific types to make the JVM
 * happy.
 * 
 * @author nkeynes
 *
 */

public class TypeAssignment extends DexAnalysis {
	

	public void analyse( DexMethodBody body ) {
		assignTypes(body);
		cleanupConstants(body);
	}

	private void setRegisterUseType( DexInstruction inst, int operand, DexType type ) {
		DexType orig = inst.getRegisterType(operand);
		if( orig != null && !orig.isCompatible(type) ) {
			throw new RuntimeException("Type conflict at " + inst.disassemble() + ": expected " + type.format() + ", but was " + orig.format());
		}
		inst.setRegisterType(operand, type);
		for( Iterator<DexInstruction> it = inst.getRegisterDefs(operand).iterator(); it.hasNext(); ) {
			DexInstruction def = it.next();
			if( def != inst ) {
				DexType old = def.getRegisterType(0);
				if( def.isWideConstant() ) {
					if( !type.equals(DexType.LONG) && !type.equals(DexType.DOUBLE) ) { 
						throw new RuntimeException("Type conflict at " + def.disassemble() + ": expected 64-bit type, but was " + type.format());
					}
				} else if( def.isConstant() ) {
					if( type.equals(DexType.LONG) || type.equals(DexType.DOUBLE) ) { 
						throw new RuntimeException("Type conflict at " + def.disassemble() + ": expected 32-bit type, but was " + type.format());
					}
				}
				if( old == null || type.isProperSubtypeOf(old) ) {
					setRegisterDefType( def, type );
				} else if( !old.isCompatible(type) ) {
					throw new RuntimeException("Type conflict at " + def.disassemble() + ": expected " + type.format() + ", but was " + old.format());
				}
			}
		}
	}

	private void setRegisterDefType( DexInstruction inst, DexType type ) {
		DexType orig = inst.getRegisterType(0);
		if( orig != null && !orig.isCompatible(type) ) {
			throw new RuntimeException("Type conflict at " + inst.disassemble() + ": expected " + type.format() + ", but was " + orig.format());
		}
		inst.setRegisterType(0, type);
		for( Iterator<DexInstruction.Use> it = inst.uses().iterator(); it.hasNext(); ) {
			DexInstruction.Use use = it.next();
			if( use.getUser() != inst ) {
				DexInstruction user = use.getUser();
				if( user.isRegisterSupertypeOf(use.getOperand(), type) ) {
					setRegisterUseType( user, use.getOperand(), type );
					/* Special cases where one operand implies the other */
					switch( user.getOpcode() ) {
					case MOVE: case MOVE_FROM16: case MOVE_16: case MOVE_WIDE: 
					case MOVE_WIDE_FROM16: case MOVE_WIDE16:
					case MOVE_OBJECT: case MOVE_OBJECT_FROM16: case MOVE_OBJECT16:
						/* Moves aren't defs, so just update the other reg */
						user.setRegisterType( use.getOperand() == 0 ? 1 : 0, type );
						break;
					case IF_EQ: case IF_NE:
						/* Set both operands to be the same */
						setRegisterUseType( user, use.getOperand() == 0 ? 1 : 0, type );
						break;
					case AGET: case AGET_WIDE: case AGET_OBJECT:
						if( use.getOperand() == 0 ) {
							DexType arrayType = type.getArrayType();
							if( user.isRegisterSupertypeOf(1, arrayType) ) { 
								setRegisterUseType(user, 1, arrayType);
							}
						} else if( use.getOperand() == 1 ) {
							DexType elemType = type.getElementType();
							if( elemType != null && user.isRegisterSupertypeOf(0, elemType) ) {
								setRegisterDefType( user, elemType );
							}
						}
						break;
					case APUT: case APUT_WIDE: case APUT_OBJECT:
						if( use.getOperand() == 0 ) {
							DexType arrayType = type.getArrayType();
							if( user.isRegisterSupertypeOf(1, arrayType) ) { 
								setRegisterUseType(user, 1, arrayType);
							}
						} else if( use.getOperand() == 1 ) {
							DexType elemType = type.getElementType();
							if( elemType != null && user.isRegisterSupertypeOf(0, elemType) ) {
								setRegisterUseType( user, 0, elemType );
							}
						}
						break;
					}
				} else if( !user.isRegisterCompatibleWith(use.getOperand(), type) ) {
					throw new RuntimeException("Type conflict at " + user.disassemble() + ": expected " + type.format() + ", but was " + user.getRegisterType(use.getOperand()));
				}
			}
		}
	}

	/**
	 * Set all types where the instruction explicitly requires a
	 * particular type, and then propagate these to direct uses/defs.
	 * @param method
	 */
	private void assignTypes(DexMethodBody method ) {
		DexType resultType = null;
		
		/* First assign uses of arguments directly */
		for( int i=0; i<method.getNumArguments(); i++ ) {
			DexArgument arg = method.getArgument(i);
			setRegisterDefType( arg, new DexType(method.getParent().getCallingParamType(i)));
		}
		
		/* Then everything else with explicit types */
		for( Iterator<DexInstruction> ii = method.instIterator(); ii.hasNext(); ) {
			DexInstruction inst = ii.next();
			switch( inst.getOpcode() ) {
			case MOVE_RESULT: case MOVE_RESULT_WIDE: case MOVE_RESULT_OBJECT:
				setRegisterDefType(inst, resultType);
				break;
			case MOVE_EXCEPTION:
				setRegisterDefType(inst, DexType.THROWABLE);
				break;
			case CONST_STRING: case CONST_STRING_JUMBO:
				setRegisterDefType(inst, DexType.STRING);
				break;
			case CONST_CLASS:
				setRegisterDefType(inst, DexType.CLASS);
				break;
			case INSTANCE_OF: case ARRAY_LENGTH: 
				setRegisterDefType(inst, DexType.INT);
				break;
			case CMPL_FLOAT: case CMPG_FLOAT:
				setRegisterUseType(inst, 1, DexType.FLOAT);
				setRegisterUseType(inst, 2, DexType.FLOAT);
				setRegisterDefType(inst, DexType.INT);
				break;
			case CMPL_DOUBLE: case CMPG_DOUBLE: 
				setRegisterUseType(inst, 1, DexType.DOUBLE);
				setRegisterUseType(inst, 2, DexType.DOUBLE);
				setRegisterDefType(inst, DexType.INT);
				break;
			case CMP_LONG:
				setRegisterUseType(inst, 1, DexType.LONG);
				setRegisterUseType(inst, 2, DexType.LONG);
				setRegisterDefType(inst, DexType.INT);
				break;
			case NEG_INT: case NOT_INT: 
			case ADD_INT_LIT16: case RSUB_INT_LIT16: case MUL_INT_LIT16: case DIV_INT_LIT16:
			case REM_INT_LIT16: case AND_INT_LIT16: case OR_INT_LIT16: case XOR_INT_LIT16:
			case ADD_INT_LIT8: case RSUB_INT_LIT8: case MUL_INT_LIT8: case DIV_INT_LIT8:
			case REM_INT_LIT8: case AND_INT_LIT8: case OR_INT_LIT8: case XOR_INT_LIT8:
			case SHL_INT_LIT8: case SHR_INT_LIT8: case USHR_INT_LIT8:
				setRegisterUseType(inst, 1, DexType.INT);
				setRegisterDefType(inst, DexType.INT);
				break;
			case ADD_INT: case SUB_INT: case MUL_INT: case DIV_INT: case REM_INT: case AND_INT: 
			case OR_INT: case XOR_INT: case SHL_INT: case SHR_INT: case USHR_INT: 
				setRegisterUseType(inst, 1, DexType.INT);
				setRegisterUseType(inst, 2, DexType.INT);
				setRegisterDefType(inst, DexType.INT);
				break;
			case ADD_INT_2ADDR: case SUB_INT_2ADDR: case MUL_INT_2ADDR: case DIV_INT_2ADDR: case REM_INT_2ADDR: case AND_INT_2ADDR: 
			case OR_INT_2ADDR: case XOR_INT_2ADDR: case SHL_INT_2ADDR: case SHR_INT_2ADDR: case USHR_INT_2ADDR: 
				setRegisterUseType(inst, 0, DexType.INT);
				setRegisterUseType(inst, 1, DexType.INT);
				setRegisterDefType(inst, DexType.INT);
				break;

			case NEG_LONG: case NOT_LONG: 
				setRegisterUseType(inst, 1, DexType.LONG);
				setRegisterDefType(inst, DexType.LONG);
				break;
			case ADD_LONG: case SUB_LONG: case MUL_LONG: case DIV_LONG: case REM_LONG: case AND_LONG:
			case OR_LONG: case XOR_LONG: 
				setRegisterUseType(inst, 1, DexType.LONG);
				setRegisterUseType(inst, 2, DexType.LONG);
				setRegisterDefType(inst, DexType.LONG);
				break;
			case SHL_LONG: case SHR_LONG: case USHR_LONG:
				setRegisterUseType(inst, 1, DexType.LONG);
				setRegisterUseType(inst, 2, DexType.INT);
				setRegisterDefType(inst, DexType.LONG);
				break;
			case ADD_LONG_2ADDR: case SUB_LONG_2ADDR: case MUL_LONG_2ADDR: case DIV_LONG_2ADDR: case REM_LONG_2ADDR: case AND_LONG_2ADDR: 
			case OR_LONG_2ADDR: case XOR_LONG_2ADDR: 
				setRegisterUseType(inst, 0, DexType.LONG);
				setRegisterUseType(inst, 1, DexType.LONG);
				setRegisterDefType(inst, DexType.LONG);
				break;
			case SHL_LONG_2ADDR: case SHR_LONG_2ADDR: case USHR_LONG_2ADDR: 
				setRegisterUseType(inst, 0, DexType.LONG);
				setRegisterUseType(inst, 1, DexType.INT);
				setRegisterDefType(inst, DexType.LONG);
				break;
			case NEG_FLOAT: 
			case ADD_FLOAT: case SUB_FLOAT: case MUL_FLOAT: case DIV_FLOAT: case REM_FLOAT:
				setRegisterUseType(inst, 1, DexType.FLOAT);
				setRegisterUseType(inst, 2, DexType.FLOAT);
				setRegisterDefType(inst, DexType.FLOAT);
				break;
			case ADD_FLOAT_2ADDR: case SUB_FLOAT_2ADDR: case MUL_FLOAT_2ADDR: case DIV_FLOAT_2ADDR: case REM_FLOAT_2ADDR:
				setRegisterUseType(inst, 0, DexType.FLOAT);
				setRegisterUseType(inst, 1, DexType.FLOAT);
				setRegisterDefType(inst, DexType.FLOAT);
				break;
			case NEG_DOUBLE: 
			case ADD_DOUBLE: case SUB_DOUBLE: case MUL_DOUBLE: case DIV_DOUBLE: case REM_DOUBLE:
				setRegisterUseType(inst, 1, DexType.DOUBLE);
				setRegisterUseType(inst, 2, DexType.DOUBLE);
				setRegisterDefType(inst, DexType.DOUBLE);
				break;
			case ADD_DOUBLE_2ADDR: case SUB_DOUBLE_2ADDR: case MUL_DOUBLE_2ADDR: case DIV_DOUBLE_2ADDR: case REM_DOUBLE_2ADDR:
				setRegisterUseType(inst, 0, DexType.DOUBLE);
				setRegisterUseType(inst, 1, DexType.DOUBLE);
				setRegisterDefType(inst, DexType.DOUBLE);
				break;

			case DOUBLE_TO_FLOAT:
				setRegisterUseType(inst, 1, DexType.DOUBLE);
				setRegisterDefType(inst, DexType.FLOAT);
				break;
			case DOUBLE_TO_INT:
				setRegisterUseType(inst, 1, DexType.DOUBLE);
				setRegisterDefType(inst, DexType.INT);
				break;
			case DOUBLE_TO_LONG:
				setRegisterUseType(inst, 1, DexType.DOUBLE);
				setRegisterDefType(inst, DexType.LONG);
				break;
			case FLOAT_TO_DOUBLE:
				setRegisterUseType(inst, 1, DexType.FLOAT);
				setRegisterDefType(inst, DexType.DOUBLE);
				break;
			case FLOAT_TO_INT:
				setRegisterUseType(inst, 1, DexType.FLOAT);
				setRegisterDefType(inst, DexType.INT);
				break;
			case FLOAT_TO_LONG:
				setRegisterUseType(inst, 1, DexType.FLOAT);
				setRegisterDefType(inst, DexType.LONG);
				break;
			case INT_TO_BYTE:
				setRegisterUseType(inst, 1, DexType.INT);
				setRegisterDefType(inst, DexType.BYTE);
				break;
			case INT_TO_CHAR:
				setRegisterUseType(inst, 1, DexType.INT);
				setRegisterDefType(inst, DexType.CHAR);
				break;
			case INT_TO_SHORT:
				setRegisterUseType(inst, 1, DexType.INT);
				setRegisterDefType(inst, DexType.SHORT);
				break;
			case INT_TO_DOUBLE: 
				setRegisterUseType(inst, 1, DexType.INT);
				setRegisterDefType(inst, DexType.DOUBLE);
				break;
			case INT_TO_FLOAT: 
				setRegisterUseType(inst, 1, DexType.INT);
				setRegisterDefType(inst, DexType.FLOAT);
				break;
			case INT_TO_LONG: 
				setRegisterUseType(inst, 1, DexType.INT);
				setRegisterDefType(inst, DexType.LONG);
				break;
			case LONG_TO_FLOAT: 
				setRegisterUseType(inst, 1, DexType.LONG);
				setRegisterDefType(inst, DexType.FLOAT);
				break;
			case LONG_TO_DOUBLE:
				setRegisterUseType(inst, 1, DexType.LONG);
				setRegisterDefType(inst, DexType.DOUBLE);
				break;
			case LONG_TO_INT:
				setRegisterUseType(inst, 1, DexType.LONG);
				setRegisterDefType(inst, DexType.INT);
				break;
			case NEW_INSTANCE: 
				setRegisterDefType(inst, inst.getTypeOperand());
				break;
			case NEW_ARRAY:
				setRegisterUseType(inst, 1, DexType.INT);
				setRegisterDefType(inst, inst.getTypeOperand());
				break;
			case AGET_WIDE:
				setRegisterUseType(inst, 2, DexType.INT);
				break;				
			case AGET:
				setRegisterUseType(inst, 2, DexType.INT);
				break;
			case AGET_OBJECT:
				setRegisterUseType(inst, 2, DexType.INT);
				setRegisterUseType(inst, 1, DexType.AOBJECT);
				setRegisterDefType(inst, DexType.OBJECT);
				break;
			case AGET_BOOLEAN: 
				setRegisterUseType(inst, 2, DexType.INT);
				setRegisterUseType(inst, 1, DexType.ABOOLEAN);
				setRegisterDefType(inst, DexType.BOOLEAN);
				break;
			case AGET_BYTE:
				setRegisterUseType(inst, 2, DexType.INT);
				setRegisterUseType(inst, 1, DexType.ABYTE);
				setRegisterDefType(inst, DexType.BYTE);
				break;
			case AGET_CHAR:
				setRegisterUseType(inst, 2, DexType.INT);
				setRegisterUseType(inst, 1, DexType.ACHAR);
				setRegisterDefType(inst, DexType.CHAR);
				break;
			case AGET_SHORT:
				setRegisterUseType(inst, 2, DexType.INT);
				setRegisterUseType(inst, 1, DexType.ASHORT);
				setRegisterDefType(inst, DexType.SHORT);
				break;
			case APUT_WIDE:
				setRegisterUseType(inst, 2, DexType.INT);
				break;
			case APUT: 
				setRegisterUseType(inst, 2, DexType.INT);
				break;
			case APUT_OBJECT:
				setRegisterUseType(inst, 2, DexType.INT);
				setRegisterUseType(inst, 1, DexType.AOBJECT);
				setRegisterUseType(inst, 0, DexType.OBJECT);
				break;
			case APUT_BOOLEAN: 
				setRegisterUseType(inst, 2, DexType.INT);
				setRegisterUseType(inst, 1, DexType.ABOOLEAN);
				setRegisterUseType(inst, 0, DexType.BOOLEAN);
				break;
			case APUT_BYTE:
				setRegisterUseType(inst, 2, DexType.INT);
				setRegisterUseType(inst, 1, DexType.ABYTE);
				setRegisterUseType(inst, 0, DexType.BYTE);
				break;
			case APUT_CHAR:
				setRegisterUseType(inst, 2, DexType.INT);
				setRegisterUseType(inst, 1, DexType.ACHAR);
				setRegisterUseType(inst, 0, DexType.CHAR);
				break;
			case APUT_SHORT:
				setRegisterUseType(inst, 2, DexType.INT);
				setRegisterUseType(inst, 1, DexType.ASHORT);
				setRegisterUseType(inst, 0, DexType.SHORT);
				break;
			case IGET: case IGET_WIDE: case IGET_OBJECT: case IGET_BOOLEAN:
			case IGET_BYTE: case IGET_CHAR: case IGET_SHORT:
				setRegisterUseType(inst, 1, new DexType(inst.getFieldOperand().getClassType()));
				setRegisterDefType(inst, new DexType(inst.getFieldOperand().getType()));
				break;
			case SGET: case SGET_WIDE: case SGET_OBJECT: case SGET_BOOLEAN:
			case SGET_BYTE: case SGET_CHAR: case SGET_SHORT:
				setRegisterDefType(inst, new DexType(inst.getFieldOperand().getType()));
				break;
			case IPUT: case IPUT_WIDE: case IPUT_OBJECT: case IPUT_BOOLEAN:
			case IPUT_BYTE: case IPUT_CHAR: case IPUT_SHORT:
				setRegisterUseType(inst, 0, new DexType(inst.getFieldOperand().getType()) );
				setRegisterUseType(inst, 1, new DexType(inst.getFieldOperand().getClassType()));
				break;
			case SPUT: case SPUT_WIDE: case SPUT_OBJECT: case SPUT_BOOLEAN:
			case SPUT_BYTE: case SPUT_CHAR: case SPUT_SHORT:
				setRegisterUseType(inst, 0, new DexType(inst.getFieldOperand().getType()) );
				break;
			case IF_LT: case IF_GT: case IF_LE: case IF_GE:
				setRegisterUseType(inst, 0, DexType.INT);
				setRegisterUseType(inst, 1, DexType.INT);
				break;
			case IF_LTZ: case IF_GEZ: case IF_GTZ: case IF_LEZ:
				setRegisterUseType(inst, 0, DexType.INT);
				break;
			case INVOKE_STATIC: case INVOKE_STATIC_RANGE:
				resultType = new DexType(inst.getMethodOperand().getReturnType());
				for( int i=0; i<inst.getNumRegisters(); i++ ) {
					setRegisterUseType(inst, i, new DexType(inst.getMethodOperand().getParamType(i)));
				}
				break;
			case INVOKE_VIRTUAL: case INVOKE_SUPER: case INVOKE_DIRECT:
			case INVOKE_INTERFACE: case INVOKE_VIRTUAL_RANGE: case INVOKE_SUPER_RANGE:
			case INVOKE_DIRECT_RANGE: case INVOKE_INTERFACE_RANGE:
				resultType = new DexType(inst.getMethodOperand().getReturnType());
				for( int i=0; i<inst.getNumRegisters(); i++ ) {
					setRegisterUseType(inst, i, new DexType(inst.getMethodOperand().getCallingParamType(i)));
				}
				break;
			case PACKED_SWITCH: case SPARSE_SWITCH:
				setRegisterUseType(inst, 0, DexType.INT);
				break;
			case RETURN: case RETURN_WIDE: case RETURN_OBJECT:
				setRegisterUseType(inst, 0, new DexType(method.getParent().getReturnType()));
				break;
			}
		}
	}
	
	/**
	 * Run through any left-over constants after the assignment, and declare 
	 * them to be simple integers of the appropriate size. (This happens with
	 * e.g. constants that are compared against other constants and not otherwise
	 * used).
	 */
	private void cleanupConstants( DexMethodBody method ) {
		for( Iterator<DexInstruction> ii = method.instIterator(); ii.hasNext(); ) {
			DexInstruction inst = ii.next();
			if( inst.isConstant() ) {
				DexType type = inst.getRegisterType(0);
				if( type == null ) {
					if( inst.isWideConstant() ) {
						setRegisterDefType(inst, DexType.LONG);
					} else {
						setRegisterDefType(inst, DexType.INT);
					}
				}
			}
		}
	}

}
