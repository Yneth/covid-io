package io.fu.covidio

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.Channel
import java.util.UUID
import org.jbox2d.common.Vec2

class GameDecoder {
    fun decode(channel: Channel, byteBuf: ByteBuf): UserCommand {
        val commandId = byteBuf.readByte().toInt()
        return when (commandId) {
            // TODO: normalize
            0 -> MoveCommand(
                channel,
                Vec2(
                    byteBuf.readShort().toFloat(),
                    byteBuf.readShort().toFloat()
                )
            )
            1 -> ShootCommand(channel)
            100 -> {
                val userNameSize = byteBuf.readByte().toInt()
                val userNameBytes = ByteArray(userNameSize)
                byteBuf.readBytes(userNameBytes)

                JoinGameCommand(
                    channel = channel,
                    userName = UUID.randomUUID().toString()
                )
            }
            101 -> LeaveGameCommand(channel)
            else -> TODO()
        }
    }
}

class GameEncoder(
    private val allocator: ByteBufAllocator
) {

    fun encode(gameObjects: List<GameObject>): ByteBuf {
        return allocator.buffer(1024)
    }

    fun encode(cmd: ListRealmsCommand): ByteBuf {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
