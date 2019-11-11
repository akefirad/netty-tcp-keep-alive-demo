package com.example.demo.binary;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Sharable
@Component
@RequiredArgsConstructor
public class KeepAliveHandler extends ChannelDuplexHandler {
    private final byte[] KEEP_ALIVE_MESSAGE = new byte[]{0x0A}; // LF

    private final AtomicInteger sentMessages = new AtomicInteger(0);
    private final AtomicInteger failedMessages = new AtomicInteger(0);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            sentMessages.incrementAndGet();
            //ctx.writeAndFlush(Unpooled.wrappedBuffer(KEEP_ALIVE_MESSAGE))
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
                    .addListener(future -> {
                        if (!future.isSuccess()) {
                            // Here we need to close the connection
                            // but only after a couple of attempts (3 maybe).
                            // To do that, we need to keep how many times
                            // sending the message failed. To achieve that
                            // some refactoring is needed in our code.
                            log.info("Failed to send heartbeat", future.cause());
                            failedMessages.incrementAndGet();
                        }
                    });
        }
    }

    @Scheduled(fixedRate = 5_000)
    public void reportStatistics() {
        log.info("Keep-Alive messages, sent {} failed {}", sentMessages, failedMessages);
    }
}
