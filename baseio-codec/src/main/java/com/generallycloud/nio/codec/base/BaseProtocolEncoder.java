package com.generallycloud.nio.codec.base;

import java.io.IOException;
import java.nio.charset.Charset;

import com.generallycloud.nio.buffer.ByteBuf;
import com.generallycloud.nio.buffer.EmptyByteBuf;
import com.generallycloud.nio.codec.base.future.BaseReadFuture;
import com.generallycloud.nio.common.MathUtil;
import com.generallycloud.nio.common.StringUtil;
import com.generallycloud.nio.component.BaseContext;
import com.generallycloud.nio.component.BufferedOutputStream;
import com.generallycloud.nio.protocol.ChannelReadFuture;
import com.generallycloud.nio.protocol.ChannelWriteFuture;
import com.generallycloud.nio.protocol.ChannelWriteFutureImpl;
import com.generallycloud.nio.protocol.ProtocolEncoder;
import com.generallycloud.nio.protocol.ProtocolException;

public class BaseProtocolEncoder implements ProtocolEncoder {

	private final int	PROTOCOL_HEADER	= BaseProtocolDecoder.PROTOCOL_HEADER;
	
	private static final byte [] EMPTY_ARRAY = EmptyByteBuf.EMPTY_BYTEBUF.array();

	private void calc_text(byte[] header, int text_length) {
		MathUtil.unsignedShort2Byte(header, text_length, BaseProtocolDecoder.TEXT_BEGIN_INDEX);
	}

	private void calc_future_id(byte[] header, int future_id) {
		MathUtil.int2Byte(header, future_id, BaseProtocolDecoder.FUTURE_ID_BEGIN_INDEX);
	}

	private void calc_session_id(byte[] header, int future_id) {
		MathUtil.int2Byte(header, future_id, BaseProtocolDecoder.SESSION_ID_BEGIN_INDEX);
	}

	private void calc_binary(byte[] header, int binary_length) {
		MathUtil.int2Byte(header, binary_length, BaseProtocolDecoder.BINARY_BEGIN_INDEX);
	}

	private void calc_hash(byte[] header, int hash) {
		MathUtil.int2Byte(header, hash, BaseProtocolDecoder.HASH_BEGIN_INDEX);
	}

	public ChannelWriteFuture encode(BaseContext context, ChannelReadFuture readFuture) throws IOException {

		if (readFuture.isHeartbeat()) {

			byte[] array = new byte[1];

			array[0] = (byte) (readFuture.isPING() ? BaseProtocolDecoder.PROTOCOL_PING
					: BaseProtocolDecoder.PROTOCOL_PONG << 6);

			ByteBuf buffer = context.getByteBufAllocator().allocate(1);

			buffer.put(array);

			buffer.flip();

			return new ChannelWriteFutureImpl(readFuture, buffer);
		}

		BaseReadFuture f = (BaseReadFuture) readFuture;

		Integer future_id = f.getFutureID();
		Integer session_id = f.getSessionID();
		String future_name = f.getFutureName();
		String writeText = f.getWriteText();
		BufferedOutputStream binaryOPS = f.getWriteBinaryBuffer();

		if (StringUtil.isNullOrBlank(future_name)) {
			throw new ProtocolException("future name is empty");
		}
		
		Charset charset = context.getEncoding();

		byte[] future_name_array = future_name.getBytes(charset);
		byte[] text_array;
		if (StringUtil.isNullOrBlank(writeText)) {
			text_array = EMPTY_ARRAY;
		}else{
			text_array = writeText.getBytes(charset);
		}

		int service_name_length = future_name_array.length;
		int text_length = text_array.length;
		int binary_length = 0;

		if (service_name_length > 255) {
			throw new IllegalArgumentException("service name too long ," + future_name);
		}

		if (binaryOPS != null) {
			binary_length = binaryOPS.size();
		}

		int all_length = PROTOCOL_HEADER + service_name_length + text_length + binary_length;

		byte[] header = new byte[PROTOCOL_HEADER];

		if (f.isBroadcast()) {
			header[0] = 0x20;
		}

		header[1] = (byte) (service_name_length);

		calc_future_id(header, future_id);
		calc_session_id(header, session_id);
		calc_hash(header, f.getHashCode());
		calc_text(header, text_length);
		calc_binary(header, binary_length);

		ByteBuf buf = context.getByteBufAllocator().allocate(all_length);

		buf.put(header);
		buf.put(future_name_array);

		if (text_length > 0) {
			buf.put(text_array, 0, text_length);
		}

		if (binary_length > 0) {
			buf.put(binaryOPS.array(), 0, binary_length);
		}

		buf.flip();

		return new ChannelWriteFutureImpl(readFuture, buf);
	}

}
