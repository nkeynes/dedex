
package com.toccatasystems.dedex;

import java.util.Iterator;
import java.util.List;
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
		return cl;
	}
	
	
	public static void main( String [] args ) {
		CommandLine cl = parseCommandLine(args);
		DexParser parser = new DexParser(); 
		for( Iterator it = cl.getArgList().iterator(); it.hasNext(); ) {
			String file = (String)it.next();
			try {
				DexFile df = parser.parseFile(file);
				if( cl.hasOption('D') ) {
					DexDump dump = new DexDump(System.out);
					df.visit(dump);
				}
			} catch( Exception e ) { 
				System.err.println( "Error: Unable to load " + file + ": " + e.getMessage() );
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
}
