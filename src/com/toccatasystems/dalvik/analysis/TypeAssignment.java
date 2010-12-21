package com.toccatasystems.dalvik.analysis;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.toccatasystems.dalvik.DexBasicBlock;
import com.toccatasystems.dalvik.DexInstruction;
import com.toccatasystems.dalvik.DexMethod;
import com.toccatasystems.dalvik.DexMethodBody;
import com.toccatasystems.dalvik.DexType;
import com.toccatasystems.dalvik.ParseException;

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

public class TypeAssignment extends ForwardDataflowAnalysis<TypeAssignment.Params> {
	
	protected class Params {
		DexType resultType;
		DexType [] types;
		
		/**
		 * Initialize params for the function entry point - the last
		 * N registers contain the N function parameters, and the
		 * rest have no value (so we set to null to indicate unitialised).
		 * @param body
		 */
		public Params( DexMethodBody body ) {
			DexMethod method = body.getParent();
			int numRegs = body.getNumRegisters();
			types = new DexType[numRegs];
			int firstParamValue = numRegs - body.getInArgWords();
			/* Assign initial values */
			int i;
			for( i=0; i<firstParamValue; i++ )
				types[i] = DexType.VOID;
			
			if( !body.getParent().isStatic() ) {
				types[i++] = new DexType(body.getParent().getClassType());
			}
			for( int param = 0;param < method.getNumParamTypes(); i++, param++ ) {
				types[i] = new DexType(method.getParamType(param));
				if(types[i].isPrimDWord()) {
					i++;
					types[i] = new DexType("hiword");
				}
			}
			resultType = DexType.VOID;
		}
		
		public Params( Params in ) {
			this.types = new DexType[in.types.length];
			for( int i=0; i<types.length; i++ ) {
				this.types[i] = in.types[i];
			}
			this.resultType = in.resultType;
		}
		
		public DexType merge( DexType type1, DexType type2 ) {
			if( !type1.equals(type2) ) {
				if( type1.isObject() && type2.isObject() ) {
					return DexType.OBJECT;
				} else if( type1.isPolymorphic() ) {
					return type1;
				} else if( type2.isPolymorphic() ) {
					return type2;
				} else if( type1.isPrimWord() && type2.isPrimWord() ){
					return DexType.INT;
				} else {
					return DexType.VOID;
				}
			} else {
				return type1;
			}
		}
		
		public boolean equals( Object o ) {
			if( o instanceof Params ) {
				return equals((Params)o);
			} else {
				return false;
			}
		}
		
		public boolean equals( Params o ) {
			if( ! resultType.equals(o.resultType) ) {
				for( int i=0; i<types.length; i++ ) {
					if( !types[i].equals(o.types[i]) ) {
						return false;
					}
				}
			}
			return true;
		}
		
		/* Merge edges at basic block entry.
		 * 
		 * Note: it's essentially a verification requirement that any live
		 * variables must be consistent at merge points, although they need
		 * only have the same declared type rather than concrete type.
		 * 
		 * If there is a type mismatch, treating all object types as 'object',
		 * then we assume both registers are dead. For an object type
		 * mismatch, we have to assume the types are convertible, so we
		 * force the type to Ljava/lang/Object;
		 */
		public void merge( Params o ) {
			for( int i=0; i<types.length; i++ ) {
				types[i] = merge(types[i], o.types[i]);
			}
			resultType = merge(resultType, o.resultType);
		}
		
		public DexType getType( int idx ) {
			return types[idx];
		}
		
		public void setType( int idx, DexType result ) {
			types[idx] = result;
		}
		
		public DexType getResultType( ) {
			return resultType;
		}
		
		public void setResultType( DexType result ) {
			resultType = result;
		}
		
		public String toString() {
			StringBuilder builder = new StringBuilder("[");
			builder.append(resultType);
			builder.append(" | ");
			for( int i=0; i<types.length; i++ ) {
				if( i != 0 )
					builder.append(", ");
				builder.append(types[i]);
			}
			builder.append("]");
			return builder.toString();
		}
	}

	private DexType methodReturnType;

	public void analyse( DexMethodBody body ) {
		methodReturnType = new DexType(body.getParent().getReturnType());
		computeDataflow(body, new Params(body));
		// cleanupConstants(body);
	}

	@Override
	protected Params enterBlock(DexBasicBlock block, Map<DexBasicBlock,Params> params) {
		Iterator<Params> it = params.values().iterator();
		Params merge = new Params(it.next());
		while( it.hasNext() ) {
			merge.merge(it.next());
		}
		return merge;
	}
	
	private void propagateType( DexInstruction inst, int operand, DexType oldType, DexType newType ) {
		Set<DexInstruction> defs = inst.getRegisterDefs(operand);
		String oldName = oldType.getName();
		for( Iterator<DexInstruction> it = defs.iterator(); it.hasNext(); ) {
			DexInstruction def = it.next();
			if( def != inst ) {
				DexType defType = def.getRegisterType(0);
				if( defType == null ) {
					def.setRegisterType(0, newType);
				} else if( defType.getName().equals(oldName) ) {
					defType.setName(newType.getName());
				}
			}
		}
	}
	
	private void requireType( DexInstruction inst, int operand, DexType type ) throws ParseException {
		DexType current = inst.getRegisterType(operand);
		if( !current.equals(type) ) {
			if( type.getName().equals("const32") ) {
				if( !current.isPrimWord() && !current.isObject() ) {
					throw new ParseException("Type verification failure: Expected " + type + ", but was " + current);
				}
			} else if( type.getName().equals("const64") ) {
				if( !current.isPrimDWord() ) {
					throw new ParseException("Type verification failure: Expected " + type + ", but was " + current);
				}
			} else if( current.getName().equals("const32") ) {
				if( type.getName() != "D" && type.getName() != "J" && type.getName() != "V" ) {
					inst.setRegisterType(operand, type);
					propagateType(inst, operand, current, type);
				} else {
					throw new ParseException("Type verification failure: Expected " + type + ", but was " + current);
				}
			} else if( current.getName().equals("const64") ) {
				if( type.getName() == "D" || type.getName() == "J" ) {
					inst.setRegisterType(operand, type);
					propagateType(inst, operand, current, type);
				} else {
					throw new ParseException("Type verification failure: Expected " + type + ", but was " + current);
				}
			} else if( current.isObject() && type.isObject() ) {
				if( current.equals(DexType.OBJECT) ) {
					/* Promote to more specific type */
					inst.setRegisterType(operand, type);
				}
			} else if( current.isPrimWord() && type.isPrimWord() ) {
				inst.setRegisterType(operand, type);
			} else if( current.isPrimDWord() && type.isPrimDWord() ) {
				inst.setRegisterType(operand, type);
			} else {
				throw new ParseException("Type verification failure: Expected " + type + ", but was " + current);
			} 
		}
	}

	@Override
	protected Params visit(DexInstruction inst, Params param) {
		try {
			/* initialize all registers from the dataflow */
			DexType origOutType = null;
			if( inst.getNumRegisters() > 0 ) {
				origOutType = inst.getRegisterType(0);
			}
			for( int i=0; i<inst.getNumRegisters(); i++ ) {
				inst.setRegisterType( i, param.getType(inst.getRegister(i)) );
			}

			/* Compute result register type where there is one */
			int outreg = 0;
			DexType outtype = null;
			switch( inst.getOpcode() ) {
			case MOVE: case MOVE_FROM16: case MOVE_16: case MOVE_OBJECT: case MOVE_OBJECT16: case MOVE_OBJECT_FROM16: 
				outtype = inst.getRegisterType(1);
				break;
			case MOVE_WIDE: case MOVE_WIDE16: case MOVE_WIDE_FROM16:
				outtype = inst.getRegisterType(1);
				/* FIXME */
				break;
			case MOVE_RESULT: case MOVE_RESULT_WIDE: case MOVE_RESULT_OBJECT:
				outtype = param.getResultType();
				break;
			case MOVE_EXCEPTION:
				outtype = DexType.THROWABLE;
				break;
			case CONST: case CONST4: case CONST16: case CONST_HIGH16:
				outtype = origOutType == null ? new DexType("const32") : origOutType;
				break;
			case CONST_WIDE: case CONST_WIDE16: case CONST_WIDE32: case CONST_WIDE_HIGH16:
				outtype = origOutType == null ? new DexType("const64") : origOutType;
				break;
			case CONST_STRING: case CONST_STRING_JUMBO:
				outtype = DexType.STRING;
				break;
			case CONST_CLASS:
				outtype = DexType.CLASS;
				break;
			case INSTANCE_OF: case ARRAY_LENGTH: 
				outtype = DexType.INT;
				break;
			case CMPL_FLOAT: case CMPG_FLOAT:
				outtype = DexType.INT;
				requireType(inst, 1, DexType.FLOAT);
				requireType(inst, 2, DexType.FLOAT);
				break;
			case CMPL_DOUBLE: case CMPG_DOUBLE: 
				outtype = DexType.INT;
				requireType(inst, 1, DexType.DOUBLE);
				requireType(inst, 2, DexType.DOUBLE);
				break;
			case CMP_LONG:
				outtype = DexType.INT;
				requireType(inst, 1, DexType.LONG);
				requireType(inst, 2, DexType.LONG);
				break;

			case NEG_INT: case NOT_INT: 
			case ADD_INT_LIT16: case RSUB_INT_LIT16: case MUL_INT_LIT16: case DIV_INT_LIT16:
			case REM_INT_LIT16: case AND_INT_LIT16: case OR_INT_LIT16: case XOR_INT_LIT16:
			case ADD_INT_LIT8: case RSUB_INT_LIT8: case MUL_INT_LIT8: case DIV_INT_LIT8:
			case REM_INT_LIT8: case AND_INT_LIT8: case OR_INT_LIT8: case XOR_INT_LIT8:
			case SHL_INT_LIT8: case SHR_INT_LIT8: case USHR_INT_LIT8:
				outtype = DexType.INT;
				requireType(inst, 1, DexType.INT);
				break;
			case ADD_INT: case SUB_INT: case MUL_INT: case DIV_INT: case REM_INT: case AND_INT: 
			case OR_INT: case XOR_INT: case SHL_INT: case SHR_INT: case USHR_INT: 
				outtype = DexType.INT;
				requireType(inst, 1, DexType.INT);
				requireType(inst, 2, DexType.INT);
				break;
			case ADD_INT_2ADDR: case SUB_INT_2ADDR: case MUL_INT_2ADDR: case DIV_INT_2ADDR: case REM_INT_2ADDR: case AND_INT_2ADDR: 
			case OR_INT_2ADDR: case XOR_INT_2ADDR: case SHL_INT_2ADDR: case SHR_INT_2ADDR: case USHR_INT_2ADDR: 
				outtype = DexType.INT;
				requireType(inst, 0, DexType.INT);
				requireType(inst, 1, DexType.INT);
				break;

			case NEG_LONG: case NOT_LONG: 
				outtype = DexType.LONG;
				requireType(inst, 1, DexType.LONG);
				break;
			case ADD_LONG: case SUB_LONG: case MUL_LONG: case DIV_LONG: case REM_LONG: case AND_LONG:
			case OR_LONG: case XOR_LONG: 
				outtype = DexType.LONG;
				requireType(inst, 1, DexType.LONG);
				requireType(inst, 2, DexType.LONG);
				break;
			case SHL_LONG: case SHR_LONG: case USHR_LONG:
				outtype = DexType.LONG;
				requireType(inst, 1, DexType.LONG);
				requireType(inst, 2, DexType.INT);
				break;
			case ADD_LONG_2ADDR: case SUB_LONG_2ADDR: case MUL_LONG_2ADDR: case DIV_LONG_2ADDR: case REM_LONG_2ADDR: case AND_LONG_2ADDR: 
			case OR_LONG_2ADDR: case XOR_LONG_2ADDR: 
				outtype = DexType.LONG;
				requireType(inst, 0, DexType.LONG);
				requireType(inst, 1, DexType.LONG);
				break;
			case SHL_LONG_2ADDR: case SHR_LONG_2ADDR: case USHR_LONG_2ADDR: 
				outtype = DexType.LONG;
				requireType(inst, 0, DexType.LONG);
				requireType(inst, 1, DexType.INT);
				break;
			case NEG_FLOAT: 
			case ADD_FLOAT: case SUB_FLOAT: case MUL_FLOAT: case DIV_FLOAT: case REM_FLOAT:
				outtype = DexType.FLOAT;
				requireType(inst, 1, DexType.FLOAT);
				requireType(inst, 2, DexType.FLOAT);
				break;
			case ADD_FLOAT_2ADDR: case SUB_FLOAT_2ADDR: case MUL_FLOAT_2ADDR: case DIV_FLOAT_2ADDR: case REM_FLOAT_2ADDR:
				outtype = DexType.FLOAT;
				requireType(inst, 0, DexType.FLOAT);
				requireType(inst, 1, DexType.FLOAT);
				break;
			case NEG_DOUBLE: 
			case ADD_DOUBLE: case SUB_DOUBLE: case MUL_DOUBLE: case DIV_DOUBLE: case REM_DOUBLE:
				outtype = DexType.DOUBLE;
				requireType(inst, 1, DexType.DOUBLE);
				requireType(inst, 2, DexType.DOUBLE);
				break;
			case ADD_DOUBLE_2ADDR: case SUB_DOUBLE_2ADDR: case MUL_DOUBLE_2ADDR: case DIV_DOUBLE_2ADDR: case REM_DOUBLE_2ADDR:
				outtype = DexType.DOUBLE;
				requireType(inst, 0, DexType.DOUBLE);
				requireType(inst, 1, DexType.DOUBLE);
				break;

			case DOUBLE_TO_FLOAT:
				requireType(inst, 1, DexType.DOUBLE);
				outtype = DexType.FLOAT;
				break;
			case DOUBLE_TO_INT:
				requireType(inst, 1, DexType.DOUBLE);
				outtype = DexType.INT;
				break;
			case DOUBLE_TO_LONG:
				requireType(inst, 1, DexType.DOUBLE);
				outtype = DexType.LONG;
				break;
			case FLOAT_TO_DOUBLE:
				requireType(inst, 1, DexType.FLOAT);
				outtype = DexType.DOUBLE;
				break;
			case FLOAT_TO_INT:
				requireType(inst, 1, DexType.FLOAT);
				outtype = DexType.INT;
				break;
			case FLOAT_TO_LONG:
				requireType(inst, 1, DexType.FLOAT);
				outtype = DexType.LONG;
				break;
			case INT_TO_BYTE:
				requireType(inst, 1, DexType.INT);
				outtype = DexType.BYTE;
				break;
			case INT_TO_CHAR:
				requireType(inst, 1, DexType.INT);
				outtype = DexType.CHAR;
				break;
			case INT_TO_SHORT:
				requireType(inst, 1, DexType.INT);
				outtype = DexType.INT;
				break;
			case INT_TO_DOUBLE: 
				requireType(inst, 1, DexType.INT);
				outtype = DexType.DOUBLE;
				break;
			case INT_TO_FLOAT: 
				requireType(inst, 1, DexType.INT);
				outtype = DexType.FLOAT;
				break;
			case INT_TO_LONG: 
				requireType(inst, 1, DexType.INT);
				outtype = DexType.LONG;
				break;
			case LONG_TO_FLOAT: 
				requireType(inst, 1, DexType.LONG);
				outtype = DexType.FLOAT;
				break;
			case LONG_TO_DOUBLE:
				requireType(inst, 1, DexType.LONG);
				outtype = DexType.DOUBLE;
				break;
			case LONG_TO_INT:
				requireType(inst, 1, DexType.LONG);
				outtype = DexType.INT;
				break;
			case NEW_INSTANCE: 
				outtype = inst.getTypeOperand();
				break;
			case NEW_ARRAY:
				requireType(inst, 1, DexType.INT);
				outtype = inst.getTypeOperand();
				break;
			case AGET_WIDE:
				requireType(inst, 2, DexType.INT);
				outtype = inst.getRegisterType(1).getElementType("const64");
				break;				
			case AGET:
				requireType(inst, 2, DexType.INT);
				outtype = inst.getRegisterType(1).getElementType("const32");
				break;
			case AGET_OBJECT:
				requireType(inst, 2, DexType.INT);
				requireType(inst, 1, DexType.AOBJECT);
				outtype = inst.getRegisterType(1).getElementType(DexType.OBJECT);
				break;
			case AGET_BOOLEAN: 
				requireType(inst, 2, DexType.INT);
				requireType(inst, 1, DexType.ABOOLEAN);
				outtype = inst.getRegisterType(1).getElementType(DexType.BOOLEAN);
				break;
			case AGET_BYTE:
				requireType(inst, 2, DexType.INT);
				requireType(inst, 1, DexType.ABYTE);
				outtype = inst.getRegisterType(1).getElementType(DexType.BYTE);
				break;
			case AGET_CHAR:
				requireType(inst, 2, DexType.INT);
				requireType(inst, 1, DexType.ACHAR);
				outtype = inst.getRegisterType(1).getElementType(DexType.CHAR);
				break;
			case AGET_SHORT:
				requireType(inst, 2, DexType.INT);
				requireType(inst, 1, DexType.ASHORT);;
				outtype = inst.getRegisterType(1).getElementType(DexType.SHORT);
				break;
			case APUT_WIDE:
				requireType(inst, 2, DexType.INT);
				requireType(inst, 0, inst.getRegisterType(1).getElementType("const64") );
				break;
			case APUT: 
				requireType(inst, 2, DexType.INT);
				requireType(inst, 0, inst.getRegisterType(1).getElementType("const32") );
				break;
			case APUT_OBJECT:
				requireType(inst, 2, DexType.INT);
				requireType(inst, 1, DexType.AOBJECT);
				requireType(inst, 0, inst.getRegisterType(1).getElementType(DexType.OBJECT) );
				break;
			case APUT_BOOLEAN: 
				requireType(inst, 2, DexType.INT);
				requireType(inst, 1, DexType.ABOOLEAN);
				requireType(inst, 0, inst.getRegisterType(1).getElementType(DexType.BOOLEAN) );
				break;
			case APUT_BYTE:
				requireType(inst, 2, DexType.INT);
				requireType(inst, 1, DexType.ABYTE);
				requireType(inst, 0, inst.getRegisterType(1).getElementType(DexType.BYTE) );
				break;
			case APUT_CHAR:
				requireType(inst, 2, DexType.INT);
				requireType(inst, 1, DexType.ACHAR);
				requireType(inst, 0, inst.getRegisterType(1).getElementType(DexType.CHAR) );
				break;
			case APUT_SHORT:
				requireType(inst, 2, DexType.INT);
				requireType(inst, 1, DexType.ASHORT);
				requireType(inst, 0, inst.getRegisterType(1).getElementType(DexType.SHORT) );
				break;
			case IGET: case IGET_WIDE: case IGET_OBJECT: case IGET_BOOLEAN:
			case IGET_BYTE: case IGET_CHAR: case IGET_SHORT:
				requireType(inst, 1, new DexType(inst.getFieldOperand().getClassType()));
				outtype = new DexType(inst.getFieldOperand().getType());
				break;
			case SGET: case SGET_WIDE: case SGET_OBJECT: case SGET_BOOLEAN:
			case SGET_BYTE: case SGET_CHAR: case SGET_SHORT:
				outtype = new DexType(inst.getFieldOperand().getType());
				break;
			case IPUT: case IPUT_WIDE: case IPUT_OBJECT: case IPUT_BOOLEAN:
			case IPUT_BYTE: case IPUT_CHAR: case IPUT_SHORT:
				requireType(inst, 0, new DexType(inst.getFieldOperand().getType()) );
				requireType(inst, 1, new DexType(inst.getFieldOperand().getClassType()));
				break;
			case SPUT: case SPUT_WIDE: case SPUT_OBJECT: case SPUT_BOOLEAN:
			case SPUT_BYTE: case SPUT_CHAR: case SPUT_SHORT:
				requireType(inst, 0, new DexType(inst.getFieldOperand().getType()) );
				break;
			case IF_LT: case IF_GT: case IF_LE: case IF_GE:
				requireType(inst, 0, DexType.INT);
				requireType(inst, 1, DexType.INT);
				break;
			case IF_EQ: case IF_NE:
				/* Either both are object references or both are integers. */
				if( inst.getRegisterType(0).isPolymorphic() ) {
					requireType(inst, 0, inst.getRegisterType(1));
				} else if( inst.getRegisterType(1).isPolymorphic() ) {
					requireType(inst, 1, inst.getRegisterType(0));
				}
				break;
			case IF_EQZ: case IF_NEZ:
				/* if-eqz and if-nez can take an object reference or an integer. */
				if( inst.getRegisterType(0).isPrimitive() ) {
					requireType(inst, 0, DexType.INT);
				}
				break;
			case IF_LTZ: case IF_GEZ: case IF_GTZ: case IF_LEZ:
				requireType(inst, 0, DexType.INT);
				break;
			case INVOKE_STATIC: case INVOKE_STATIC_RANGE:
				inst.fixInvokeRegisters();
				param.setResultType( new DexType(inst.getMethodOperand().getReturnType()) );
				for( int i=0; i<inst.getNumRegisters(); i++ ) {
					requireType(inst, i, new DexType(inst.getMethodOperand().getParamType(i)));
				}
				break;
			case INVOKE_VIRTUAL: case INVOKE_SUPER: case INVOKE_DIRECT:
			case INVOKE_INTERFACE: case INVOKE_VIRTUAL_RANGE: case INVOKE_SUPER_RANGE:
			case INVOKE_DIRECT_RANGE: case INVOKE_INTERFACE_RANGE:
				inst.fixInvokeRegisters();
				param.setResultType( new DexType(inst.getMethodOperand().getReturnType()) );
				for( int i=0; i<inst.getNumRegisters(); i++ ) {
					requireType(inst, i, new DexType(inst.getMethodOperand().getCallingParamType(i)));
				}
				break;
			case PACKED_SWITCH: case SPARSE_SWITCH:
				requireType(inst, 0, DexType.INT);
				break;
			case RETURN: case RETURN_WIDE: case RETURN_OBJECT:
				requireType(inst, 0, methodReturnType);
				break;
			}

			if( outtype != null ) {
				param.setType(inst.getRegister(outreg), outtype);
				inst.setRegisterType(outreg, outtype);
			}
			return param;
		} catch( ParseException e ) {
			System.err.println( "Type assignment failed for " + inst.getHexPC() + " " + inst.disassemble() + ": " + e.getMessage() );
			DexMethodBody body = inst.getParent().getParent();
			DexMethod method = body.getParent();
			System.err.println( "In: " + method.getDisplaySignature() );
			body.disassemble(System.err, true);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Run through any left-over constants after the dataflow pass, and assign 
	 * them to simple integers of the appropriate size.
	 */
	private void cleanupConstants( DexMethodBody method ) {
		for( Iterator<DexInstruction> ii = method.instIterator(); ii.hasNext(); ) {
			DexInstruction inst = ii.next();
			if( inst.isConstant() ) {
				DexType type = inst.getRegisterType(0);
				if( type.getName().equals("const32") ) {
					type.setName("I");
				} else if( type.getName().equals("const64") ) {
					type.setName("J");
				}
			}
		}
	}
}
