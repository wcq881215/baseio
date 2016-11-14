package com.generallycloud.nio.codec.http11;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.generallycloud.nio.buffer.ByteBuf;
import com.generallycloud.nio.buffer.EmptyByteBuf;
import com.generallycloud.nio.codec.http11.future.Cookie;
import com.generallycloud.nio.codec.http11.future.ServerHttpReadFuture;
import com.generallycloud.nio.common.StringUtil;
import com.generallycloud.nio.component.BaseContext;
import com.generallycloud.nio.component.BufferedOutputStream;
import com.generallycloud.nio.protocol.ChannelReadFuture;
import com.generallycloud.nio.protocol.ChannelWriteFuture;
import com.generallycloud.nio.protocol.ChannelWriteFutureImpl;
import com.generallycloud.nio.protocol.ProtocolEncoder;

public class ServerHTTPProtocolEncoder implements ProtocolEncoder {

	public ChannelWriteFuture encode(BaseContext context, ChannelReadFuture readFuture) throws IOException {
		
		ServerHttpReadFuture f = (ServerHttpReadFuture) readFuture;

		String write_text = f.getWriteText();
		
		byte [] text_array;
		
		BufferedOutputStream os = f.getBinaryBuffer();
		
		int length;
		
		if (StringUtil.isNullOrBlank(write_text)) {
			
			text_array = EmptyByteBuf.EMPTY_BYTEBUF.array();
			
			length = 0;
			
			if (os != null) {
				
				length = os.size();
				
				text_array = os.array();
			}
			
		}else{
			
			text_array = write_text.getBytes(context.getEncoding());
			
			length = text_array.length;
			
			if (os != null) {
				
				int size = os.size();
				int newLength = length + size;
				
				byte [] newArray = new byte[newLength];
				System.arraycopy(text_array, 0, newArray, 0, length);
				System.arraycopy(os.array(), 0, newArray, length, size);
				
				text_array = newArray;
				length = newLength;
			}
		}
		
		StringBuilder h = new StringBuilder();

		h.append("HTTP/1.1 ");
		h.append(f.getStatus().getHeaderText());
		h.append("\r\n");
		h.append("Server: baseio/0.0.1\r\n");
		h.append("Content-Length:");
		h.append(length);
		h.append("\r\n");
		
		Map<String,String> headers = f.getResponseHeaders();
		
		if (headers != null) {
			Set<Entry<String, String>> hs = headers.entrySet();
			for(Entry<String,String> header : hs){
				h.append(header.getKey());
				h.append(":");
				h.append(header.getValue());
				h.append("\r\n");
			}
		}

		List<Cookie> cookieList = f.getCookieList();
		
		if (cookieList != null) {
			for(Cookie c : cookieList){
				h.append("Set-Cookie:");
				h.append(c.toString());
				h.append("\r\n");
			}
		}
		
		h.append("\r\n");
		
		ByteBuf buffer = context.getByteBufAllocator().allocate(h.length() + length);
		
		buffer.put(h.toString().getBytes(context.getEncoding()));
		
		if (length != 0) {
			buffer.put(text_array, 0, length);
		}
		
		buffer.flip();

		ChannelWriteFutureImpl textWriteFuture = new ChannelWriteFutureImpl(readFuture, buffer);

		return textWriteFuture;
	}

}
