package com.toccatasystems.dalvik;

import java.io.PrintStream;
import java.util.Formatter;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Opcode specifications, based on document at
 *   http://www.netmite.com/android/mydroid/dalvik/docs/instruction-formats.html and
 *   http://www.netmite.com/android/mydroid/dalvik/docs/dalvik-bytecode.html
 */

public class DexInstruction {
	/** Opcode constants */
	public final static int NOP  = 0x00;
	public final static int MOVE = 0x01;
	public final static int MOVE_FROM16 = 0x02;
	public final static int MOVE_16 = 0x03;
	public final static int MOVE_WIDE = 0x04;
	public final static int MOVE_WIDE_FROM16 = 0x05;
	public final static int MOVE_WIDE16 = 0x06;
	public final static int MOVE_OBJECT = 0x07;
	public final static int MOVE_OBJECT_FROM16 = 0x08;
	public final static int MOVE_OBJECT16 = 0x09;
	public final static int MOVE_RESULT = 0x0A;
	public final static int MOVE_RESULT_WIDE = 0x0B;
	public final static int MOVE_RESULT_OBJECT = 0x0C;
	public final static int MOVE_EXCEPTION = 0x0D;
	public final static int RETURN_VOID = 0x0E;
	public final static int RETURN = 0x0F;
	public final static int RETURN_WIDE = 0x10;
	public final static int RETURN_OBJECT = 0x11;
	public final static int CONST4 = 0x12;
	public final static int CONST16 = 0x13;
	public final static int CONST = 0x14;
	public final static int CONST_HIGH16 = 0x15;
	public final static int CONST_WIDE16 = 0x16;
	public final static int CONST_WIDE32 = 0x17;
	public final static int CONST_WIDE = 0x18;
	public final static int CONST_WIDE_HIGH16 = 0x19;
	public final static int CONST_STRING = 0x1A;
	public final static int CONST_STRING_JUMBO = 0x1B;
	public final static int CONST_CLASS = 0x1C;
	public final static int MONITOR_ENTER = 0x1D;
	public final static int MONITOR_EXIT = 0x1E;
	public final static int CHECK_CAST = 0x1F;
	public final static int INSTANCE_OF = 0x20;
	public final static int ARRAY_LENGTH = 0x21;
	public final static int NEW_INSTANCE = 0x22;
	public final static int NEW_ARRAY = 0x23;
	public final static int FILLED_NEW_ARRAY = 0x24;
	public final static int FILLED_NEW_ARRAY_RANGE = 0x25;
	public final static int FILL_ARRAY_DATA = 0x26;
	public final static int THROW = 0x27;
	public final static int GOTO = 0x28;
	public final static int GOTO16 = 0x29;
	public final static int GOTO32 = 0x2A;
	public final static int PACKED_SWITCH = 0x2B;
	public final static int SPARSE_SWITCH = 0x2C;
	public final static int CMPL_FLOAT = 0x2D;
	public final static int CMPG_FLOAT = 0x2E;
	public final static int CMPL_DOUBLE = 0x2F;
	public final static int CMPG_DOUBLE = 0x30;
	public final static int CMP_LONG = 0x31;
	public final static int IF_EQ = 0x32;
	public final static int IF_NE = 0x33;
	public final static int IF_LT = 0x34;
	public final static int IF_GE = 0x35;
	public final static int IF_GT = 0x36;
	public final static int IF_LE = 0x37;
	public final static int IF_EQZ = 0x38;
	public final static int IF_NEZ = 0x39;
	public final static int IF_LTZ = 0x3A;
	public final static int IF_GEZ = 0x3B;
	public final static int IF_GTZ = 0x3C;
	public final static int IF_LEZ = 0x3D;
	public final static int AGET = 0x44;
	public final static int AGET_WIDE = 0x45;
	public final static int AGET_OBJECT = 0x46;
	public final static int AGET_BOOLEAN = 0x47;
	public final static int AGET_BYTE = 0x48;
	public final static int AGET_CHAR = 0x49;
	public final static int AGET_SHORT = 0x4A;
	public final static int APUT = 0x4B;
	public final static int APUT_WIDE = 0x4C;
	public final static int APUT_OBJECT = 0x4D;
	public final static int APUT_BOOLEAN = 0x4E;
	public final static int APUT_BYTE = 0x4F;
	public final static int APUT_CHAR = 0x50;
	public final static int APUT_SHORT = 0x51;
	public final static int IGET = 0x52;
	public final static int IGET_WIDE = 0x53;
	public final static int IGET_OBJECT = 0x54;
	public final static int IGET_BOOLEAN = 0x55;
	public final static int IGET_BYTE = 0x56;
	public final static int IGET_CHAR = 0x57;
	public final static int IGET_SHORT = 0x58;
	public final static int IPUT = 0x59;
	public final static int IPUT_WIDE = 0x5A;
	public final static int IPUT_OBJECT = 0x5B;
	public final static int IPUT_BOOLEAN = 0x5C;
	public final static int IPUT_BYTE = 0x5D;
	public final static int IPUT_CHAR = 0x5E;
	public final static int IPUT_SHORT = 0x5F;
	public final static int SGET = 0x60;
	public final static int SGET_WIDE = 0x61;
	public final static int SGET_OBJECT = 0x62;
	public final static int SGET_BOOLEAN = 0x63;
	public final static int SGET_BYTE = 0x64;
	public final static int SGET_CHAR = 0x65;
	public final static int SGET_SHORT = 0x66;
	public final static int SPUT = 0x67;
	public final static int SPUT_WIDE = 0x68;
	public final static int SPUT_OBJECT = 0x69;
	public final static int SPUT_BOOLEAN = 0x6A;
	public final static int SPUT_BYTE = 0x6B;
	public final static int SPUT_CHAR = 0x6C;
	public final static int SPUT_SHORT = 0x6D;
	public final static int INVOKE_VIRTUAL = 0x6E;
	public final static int INVOKE_SUPER = 0x6F;
	public final static int INVOKE_DIRECT = 0x70;
	public final static int INVOKE_STATIC = 0x71;
	public final static int INVOKE_INTERFACE = 0x72;
	public final static int INVOKE_VIRTUAL_RANGE = 0x74;
	public final static int INVOKE_SUPER_RANGE = 0x75;
	public final static int INVOKE_DIRECT_RANGE = 0x76;
	public final static int INVOKE_STATIC_RANGE = 0x77;
	public final static int INVOKE_INTERFACE_RANGE = 0x78;
	public final static int NEG_INT = 0x7B;
	public final static int NOT_INT = 0x7C;
	public final static int NEG_LONG = 0x7D;
	public final static int NOT_LONG = 0x7E;
	public final static int NEG_FLOAT = 0x7F;
	public final static int NEG_DOUBLE = 0x80;
	public final static int INT_TO_LONG = 0x81;
	public final static int INT_TO_FLOAT = 0x82;
	public final static int INT_TO_DOUBLE = 0x83;
	public final static int LONG_TO_INT = 0x84;
	public final static int LONG_TO_FLOAT = 0x85;
	public final static int LONG_TO_DOUBLE = 0x86;
	public final static int FLOAT_TO_INT = 0x87;
	public final static int FLOAT_TO_LONG = 0x88;
	public final static int FLOAT_TO_DOUBLE = 0x89;
	public final static int DOUBLE_TO_INT = 0x8A;
	public final static int DOUBLE_TO_LONG = 0x8B;
	public final static int DOUBLE_TO_FLOAT = 0x8C;
	public final static int INT_TO_BYTE = 0x8D;
	public final static int INT_TO_CHAR = 0x8E;
	public final static int INT_TO_SHORT = 0x8F;
	public final static int ADD_INT = 0x90;
	public final static int SUB_INT = 0x91;
	public final static int MUL_INT = 0x92;
	public final static int DIV_INT = 0x93;
	public final static int REM_INT = 0x94;
	public final static int AND_INT = 0x95;
	public final static int OR_INT = 0x96;
	public final static int XOR_INT = 0x97;
	public final static int SHL_INT = 0x98;
	public final static int SHR_INT = 0x99;
	public final static int USHR_INT = 0x9A;
	public final static int ADD_LONG = 0x9B;
	public final static int SUB_LONG = 0x9C;
	public final static int MUL_LONG = 0x9D;
	public final static int DIV_LONG = 0x9E;
	public final static int REM_LONG = 0x9F;
	public final static int AND_LONG = 0xA0;
	public final static int OR_LONG = 0xA1;
	public final static int XOR_LONG = 0xA2;
	public final static int SHL_LONG = 0xA3;
	public final static int SHR_LONG = 0xA4;
	public final static int USHR_LONG = 0xA5;
	public final static int ADD_FLOAT = 0xA6;
	public final static int SUB_FLOAT = 0xA7;
	public final static int MUL_FLOAT = 0xA8;
	public final static int DIV_FLOAT = 0xA9;
	public final static int REM_FLOAT = 0xAA;
	public final static int ADD_DOUBLE = 0xAB;
	public final static int SUB_DOUBLE = 0xAC;
	public final static int MUL_DOUBLE = 0xAD;
	public final static int DIV_DOUBLE = 0xAE;
	public final static int REM_DOUBLE = 0xAF;
	public final static int ADD_INT_2ADDR = 0xB0;
	public final static int SUB_INT_2ADDR = 0xB1;
	public final static int MUL_INT_2ADDR = 0xB2;
	public final static int DIV_INT_2ADDR = 0xB3;
	public final static int REM_INT_2ADDR = 0xB4;
	public final static int AND_INT_2ADDR = 0xB5;
	public final static int OR_INT_2ADDR= 0xB6;
	public final static int XOR_INT_2ADDR = 0xB7;
	public final static int SHL_INT_2ADDR = 0xB8;
	public final static int SHR_INT_2ADDR = 0xB9;
	public final static int USHR_INT_2ADDR = 0xBA;
	public final static int ADD_LONG_2ADDR = 0xBB;
	public final static int SUB_LONG_2ADDR = 0xBC;
	public final static int MUL_LONG_2ADDR = 0xBD;
	public final static int DIV_LONG_2ADDR = 0xBE;
	public final static int REM_LONG_2ADDR = 0xBF;
	public final static int AND_LONG_2ADDR = 0xC0;
	public final static int OR_LONG_2ADDR = 0xC1;
	public final static int XOR_LONG_2ADDR = 0xC2;
	public final static int SHL_LONG_2ADDR = 0xC3;
	public final static int SHR_LONG_2ADDR = 0xC4;
	public final static int USHR_LONG_2ADDR = 0xC5;
	public final static int ADD_FLOAT_2ADDR = 0xC6;
	public final static int SUB_FLOAT_2ADDR = 0xC7;
	public final static int MUL_FLOAT_2ADDR = 0xC8;
	public final static int DIV_FLOAT_2ADDR = 0xC9;
	public final static int REM_FLOAT_2ADDR = 0xCA;
	public final static int ADD_DOUBLE_2ADDR = 0xCB;
	public final static int SUB_DOUBLE_2ADDR = 0xCC;
	public final static int MUL_DOUBLE_2ADDR = 0xCD;
	public final static int DIV_DOUBLE_2ADDR = 0xCE;
	public final static int REM_DOUBLE_2ADDR = 0xCF;
	public final static int ADD_INT_LIT16 = 0xD0;
	public final static int RSUB_INT_LIT16 = 0xD1;
	public final static int MUL_INT_LIT16 = 0xD2;
	public final static int DIV_INT_LIT16 = 0xD3;
	public final static int REM_INT_LIT16 = 0xD4;
	public final static int AND_INT_LIT16 = 0xD5;
	public final static int OR_INT_LIT16 = 0xD6;
	public final static int XOR_INT_LIT16 = 0xD7;
	public final static int ADD_INT_LIT8 = 0xD8;
	public final static int RSUB_INT_LIT8 = 0xD9;
	public final static int MUL_INT_LIT8 = 0xDA;
	public final static int DIV_INT_LIT8 = 0xDB;
	public final static int REM_INT_LIT8 = 0xDC;
	public final static int AND_INT_LIT8 = 0xDD;
	public final static int OR_INT_LIT8 = 0xDE;
	public final static int XOR_INT_LIT8 = 0xDF;
	public final static int SHL_INT_LIT8 = 0xE0;
	public final static int SHR_INT_LIT8 = 0xE1;
	public final static int USHR_INT_LIT8 = 0xE2;

	/* Addressing modes. The names are per Dalvik documentation, such that
	 *   First digit is the number of 16-bit words used by the instruction
	 *   Second digit is the number of registers encoded (R = range)
	 *   Third letter is an operand type:
	 *   1   B = immediate signed byte
	 *   2   C = constant pool index
	 *   3   F = interface constant (statically linked only)
	 *   4   H = immediate signed high-order 16-bits of a 32 or 64-bit value
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
		public I(String mnemonic, int mode) {
			this.mnemonic = mnemonic;
			this.mode = mode;
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
		
	}

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
		new I("const-wide", M_51L), new I("const-wide/high16", M_21H), new I("const-string", M_21Cstring), new I("const-string/jumbo", M_31Cstring),
		new I("const-class", M_21Ctype), new I("monitor-enter", M_11X), new I("monitor-exit", M_11X), new I("check-cast", M_21Ctype),
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
	private final static int sextHighByte( short word ) {
		return ((int)word >> 8);
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
	private I instruction;
	private int pc;
	private short[]code;
	
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
	}
	
	public int getPC() {
		return pc;
	}
	
	/**
	 * @return the number of words used by the instruction
	 */
	public int size() {
		return code.length;
	}
	
	public int getOpcode() {
		return code[0] & 0xFF;
	}
	
	public String getMnemonic() {
		return instruction.getMnemonic();
	}
	
	public int getNumRegisters() {
		return instruction.getNumRegisters();
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
	
	public String formatOperands( ) {
		StringBuilder result = new StringBuilder();
		Formatter fmt = new Formatter(result);
		int b;
		int args[];
		int mode = instruction.getMode();
		short mainWord = code[0];
		switch( mode ) {
		case M_10T: fmt.format("%s (%04X)", getBranchLabel(), getBranchTarget()); break;
		case M_10X: break; /* No operands */
		case M_DATA: break;
		case M_11N: fmt.format("v%d, #%02X", thirdNibble(mainWord), sextHighNibble(mainWord) ); break;
		case M_11X: fmt.format("v%d", highByte(mainWord)); break;
		case M_12X: fmt.format("v%d, v%d", thirdNibble(mainWord), highNibble(mainWord) ); break;
		case M_20T: fmt.format("%s (%04X)", getBranchLabel(), getBranchTarget()); break;
		case M_21Cstring: fmt.format("v%d, %s", highByte(mainWord), getString16(1)); break;
		case M_21Ctype: fmt.format("v%d, %s", highByte(mainWord), getTypeId(1)); break;
		case M_21Cfield: fmt.format("v%d, %s", highByte(mainWord), getFieldId(1)); break;
		case M_21H: fmt.format("v%d, #%04X0000(00000000)", highByte(mainWord), getUShort(1)); break;
		case M_21S: fmt.format("v%d, #%04X", highByte(mainWord), getShort(1)); break;
		case M_21T: fmt.format("v%d, %s (%04X)", highByte(mainWord), getBranchLabel(), getBranchTarget()); break;
		case M_22B: fmt.format("v%d, v%d, #%02X", highByte(mainWord), getULowByte(1), getSHighByte(1)); break; 
		case M_22Cfield: fmt.format("v%d, v%d, %s", thirdNibble(mainWord), highNibble(mainWord), getFieldId(1)); break;
		case M_22Ctype:  fmt.format("v%d, v%d, %s", thirdNibble(mainWord), highNibble(mainWord), getTypeId(1)); break;
		case M_22S: fmt.format("v%d, v%d, #%04X", thirdNibble(mainWord), highNibble(mainWord), getShort(1)); break;
		case M_22T: fmt.format("v%d, v%d, %s (%04X)", thirdNibble(mainWord), highNibble(mainWord), getBranchLabel(), getBranchTarget() ); break;
		case M_22X: fmt.format("v%d, v%d", highByte(mainWord), getUShort(1)); break;
		case M_23X: fmt.format("v%d, v%d, v%d", highByte(mainWord), getULowByte(1), getUHighByte(1)); break;
		case M_30T: fmt.format("%s (%08X)", getBranchLabel(), getBranchTarget()); break;
		case M_31Cstring: fmt.format("v%d, %s", highByte(mainWord), getString32(1)); break;
		case M_31I: fmt.format("v%d, #%08X", highByte(mainWord), getInt(1)); break;
		case M_31T: fmt.format("v%d, %s (%08X)", highByte(mainWord), getBranchLabel(), getBranchTarget()); break;
		case M_32X: fmt.format("v%d, v%d", getUShort(1), getUShort(2)); break;
		case M_35Cmethod: /* B|A|op CCCC G|F|E|D : [B=count] op {vD, vE, vF, vG, vA}, (meth|type|kind)@CCCC */
		case M_35Ctype:
			b = highNibble(mainWord);
			if( b > 5 ) b = 5;
			args = new int[5];
			args[0] = code[2] & 0x0F;
			args[1] = (code[2] >> 4) & 0x0F;
			args[2] = (code[2] >> 8) & 0x0F;
			args[3] = (code[2] >> 12) & 0x0F;
			args[4] = thirdNibble(mainWord);
			for( int i=0; i<b; i++ ) {
				fmt.format("v%d, ", args[i], args);
			}
			if( mode == M_35Ctype ) {
				fmt.format("%s", getTypeId(1));
			} else {
				fmt.format("%s", getMethodId(1));
			}
			break;
		case M_3RCmethod:  /* AA|op BBBB CCCC : op {vCCCC .. vNNNN}, (meth|type)@BBBB */
		case M_3RCtype:
			b = highByte(mainWord);
			for( int i=0; i<b; i++ ) {
				fmt.format("v%d, ", getUShort(2)+i);
			}
			if( mode == M_3RCtype ) {
				fmt.format("%s", getTypeId(1));
			} else {
				fmt.format("%s", getMethodId(1));
			}
			break;
				
		case M_51L:  /* AA|op BBBBl BBBB BBBB BBBBh : op vAA, #+BBBBBBBBBBBBBBBB */
		default:
			fmt.format("Unhandled operand format");
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
					fmt.format("%d: %08X\n", first+i, pc + getInt(start + 4 + i*2));
				}
				break;
			case 0x0200: /* sparse-switch */
				size = getUShort(start+1);
				for( int i=0; i<size; i++ ) {
					if( indent != null ) {
						fmt.format("%s", indent);
					}
					fmt.format("%d: %08X\n", getInt(start + 4 + i*2), pc + getInt(start + 4 + i*2 + size*2));
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
	
	private int getUHighNibble( int posn ) {
		return (((int)code[posn]) >> 12) & 0x0F;
	}
	
	private int getSHighNibble( int posn ) {
		return ((int)code[posn]) >> 12;
	}
	
	private String getString16( int posn ) {
		return formatStringId( getUShort(posn) );
	}
	
	private String getString32( int posn ) {
		return formatStringId( getInt(posn) );
	}
	
	private String getTypeId( int posn ) {
		return method.getFile().getDisplayTypeName( ((int)code[posn])&0xFFFF );
	}
	private String getFieldId( int posn ) {
		return method.getFile().getDisplayFieldName( ((int)code[posn])&0xFFFF );
	}
	private String getMethodId( int posn ) {
		return method.getFile().getDisplayMethodSignature( ((int)code[posn])&0xFFFF );
	}
	
	private String formatStringId( int word ) {
		return "\"" + StringEscapeUtils.escapeJava(method.getFile().getString( word )) + "\"";
	}

}
