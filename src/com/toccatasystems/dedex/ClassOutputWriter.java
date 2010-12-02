package com.toccatasystems.dedex;

public interface ClassOutputWriter {

	/**
	 * Invoked at the start of a .dex file 
	 * @param file
	 */
	public void begin( String filename );
	
	/**
	 * Write the class with the given name and contents to output
	 * @param internalClassName The 'internal' class name, ie java/lang/String
	 * @param classData a byte array containing the class file.
	 */
	public void write( String internalClassName, byte []classData ); 
	
	/**
	 * Invoked at the end of a .dex file
	 * @param filename
	 */
	public void end( String filename );
	
	/**
	 * Invoked after all .dex files have been processed.
	 */
	public void close();
}
