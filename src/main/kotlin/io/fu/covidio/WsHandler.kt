package io.fu.covidio

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.util.AttributeKey

val RequestParamAttr: AttributeKey<Map<String, String>> =
    AttributeKey.valueOf("request.parameter")

@ChannelHandler.Sharable
class WsHandler(
    private val lobby: Lobby
) : SimpleChannelInboundHandler<WebSocketFrame>() {

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is WebSocketServerProtocolHandler.HandshakeComplete) {
            val queryParams = parseQueryParams(evt.requestUri())
            ctx.channel().attr(RequestParamAttr).set(queryParams)
        } else {
            super.userEventTriggered(ctx, evt)
        }
    }

    override fun channelInactive(
        ctx: ChannelHandlerContext
    ) {
        super.channelInactive(ctx)
        lobby.removeChannel(ctx.channel())
    }

    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: WebSocketFrame
    ) {
        try {
            doHandleMessage(ctx, msg)
        } catch (e: Exception) {
            println("Failed to handle message")
        }
    }

    private fun doHandleMessage(
        ctx: ChannelHandlerContext,
        msg: WebSocketFrame
    ) {
        when (msg) {
            is PingWebSocketFrame ->
                ctx.writeAndFlush(PongWebSocketFrame())
            is BinaryWebSocketFrame ->
                lobby.handleMessage(ctx.channel(), msg)
            else ->
                TODO("invalid input")
        }
    }
}

fun parseQueryParams(queryStr: String): Map<String, String> {
    return queryStr.split("?").asSequence()
        .drop(1)
        .flatMap { it.split("&").asSequence() }
        .map { it.split("=") }
        .map { (k, v) -> k to v }
        .toMap()
}
