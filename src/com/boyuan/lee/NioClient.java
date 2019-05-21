package com.boyuan.lee;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NioClient {

	/**
	 * 启动客户端，连接服务器端，向服务器端发送数据
	 */
	public void start() throws IOException {
		/**
		 * 连接服务器端
		 */
		SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 9999));

		/**
		 * 接收服务器端响应
		 */
		Selector selector = Selector.open();
		socketChannel.configureBlocking(false);
		socketChannel.register(selector, SelectionKey.OP_READ);

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(new NioClientHandler(selector));

		/**
		 * 向服务器端发送数据
		 */
		Scanner scanner = new Scanner(System.in);
		while (scanner.hasNextLine()) {
			String request = scanner.nextLine();
			if (Objects.nonNull(request) && request.length() > 0) {
				socketChannel.write(Charset.forName("UTF-8").encode(request));
			}
		}

	}

	public static void main(String[] args) throws IOException {
		NioClient nioClient = new NioClient();
		nioClient.start();
	}
}
