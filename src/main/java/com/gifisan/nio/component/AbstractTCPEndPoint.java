package com.gifisan.nio.component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.gifisan.nio.NetworkException;
import com.gifisan.nio.common.Logger;
import com.gifisan.nio.common.LoggerFactory;
import com.gifisan.nio.component.future.IOReadFuture;
import com.gifisan.nio.server.NIOContext;

public abstract class AbstractTCPEndPoint extends AbstractEndPoint implements TCPEndPoint {

	private AtomicBoolean		_closed		= new AtomicBoolean(false);
	private boolean 			_networkWeak	= false;
	private int				attempts		= 0;
	private SocketChannel		channel		= null;
	private IOWriteFuture		currentWriter	= null;
	private EndPointWriter		endPointWriter = null;
	private IOReadFuture		readFuture	= null;
	private SelectionKey		selectionKey	= null;
	private Socket				socket		= null;
	private AtomicInteger		writers		= new AtomicInteger();
	private boolean			endConnect	= false;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTCPEndPoint.class);
	

	public AbstractTCPEndPoint(NIOContext context, SelectionKey selectionKey,EndPointWriter endPointWriter) throws SocketException {
		super(context);
		this.selectionKey = selectionKey;
		this.endPointWriter = endPointWriter;
		this.channel = (SocketChannel) selectionKey.channel();
		this.socket = channel.socket();
		if (socket == null) {
			throw new SocketException("socket is empty");
		}
	}

	public void attackNetwork(int length) {
		
		if (length == 0) {
			if (_networkWeak) {
				return;
			}
			
			if (++attempts > 255) {
				this.interestWrite();
				_networkWeak = true;
			}
			return;
		}
		attempts = 0;
	}

	public void close() throws IOException {
		
		if (writers.get() > 0) {
			return ;
		}
		
		if (_closed.compareAndSet(false, true)) {
			
			this.endConnect = true;
			
			this.selectionKey.attach(null);

			LOGGER.debug(">>>> rm {}",this.toString());

			Session session = getSession();
			
			session.destroyImmediately();

			this.channel.close();
			
			this.extendClose();
			
		}
	}

	public void decrementWriter(){
		writers.decrementAndGet();
	}

	protected void extendClose(){}

//	interface Pusher {
//
//		void push(IOWriteFuture future) throws IOException;
//	}
//
//	private Pusher	_localPusher	= new Pusher() {
//
//		public void push(IOWriteFuture future) {
//			writers.add(future);
//		}
//	};
//
//	private Pusher	_remotePusher	= new Pusher() {
//
//		public void push(IOWriteFuture future) throws IOException {
//			if (!context.getEndPointWriter().offer(future)) {
//				future.catchException(WriterOverflowException.INSTANCE);
//			}
//		}
//	};

//	private Pusher	_currentPusher	= _localPusher;

	public void flushWriters() throws IOException {
//		this._currentPusher = _remotePusher;
		
//		List<IOWriteFuture> writers = this.writers;

		// ReentrantLock lock = this.lock;
		//
		// lock.lock();

//		if (this.currentWriter == null) {
			this.flushWriters0();
//		}else{
//			
//			if(this.currentWriter.write()){
//				this.decrementWriter();
//				this.currentWriter.onSuccess();
//				this.currentWriter = null;
//				this.flushWriters0();
//			 }else{
//				 return;
//			 }
//		}

//		for (IOWriteFuture writer : writers) {
//			if (!endPointWriter.offer(writer)) {
//				writer.catchException(WriterOverflowException.INSTANCE);
//			}
//		}

//		writers.clear();

//		_currentPusher = _localPusher;

		// lock.unlock();

	}
	
	protected void flushWriters0(){
		
		endPointWriter.collect();
		
		this.attempts = 0;
		
		this._networkWeak = false;
		
		selectionKey.interestOps(SelectionKey.OP_READ);
		
//		if (endConnect) {
//			
//			EndPoint endPoint = this;
//			
//			CloseUtil.close(endPoint);
//		}
	}

	public EndPointWriter getEndPointWriter() {
		return endPointWriter;
	}

	protected InetSocketAddress getLocalSocketAddress() {
		if (local == null) {
			local = (InetSocketAddress)socket.getLocalSocketAddress();
		}
		return local;
	}

	public int getMaxIdleTime() throws SocketException {
		return socket.getSoTimeout();
	}

	public IOReadFuture getReadFuture() {
		return readFuture;
	}

	protected InetSocketAddress getRemoteSocketAddress() {
		if (remote == null) {
			remote = (InetSocketAddress)socket.getRemoteSocketAddress();
		}
		return remote;
	}

	public void incrementWriter(){
		writers.incrementAndGet();
	}

	private void interestWrite() {
		selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
	}

	public boolean isBlocking() {
		return channel.isBlocking();
	}

	public boolean isNetworkWeak() {
		return _networkWeak;
	}

	public boolean isOpened() {
		return this.channel.isOpen();
	}
	
	public ByteBuffer read(int limit) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(limit);
		this.read(buffer);
		if (buffer.position() < limit) {
			throw new NetworkException("poor network ");
		}
		return buffer;
	}

	public int read(ByteBuffer buffer) throws IOException {
		return this.channel.read(buffer);
	}
	
	public void setCurrentWriter(IOWriteFuture writer) {
		this.currentWriter = writer;
	}

	public void setReadFuture(IOReadFuture readFuture) {
		this.readFuture = readFuture;
	}

	public int write(ByteBuffer buffer) throws IOException {
		return channel.write(buffer);
	}
	
	protected String getMarkPrefix() {
		return "TCP";
	}
	
	public void endConnect() {
		this.endConnect = true;
	}
	
	public boolean isEndConnect() {
		return endConnect;
	}

	public IOWriteFuture getCurrentWriter() {
		return currentWriter;
	}
	
}