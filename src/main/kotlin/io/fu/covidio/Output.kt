package io.fu.covidio

import io.netty.channel.group.ChannelGroup
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame

class Output(
    private val encoder: GameEncoder,
    private val physics: Physics,
    private val channelGroup: ChannelGroup
) {
    fun broadcast(state: GameState) {
        state.playersByChannelId.values.forEach {
            val component = it.components.first() as PlayerComponent
            val visible = physics.getVisibleObjects(it)
                .map { fixture -> fixture.body.userData as GameObject }

            val byteBuf = encoder.encode(visible)
            component.channel.writeAndFlush(BinaryWebSocketFrame(byteBuf))
        }
    }
}
