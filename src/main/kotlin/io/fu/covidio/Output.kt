package io.fu.covidio

import io.netty.channel.group.ChannelGroup
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame

class Output(
    private val encoder: GameEncoder,
    private val channelGroup: ChannelGroup
) {
    fun broadcast(state: GameState) {
        val encode = encoder.encode(state.gameObjects().toList())
        channelGroup.writeAndFlush(BinaryWebSocketFrame(encode))
    }
}
