package com.toccatasystems.dalvik;

import java.util.Arrays;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;

import com.toccatasystems.dalvik.analysis.ComputeUseDefInfo;

/**
 * DexParser parses a .dex file and returns a new DexFile. The parser can be
 * reused, but is not thread-safe.
 * @author nkeynes
 */

public class DexParser {
    /* expected magic values */
    private static final byte[] DEX_FILE_MAGIC = { 0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x35, 0x00 };
    private static final int LITTLE_ENDIAN_TAG = 0x12345678;
    private static final int BIG_ENDIAN_TAG = 0x78563412;

	private final static int NO_INDEX = -1;

	/**
	 * Underlying data source.
	 */
	private ByteBuffer data;
	
	/* Various intermediate data / tables that we need during parsing, as
	 * they can be referenced by index from various places
	 */
	private String []stringTable;
	private String []typeNameTable;
	private DexField []fieldTable;
	private DexMethod []methodTable;
	
	public DexFile parseFile( String filename ) throws IOException, ParseException {
		return parseFile( filename, new FileInputStream(filename).getChannel() );
	}
	
	public DexFile parseFile( File filename ) throws IOException, ParseException {
		return parseFile( filename.toString(), new FileInputStream(filename).getChannel() );
	}
	
	public DexFile parseFile( String filename, FileInputStream ins ) throws IOException, ParseException {
		return parseFile( filename, ins.getChannel() );
	}
	
	public DexFile parseFile( String filename, FileChannel channel ) throws IOException, ParseException {
		data = channel.map(MapMode.READ_ONLY, 0, channel.size());
		DexClass []result = readFile();
		channel.close();
		DexFile file = new DexFile(filename, stringTable, typeNameTable, fieldTable, methodTable, result);
		postParse(file);
		return file;
	}
	
	/**************************** File parsing ******************************/	
	
	private DexClass[] readFile() throws ParseException {
		data.order(ByteOrder.LITTLE_ENDIAN);
		checkMagic();
		
        data.position(0x38);
        int stringTableSize = data.getInt();
        int stringTableOffset = data.getInt();
        int typeTableSize = data.getInt();
        int typeTableOffset = data.getInt();
        int protoTableSize = data.getInt();
        int protoTableOffset = data.getInt();
        int fieldTableSize = data.getInt();
        int fieldTableOffset = data.getInt();
        int methodTableSize = data.getInt();
        int methodTableOffset = data.getInt();
        int classTableSize = data.getInt();
        int classTableOffset = data.getInt();
        
		stringTable = readStringTable(stringTableOffset, stringTableSize);
		typeNameTable = readTypeNameTable(typeTableOffset, typeTableSize);
		fieldTable = readFieldTable(fieldTableOffset, fieldTableSize);
		methodTable = readMethodTable(protoTableOffset, protoTableSize,
				methodTableOffset, methodTableSize);
		return readClassDefTable(classTableOffset, classTableSize);
	}

	private void checkMagic() throws ParseException {
		byte[] b = new byte[8];
		data.position(0);
		data.get(b);
		if( !Arrays.equals(b, DEX_FILE_MAGIC) ) {
			throw new ParseException("Not a Dex file");
		}
		
		int endianTag = data.getInt(0x28);
		if( endianTag == LITTLE_ENDIAN_TAG ) {
			data.order(ByteOrder.LITTLE_ENDIAN);
		} else if( endianTag == BIG_ENDIAN_TAG ) {
			data.order(ByteOrder.BIG_ENDIAN);
		} else {
			throw new ParseException("Invalid endian tag");
		}
	}
	
	/**
	 * Read the class definitions and details.
	 * The various String and type tables must have already been read before invoking
	 * this method
	 */
	private DexClass [] readClassDefTable(int offset, int size) throws ParseException {
		DexClass[] result = new DexClass[size];
		for( int i=0; i<size; i++ ) {
			int classOffset = offset + 32*i;
			/* class_def_item */
			String className = readTypeId(classOffset);
			int flags = data.getInt(classOffset+4);
			String superclass = readTypeId(classOffset+8);
			String interfaces[] = readTypeListPtr(classOffset+12);
			String sourceFile = readStringId(classOffset+16);
			int annotationOffset = data.getInt(classOffset+20);
			int dataOffset = data.getInt(classOffset+24);
			int staticOffset = data.getInt(classOffset+28);
			
			DexField []staticFields, instanceFields;
			DexMethod []directMethods, virtualMethods;
			
			if( dataOffset == 0 ) {
				staticFields = new DexField[0];
				instanceFields = new DexField[0];
				directMethods = new DexMethod[0];
				virtualMethods = new DexMethod[0];
			} else {
				/* class_data_item */
				data.position(dataOffset);
				int staticFieldsSize = readULEB128();
				int instanceFieldsSize = readULEB128();
				int directMethodsSize = readULEB128();
				int virtualMethodsSize = readULEB128();

				staticFields = readEncodedFields(staticFieldsSize);
				instanceFields = readEncodedFields(instanceFieldsSize);
				directMethods = readEncodedMethods(directMethodsSize);
				virtualMethods = readEncodedMethods(virtualMethodsSize);
			} 
			DexClass clz = new DexClass(className, flags, superclass,
					interfaces, sourceFile, staticFields, instanceFields, directMethods, virtualMethods);
			
			result[i] = clz;
			
			if( annotationOffset != 0 ) {
				readClassAnnotations( clz, annotationOffset );
			}
			
			if( staticOffset != 0 ) {
				int oldposn = data.position();
				data.position(staticOffset);
				DexValue []init = readEncodedArray();
				data.position(oldposn);
				if( init.length > staticFields.length ) {
					throw new ParseException ("Too many static initializers");
				}
				for( int j=0; j<init.length; j++ ) {
					staticFields[j].setInitializer(init[j]);
				}
			}
			
		}
		return result;
	}
	
	/**
	 * Read a list of encoded_fields from the current file position.
	 * After executing, the file position will point to just past the
	 * encoded_field table.
	 * @param fieldTable
	 * @return a new DexField with flags updated from the file data.
	 * @throws ParseException
	 */
	private DexField[] readEncodedFields(int nEntries) throws ParseException {
		DexField[] result = new DexField[nEntries];
		int fieldIdx = 0;
		for( int i=0; i<nEntries; i++ ) {
			fieldIdx += readULEB128();
			int flags = readULEB128();
			result[i] = lookupFieldId(fieldIdx);
			result[i].setFlags(flags);
		}
		return result;
	}
	
	private DexMethod[] readEncodedMethods(int nEntries) throws ParseException {
		DexMethod[] result = new DexMethod[nEntries];
		int methodIdx = 0;
		for( int i=0; i<nEntries; i++ ) {
			methodIdx += readULEB128();
			int flags = readULEB128();
			int codeOffset = readULEB128();
			result[i] = lookupMethodId(methodIdx);
			DexMethodBody code = null;
			if( codeOffset != 0 ) {
				code = readMethodBody(codeOffset);
				code.setParent(result[i]);
			}
			result[i].setFlags(flags);
			result[i].setBody(code);
		}
		return result;
	}

	DexMethodBody readMethodBody( int codeOffset ) throws ParseException {
		int saveposn = data.position();
		data.position(codeOffset);
		int numRegisters = data.getShort();
		int inArgWords = data.getShort();
		int outArgWords = data.getShort();
		int numTries = data.getShort();
		int debugInfoOffset = data.getInt();
		int numInst = data.getInt();
		short []code = new short[numInst];
		for( int i=0; i<numInst; i++ ) {
			code[i] = data.getShort();
		}
		List<DexTryCatch> handlers = new ArrayList<DexTryCatch>();
		if( numTries > 0 ) {
			if( (numInst & 1) != 0 ) 
				data.getShort(); /* Skip padding */
			int handlerBase = data.position() + (numTries * 8);
			for( int i=0; i<numTries; i++ ) {
				int addr = data.getInt();
				int count = data.getShort();
				int handler = handlerBase + data.getShort();
				handlers.addAll(readCatchHandlers(addr, count, handler));
			}
		}
		
		DexDebug debug = null;
		if( debugInfoOffset != 0 ) {
			data.position(debugInfoOffset);
			debug = readDebugInfo();
		}
		data.position(saveposn);
		return new DexMethodBody(numRegisters, inArgWords, outArgWords, code, debug, handlers);
	}
	
	private List<DexTryCatch> readCatchHandlers( int addr, int count, int fileOffset ) throws ParseException {
		int saveposn = data.position();
		data.position(fileOffset);
		int size = readSLEB128();
		boolean catchall = false;
		List<DexTryCatch> handlers = new ArrayList<DexTryCatch>();
		if( size <= 0 ) {
			catchall = true;
			size = -size;
		}
		for( int i=0; i<size; i++ ) {
			int typeId = readULEB128();
			int handler = readULEB128();
			if( typeId < 0 || typeId >= typeNameTable.length ) 
				throw new ParseException( "Invalid Type ID" );
			String type = typeNameTable[typeId];
			handlers.add(new DexTryCatch(addr, count, handler, type));
		}
		if( catchall ) {
			int handler = readULEB128();
			handlers.add(new DexTryCatch(addr, count, handler, null));
		}
		
		data.position(saveposn);
		return handlers;
	}
	
	private DexDebug readDebugInfo( ) throws ParseException {
		int startLine = readULEB128();
		int paramCount = readULEB128();
		String paramNames[] = new String[paramCount];
		for( int i=0; i<paramCount; i++ ) {
			int idx = readULEB128p1();
			if( idx == NO_INDEX ) {
				paramNames[i] = null;
			} else {
				paramNames[i] = lookupStringId(idx);
			}
		}
		DexDebug debug = new DexDebug(startLine, paramNames);
		
		/* Rest is a variable-length sequence of "instructions" */
		while(true) {
			int op = data.get();
			if( op == DexDebug.END_SEQUENCE )
				break;
			switch( op ) {
			case DexDebug.ADVANCE_PC:
			case DexDebug.END_LOCAL:
			case DexDebug.RESTART_LOCAL:
				debug.add(op, readULEB128());
				break;
			case DexDebug.ADVANCE_LINE:
				debug.add(op, readSLEB128());
				break;
			case DexDebug.START_LOCAL:
				debug.add(op, readULEB128(), 
						lookupStringId(readULEB128p1()),
						lookupTypeNameId(readULEB128p1()),
						null);
				break;
			case DexDebug.START_LOCAL_EXT:
				debug.add(op, readULEB128(),
						lookupStringId(readULEB128p1()),
						lookupTypeNameId(readULEB128p1()),
						lookupStringId(readULEB128p1()));
				break;
			case DexDebug.SET_PROLOGUE_END:
			case DexDebug.SET_EPILOGUE_BEGIN:
				debug.add(op);
				break;
			case DexDebug.SET_FILE:
				debug.add(op, 0, lookupStringId(readULEB128p1()), null, null);
				break;
			default: /* "Special" */
				debug.add(op);
				break;
			}
		}
		return debug;
	}
	
	/**
	 * Read the annotations for the class and attach them to the appropriate places.
	 * @param clz
	 * @param fileOffset
	 */
	private void readClassAnnotations( DexClass clz, int fileOffset ) throws ParseException {
		int classAnnOffset = data.getInt(fileOffset);
		if( classAnnOffset != 0 ) {
			clz.add(readAnnotationSet(classAnnOffset));
		}
		int fieldCount = data.getInt(fileOffset+4);
		int methodCount = data.getInt(fileOffset+8);
		int paramsCount = data.getInt(fileOffset+12);
		fileOffset += 16;
		for( int i=0; i<fieldCount; i++ ) {
			DexField field = lookupFieldId( data.getInt(fileOffset) );
			if( field.getParent() != clz ) {
				throw new ParseException( "Field annotation belonging to unexpected class" );
			}
			field.add( readAnnotationSet( data.getInt(fileOffset+4)) );
			fileOffset += 8;
		}
		
		for( int i=0; i<methodCount; i++ ) {
			DexMethod method = lookupMethodId( data.getInt(fileOffset) );
			if( method.getParent() != clz ) {
				throw new ParseException( "Method annotation belonging to unexpected class" );
			}
			method.add( readAnnotationSet( data.getInt(fileOffset+4)) );
			fileOffset += 8;
		}
		for( int i=0; i<paramsCount; i++ ) {
			DexMethod method = lookupMethodId( data.getInt(fileOffset) );
			if( method.getParent() != clz ) {
				throw new ParseException( "Method annotation belonging to unexpected class" );
			}
			int annOffset = data.getInt(fileOffset+4);
			fileOffset += 8;
			int subCount = data.getInt(annOffset);
			for( int j=0; j<subCount; j++ ) {
				annOffset += 4;
				method.addParamAnnotations(j, readAnnotationSet( data.getInt(annOffset)));
			}
		}
	}

	private DexAnnotation[] readAnnotationSet( int fileOffset ) throws ParseException {
		int posn = data.position();
		int count = data.getInt(fileOffset);
		DexAnnotation []result = new DexAnnotation[count];
		for( int i=0; i<count; i++ ) {
			int offset = data.getInt(fileOffset + i*4 + 4);
			data.position(offset);
			int visibility = data.get();
			result[i] = readEncodedAnnotation(visibility);
		}
		data.position(posn);
		return result;
	}
	
	private DexAnnotation readEncodedAnnotation(int visibility) throws ParseException {
		String type = lookupTypeNameId(readULEB128());
		DexAnnotation ann = new DexAnnotation(type,visibility);
		int count = readULEB128();
		for( int i=0; i<count; i++ ) {
			String name = lookupStringId(readULEB128());
			DexValue value = readEncodedValue();
			ann.add(name, value);
		}
		return ann;
	}

	
	private DexValue[] readEncodedArray() throws ParseException {
		int count = readULEB128();
		DexValue []values = new DexValue[count]; 
		for( int i=0; i<count; i++ ) {
			values[i] = readEncodedValue();
		}
		return values;
	}
	private DexValue readEncodedValue( ) throws ParseException {
		byte tmp[] = new byte[8];
		int type = data.get();
		int size = ((type >> 5) & 0x07);
		type &= 0x1f;
		
		Object obj = null;
		if( type == DexValue.ARRAY ) {
			obj = readEncodedArray();
		} else if( type == DexValue.ANNOTATION ) {
			obj = readEncodedAnnotation(DexAnnotation.VISIBILITY_NONE);
		} else if( type == DexValue.BOOLEAN ) {
			obj = (size & 1) == 0 ? Boolean.FALSE : Boolean.TRUE;
		} else if( type == DexValue.NULL ) {
			obj = null;
		} else {
			/* Read bytes and zero-pad to the right */
			data.get(tmp, 0, size+1);
			byte pad = 0;
			if( (type == DexValue.SHORT || type == DexValue.INT || type == DexValue.LONG) &&
				(tmp[size] & 0x80) != 0 ) {
				pad = -1;
			}
					
			for( int i=size+1; i<8; i++ ) 
				tmp[i] = pad;
			ByteBuffer buf = ByteBuffer.wrap(tmp);
			buf.order(data.order());
			switch( type ) {
			case DexValue.BYTE: obj = new Byte(buf.get()); break;
			case DexValue.SHORT: obj = new Short(buf.getShort()); break;
			case DexValue.CHAR: obj = new Character(buf.getChar()); break;
			case DexValue.INT: obj = new Integer(buf.getInt()); break;
			case DexValue.LONG: obj = new Long(buf.getLong()); break;
			case DexValue.FLOAT: obj = new Float(buf.getFloat()); break;
			case DexValue.DOUBLE: obj = new Double(buf.getDouble()); break;
			case DexValue.STRING: obj = lookupStringId(buf.getInt()); break;
			case DexValue.TYPE: obj = lookupTypeNameId(buf.getInt()); break;
			case DexValue.FIELD: obj = lookupFieldId(buf.getInt()); break; 
			case DexValue.METHOD: obj = lookupMethodId(buf.getInt()); break;
			case DexValue.ENUM: obj = lookupFieldId(buf.getInt()); break;
			default:
				throw new ParseException( "Invalid value type: " + type );
			}
		}
		
		return new DexValue(type,obj);
	}

	/**
	 * Read the method + prototype tables together into a single array of
	 * DexMethod objects.
	 * @param protoOffset
	 * @param protoSize
	 * @param methodOffset
	 * @param methodSize
	 * @return
	 * @throws ParseException
	 */
	private DexMethod [] readMethodTable( int protoOffset, int protoSize,
			int methodOffset, int methodSize ) throws ParseException {
		DexMethod[] protos = new DexMethod[protoSize];
		for( int i=0; i<protoSize; i++ ) {
			int offset = protoOffset + (i*12);
			String returnType = readTypeId(offset+4);
			String []paramTypes = readTypeListPtr(offset+8);
			protos[i] = new DexMethod(null, returnType, paramTypes,0);
		}
	
		DexMethod[] methods = new DexMethod[methodSize];
		for( int i=0; i<methodSize; i++ ) {
			int offset = methodOffset + (i*8);
			String type = readShortTypeId(offset);
			int protoIdx = data.getShort(offset+2);
			if( protoIdx < 0 || protoIdx >= protos.length)
				throw new ParseException("Invalid prototype ID");
			String name = readStringId(offset+4);
			methods[i] = new DexMethod(type, name, protos[protoIdx]);
		}
		return methods;
	}
	
	/**
	 * Read the field table
	 * @param fieldOffset
	 * @param fieldSize
	 * @return
	 * @throws ParseException
	 */
	private DexField [] readFieldTable( int fieldOffset, int fieldSize ) 
		throws ParseException {
		DexField[] fields = new DexField[fieldSize];
		for( int i=0; i<fieldSize; i++ ) {
			int offset = fieldOffset + (i*8);
			String classType = readShortTypeId(offset);
			String type = readShortTypeId(offset+2);
			String name = readStringId(offset+4);
			fields[i] = new DexField(classType, name,type,0);
		}
		return fields;
	}
	
	/**
	 * Read the string table from the Dex file, given the offset and count
	 * of entries. The table contains a set of absolute 32-bit offsets in
	 * the file for the actual string data.
	 */
	private String [] readStringTable(int fileOffset, int nEntries) throws ParseException {
		String [] strings = new String[nEntries];
		for( int i=0; i<nEntries; i++ ) {
			int stroffset = data.getInt(fileOffset + i*4);
			data.position(stroffset);
			strings[i] = readModifiedUTF8String();
		}
		return strings;
	}
	
	/**
	 * Read the type name table from the Dex file, given the offset and count
	 * of entries. The stringTable must have already been read.
	 * @param fileOffset
	 * @param nEntries
	 * @return
	 * @throws ParseException
	 */
	private String [] readTypeNameTable(int fileOffset, int nEntries)
		throws ParseException {
		String [] result = new String[nEntries];
		for( int i=0; i<nEntries; i++ ) {
			result[i] = readStringId(fileOffset + i*4);
		}		
		return result;
	}
	
	private String [] readTypeListPtr(int fileOffset) throws ParseException {
		return readTypeList( data.getInt(fileOffset) );
	}
	
	/**
	 * Read a list of types from the Dex file at a given offset. The
	 * typeNameTable must have already been read. Does not modify
	 * the file position.
	 * @param fileOffset Start of the type list.
	 * @return a (possible empty) array of type names.
	 * @throws ParseException if the list contains invalid entries.
	 */
	private String [] readTypeList(int fileOffset) throws ParseException {
		if( fileOffset == 0 ) {
			return new String[0];
		}
		int nEntries = data.getInt(fileOffset);
		String [] result = new String[nEntries];
		for( int i=0; i<nEntries; i++ ) {
			result[i] = readShortTypeId(fileOffset + (i*2)+4 );
		}
		return result;
	}

	/**
	 * Read a type id from the file at the given offset. The type name
	 * table must have been previously read.
	 * @param fileOffset Offset to read the type id from.
	 * @return The type name string, or null if the file contains the
	 * NO_INDEX value.
	 * @throws ParseException if the index is invalid.
	 */
	private String readTypeId(int fileOffset) throws ParseException {
		int index = data.getInt(fileOffset);
		if( index == NO_INDEX ) 
			return null;
		else return lookupTypeNameId(index);
	}
	
	/**
	 * Read a type id from the file at the given offset. The type name
	 * table must have been previously read.
	 * @param fileOffset Offset to read the type id from.
	 * @return The type name string, or null if the file contains the
	 * NO_INDEX value.
	 * @throws ParseException if the index is invalid.
	 */
	private String readShortTypeId(int fileOffset) throws ParseException {
		int index = data.getShort(fileOffset);
		if( index == NO_INDEX ) 
			return null;
		else return lookupTypeNameId(index);
	}

	/**
	 * Read a string id from the file at the given offset. The string
	 * table must have been previously read.
	 * @param fileOffset Offset to read the type id from.
	 * @return The string, or null if the file contains the
	 * NO_INDEX value.
	 * @throws ParseException if the index is invalid.
	 */
	private String readStringId(int fileOffset) throws ParseException {
		int index = data.getInt(fileOffset);
		if( index == NO_INDEX )
			return null;
		else return lookupStringId(index);
	}
	
	/**
	 * Read a String from the current position in modified UTF8 format 
	 * (as defined in java.io.DataInput)
	 * @return the String
	 */
	private String readModifiedUTF8String() throws ParseException {
		int len = readULEB128();
		char tmp[] = new char[len];
		for( int i=0; i<len; i++ ) {
			int b1 = (int)data.get();
			if( (b1 & 0x80) == 0 ) { 
				tmp[i] = (char)b1;
			} else if( (b1 & 0xE0) == 0xC0 ) {
				int b2 = (int)data.get();
				if( (b2 & 0xC0) != 0x80 ) 
					throw new ParseException( "Error decoding string (invalid data)");
				tmp[i] = (char)(((b1&0x1F)<<6) | (b2&0x3F));
			} else if( (b1 & 0xF0) == 0xE0 ) {
				int b2 = (int)data.get();
				int b3 = (int)data.get();
				if( ((b2 & 0xC0) != 0x80) || ((b3 & 0xC0) != 0x80) ) 
					throw new ParseException( "Error decoding string (invalid data)");
				tmp[i] = (char)(((b1&0x0F)<<12) | ((b2&0x3F)<<6) | (b3&0x3F));
			} else {
				throw new ParseException( "Error decoding string (invalid data)" );
			}
		}
		if( data.get() != 0 ) {
			throw new ParseException( "Error decoding string (missing null terminator)" );
		}
		return new String(tmp);
	}

	/**
	 * Read an unsigned little-endian base 128 number from the current buffer position.
	 * @return
	 */
	private int readULEB128() {
		int result = 0;
		int shift = 0;
		byte b;
		do {
			b = data.get();
			result |= (((int)(b & 0x7F))<<shift);
			shift += 7;
		} while( (b & 0x80) != 0 );
		return result;
	}
	
	private int readULEB128p1() {
		return readULEB128()-1;
	}
	
	private int readSLEB128() {
		int result = 0;
		int shift = 0;
		byte b;
		do {
			b = data.get();
			result |= (((int)(b & 0x7F))<<shift);
			shift += 7;
		} while( (b & 0x80) != 0 );
		if( (b & 0x40) != 0) {
			int sext = (-1 << shift);
			result |= sext;
		}
		return result;		
	}
	
	private String lookupStringId( int id ) throws ParseException {
		if( id < 0 || id >= stringTable.length ) {
			throw new ParseException( "Invalid string id " + id );
		}
		return stringTable[id];
	}

	private String lookupTypeNameId( int id ) throws ParseException {
		if( id < 0 || id >= typeNameTable.length ) {
			throw new ParseException( "Invalid type id " + id );
		}
		return typeNameTable[id];
	}
	
	private DexField lookupFieldId( int id ) throws ParseException {
		if( id < 0 || id >= fieldTable.length ) {
			throw new ParseException( "Invalid field id " + id );
		}
		return fieldTable[id];		
	}
	
	private DexMethod lookupMethodId( int id ) throws ParseException {
		if( id < 0 || id >= methodTable.length ) {
			throw new ParseException( "Invalid method id " + id );
		}
		return methodTable[id];				
	}

	/************************** Post-parse analysis *************************/
	
	private void postParse( DexFile file ) {
		ComputeUseDefInfo info = new ComputeUseDefInfo();
		info.analyse(file);
	}
}
