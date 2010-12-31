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

import java.util.ListIterator;

public class ArrayIterator<E> implements ListIterator<E> {
	private E[] array;
	private int index;
	private int fwdDir;
	
	
	public ArrayIterator( E[] arr ) {
		this.array = arr;
		this.index = 0;
		this.fwdDir = 1;
	}
	
	@Override
	public void add(E e) {
		throw new UnsupportedOperationException("Add not supported");
	}

	@Override
	public boolean hasNext() {
		return index < array.length;
	}

	@Override
	public boolean hasPrevious() {
		return index > 0;
	}

	@Override
	public E next() {
		fwdDir = 1;
		return array[index++];
	}

	@Override
	public int nextIndex() {
		return index;
	}

	@Override
	public E previous() {
		fwdDir = 0;
		return array[--index];
	}

	@Override
	public int previousIndex() {
		return index-1;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove not supported");
	}

	@Override
	public void set(E e) {
		array[index-fwdDir] = e;
		
	}

}
