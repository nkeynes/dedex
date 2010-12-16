package com.toccatasystems.dalvik;

import java.util.Formatter;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import static com.toccatasystems.dalvik.DexOpcodes.*;

/**
 * Class representing a single dalvik instruction, which has an opcode, 
 * 0 or more register operands, and 0 or 1 non-register operands. 
 * 
 * Based on documents at
 *   http://www.netmite.com/android/mydroid/dalvik/docs/instruction-formats.html and
 *   http://www.netmite.com/android/mydroid/dalvik/docs/dalvik-bytecode.html
 */

public class DexInstruction {
	
	private final static int OPTYPE_NONE = 0;
	private final static int OPTYPE_INT = 5;
	private final static int OPTYPE_TARGET = 10;
	private final static int OPTYPE_STRING = 11;
	private final static int OPTYPE_TYPE = 12;
	private final static int OPTYPE_FIELD = 13;
	private final static int OPTYPE_METHOD = 14;
	

	

	/* Addressing modes. The names are per Dalvik documentation, such that
	 *   First digit is the number of 16-bit words used by the instruction
	 *   Second digit is the number of registers encoded (R = range)
	 *   Third letter is an operand type:
	 *   1   B = immediate signed byte
	 *   2   H64 = immediate signed high-order 16-bits of a 64-bit value
	 *   3   F = interface constant (statically linked only)
	 *   4   H = immediate signed high-order 16-bits of a 32-bit value
	 *   5   I = immediate signed int or float
	 *   6   L = immediate signed long or double
	 *   7   M = method constant (statically linked only)
	 *   8   N = immediate signed nibble
	 *   9   S = immediate signed short
	 *   A   T = branch target
	 *   0   X = none.
	 *   B     = String index
	 *   C     = Type index
	 *   D     = Field index
	 *   E     = Method index
	 *   F     = 
	 */
	private final static int M_DATA = 0x1FF; /* data rather than instruction */
	private final static int M_10T = 0x10A; /* AA|op  : op +AA */
	private final static int M_10X = 0x100; /* 00|op  : op */
	private final static int M_11N = 0x118; /* B|A|op : op vA, #+B */
	private final static int M_11X = 0x110; /* AA|op  : op vAA */
	private final static int M_12X = 0x120; /* B|A|op : op vA, vB */
	private final static int M_20T = 0x20A; /* 00|op AAAA : op +AAAA */
	private final static int M_21Cstring = 0x21B; /* AA|op BBBB : op vAA, string@BBBB */
	private final static int M_21Ctype   = 0x21C; /* AA|op BBBB : op vAA, type@BBBB */
	private final static int M_21Cfield  = 0x21D; /* AA|op BBBB : op vAA, field@BBBB */
	private final static int M_21H = 0x214; /* AA|op BBBB : op vAA, #+BBBB0000(00000000) */
	private final static int M_21H64 = 0x212;
	private final static int M_21S = 0x219; /* AA|op BBBB : op vAA, #+BBBB */
	private final static int M_21T = 0x21A; /* AA|op BBBB : op vAA, +BBBB */
	private final static int M_22B = 0x221; /* AA|op CC|BB : op vAA, vBB, #+CC */
	private final static int M_22Ctype = 0x22C; /* B|A|op CCCC : op vA, vB, type@CCCC */
	private final static int M_22Cfield = 0x22D; /* B|A|op CCCC : op vA, vB, field@CCCC */
	private final static int M_22S = 0x229; /* B|A|op CCCC : op vA, vB, #+CCCC */
	private final static int M_22T = 0x22A; /* B|A|op CCCC : op vA, vB, +CCCC */
	private final static int M_22X = 0x220; /* AA|op BBBB : op vAA, vBBBB */
	private final static int M_23X = 0x230; /* AA|op CC|BB : op vAA, vBB, vCC */
	private final static int M_30T = 0x30A; /* 00|op AAAl AAAAh : op +AAAAAAAA */
	private final static int M_31Cstring = 0x31B; /* AA|op BBBBl BBBBh : op vAA, string@BBBBBBBB */
	private final static int M_31I = 0x315; /* AA|op BBBBl BBBBh : op vAA, #+BBBBBBBB */
	private final static int M_31T = 0x31A; /* AA|op BBBBl BBBBh : op vAA, +BBBBBBBB */
	private final static int M_32X = 0x320; /* 00|op AAAA BBBB  : op vAAAA, vBBBB */
	private final static int M_35Ctype = 0x35C; /* B|A|op CCCC G|F|E|D : [B=count] op {vD, vE, vF, vG, vA}, type@CCCC */
	private final static int M_35Cmethod = 0x35E; /* B|A|op CCCC G|F|E|D : [B=count] op {vD, vE, vF, vG, vA}, method@CCCC */
	private final static int M_3RCtype = 0x3FC; /* AA|op BBBB CCCC : op {vCCCC .. vNNNN}, type@BBBB */
	private final static int M_3RCmethod = 0x3FE; /* AA|op BBBB CCCC : op {vCCCC .. vNNNN}, method@BBBB */
	private final static int M_51L = 0x516; /* AA|op BBBBl BBBB BBBB BBBBh : op vAA, #+BBBBBBBBBBBBBBBB */

	/* Forms that should never appear in unlinked .dex files */
	private final static int M_22CS = 0x1222; /* B|A|op CCCC : op vA, vB, fieldoff@CCCC */
	private final static int M_35MS = 0x1357; /* B|A|op CCCC G|F|E|D : [B=count] op {vD, vE, vF, vG, vA}, vtaboff@CCCC */
	private final static int M_35FS = 0x1353; /* B|A|op DDCC H|G|F|E : [B=count] op vD, {vE, vF, vG, vH, vA}, (vtaboff|iface)@CCCC */
	private final static int M_3RFS = 0x13F3; /* AA|op CCBB DDDD : op {vDDDD .. vNNNN}, (vtaboff|iface)@CC */
	private final static int M_3RMS = 0x13F7; /* AA|op BBBB CCCC : op {vCCCC .. vNNNN}, vtaboff@BBBB */

	private static class I {
		public String mnemonic;
		public int mode;
		public DexType[] mayThrowTypes;
		
		public I(String mnemonic, int mode) {
			this.mnemonic = mnemonic;
			this.mode = mode;
		}
		
		public I(String mnemonic, int mode, DexType[] mayThrowTypes ) {
			this.mnemonic = mnemonic;
			this.mode = mode;
			this.mayThrowTypes = mayThrowTypes;
		}
		
		public String getMnemonic() {
			return mnemonic;
		}
		
		public int getNumWords() {
			return (this.mode & 0xF00) >> 8;
		}
		public int getNumRegisters() {
			return (this.mode & 0x0F0) >> 4;
		}
		
		public int getMode() {
			return this.mode;
		}
		
		public boolean mayThrow() {
			return mayThrowTypes != null;
		}
		
		public DexType[] getThrows() {
			return mayThrowTypes;
		}
		
		public int getOperandType() {
			int type = (this.mode & 0x00F); 
			if( type > OPTYPE_NONE && type < OPTYPE_TARGET )
				return OPTYPE_INT;
			else return type;
		}
		
	}

	private final static DexType[] THROWS_ANYTHING = { };
	private final static DexType[] THROWS_NULL_PTR = { DexType.NULL_POINTER_EXCEPTION };
	private final static DexType[] THROWS_CLASS_CAST = { DexType.CLASS_CAST_EXCEPTION };
	private final static DexType[] MONITOR_EXIT_THROWS = { DexType.NULL_POINTER_EXCEPTION, DexType.ILLEGAL_MONITOR_STATE_EXCEPTION };
	/**
	 * Core instruction table mapping opcode to name + operand form
	 */
	private final static I OPCODES[] = {
		/* 00 */
		new I("nop", M_10X), new I("move", M_12X), new I("move/from16", M_22X), new I("move/16", M_32X),
		new I("move-wide", M_12X), new I("move-wide/from16", M_22X), new I("move-wide/16", M_32X), new I("move-object", M_12X),
		new I("move-object/from16", M_22X), new I("move-object/16", M_32X), new I("move-result", M_11X), new I("move-result-wide", M_11X),
		new I("move-result-object", M_11X), new I("move-exception", M_11X),	new I("return-void", M_10X), new I("return", M_11X),
		/* 10 */
		new I("return-wide", M_11X), new I("return-object", M_11X),	new I("const/4", M_11N), new I("const/16", M_21S),
		new I("const", M_31I), new I("const/high16", M_21H), new I("const-wide/16", M_21S), new I("const-wide/32", M_31I),
		new I("const-wide", M_51L), new I("const-wide/high16", M_21H64), new I("const-string", M_21Cstring), new I("const-string/jumbo", M_31Cstring),
		new I("const-class", M_21Ctype), new I("monitor-enter", M_11X, THROWS_NULL_PTR), new I("monitor-exit", M_11X, MONITOR_EXIT_THROWS), new I("check-cast", M_21Ctype, THROWS_CLASS_CAST),
		/* 20 */
		new I("instance-of", M_22Ctype), new I("array-length", M_12X), new I("new-instance", M_21Ctype), new I("new-array", M_22Ctype),
		new I("filled-new-array", M_35Ctype), new I("filled-new-array/range", M_3RCtype), new I("fill-array-data", M_31T), new I("throw", M_11X),
		new I("goto", M_10T), new I("goto/16", M_20T), new I("goto/32", M_30T), new I("packed-switch", M_31T),
		new I("sparse-switch", M_31T), new I("cmpl-float", M_23X), new I("cmpg-float", M_23X), new I("cmpl-double", M_23X),
		/* 30 */
		new I("cmpg-double", M_23X), new I("cmp-long", M_23X), new I("if-eq", M_22T), new I("if-ne", M_22T),
		new I("if-lt", M_22T), new I("if-ge", M_22T), new I("if-gt", M_22T), new I("if-le", M_22T),
		new I("if-eqz", M_21T), new I("if-nez", M_21T), new I("if-ltz", M_21T), new I("if-gez", M_21T),
		new I("if-gtz", M_21T), new I("if-lez", M_21T), new I("undef-3e", M_10X), new I("undef-3f", M_10X),
		/* 40 */
		new I("undef-40", M_10X), new I("undef-41", M_10X), new I("undef-42", M_10X), new I("undef-43", M_10X),
		new I("aget", M_23X), new I("aget-wide", M_23X), new I("aget-object", M_23X), new I("aget-boolean", M_23X),
		new I("aget-byte", M_23X), new I("aget-char", M_23X), new I("aget-short", M_23X), new I("aput", M_23X),
		new I("aput-wide", M_23X), new I("aput-object", M_23X), new I("aput-boolean", M_23X), new I("aput-byte", M_23X),
		/* 50 */
		new I("aput-char", M_23X), new I("aput-short", M_23X), new I("iget", M_22Cfield), new I("iget-wide", M_22Cfield),
		new I("iget-object", M_22Cfield), new I("iget-boolean", M_22Cfield), new I("iget-byte", M_22Cfield), new I("iget-char", M_22Cfield),
		new I("iget-short", M_22Cfield), new I("iput", M_22Cfield), new I("iput-wide", M_22Cfield), new I("iput-object", M_22Cfield),
		new I("iput-boolean", M_22Cfield), new I("iput-byte", M_22Cfield), new I("iput-char", M_22Cfield), new I("iput-short", M_22Cfield),
		/* 60 */
		new I("sget", M_21Cfield), new I("sget-wide", M_21Cfield), new I("sget-object", M_21Cfield), new I("sget-boolean", M_21Cfield), 
		new I("sget-byte", M_21Cfield), new I("sget-char", M_21Cfield), new I("sget-short", M_21Cfield), new I("sput", M_21Cfield), 
		new I("sput-wide", M_21Cfield), new I("sput-object", M_21Cfield), new I("sput-boolean", M_21Cfield), new I("sput-byte", M_21Cfield), 
		new I("sput-char", M_21Cfield), new I("sput-short", M_21Cfield), new I("invoke-virtual", M_35Cmethod), new I("invoke-super", M_35Cmethod),
		/* 70 */
		new I("invoke-direct", M_35Cmethod), new I("invoke-static", M_35Cmethod), new I("invoke-interface", M_35Cmethod), new I("undef-73", M_10X),
		new I("invoke-virtual/range", M_3RCmethod), new I("invoke-super/range", M_3RCmethod), new I("invoke-direct/range", M_3RCmethod), new I("invoke-static/range", M_3RCmethod),
		new I("invoke-interface/range", M_3RCmethod), new I("undef-79", M_10X), new I("undef-7a", M_10X), new I("neg-int", M_12X),
		new I("not-int", M_12X), new I("neg-long", M_12X), new I("not-long", M_12X), new I("neg-float", M_12X),
		/* 80 */
		new I("neg-double", M_12X), new I("int-to-long", M_12X), new I("int-to-float", M_12X), new I("int-to-double", M_12X),
		new I("long-to-int", M_12X), new I("long-to-float", M_12X), new I("long-to-double", M_12X), new I("float-to-int", M_12X),
		new I("float-to-long", M_12X), new I("float-to-double", M_12X), new I("double-to-int", M_12X), new I("double-to-long", M_12X), 
		new I("double-to-float", M_12X), new I("int-to-byte", M_12X), new I("int-to-char", M_12X), new I("int-to-short", M_12X),
		/* 90 */
		new I("add-int", M_23X), new I("sub-int", M_23X), new I("mul-int", M_23X), new I("div-int", M_23X),
		new I("rem-int", M_23X), new I("and-int", M_23X), new I("or-int", M_23X), new I("xor-int", M_23X), 
		new I("shl-int", M_23X), new I("shr-int", M_23X), new I("ushr-int", M_23X), new I("add-long", M_23X), 
		new I("sub-long", M_23X), new I("mul-long", M_23X), new I("div-long", M_23X), new I("rem-long", M_23X), 
		/* A0 */
		new I("and-long", M_23X), new I("or-long", M_23X), new I("xor-long", M_23X), new I("shl-long", M_23X), 
		new I("shr-long", M_23X), new I("ushr-long", M_23X), new I("add-float", M_23X), new I("sub-float", M_23X), 
		new I("mul-float", M_23X), new I("div-float", M_23X), new I("rem-float", M_23X), new I("add-double", M_23X), 
		new I("sub-double", M_23X), new I("mul-double", M_23X), new I("div-double", M_23X), new I("rem-double", M_23X), 
		/* B0 */
		new I("add-int/2addr", M_12X), new I("sub-int/2addr", M_12X), new I("mul-int/2addr", M_12X), new I("div-int/2addr", M_12X),
		new I("rem-int/2addr", M_12X), new I("and-int/2addr", M_12X), new I("or-int/2addr", M_12X), new I("xor-int/2addr", M_12X), 
		new I("shl-int/2addr", M_12X), new I("shr-int/2addr", M_12X), new I("ushr-int/2addr", M_12X), new I("add-long/2addr", M_12X), 
		new I("sub-long/2addr", M_12X), new I("mul-long/2addr", M_12X), new I("div-long/2addr", M_12X), new I("rem-long/2addr", M_12X), 
		/* C0 */
		new I("and-long/2addr", M_12X), new I("or-long/2addr", M_12X), new I("xor-long/2addr", M_12X), new I("shl-long/2addr", M_12X), 
		new I("shr-long/2addr", M_12X), new I("ushr-long/2addr", M_12X), new I("add-float/2addr", M_12X), new I("sub-float/2addr", M_12X), 
		new I("mul-float/2addr", M_12X), new I("div-float/2addr", M_12X), new I("rem-float/2addr", M_12X), new I("add-double/2addr", M_12X), 
		new I("sub-double/2addr", M_12X), new I("mul-double/2addr", M_12X), new I("div-double/2addr", M_12X), new I("rem-double/2addr", M_12X),
		/* D0 */
		new I("add-int/lit16", M_22S), new I("rsub-int", M_22S), new I("mul-int/lit16", M_22S), new I("div-int/lit16", M_22S), 
		new I("rem-int/lit16", M_22S), new I("and-int/lit16", M_22S), new I("or-int/lit16", M_22S), new I("xor-int/lit16", M_22S), 
		new I("add-int/lit8", M_22B), new I("rsub-int/lit8", M_22B), new I("mul-int/lit8", M_22B), new I("div-int/lit8", M_22B), 
		new I("rem-int/lit8", M_22B), new I("and-int/lit8", M_22B), new I("or-int/lit8", M_22B), new I("xor-int/lit8", M_22B),
		/* E0 */
		new I("shl-int/lit8", M_22B), new I("shr-int/lit8", M_22B), new I("ushr-int/lit8", M_22B), new I("undef-e3", M_10X), 
		new I("undef-e4", M_10X), new I("undef-e5", M_10X), new I("undef-e6", M_10X), new I("undef-e7", M_10X), 
		new I("undef-e8", M_10X), new I("undef-e9", M_10X), new I("undef-ea", M_10X), new I("undef-eb", M_10X), 
		new I("undef-ec", M_10X), new I("undef-ed", M_10X), new I("undef-ee", M_10X), new I("undef-ef", M_10X), 
		/* F0 */
		new I("undef-f0", M_10X), new I("undef-f1", M_10X), new I("undef-f2", M_10X), new I("undef-f3", M_10X), 
		new I("undef-f4", M_10X), new I("undef-f5", M_10X), new I("undef-f6", M_10X), new I("undef-f7", M_10X), 
		new I("undef-f8", M_10X), new I("undef-f9", M_10X), new I("undef-fa", M_10X), new I("undef-fb", M_10X), 
		new I("undef-fc", M_10X), new I("undef-fd", M_10X), new I("undef-fe", M_10X), new I("undef-ff", M_10X) 
	};
	
	private final static I NOPCODES[] = {
		new I("nop", M_10X), new I("packed-switch-table", M_DATA), new I("sparse-switch-table", M_DATA), new I("file-array-data-table", M_DATA)
	};
	
	private final static int highByte( short word ) {
		return ((int)word >> 8) & 0xFF;
	}
	private final static int highNibble( short word ) {
		return ((int)word >> 12) & 0x0F;
	}
	private final static int sextHighNibble( short word ) {
		return ((int)word >> 12);
	}
	private final static int thirdNibble( short word ) {
		return ((int)word >> 8) & 0x0F;
	}
	private final static int signed32( short word1, short word2 ) {
		return (((int)word1) & 0xFFFF) | (((int)word2) << 16);
	}
	
	private DexMethodBody method;
	private DexBasicBlock parent;
	private I instruction;
	private int pc;
	private short[]code;
	private int registers[];
	private long constOperand;
	private DexType[]registerTypes;
	
	
	/**
	 * Construct a new DexInstruction from the given position in the method body
	 * @param data
	 * @param posn
	 */
	public DexInstruction( DexMethodBody body, int posn ) {
		this.method = body;
		this.pc = posn;
		short code[] = body.getCode();
		int opcode = code[posn] & 0xFF;
		
		instruction = OPCODES[opcode];
		if( opcode == NOP ) {
			int datacode = (code[posn] >> 8) & 0xFF;
			if( datacode < NOPCODES.length ) {
				instruction = NOPCODES[datacode];
			}
		}
		int words = instruction.getNumWords();
		
		/* Additional data for some cases - copy into the local code array */
		int extra = 0, target=0;
		if( opcode == PACKED_SWITCH ) {
			target = posn + signed32(code[posn+1], code[posn+2]);
			extra = code[target+1]*2 + 4;
		} else if( opcode == SPARSE_SWITCH ) {
			target = posn + signed32(code[posn+1], code[posn+2]);
			extra = code[target+1]*4 + 4;
		} else if( opcode == FILL_ARRAY_DATA ) {
			target = posn + signed32(code[posn+1], code[posn+2]);
			int width = code[target+1];
			extra = (signed32(code[target+2], code[target+3]) * width + 1) / 2 + 4;
		}
		this.code = new short[words+extra];
		int avail = code.length - posn;
		if( avail < words ) {
			System.arraycopy(code, posn, this.code, 0, avail );
			for( int i=avail; i<words; i++ ) {
				this.code[i] = 0;
			}
		} else {
			System.arraycopy(code, posn, this.code, 0, words);
		}
		System.arraycopy(code, target, this.code, words, extra);
		
		parseOperands();
		this.registerTypes = new DexType[registers.length];
	}
	
	protected void setParent( DexBasicBlock parent ) {
		this.parent = parent;
	}
	
	public DexBasicBlock getParent() {
		return parent;
	}
	
	public int getPC() {
		return pc;
	}
	
	public String getHexPC() {
		return StringUtils.leftPad(Integer.toHexString(pc), 4, '0');
	}
	
	/**
	 * @return the number of words used by the instruction
	 */
	public int size() {
		return instruction.getNumWords();
	}
	
	public int getOpcode() {
		return code[0] & 0xFF;
	}
	
	public String getMnemonic() {
		return instruction.getMnemonic();
	}
	
	public int getNumRegisters() {
		return registers.length;
	}
	
	public int getRegister(int idx) {
		return registers[idx];
	}
	
	public DexType getRegisterType( int idx ) {
		return registerTypes[idx];
	}
	
	public void setRegisterType( int idx, DexType value ) {
		registerTypes[idx] = value;
	}
	
	public void checkRegisterType( int idx, DexType type ) throws ParseException {
		registerTypes[idx].checkType(type);
	}
	
	/**
	 * @return true if the instruction is an actual instruction and not the start
	 * of an inline table (packed-switch, etc)
	 */
	public boolean isInstruction() {
		return instruction.getMode() != M_DATA;
	}
	
	public boolean isUncondBranch() {
		int opcode = getOpcode();
		return opcode == GOTO || opcode == GOTO16 || opcode == GOTO32;
	}
	
	public boolean isCondBranch() {
		int opcode = getOpcode();
		return opcode >= IF_EQ && opcode <= IF_LEZ;
	}
	
	public boolean isSwitch() {
		int opcode = getOpcode();
		return opcode == PACKED_SWITCH || opcode == SPARSE_SWITCH;
	}
	
	public boolean isReturn() {
		int opcode = getOpcode();
		return opcode >= RETURN_VOID && opcode <= RETURN_OBJECT;
	}
	
	public boolean isInvoke() {
		int opcode = getOpcode();
		return opcode >= INVOKE_VIRTUAL && opcode <= INVOKE_INTERFACE_RANGE &&
		    opcode != 0x73; /* Undef in the middle */
	}
	
	public boolean isThrow() {
		return getOpcode() == THROW;
	}
	
	public int getBranchTarget() {
		switch( instruction.mode ) {
		case M_10T: 
			return pc + getSHighByte(0);
		case M_20T: 
		case M_21T: 
		case M_22T:
			return pc + getShort(1);
		case M_30T:
		case M_31T:
			return pc + getInt(1);
		default:
			throw new IllegalArgumentException("Instruction does not have a branch target");
		}
	}
	
	public String getBranchLabel() {
		int target = getBranchTarget();
		DexBasicBlock bb = method.getBlockForPC(target);
		if( bb == null ) {
			return "";
		} else {
			return bb.getName();
		}
	}
	
	public DexBasicBlock getBranchBlock() {
		return method.getBlockForPC(getBranchTarget());
	}
	
	public String getStringOperand() {
		return method.getFile().getString((int)constOperand);
	}
	
	public String getTypeOperand() {
		return method.getFile().getTypeName((int)constOperand);
	}
	
	public DexMethod getMethodOperand() {
		return method.getFile().getMethod((int)constOperand);
	}
	
	public DexField getFieldOperand() {
		return method.getFile().getField((int)constOperand);
	}
	
	public int[] getSwitchTargets() {
		int opcode = getOpcode();
		int start = size();
		if( opcode == PACKED_SWITCH ) {
			int size = getUShort(start+1);
			int[] result = new int[size];
			for( int i=0; i<size; i++ ) {
				result[i] =  pc + getInt(start + 4 + i*2);
			}
			return result;
		} else if( opcode == SPARSE_SWITCH ) {
			int size = getUShort(start+1);
			int []result = new int[size];
			for( int i=0; i<size; i++ ) {
				result[i] = pc + getInt(start + 4 + i*2 + size*2);
			}
			return result;
		} else {
			return null;
		}
	}
	
	public DexBasicBlock[] getSwitchBlocks() {
		int [] addrs = getSwitchTargets();
		if( addrs != null ) {
			DexBasicBlock []blocks = new DexBasicBlock[addrs.length];
			for( int i=0; i<addrs.length; i++ ) {
				blocks[i] = method.getBlockForPC(addrs[i]);
			}
			return blocks;
		} else {
			return null;
		}
	}
	
	/**
	 * Return the minimum key value for a switch statement. Result is undefined
	 * for any other opcode.
	 */
	public int getMinSwitchKey() {
		return getInt(size()+2);
	}

	/**
	 * Return the maximum key value for a switch statement. Result is undefined
	 * for any other opcode.
	 */
	public int getMaxSwitchKey() {
		int start = size();
		if( getOpcode() == PACKED_SWITCH ) {
			return getInt(start+2) + getUShort(start+1) - 1;
		} else {
			int tableSize = getUShort(start+1);
			return getInt(start + 2 + (tableSize*2));
		} 
	}
	
	public int[] getSwitchKeys() {
		int start = size();
		if( getOpcode() == PACKED_SWITCH ) {
			int first = getInt(start+2);
			int size = getUShort(start+1);
			int[] result = new int[size];
			for( int i=0; i<size; i++ ) {
				result[i] = first + i;
			}
			return result;
		} else {
			int size = getUShort(start+1);
			int []result = new int[size];
			for( int i=0; i<size; i++ ) {
				result[i] = pc + getInt(start + 4 + i*2);
			}
			return result;
		}
	}
	
	public DexType[] getThrows() {
		return instruction.getThrows();
	}
		
		
	public String formatOperands( ) {
		StringBuilder result = new StringBuilder();
		Formatter fmt = new Formatter(result);
		for( int i=0; i<registers.length; i++ ) {
			if( i != 0 )
				fmt.format( ", " );
			fmt.format("v%d", registers[i]);
			DexType type = registerTypes[i];
			if( type != null ) {
				fmt.format(" [%s]", type.format());
			}
		}
		int optype = instruction.getOperandType();
		if( optype != OPTYPE_NONE ) {
			if( registers.length != 0 )
				fmt.format( ", " );

			switch( instruction.getOperandType() ) {
			case OPTYPE_STRING:
				fmt.format( "\"%s\"", StringEscapeUtils.escapeJava(getStringOperand()));
				break;
			case OPTYPE_TYPE:
				fmt.format( "%s", DexType.format(getTypeOperand()));
				break;
			case OPTYPE_FIELD:
				fmt.format("%s", getFieldOperand().getDisplayName());
				break;
			case OPTYPE_METHOD:
				fmt.format( "%s", getMethodOperand().getDisplaySignature() );
				break;
			case OPTYPE_TARGET:
				fmt.format( "%s (%04x)", getBranchLabel(), constOperand) ;
				break;
			default:
				fmt.format( "%d", constOperand );
				break;
			}
		}
		return result.toString();
	}
	
	public String disassemble() {
		return instruction.getMnemonic() + " " + formatOperands(); 
	}

	public String formatTable(String indent) {
		StringBuilder builder = new StringBuilder();
		Formatter fmt = new Formatter(builder);
		int start = instruction.getNumWords();
		int size, first, width;
		if( code.length > start ) {
			switch( code[start] ) {
			case 0x0100: /* packed-switch */
				size = getUShort(start+1);
				first = getInt(start+2);
				for( int i=0; i<size; i++ ) {
					if( indent != null ) {
						fmt.format("%s", indent);
					}
					int targetpc = pc + getInt(start + 4 + i*2);
					fmt.format("%d: %s (%04X)\n", first+i, method.getBlockForPC(targetpc), targetpc);
				}
				break;
			case 0x0200: /* sparse-switch */
				size = getUShort(start+1);
				for( int i=0; i<size; i++ ) {
					if( indent != null ) {
						fmt.format("%s", indent);
					}
					int targetpc = pc + getInt(start + 4 + i*2 + size*2);
					fmt.format("%d: %s (%04X)\n", getInt(start + 4 + i*2), method.getBlockForPC(targetpc), targetpc); 
				}
				break;
			case 0x0300: /* fill-array-data */
				width = getUShort(start+1);
				size = getInt(start+2);
				break;
			default:
				break; /* Do nothing */
			}
		}
		return builder.toString();
	}

	public long getLong( int posn ) {
		return (((long)code[posn]) & 0xFFFF) | ((((long)code[posn+1]) & 0xFFFF) << 16) |
			((((long)code[posn+2]) & 0xFFFF) << 32) | ((((long)code[posn+3]) & 0xFFFF) << 48);
	}
	
	public long getUInt( int posn ) {
		return (((long)code[posn]) & 0xFFFF) | ((((long)code[posn+1]) & 0xFFFF) << 16);
	}
	
	public int getInt( int posn ) {
		return (((int)code[posn]) & 0xFFFF) | (((int)code[posn+1]) << 16);
	}
	
	public int getUShort( int posn ) {
		return ((int)code[posn]) & 0xFFFF;
	}
	
	public int getShort( int posn ) {
		return ((int)code[posn]);
	}
	
	private int getULowByte( int posn ) {
		return ((int)code[posn]) & 0xFF;
	}
	
	private int getUHighByte( int posn ) {
		return (((int)code[posn]) >> 8) & 0xFF;
	}
	
	private int getSHighByte( int posn ) {
		return ((int)code[posn]) >> 8;
	}

	private String formatStringId( int word ) {
		return "\"" + StringEscapeUtils.escapeJava(method.getFile().getString( word )) + "\"";
	}


	private void parseOperands( ) {
		int b;
		if( instruction.getNumRegisters() < 5 ) 
			registers = new int[instruction.getNumRegisters()];
	
		int mode = instruction.getMode();
		short mainWord = code[0];
		switch( mode ) {
		case M_10T:
			constOperand = pc + getSHighByte(0);
			break;
		case M_10X: break;
		case M_DATA: break;
		case M_11N:
			registers[0] = thirdNibble(mainWord);
			constOperand = sextHighNibble(mainWord);
			break;
		case M_11X: 
			registers[0] = highByte(mainWord);
			break;
		case M_12X:
			registers[0] = thirdNibble(mainWord);
			registers[1] = highNibble(mainWord);
			break;
		case M_20T:
			constOperand = pc + getShort(1);
			break;
		case M_21Cstring:
		case M_21Ctype:
		case M_21Cfield:
			registers[0] = highByte(mainWord);
			constOperand = getUShort(1);
			break;
		case M_21H:
			registers[0] = highByte(mainWord);
			constOperand = getUShort(1) << 16;
			break;
		case M_21H64:
			registers[0] = highByte(mainWord);
			constOperand = ((long)getUShort(1)) << 48;
			break;
		case M_21S:
			registers[0] = highByte(mainWord);
			constOperand = getShort(1);
			break;
		case M_21T:
			registers[0] = highByte(mainWord);
			constOperand = pc + getShort(1);
			break;
		case M_22B:
			registers[0] = highByte(mainWord);
			registers[1] = getULowByte(1);
			constOperand = getSHighByte(1);
			break;
		case M_22Cfield:
		case M_22Ctype:
			registers[0] = thirdNibble(mainWord);
			registers[1] = highNibble(mainWord);
			constOperand = getUShort(1);
			break;
		case M_22S:
			registers[0] = thirdNibble(mainWord);
			registers[1] = highNibble(mainWord);
			constOperand = getShort(1);
			break;
		case M_22T:
			registers[0] = thirdNibble(mainWord);
			registers[1] = highNibble(mainWord);
			constOperand = pc + getShort(1);
			break;
		case M_22X:
			registers[0] = highByte(mainWord);
			registers[1] = getUShort(1);
			break;
		case M_23X:
			registers[0] = highByte(mainWord);
			registers[1] = getULowByte(1);
			registers[2] = getUHighByte(1);
			break;
		case M_30T:
			constOperand = pc + getInt(1);
			break;
		case M_31Cstring:
			registers[0] = highByte(mainWord);
			constOperand = getUInt(1);
			break;
		case M_31I:
			registers[0] = highByte(mainWord);
			constOperand = getInt(1);
			break;
		case M_31T:
			registers[0] = highByte(mainWord);
			constOperand = pc + getInt(1);
			break;
		case M_32X:
			registers[0] = getUShort(1);
			registers[1] = getUShort(2);
			break;
		case M_35Cmethod:
		case M_35Ctype:
			b = highNibble(mainWord);
			if( b >= 5 ) b = 5;
			registers = new int[b];
			switch( b ) {
			case 5: registers[4] = thirdNibble(mainWord);
			case 4: registers[3] = (code[2] >> 12) & 0x0F;
			case 3: registers[2] = (code[2] >> 8) & 0x0F;
			case 2: registers[1] = (code[2] >> 4) & 0x0F;
			case 1: registers[0] = code[2] & 0x0F;
			default:
				break;
			}
			constOperand = getUShort(1);
			break;
		case M_3RCmethod:
		case M_3RCtype:
			b = highByte(mainWord);
			registers = new int[b];
			for( int i=0; i<b; i++ ) {
				registers[i] = getUShort(2) + i;
			}
			constOperand = getUShort(1);
			break;
		case M_51L:
			registers[0] = highByte(mainWord);
			constOperand = getLong(1);
			break;
			
		}
	}
}
