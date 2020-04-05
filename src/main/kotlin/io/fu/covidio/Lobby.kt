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
        withRealm(ch) {
            it.gameLoop.input.push(LeaveGameCommand(ch))
        }
    }

    fun handleMessage(ch: Channel, ws: BinaryWebSocketFrame) {
        withRealm(ch) {
            val cmd = decoder.decode(ch, ws.content())
            it.gameLoop.input.push(cmd)
        }
    }

    private fun withRealm(ch: Channel, block: (Realm) -> Unit) {
        val realmId = ch.realmId
        if (realmId != null) {
            val realm = realmsById[realmId]
            realm
                ?.run(block)
                ?: println("Realm(id=$realmId) was not found")
        } else {
            println("Channel(id=${ch.id()} is missing realmId")
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
