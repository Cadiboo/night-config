package com.electronwill.nightconfig.core.impl;

import java.util.NoSuchElementException;

/**
 * A double-ended queue of chars that increases its capacity as necessary.
 * A deque can be used as a FIFO (First-In-First-Out) queue and as a LIFO (Last-In-First-Out) stack.
 *
 * @author TheElectronWill
 * @see java.util.Deque
 */
final class CharDeque {
	/**
	 * The array that contains the data. It is used as a circular buffer.
	 */
	char[] data;

	/**
	 * The position of the first element.
	 */
	int head = 0;

	/**
	 * The position of the last element + 1.
	 */
	int tail = 0;

	/**
	 * Bitmask to calculate (x MODULO data.length) faster, by doing (x AND mask).
	 * It works only if data.length is a power of two.
	 */
	int mask;

	/**
	 * Creates a new IntDeque with an initial capacity of 4.
	 */
	public CharDeque() {
		this(4);
	}

	/**
	 * Creates a new deque with the specified initial capacity. The capacity must be positive and
	 * non-zero.
	 *
	 * @param initialCapacity the initial capacity, strictly positive
	 */
	public CharDeque(int initialCapacity) {
		if (initialCapacity <= 0) {
			throw new IllegalArgumentException("The capacity must be positive and non-zero.");
		}
		if (!isPowerOfTwo(initialCapacity)) {
			initialCapacity = nextPowerOfTwo(initialCapacity);
		}
		data = new char[initialCapacity];
		mask = initialCapacity - 1;
	}

	private boolean isPowerOfTwo(int n) {
		return (n & -n) == n;// clever check based on how the numbers are represented
	}

	/**
	 * @return the smallest power of two that is strictly greater than n
	 */
	private int nextPowerOfTwo(int n) {
		return Integer.highestOneBit(n) << 1;
	}

	/**
	 * Clears this deque. After a call of this method, the size of the deque will be zero.
	 */
	public void clear() {
		head = 0;
		tail = 0;
	}

	/**
	 * Consumes the CharDeque in FIFO order.
	 * @return a new array containing the data
	 */
	public char[] consumeAllQueue() {
		if (isEmpty())
			return new char[0];

		char[] fifo = new char[size()];
		consumeAllNonEmptyQueue(fifo);
		return fifo;
	}

	/**
	 * Consumes the CharDeque in FIFO order. <b>The CharDeque must not be empty.</b>
	 * @param dst where to put the data
	 */
	public void consumeAllNonEmptyQueue(char[] dst) {
		fullNonEmptyCopy(dst);
		clear();
	}

	/**
	 * Consumes a portion of the CharDeque in FIFO order.
	 *
	 * @param dst    where to put the data
	 * @param offset where to start
	 * @param length where to end (exclusive)
	 */
	public void consumeQueue(char[] dst, int offset, int length) {
		if (length > size())
			throw new IllegalArgumentException(
				String.format("Array length (%d) is too big", length)
			);
		partialCopy(dst, offset, length);
		head = (head + length) & mask;
	}

	/**
	 * @return true if the deque is empty, false if it's not
	 */
	public boolean isEmpty() {
		return tail == head;
	}

	/**
	 * @return the size of the deque
	 */
	public int size() {
		if (tail >= head) {
			return tail - head;
		}
		return data.length - head + tail;
	}

	/**
	 * Compacts this deque, minimizing its size in memory.
	 */
	public void compact() {
		if (tail == head) {// the deque is empty
			data = new char[1];// the capacity must be non-zero
			head = 0;
			tail = 0;
			mask = 0;
			return;
		}
		int size = size();
		int newCapacity = size + 1;// +1 because the array is never kept full
		if (!isPowerOfTwo(newCapacity)) {
			newCapacity = nextPowerOfTwo(newCapacity);
		}
		char[] newData = new char[newCapacity];
		fullNonEmptyCopy(newData);
		head = 0;
		tail = size;
		data = newData;
		mask = newData.length - 1;
	}

	private void fullNonEmptyCopy(char[] dst) {
		if (tail > head) {
			System.arraycopy(data, head, dst, 0, tail - head); // [head; tail[
		} else {
			int len1 = data.length - head; // length of [head; array.length[
			System.arraycopy(data, head, dst, 0, len1);   // [head; array.length[
			System.arraycopy(data, 0, dst, len1, tail); // [0; tail[
		}
	}

	private void partialCopy(char[] dst, int dstPos, int len) {
		if (tail > head) {
			System.arraycopy(data, head, dst, dstPos, len);
		} else {
			final int rightLen = data.length - head; // [head; array.length[
			if (rightLen >= len) {
				System.arraycopy(data, head, dst, dstPos, len);
			} else {
				assert tail >= len - rightLen;
				System.arraycopy(data, head, dst, dstPos, rightLen);
				System.arraycopy(data, 0, dst, dstPos+rightLen, len-rightLen);
			}
		}
	}

	/** Doubles the deque's capacity */
	private void grow() {
		grow(data.length << 1);
	}


	/** Sets the deque's capacity. <b>cap must be a power of two!</b> */
	private void grow(int cap) {
		if (cap < 0) {// overflow!
			throw new IllegalStateException("Charray too big");
		}
		final char[] newData = new char[cap];
		final int lenght1 = data.length - head;// length of the part from the head to the end of the array
		System.arraycopy(data, head, newData, 0, lenght1);// head to end
		System.arraycopy(data, 0, newData, lenght1, tail);// start to tail
		head = 0;
		tail = data.length;
		data = newData;
		mask = newData.length - 1;
	}

	/**
	 * Inserts an element before the head of this deque. The deque increases its capacity if
	 * necessary.
	 *
	 * @param element the element to add
	 */
	public void addFirst(char element) {
		head = (head - 1) & mask;
		data[head] = element;
		if (head == tail) {//deque full
			grow();
		}
	}

	public void addFirst(Charray chars) {
		final int len = chars.length();
		if (data.length <= len) {
			grow(nextPowerOfTwo(len+1)); // +1 (and <= len) because the array is never kept full
		}
		for (int i = chars.offset; i < chars.limit; i++) {
			head = (head - 1) & mask;
			data[head] = chars.get(i);
		}
	}

	/**
	 * Inserts an element at the tail of this deque. The deque increases its capacity if necessary.
	 *
	 * @param element the element to add
	 */
	public void addLast(char element) {
		data[tail] = element;
		tail = (tail + 1) & mask;
		if (tail == head) {// deque full
			grow();
		}
	}

	public void addLast(Charray chars) {
		final int len = chars.length();
		if (data.length <= len) {
			grow(nextPowerOfTwo(len+1)); // +1 (and <= len) because the array is never kept full
		}
		for (int i = chars.offset; i < chars.limit; i++) {
			data[tail] = chars.get(i);
			tail = (tail + 1) & mask;
		}
	}

	/**
	 * Gets the element at the specified index of this deque, without removing it.
	 * <p>
	 * The index is relative to the head: the first element is at index 0, the next element is at
	 * index 1, etc.
	 *
	 * @param index the index of the element, relative to the head
	 * @return the element at the specified index
	 *
	 * @throws NoSuchElementException if the deque contains less than {@code index+1} elements
	 */
	public char get(int index) {
		if (index >= size()) {
			throw new NoSuchElementException("No element at index " + index);
		}
		return data[(head + index) & mask];
	}

	/**
	 * Gets the first element (head) of this deque without removing it.
	 *
	 * @return the first element of this deque
	 *
	 * @throws NoSuchElementException if the deque is empty
	 */
	public char getFirst() {
		if (tail == head) {
			throw new NoSuchElementException("Empty deque");
		}
		return data[head];
	}

	/**
	 * Gets the last element of this deque without removing it.
	 *
	 * @return the last element of this deque
	 *
	 * @throws NoSuchElementException if the deque is empty
	 */
	public char getLast() {
		if (tail == head) {
			throw new NoSuchElementException("Empty deque");
		}
		return data[(tail - 1) & mask];
	}

	/**
	 * Retrieves and removes the first element (head) of this deque.
	 *
	 * @return the first element of this deque
	 *
	 * @throws NoSuchElementException if the deque is empty
	 */
	public char removeFirst() {
		if (tail == head) {
			throw new NoSuchElementException("Empty deque");
		}
		char element = data[head];
		head = (head + 1) & mask;
		return element;
	}

	/**
	 * Retrieves and removes the last element of this deque.
	 *
	 * @return the last element of this deque
	 *
	 * @throws NoSuchElementException if the deque is empty
	 */
	public char removeLast() {
		if (tail == head) {
			throw new NoSuchElementException("Empty deque");
		}
		tail = (tail - 1) & mask;
		return data[tail];
	}
}
