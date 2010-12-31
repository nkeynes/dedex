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

package com.toccatasystems.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A Reader that takes it's input from multiple input readers, producing
 * interleaved line output from all inputs.
 * 
 * Ideally this would be implemented as a select loop, but this isn't
 * necessarily possible for arbitrary Readers.
 * @author nkeynes
 *
 */

public class MultiplexReader extends Reader {

	private final static int MAX_QUEUE_SIZE = 10;
	private final static String EOF_SENTINEL = "";
	
	private List<QueueAdapter> readers;
	private BlockingQueue<String> queue;
	private int activeStreams = 0;
	private String currentString;
	private int currentPosn;
	
	private class QueueAdapter implements Runnable {
		private BufferedReader in;
		private String linePrefix;
		
		public QueueAdapter( BufferedReader in, String linePrefix ) {
			this.in = in;
			this.linePrefix = linePrefix;
		}
		
		public void run() {
			try {
				String line;
				while( (line = in.readLine()) != null ) {
					queue.put( linePrefix + line + "\n" );
				}
			} catch( IOException e ) {
				/* FIXME: Swallow for now */
			} catch( InterruptedException e ) {
			} finally {
				putSentinel();
				remove(this);
			}
		}
		
		public void close() {
			try {
				in.close();
			} catch( IOException e ) {
				/* FIXME: Swallow for now */
			}
		}
		
		private void putSentinel() {
			while(true) {
				try {
					queue.put(EOF_SENTINEL);
					return;
				} catch( InterruptedException e ) {
					/* Retry */
					
				}
			}
		}	
	}
	
	public MultiplexReader() {
		init();
	}
	
	/**
	 * Convenience constructor for the common case of multiplexing
	 * stdout and stderr, using the default charset encoding.
	 * 
	 * @param in1
	 * @param prefix1
	 * @param in2
	 * @param prefix2
	 */
	public MultiplexReader( InputStream in1, String prefix1,
			InputStream in2, String prefix2 ) {
		init();
		add(in1, prefix1);
		add(in2, prefix2);
	}
	
	
	public void add( InputStream in, String linePrefix ) {
		add( new BufferedReader(new InputStreamReader(in)), linePrefix );
	}
	public void add( Reader in, String linePrefix ) {
		add( new BufferedReader(in), linePrefix );
	}
	
	public void add( BufferedReader in, String linePrefix ) {
		QueueAdapter adapter = new QueueAdapter( in, linePrefix );
		synchronized(lock) {
			readers.add(adapter);
		}
		activeStreams++;
		Thread thread = new Thread(adapter);
		thread.start();
	}
	
	protected void remove( QueueAdapter adapter ) {
		synchronized(lock) {
			readers.remove(adapter);
		}
	}
	
	@Override
	public void close() throws IOException {
		synchronized(lock) {
			for( Iterator<QueueAdapter> it = readers.iterator(); it.hasNext(); ) {
				it.next().close();
			}
			readers.clear();
		}
		queue.clear();
	}
	
	private void init() {
		queue = new ArrayBlockingQueue<String>(MAX_QUEUE_SIZE);
		readers = new ArrayList<QueueAdapter>();
		currentString = null;
		activeStreams = 0;
	}

	private int fillBuffer(char []cbuf, int off, int len) {
		int avail = currentString.length() - currentPosn;
		if( avail > len ) {
			currentString.getChars(currentPosn, currentPosn+len, cbuf, off);
			currentPosn += len;
			return len;
		} else {
			currentString.getChars(currentPosn, currentString.length(), cbuf, off);
			currentString = null;
			return avail;
		}
	}
	
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		if( currentString == null ) {
			currentString = getQueue();
			currentPosn = 0;
			if( currentString == null )
				return -1;
		}
		return fillBuffer(cbuf, off, len);		 
	}
	
	public String readLine() throws IOException {
		String result;
		if( currentString != null ) {
			result = currentString;
			currentString = null;
		} else {
			result = getQueue();
			currentPosn = 0;
		}
		
		/* Return the unread substring, excluding the trailing newline */ 
		if( result != null ) 
			result = result.substring(currentPosn, result.length()-1);
		return result;
	}	
	
	private String getQueue() throws IOException {
		while( activeStreams != 0 ) {
			try {
				String item = queue.take();
				if( item == EOF_SENTINEL ) {
					activeStreams--;
				} else {
					return item;
				}
			} catch( InterruptedException e ) {
				throw new IOException("Read interrupted", e);
			}
		}
		return null;		
	}
}
