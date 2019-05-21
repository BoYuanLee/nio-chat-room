package com.boyuan.lee;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * @author longm
 * nio 的服务器端
 */
public class NioServer {

	/**
	 * 开启nio，启动服务
	 */
	public void start() throws IOException {
		/**
		 * 1. 创建多路复用器 Selector
		 */
		Selector selector = Selector.open();

		/**
		 * 创建serverSocketChannel ，创建通道
		 */
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

		/**
		 * 为channel通道绑定监听端口
		 */
		serverSocketChannel.bind(new InetSocketAddress(9999));


		/**
		 * 设置channel通道为非阻塞模式
		 */
		serverSocketChannel.configureBlocking(false);

		/**
		 * 将channel通道注册到多路复用器上, 以及感兴趣关注的事件
		 */
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

		System.out.println("服务器启动成功");

		/**
		 * 循环等待新接入的连接
		 */
		for (;;){
			/**
			 * 获取就绪可用的channel通道数量
			 */
			int readyChannels = selector.select();
			System.out.println("已就绪通道数：" + readyChannels);
			/**
			 * 为什么要这样？
			 */
			if (readyChannels == 0) {
				continue;
			}

			/**
			 * 获取可用channel通道的事件集合，每次channel连接/通信就会产生一个新的事件；
			 * 后续消费过事件之后，如果不清空对应的selectionkeys事件，
			 * 会出现重复事件的问题，引起nullpointException，因为第一个事件已经被消费过了
			 * 所以，处理完事件，需要清空一下这个set
			 */
			Set<SelectionKey> selectionKeys = selector.selectedKeys();

			/**
			 * 根据就绪状态，处理相应的业务逻辑
			 */
			selectionKeys.forEach(selectionKey -> {
				/**
				 * 如果是接入事件，如何处理？
				 */
				if (selectionKey.isAcceptable()) {
					try {
						acceptHandler(serverSocketChannel, selector);
					} catch (IOException e) {
						System.out.println("处理接入事件失败");
						e.printStackTrace();
					}
				}

				/**
				 * 如果是可读事件，如何处理？
				 */
				if (selectionKey.isReadable()) {
					try {
						readHandler(selectionKey, selector);
					} catch (IOException e) {
						System.out.println("处理可读事件失败");
						e.printStackTrace();
					}
				}
			});

			// 必须清楚通道，否则回导致连接失败
			selectionKeys.clear();
		}

	}

	/**
	 * 接入事件处理器
	 */
	private void acceptHandler(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
		// 如果是接入事件，创建与服务器端的连接socketChannel
		SocketChannel socketChannel = serverSocketChannel.accept();

		// 将socketChannel设置为非阻塞模式
		socketChannel.configureBlocking(false);

		// 将channel注册到Selector上，监听可读事件
		socketChannel.register(selector, SelectionKey.OP_READ);

		// 回复客户端建立连接成功的回复信息
		socketChannel.write(Charset.forName("UTF-8").encode("恭喜，您已连接聊天室成功，您与其他人非好友关系，请注意隐私安全！"));
	}

	/**
	 * 可读事件处理器
	 */
	private void readHandler(SelectionKey selectionKey, Selector selector) throws IOException {
		/**
		 * 从selectionKey获取channel渠道
		 */
		SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

		/**
		 * 只有buffer才能进行channel的读和写
		 */
		ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

		/**
		 * 循环读取客户端请求信息
		 */
		StringBuilder request = new StringBuilder();
		while (socketChannel.read(byteBuffer) > 0) {
			/**
			 * 切换byteBuf为读模式
			 */
			byteBuffer.flip();

			/**
			 * 读取buffer中内容
			 */
			request.append(Charset.forName("utf-8").decode(byteBuffer));
		}

		/**
		 * 将channel再次注册到selector多路复用器，监听其他的可读事件
		 */
		socketChannel.register(selector, SelectionKey.OP_READ);

		/**
		 * 将客户端发送的请求信息广播给其他客户端
		 */
		if (request.length() > 0) {
			System.out.println(request.toString());
			broadCast(selector, socketChannel, request.toString());
		}
	}

	/**
	 * 广播接收到的信息给聊天室成员
	 */
	public void broadCast(Selector selector, SocketChannel sourceChannel, String requestMsg) {
		/**
		 * 获取所有已接入客户端channel的集合，区别于selectedKeys
		 */
		Set<SelectionKey> selectionKeys = selector.keys();
		selectionKeys.forEach(selectionKey -> {
			Channel targetChannel = selectionKey.channel();
			// 剔除发消息的客户端
			if (targetChannel instanceof SocketChannel && targetChannel != sourceChannel) {
				try {
					// 将消息发送到channel客户端
					((SocketChannel) targetChannel).write(Charset.forName("UTF-8").encode(requestMsg));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		});
	}

	public static void main(String[] args) throws IOException {
		NioServer nioServer = new NioServer();
		nioServer.start();
	}
}
