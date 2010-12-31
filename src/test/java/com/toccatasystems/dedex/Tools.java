package com.toccatasystems.dedex;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import com.toccatasystems.io.MultiplexReader;

/**
 * Test environment - locates and executes the tools needed to run the test
 * cases.
 * @author nkeynes
 *
 */
public class Tools {

	private final static String DX = "dx";
	private final static String JAVAC = "javac";
	
	private File dxcmd;
	private File javaccmd;
	private String classpath;
	
	public Tools() {
		dxcmd = findDx();
		javaccmd = findJavac();
		classpath = System.getProperty("java.class.path");
	}
	
	public void printEnv( PrintStream out ) {
		out.println("Environment:");
		Map<String,String> env = System.getenv();
		for( Iterator<Map.Entry<String,String>> it = env.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String,String> ent = it.next();
			out.println( ent.getKey() + " = " + ent.getValue() );
		}
		Properties props = System.getProperties();
		out.println("Properties:");
		for( Iterator<Map.Entry<Object,Object>> it = props.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Object,Object> ent = it.next();
			out.println( ent.getKey().toString() + " = " + ent.getValue() );
		}
		
	}
	
	public void compile( File classDir, File []sourceFiles ) throws IOException {
		if( javaccmd == null ) {
			throw new RuntimeException("Error: Unable to find javac - ensure JAVA_HOME is set");
		}
		String args[] = new String[sourceFiles.length+5];
		args[0] = javaccmd.toString();
		args[1] = "-classpath";
		args[2] = classpath;
		args[3] = "-d";
		args[4] = classDir.toString();
		for( int i=0; i<sourceFiles.length; i++ ) {
			args[i+5] = sourceFiles[i].toString();
		}
		System.out.println( StringUtils.join(args, " ") );
		exec( "javac", args );
	}
	
	public void dex( File dexFile, File classDir ) throws IOException {
		if( dxcmd == null ) {
			throw new RuntimeException("Error: Unable to find dx - ensure ANDROID_HOME is set");
		}
		String args[] = new String[4];
		args[0] = dxcmd.toString();
		args[1] = "--dex";
		args[2] = "--output=" + dexFile.toString();
		args[3] = classDir.toString();
		System.out.println( StringUtils.join(args, " ") );
		exec( "dx", args );
	}
	

	private void exec( String name, String []cmd ) throws IOException {
		Process p = Runtime.getRuntime().exec(cmd);
		p.getOutputStream().close();
		MultiplexReader in = new MultiplexReader(p.getInputStream(), name+": ",
				p.getErrorStream(), name+"-err: ");
		String line;
		boolean first = true;
		while( (line = in.readLine()) != null ) {
			if( first ) /* Print a new-line before the start of output */
				System.out.println();
			first = false;
			System.out.println(line);
		}
		try {
			int result = p.waitFor();
			if( result != 0 ) {
				throw new IOException("Error: " + name + " returned error code " + result );
			}
		} catch( InterruptedException e ) {
			throw new IOException("Error: Interrupted while waiting for " + name + " to complete", e );
		}
	}
	

	private File searchPath( String filename ) {
		String PATH = System.getenv("PATH");
		if( PATH != null ) {
			String []paths = PATH.split(File.pathSeparator);
			for( int i=0; i<paths.length; i++ ) {
				File f = new File(paths[i], filename);
				if( f.canExecute() ) {
					return f;
				}
			}
		}
		return null;
	}
	
	private File findDx() {
		String androidHome = System.getenv("ANDROID_HOME");
		if( androidHome != null ) {
			File android = new File(androidHome);
			if( android.isDirectory() ) {
				File platforms[] = new File(android, "platforms").listFiles();
				File dx = null;
				if( platforms != null && platforms.length > 1 ) {
					long timestamp = 0;
					/* Pick the most recent dx */
					for( int i=0; i<platforms.length; i++ ) {
						File f = new File(new File(platforms[i], "tools"), DX);
						if( f.canExecute() ) {
							long tmp = platforms[i].lastModified();
							if( tmp > timestamp ) {
								timestamp = tmp;
								dx = f;
							}
						}
					}
				}
				
				if( dx != null ) {
					return dx;
				}
			}
		}
		
		return searchPath(DX);
	}
	
	private File findJavac() {
		File javaBin = new File(System.getProperty("java.home"), "bin");
		File javac = new File(javaBin,JAVAC);
		if( javac.canExecute() ) {
			return javac;
		}
		javaBin = new File(System.getenv("JAVA_HOME"), "bin");
		javac = new File(javaBin, JAVAC);
		if( javac.canExecute() ) {
			return javac;
		}
		return searchPath(JAVAC);
	}	

}
