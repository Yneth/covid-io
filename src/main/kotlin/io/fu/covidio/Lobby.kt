package io.fu.covidio

import io.netty.channel.Channel
import io.netty.channel.DefaultEventLoop
import io.netty.channel.EventLoop
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.util.concurrent.GlobalEventExecutor
import java.util.concurrent.ConcurrentHashMap

data class Realm(
    val id: String,
    val gameLoop: GameLoop
)

const val RealmIdParam = "realmId"

class Lobby(
    realms: List<Realm>,
    private val decoder: GameDecoder
) {
    private val realmsById = ConcurrentHashMap<String, Realm>()
        .apply { realms.forEach { this[it.id] = it } }

    fun listRealms(): List<String> {
        return realmsById.keys.toList()
    }

    fun removeChannel(ch: Channel) {
        val realmId = ch.realmId
        if (realmId != null) {
            realmsById[realmId]
                ?.run { this.gameLoop.input.push(LeaveGameCommand(ch)) }
                ?: println("Realm(id=$realmId) was not found")
        } else {
            println("Channel(id=${ch.id()}) is missing realmId")
        }
    }

    fun handleMessage(ch: Channel, ws: BinaryWebSocketFrame) {
        val realmId = ch.realmId
        if (realmId != null) {
            realmsById[realmId]
                ?.run {
                    val cmd = decoder.decode(ch, ws.content())
                    this.gameLoop.input.push(cmd)
                }
                ?: println("Realm(id=$realmId) was not found")
        }
    }

    private val Channel.realmId: String?
        get() = this.attr(RequestParamAttr).get()?.get(RealmIdParam)
}

fun initLobby(
    realmCount: Int,
    encoder: GameEncoder,
    decoder: GameDecoder,
    chGroupSupplier: () -> ChannelGroup = { DefaultChannelGroup(GlobalEventExecutor.INSTANCE) },
    eventLoopSupplier: () -> EventLoop = { DefaultEventLoop() }
): Lobby {
    val realms = (0 until realmCount).map {
        Realm(
            id = "realm-$it",
            gameLoop = createGameLoop(
                encoder = encoder,
                channelGroup = chGroupSupplier(),
                eventLoop = eventLoopSupplier()
            )
        )
    }
    return Lobby(realms, decoder)
}
