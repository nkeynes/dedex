/**
 * Copyright (c) 2010 Toccata Systems.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

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
