package io.fu.covidio

import io.netty.channel.Channel
import java.util.concurrent.ConcurrentLinkedQueue
import org.jbox2d.common.Vec2

sealed class UserCommand {
    abstract val channel: Channel
}

class JoinGameCommand(
    override val channel: Channel,
    val userName: String
) : UserCommand()

class MoveCommand(
    override val channel: Channel,
    val direction: Vec2
) : UserCommand()

class ShootCommand(
    override val channel: Channel
) : UserCommand()

class LeaveGameCommand(
    override val channel: Channel
) : UserCommand()

class Input {

    private val queue: ConcurrentLinkedQueue<UserCommand> =
        ConcurrentLinkedQueue()

    fun push(command: UserCommand) {
        queue.add(command)
    }

    fun poll(n: Int): List<UserCommand> {
        val result = mutableListOf<UserCommand>()

        var i = n
        while (queue.isNotEmpty() && i >= 0) {
            result.add(queue.poll())
            i--
        }

        return result
    }
}
