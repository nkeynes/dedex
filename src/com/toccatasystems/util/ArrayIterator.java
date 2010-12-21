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
