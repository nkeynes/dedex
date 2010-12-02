
package com.toccatasystems.dedex;

import java.io.IOException;
import java.util.Iterator;
import org.apache.commons.cli.*;

import com.toccatasystems.dalvik.*;

/**
 * 
 * @author nkeynes
 *
 */
public class Main {

	private final static String DEDEX_VERSION = "0.1";
	private final static String DEDEX_COPYRIGHT = "Copyright (c) 2010 Toccata Systems. All Rights Reserved.";
	private static Options commandLineOptions;
	
	/**
	 * Construct the command line options statically
	 */
	static {
		Options options = new Options();
		options.addOption("d", "dir", true, "Specify where to place generated class files");
		options.addOption("D", "dump", false, "Dump file information to console");
		options.addOption("j","jar",true,"Specify an output JAR file to generate");
		options.addOption("h","help",false, "Print this help message");
		commandLineOptions = options;
	}
	
	private static void printUsage() {
		System.out.println("dedex " + DEDEX_VERSION + " " + DEDEX_COPYRIGHT );
		new HelpFormatter().printHelp("dedex [options] <input-dex-file>", commandLineOptions);
		System.out.println("  If no options are given, dedex will create a .jar file in the current\n" +
						   "  directory for each input .dex file." );
	}
	
	private static CommandLine parseCommandLine( String [] args ) {
		GnuParser parser = new GnuParser();
		CommandLine cl = null;
		try {
			cl = parser.parse(commandLineOptions, args);
		} catch (org.apache.commons.cli.ParseException e) {
			System.err.println( e.getMessage() );
			printUsage();
			System.exit(1);
		}

		if( cl.hasOption('h') || cl.getArgList().size() == 0 ) {
			printUsage();
			System.exit(0);
		}
		
		int i = 0;
		if( cl.hasOption('j') )
			i++;
		if( cl.hasOption('d') )
			i++;
		if( cl.hasOption('D') )
			i++;
		if( i > 1 ) {
			System.err.println( "Please specify only one of -d, -D, or -j on the command-line" );
			printUsage();
			System.exit(1);
		}
		return cl;
	}
		
	
	@SuppressWarnings("unchecked")
	public static void main( String [] args ) {
		CommandLine cl = parseCommandLine(args);

		ClassOutputWriter writer = null;

		String jar = cl.getOptionValue('j');
		String outputdir = cl.getOptionValue('d');
		if( jar != null ) {
			try {
				writer = new JarClassWriter(jar);
			} catch( IOException e ) {
				System.err.println( "Error: Unable to create jar '" + jar + "': " + e.getMessage() );
				System.exit(2);
			}
		} else if( outputdir != null ) {
			writer = new FileClassWriter(outputdir);
		} else {
			writer = new MultiJarClassWriter();
		}
		DexToClassTransformer transform = new DexToClassTransformer(writer);

		DexParser parser = new DexParser(); 
		for( Iterator<String> it = cl.getArgList().iterator(); it.hasNext(); ) {
			String file = it.next();
			DexFile dex = null;
			try {
				dex = parser.parseFile(file);
			} catch( Exception e ) { 
				System.err.println( "Error: Unable to load " + file + ": " + e.getMessage() );
				e.printStackTrace();
				System.exit(1);
			}
			
			if( cl.hasOption('D') ) {
				DexDump dump = new DexDump(System.out);
				dex.visit(dump);
			} else {
				dex.visit( transform );
			}
		}
		writer.close();
	}
	
}
