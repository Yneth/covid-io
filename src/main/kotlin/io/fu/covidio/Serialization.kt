package io.fu.covidio

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.Channel
import java.util.UUID
import org.dyn4j.geometry.Vector2

// input codes
const val MOVE_CODE = 0
const val SHOOT_CODE = 1
const val JOIN_GAME_CODE = 100
const val LEAVE_GAME_CODE = 101

// output codes
const val STATE_CODE = 0

// output game obj type codes
const val PLAYER_CODE = 0
const val BULLET_CODE = 1
const val UNKNOWN_CODE = -1

class GameDecoder {
    fun decode(channel: Channel, byteBuf: ByteBuf): UserCommand {
        // TODO: validation
        // TODO: error handling
        return when (byteBuf.readByte().toInt()) {
            MOVE_CODE -> MoveCommand(
                channel,
                // TODO: normalize
                Vector2(
                    byteBuf.readShort().toDouble(),
                    byteBuf.readShort().toDouble()
                )
            )
            SHOOT_CODE -> ShootCommand(channel)
            JOIN_GAME_CODE -> {
                val userNameSize = byteBuf.readByte().toInt()
                val userNameBytes = ByteArray(userNameSize)
                byteBuf.readBytes(userNameBytes)

                JoinGameCommand(
                    channel = channel,
                    userName = UUID.randomUUID().toString()
                )
            }
            LEAVE_GAME_CODE -> LeaveGameCommand(channel)
            else -> TODO()
        }
    }
}

class GameEncoder(
    private val allocator: ByteBufAllocator
) {
    fun encode(frustum: List<GameObject>): ByteBuf {
        return allocator.directBuffer().apply {
            this.writeByte(STATE_CODE)

            this.writeInt(frustum.size)

            frustum.forEach {
                writeType(it, this)
                writePosition(it, this)
            }
        }
    }

    private fun writeType(go: GameObject, buf: ByteBuf) {
        val type = when {
            go.isPlayer -> PLAYER_CODE
            go.isBullet -> BULLET_CODE
            else -> UNKNOWN_CODE // ignore
        }
        buf.writeByte(type)
    }

    private fun writePosition(go: GameObject, buf: ByteBuf) {
        // writing as short as we are never
        // going to exceed its' value
        buf.writeShort(double2int(go.position.x))
        buf.writeShort(double2int(go.position.y))
    }

    private fun double2int(f: Double): Int {
        return (f * 10).toInt()
    }
}
