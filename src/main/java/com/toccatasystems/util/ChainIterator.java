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

package com.toccatasystems.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * Iterator that combines two or more child iterators sequentially.
 * @author nkeynes
 *
 */
public class ChainIterator<E> implements Iterator<E> {

	private List<Iterator<E>> iterators; 
	
	public ChainIterator( Iterator<E> a, Iterator<E> b ) {
		iterators = new LinkedList<Iterator<E>>();
		iterators.add( a );
		iterators.add( b );
	}
	
	public ChainIterator( Collection<E> a, Collection<E> b ) {
		iterators = new LinkedList<Iterator<E>>();
		iterators.add( a.iterator() );
		iterators.add( b.iterator() );
	}
		
	
	public boolean hasNext() {
		while(true) {
			if( iterators.isEmpty() )
				return false;
			boolean test = iterators.get(0).hasNext();
			if( test )
				return true;
			iterators.remove(0);
		}
	}

	public E next() {
		if( iterators.isEmpty() )
			throw new NoSuchElementException();
		return iterators.get(0).next();
	}

	public void remove() {
		if( iterators.isEmpty() )
			throw new NoSuchElementException();
		iterators.get(0).remove();
	}

}
