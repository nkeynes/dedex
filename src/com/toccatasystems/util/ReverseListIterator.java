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