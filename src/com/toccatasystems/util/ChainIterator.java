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
