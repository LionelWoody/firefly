package com.firefly.codec.http2.hpack;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Arrays;

import com.firefly.codec.http2.model.HttpFieldPreEncoder;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpVersion;

public class Http1FieldPreEncoder implements HttpFieldPreEncoder {

	@Override
	public HttpVersion getHttpVersion() {
		return HttpVersion.HTTP_1_0;
	}

	@Override
	public byte[] getEncodedField(HttpHeader header, String headerString, String value) {
		if (header != null) {
			int cbl = header.getBytesColonSpace().length;
			byte[] bytes = Arrays.copyOf(header.getBytesColonSpace(), cbl + value.length() + 2);
			System.arraycopy(value.getBytes(UTF_8), 0, bytes, cbl, value.length());
			bytes[bytes.length - 2] = (byte) '\r';
			bytes[bytes.length - 1] = (byte) '\n';
			return bytes;
		}

		byte[] n = headerString.getBytes(UTF_8);
		byte[] v = value.getBytes(UTF_8);
		byte[] bytes = Arrays.copyOf(n, n.length + 2 + v.length + 2);
		bytes[n.length] = (byte) ':';
		bytes[n.length] = (byte) ' ';
		bytes[bytes.length - 2] = (byte) '\r';
		bytes[bytes.length - 1] = (byte) '\n';
		return bytes;
	}
}
