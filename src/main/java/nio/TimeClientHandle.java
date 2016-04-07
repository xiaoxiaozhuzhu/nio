package nio;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class TimeClientHandle implements Runnable{

	private SocketChannel socketChannel;
	private int port;
	private Selector selector;
	private String host;
	private volatile boolean stop;
	
	//构造函数，初始化host和port selector socketchannel
	public  TimeClientHandle(String host, int port)
	{
		this.host = host==null ? "127.0.0.1" : host;
		this.port = port;
		try {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	public void run() {
		try {
			doConnect();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		while (!stop) {
			try {
				selector.select(1000);
				Set<SelectionKey> selectorKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectorKeys.iterator();
				SelectionKey key = null;
				while (it.hasNext()) {
					key = (SelectionKey) it.next();
					it.remove();
					handleInput(key);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			
		}
	}

	public void doConnect() throws IOException
	{
		if (socketChannel.connect(new InetSocketAddress(host, port))) {
			socketChannel.register(selector, SelectionKey.OP_READ);
			System.out.println("do connect : \n");
			doWrite(socketChannel);
		} else {
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
		}
	}
	
	public void doWrite(SocketChannel sc) throws IOException 
	{
		try {
			byte[] req = "QUERY TIME ORDER".getBytes();
			ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
			writeBuffer.put(req);
			writeBuffer.flip();
			sc.write(writeBuffer);
			if (writeBuffer.hasRemaining()) {
				System.out.println("send order 2 server succeed!");
			}
		} catch ( java.nio.channels.NotYetConnectedException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void handleInput(SelectionKey key) throws IOException 
	{
		if (key.isValid()) {
			SocketChannel sc = (SocketChannel) key.channel();
			if (key.isConnectable()) {
				if (sc.finishConnect()) {
					sc.register(selector, SelectionKey.OP_READ);
					System.out.println("finish connect : \n");
					doWrite(sc);
				}
			}
			if (key.isReadable()) {
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes = sc.read(readBuffer);
				if (readBytes > 0) {
					readBuffer.flip();
					byte [] bytes = new byte[readBytes];
					readBuffer.get(bytes);
					String body = new String(bytes);
					System.out.println("NOW IS: " + body);
					this.stop = true;
				} else if (readBytes < 0) {
					key.cancel();
					sc.close();
				} else {
					;
				}
			}
		}else{
			if (key != null) {
				key.cancel();
				key.channel().close();
				System.exit(1);
			}
		}
	}
}
