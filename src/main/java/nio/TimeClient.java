package nio;

public class TimeClient {

	public static void main(String[] args) {
		TimeClientHandle client = new TimeClientHandle("127.0.0.1", 9999);
		new Thread(client).start();
	}
}
