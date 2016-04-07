package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerTimeServer implements Runnable{
	private Selector selector;
	
	private ServerSocketChannel serverChannle;
	
	private volatile boolean stop;
	
	public MultiplexerTimeServer(int port)
	{
		
		try {
			selector = Selector.open();
			serverChannle = ServerSocketChannel.open();
			serverChannle.socket().bind(new InetSocketAddress("127.0.0.1",port));
			serverChannle.configureBlocking(false);
			//监听某个端口的OP_ACCEPT
			serverChannle.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void stop()
	{
		this.stop = true;
	}
	
	public void run() {
		while(!stop)
		{
			try {
				selector.select(1000);
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectedKeys.iterator();
				SelectionKey key = null;
				while(it.hasNext())
				{
					key = it.next();
					it.remove();
					try {
						handleInput(key);
					} catch (Exception e) {
						if (key != null) {
							key.cancel();
							if (key.channel() != null) {
								key.channel().close();
							}
						}
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		if (selector != null) {
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	private void handleInput(SelectionKey key) throws IOException {
		if (key.isValid()) {
			if (key.isAcceptable()) {
				ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
				SocketChannel sc = ssc.accept();
				sc.configureBlocking(false);
				sc.register(selector, SelectionKey.OP_READ);
				System.out.println("client is comming: "+ sc.getRemoteAddress());
			}
			if (key.isReadable()) {
				SocketChannel sc1 = (SocketChannel) key.channel();
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes = sc1.read(readBuffer);
				if (readBytes > 0) {
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("the time server receive order: "+ body);
					String currentTime = "QUERY TIME ORDER".endsWith(body)?new java.util.Date(System.currentTimeMillis()).toString():"BAD ORDER";
					doWrite(sc1, currentTime);
				} else if (readBytes < 0) {
					key.cancel();
					sc1.close();
				}
			}else{
				;
			}
		}
	}

	private void doWrite(SocketChannel channel, String response) throws IOException {
		byte[] bytes = response.getBytes();
		ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
		writeBuffer.put(bytes);
		writeBuffer.flip();
		channel.write(writeBuffer);
	}

}
