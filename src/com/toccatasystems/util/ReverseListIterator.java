package com.toccatasystems.util;

import java.util.List;
import java.util.ListIterator;

/**
 * ListIterator implementation that traverses a list in reverse order (ie
 * last element first).
 * @author nkeynes
 */
public class ReverseListIterator<T> implements ListIterator<T> {
	private ListIterator<T> it;
	
	public ReverseListIterator(List<T> list) {
		it = list.listIterator(list.size()-1);
	}
	
	public ReverseListIterator(List<T> list, int posn) {
		it = list.listIterator(posn);
	}

	public boolean hasNext() {
		return it.hasPrevious();
	}

	public T next() {
		return it.previous();
	}

	public void remove() {
		it.remove();
	}

	public void add(T e) {
		it.add(e);
	}

	public boolean hasPrevious() {
		return it.hasNext();
	}

	public int nextIndex() {
		return it.previousIndex();
	}

	public T previous() {
		return it.next();
	}

	public int previousIndex() {
		return it.nextIndex();
	}

	public void set(T e) {
		it.set(e);
	}
}