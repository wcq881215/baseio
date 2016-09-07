package com.generallycloud.nio.buffer;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import com.generallycloud.nio.component.TCPEndPoint;

public class PooledByteBufferGroup implements ByteBuf {

	private ByteBuf[]		bufs;

	private ByteBuf		currentBuf;

	private ByteBuf		tailBuf;
	
	private ByteBuf		headBuf;

	private int			capacity;

	private int			position;

	private int			limit;

	private boolean		released;

	private ReferenceCount	referenceCount;
	
	private int bufIndex = 0;

	private ReentrantLock	lock	= new ReentrantLock();

	public PooledByteBufferGroup(ByteBuf[] bufs, int capacity, int limit) {
		this.bufs = bufs;
		this.capacity = capacity;
		this.limit = limit;
		this.headBuf = bufs[0];
		this.currentBuf = headBuf;
		this.tailBuf = bufs[bufs.length - 1];
		this.referenceCount = new ReferenceCount();
		initBuf(tailBuf, limit);
	}

	private void initBuf(ByteBuf lastBuf, int limit) {

		int _limit = limit % lastBuf.capacity();

		lastBuf.limit(_limit);
	}

	PooledByteBufferGroup() {
	}

	public void release() {

		ReentrantLock lock = this.lock;

		lock.lock();

		try {

			if (released) {
				throw new ReleasedException("released");
			}

			if (referenceCount.deincreament() > 0) {
				return;
			}

			released = true;

			for (ByteBuf buf : bufs) {
				buf.release();
			}

		} finally {
			lock.unlock();
		}
	}

	public int read(TCPEndPoint endPoint) throws IOException {
		
		ByteBuf buf = findBuf();
		
		int read = buf.read(endPoint);
		
		position += read;
		
		return read;
	}

	private ByteBuf findBuf() {
		
		ByteBuf buf = currentBuf;
		
		if (!buf.hasRemaining()) {
			
			bufIndex++;
			
			if (bufIndex < bufs.length) {
				
				buf = currentBuf = bufs[bufIndex];
				
			}else{
				
				throw new BufferException("no buf available");
			}
		}
		return buf;
	}

	public int write(TCPEndPoint endPoint) throws IOException {
		
		ByteBuf buf = findBuf();
		
		int read = buf.write(endPoint);
		
		position += read;
		
		return read;
	}

	public void getBytes(byte[] dst) {
		getBytes(dst, 0, dst.length);
	}

	//FIXME offset buwei0shiyouwenti
	public void getBytes(byte[] dst, int offset, int length) {
		
		if (offset != 0) {
			throw new UnsupportedOperationException();
		}
		
		int unit_capacity = ByteBuf.UNIT_CAPACITY;
		
		if (length < unit_capacity) {
			
			headBuf.getBytes(dst, offset, length);
			return;
		}
		
		int size = length / unit_capacity;
		
		for (int i = 0; i < size; i++) {
			
			bufs[i].getBytes(dst, unit_capacity * i, unit_capacity);
		}
		
		int remain = length % unit_capacity;
		
		if (remain > 0) {
			bufs[size].getBytes(dst, size * unit_capacity, remain);
		}
	}

	public void putBytes(byte[] src) {
		putBytes(src, 0, src.length);
	}

	//FIXME offset buwei0shiyouwenti
	public void putBytes(byte[] src, int offset, int length) {
		
		if (offset != 0) {
			throw new UnsupportedOperationException();
		}
		
		int unit_capacity = ByteBuf.UNIT_CAPACITY;
		
		if (length < unit_capacity) {
			
			headBuf.putBytes(src, offset, length);
			return;
		}
		
		int size = length / unit_capacity;
		
		for (int i = 0; i < size; i++) {
			
			bufs[i].putBytes(src, unit_capacity * i, unit_capacity);
		}
		
		int remain = length % unit_capacity;
		
		if (remain > 0) {
			bufs[size+1].putBytes(src, size * unit_capacity, remain);
		}
	}
	
	public void flip(){
		limit = position;
		position = 0;
		
		int size = (limit + ByteBuf.UNIT_CAPACITY - 1) / ByteBuf.UNIT_CAPACITY;
		
		for (int i = 0; i < size; i++) {
			bufs[i].flip();
		}
	}

	public ByteBuf duplicate() {

		ReentrantLock lock = this.lock;

		lock.lock();

		try {

			if (released) {
				throw new ReleasedException("released");
			}

			PooledByteBufferGroup group = new PooledByteBufferGroup();

			group.bufs = copyBufs();
			group.referenceCount = referenceCount;
			group.referenceCount.increament();

			return group;

		} finally {
			lock.unlock();
		}
	}

	private ByteBuf[] copyBufs() {

		ByteBuf[] bufs = this.bufs;

		ByteBuf[] copy = new ByteBuf[this.bufs.length];

		for (int i = 0; i < copy.length; i++) {
			copy[i] = bufs[i].duplicate();
		}

		return copy;
	}

	public int remaining() {
		return limit - position;
	}

	public int position() {
		return position;
	}

	public void position(int position) {
		this.position = position;
	}

	public int limit() {
		return limit;
	}

	public void limit(int limit) {
		
		this.limit = limit;
		
		int _limit = limit % ByteBuf.UNIT_CAPACITY;
		
		if (_limit == 0) {
			_limit = ByteBuf.UNIT_CAPACITY;
		}
		
		tailBuf.limit(_limit);
	}

	public int capacity() {
		return capacity;
	}

	public boolean hasRemaining() {
		return remaining() > 0;
	}

	public boolean hasArray() {

		if (bufs.length == 1) {
			return bufs[0].hasArray();
		}

		return false;
	}

	public void clear() {
		limit = capacity;
		position = 0;
		for (ByteBuf buf : bufs) {
			buf.clear();
		}
	}
	
	public void touch() {
		
	}

	public int getInt() {
		return currentBuf.getInt();
	}

	public long getLong() {
		return currentBuf.getLong();
	}
	
	private ByteBuf findBuf(int offset){
		int bufIndex = offset / ByteBuf.UNIT_CAPACITY;
		return bufs[bufIndex];
	}

	public int getInt(int offset) {
		return findBuf(offset).getInt(offset % ByteBuf.UNIT_CAPACITY);
	}

	public long getLong(int offset) {
		return findBuf(offset).getLong(offset % ByteBuf.UNIT_CAPACITY);
	}

	public byte[] array() {
		return bufs[0].array();
	}
	
}
