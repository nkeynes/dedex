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

/**
 * ClassOutputWriter that writes class files out individually to a particular 
 * directory (preserving package directories).
 * 
 * @author nkeynes
 *
 */
public class FileClassWriter implements ClassOutputWriter {

	boolean failure;
	private String baseDir;
	
	public FileClassWriter( String baseDir ) {
		this.baseDir = baseDir;
		if( baseDir == null || baseDir.length() == 0 ) {
			baseDir = ".";
		}
	}
	
	public boolean hasFailure() {
		return failure;
	}
	
	public void close() {
	}
	
	public void begin( String filename ) { }
	public void end( String filename ) { }
	
	public void write(String internalClassName, byte[] classData) {
		// TODO Auto-generated method stub
		File f = new File(baseDir, internalClassName + ".class");
		File dir = f.getParentFile();
		dir.mkdirs();
		if( !dir.isDirectory() ) {
			System.err.println("Error: Unable to create directory '" + dir.toString() + "'" );
			failure = true;
			return;
		}
		
		try {
			FileOutputStream out = new FileOutputStream(f);
			out.write(classData);
			out.close();
		} catch( IOException e ) {
			f.delete();
			System.err.println("Error: Unable to write file '" + f.toString() + "':" + e.getMessage() );
			failure = true;
		}
	}

}
