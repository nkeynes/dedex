package com.toccatasystems.dedex;

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
	
	public JarClassWriter( String jarFile ) throws IOException {
		filename = jarFile;
		jar = new JarOutputStream(new FileOutputStream(jarFile));
		failure = false;
	}
	
	public boolean hasFailure() {
		return failure;
	}

	public void begin( String filename ) { }
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
			jar.putNextEntry( new JarEntry(internalClassName + ".class") );
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
