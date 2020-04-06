package io.fu.covidio

import io.netty.channel.Channel
import java.util.UUID
import org.dyn4j.dynamics.Body
import org.dyn4j.geometry.Circle
import org.dyn4j.geometry.Vector2

sealed class Component {
    abstract val gameObject: GameObject
}

class PlayerComponent(
    val channel: Channel,
    val userName: String,
    override val gameObject: GameObject
) : Component()

class BulletComponent(
    val ownerId: String,
    override val gameObject: GameObject
) : Component()

class GameObject(
    createRigidBody: (GameObject) -> Body,
    createComponents: (GameObject) -> List<Component>,
    val id: String = UUID.randomUUID().toString(),
    var position: Vector2,
    var direction: Vector2,
    var rotation: Vector2,
    var moveSpeed: Float
) {
    val components: List<Component> = createComponents(this)
    val rigidBody: Body = createRigidBody(this)

    val isPlayer: Boolean by lazy {
        components.any { it is PlayerComponent }
    }

    val isBullet: Boolean by lazy {
        components.any { it is BulletComponent }
    }
}

fun createPlayer(
    physics: Physics,
    channel: Channel,
    userName: String
): GameObject {
    return GameObject(
        position = Vector2(),
        direction = Vector2(),
        rotation = Vector2(),
        createRigidBody = {
            Body()
                .apply { this.addFixture(Circle(10.0)) }
                .also { physics.addBody(it) }
        },
        createComponents = {
            listOf(
                PlayerComponent(
                    channel = channel,
                    userName = userName,
                    gameObject = it
                )
            )
        },
        moveSpeed = 100f
    )
}

fun createBullet(
    physics: Physics,
    player: GameObject
): GameObject {
    return GameObject(
        position = player.position.copy(),
        direction = player.direction.copy(),
        rotation = player.rotation.copy(),
        createRigidBody = {
            Body()
                .apply { this.addFixture(Circle(2.0)) }
                .also { physics.addBody(it) }
        },
        createComponents = {
            listOf(
                BulletComponent(player.id, it)
            )
        },
        moveSpeed = 200f
    )
}
