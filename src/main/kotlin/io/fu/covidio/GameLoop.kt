package io.fu.covidio

import io.netty.channel.ChannelId
import io.netty.channel.EventLoop
import io.netty.channel.group.ChannelGroup
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import org.jbox2d.callbacks.ContactImpulse
import org.jbox2d.callbacks.ContactListener
import org.jbox2d.collision.Manifold
import org.jbox2d.dynamics.contacts.Contact

data class GameState(
    val playersByChannelId: MutableMap<ChannelId, GameObject>,
    val bullets: MutableList<GameObject>
)

class GameLoop(
    eventLoop: EventLoop,
    val input: Input,
    private val physics: Physics,
    private val gameState: GameState,
    private val output: Output
) {
    private val gameLoopHolder: ScheduledFuture<*> = eventLoop.scheduleAtFixedRate({
        input.poll(100)
            .forEach { handleInput(it) }
        physics.update(1 / 60f, gameState)
    }, 0, 17, TimeUnit.MILLISECONDS)

    private val broadcastLoopHolder: ScheduledFuture<*> = eventLoop.scheduleAtFixedRate({
        output.broadcast(gameState)
    }, 0, 33, TimeUnit.MILLISECONDS)

    init {
        // TODO
        physics.setContactListener(object : ContactListener {
            override fun endContact(contact: Contact) {
            }

            override fun beginContact(contact: Contact) {
            }

            override fun preSolve(contact: Contact, oldManifold: Manifold) {
            }

            override fun postSolve(contact: Contact, impulse: ContactImpulse) {
            }
        })
    }

    private fun handleInput(cmd: UserCommand) {
        when (cmd) {
            is JoinGameCommand ->
                createPlayer(physics, cmd.channel, cmd.userName)
                    .also { gameState.playersByChannelId[cmd.channel.id()] = it }
            is LeaveGameCommand -> {
                gameState.playersByChannelId.remove(cmd.channel.id())
                    ?.also { physics.removeBody(it.rigidBody) }
            }
            is MoveCommand -> {
                gameState.playersByChannelId[cmd.channel.id()]
                    ?.let { it.direction = cmd.direction }
            }
            is ShootCommand ->
                createBullet(physics, gameState.playersByChannelId.values.first())
                    .also { gameState.bullets.add(it) }
        }
    }
}

fun createGameLoop(
    encoder: GameEncoder,
    channelGroup: ChannelGroup,
    eventLoop: EventLoop
): GameLoop {
    val physics = Physics()
    val output = Output(encoder, physics, channelGroup)
    val input = Input()
    val gameState = GameState(mutableMapOf(), mutableListOf())
    return GameLoop(
        physics = physics,
        output = output,
        input = input,
        gameState = gameState,
        eventLoop = eventLoop
    )
}
