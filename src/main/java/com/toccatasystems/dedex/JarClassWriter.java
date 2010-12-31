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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * ClassOutputWriter that writes class files out into a .jar file 
 * 
 * @author nkeynes
 *
 */
public class JarClassWriter implements ClassOutputWriter {

	JarOutputStream jar;
	String filename;
	boolean failure;
	long timestamp;
	
	public JarClassWriter( File jarFile ) throws IOException {
		filename = jarFile.toString();
		jar = new JarOutputStream(new FileOutputStream(jarFile));
		failure = false;
	}
	
	public JarClassWriter( String jarFile ) throws IOException {
		filename = jarFile;
		jar = new JarOutputStream(new FileOutputStream(jarFile));
		failure = false;
	}
	
	public boolean hasFailure() {
		return failure;
	}
	
	public void begin( String filename, long timestamp ) { this.timestamp = timestamp; }
	public void end( String filename ) { }
	
	public void close() {
		try {
			jar.close();
		} catch( IOException e ) {
			System.err.println( "Error finalizing jar '" + filename + "': " + e.getMessage() );
		}
	}
	
	public void write(String internalClassName, byte[] classData) {
		try {
			JarEntry entry = new JarEntry(internalClassName + ".class");
			if( timestamp != 0 ) 
				entry.setTime(timestamp);
			jar.putNextEntry( entry );
			jar.write(classData);
			jar.closeEntry();
		} catch( IOException e ) {
			if( !failure ) {
				failure = true;
				System.err.println( "Error writing to jar '" + filename + "': " + e.getMessage() );
			}
		}
	}

}
