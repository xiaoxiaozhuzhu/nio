package nio;

public class TimeServer {

	public static void main(String[] args)
	{
		//MultiplexerTimeServer 是一个独立的线程，负责轮询多路复用selector，可以处理多个客户端的并发接入
		MultiplexerTimeServer server = new MultiplexerTimeServer(9999);
		new Thread(server,"NIO-MultiplexerTimeServer").start();
	}
}
