package com.integration.sample.config;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import io.micrometer.context.ContextSnapshot;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.logging.ByteBufFormat;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import reactor.netty.http.client.HttpClient;

@Component
public class CustomWebClientLogger extends LoggingHandler {

	public CustomWebClientLogger() {

		super(HttpClient.class, LogLevel.DEBUG, ByteBufFormat.SIMPLE);
	}

	@Override
	protected String format(ChannelHandlerContext ctx, String event, Object arg) {

		var channel = ctx.channel();

		if (arg instanceof ByteBufHolder byteBufHolder && StringUtils.equalsAny(event, "READ", "WRITE")) {

			var msg = byteBufHolder.content();
			var logMsg = msg.toString(StandardCharsets.UTF_8);

			if ("WRITE".equals(event))
				return "DownStream Request: " + logMsg;

			if ("READ".equals(event)) {

				var channelId = channel.id().asLongText();

				AttributeKey<StringBuilder> readMsgAttrKey = AttributeKey.valueOf(channelId);
				var readMsgAttr = channel.attr(readMsgAttrKey);
				var readMsgAttrStrBldr = readMsgAttr.get();

				if (Objects.isNull(readMsgAttrStrBldr)) {
					readMsgAttrStrBldr = new StringBuilder(logMsg);
					readMsgAttr.set(readMsgAttrStrBldr);
				} else {
					readMsgAttrStrBldr.append(logMsg);
				}

				if (arg instanceof DefaultLastHttpContent || msg instanceof EmptyByteBuf)
					return "DownStream Response: " + readMsgAttr.getAndSet(null);
			}
		}

		return "";
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		try (ContextSnapshot.Scope scope = ContextSnapshot.setAllThreadLocalsFrom(ctx.channel())) {
			super.channelReadComplete(ctx);
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try (ContextSnapshot.Scope scope = ContextSnapshot.setAllThreadLocalsFrom(ctx.channel())) {
			super.channelRead(ctx, msg);
		}
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		try (ContextSnapshot.Scope scope = ContextSnapshot.setAllThreadLocalsFrom(ctx.channel())) {
			super.write(ctx, msg, promise);
		}
	}

}
