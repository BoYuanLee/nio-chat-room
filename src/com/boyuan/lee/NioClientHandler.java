package com.boyuan.lee;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Set;

/**
 * 客户端线程类，专门接收服务端响应数据
 * @author longm
 */
public class NioClientHandler implements Runnable {

	private Selector selector;

	public NioClientHandler(Selector selector) {
		this.selector = selector;
	}

	@Override
	public void run() {
		while (true) {
			try {
				int readyChannels = selector.select();

				if (readyChannels == 0) {
					continue;
				}

				// 获取与绑定的selector相关的所有channel通道事件
				Set<SelectionKey> selectionKeys = selector.selectedKeys();

				selectionKeys.forEach(selectionKey -> {
					try {
						readHandler(selectionKey, selector);
					} catch (IOException e) {
						e.printStackTrace();
					}
				});

				selectionKeys.clear();

			} catch (IOException e) {
				System.out.println("客户端处理失败，未连接");
				e.printStackTrace();
			}
		}
	}

	/**
	 * 循环读取服务器端相应信息
	 * @param selectionKey
	 * @param selector
	 * @throws IOException
	 */
	private void readHandler(SelectionKey selectionKey, Selector selector) throws IOException {
		SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

		ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

		StringBuilder response = new StringBuilder();

		while (socketChannel.read(byteBuffer) > 0) {
			byteBuffer.flip();
			response.append(Charset.forName("UTF-8").decode(byteBuffer));
		}

		socketChannel.register(selector, SelectionKey.OP_READ);

		if (response.length() > 0) {
			System.out.println("服务器端相应：" + response.toString());
		}
	}
}
