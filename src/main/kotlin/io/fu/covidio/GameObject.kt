package io.fu.covidio

import io.netty.channel.Channel
import java.util.UUID
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef

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
    var position: Vec2,
    var direction: Vec2,
    var rotation: Vec2
) {
    val components: List<Component> = createComponents(this)
    val rigidBody: Body = createRigidBody(this)
}

fun createPlayer(
    physics: Physics,
    channel: Channel,
    userName: String
): GameObject {
    return GameObject(
        position = Vec2(),
        direction = Vec2(),
        rotation = Vec2(),
        createRigidBody = {
            physics.addBody(
                bodyDef = BodyDef(),
                shape = CircleShape().apply {
                    this.m_radius = 10f
                }
            ).apply { this.userData = it }
        },
        createComponents = {
            listOf(
                PlayerComponent(
                    channel = channel,
                    userName = userName,
                    gameObject = it
                )
            )
        }
    )
}

fun createBullet(
    physics: Physics,
    player: GameObject
): GameObject {
    return GameObject(
        position = player.position,
        direction = player.direction,
        rotation = player.rotation,
        createRigidBody = {
            physics.addBody(
                bodyDef = BodyDef(),
                shape = CircleShape().apply {
                    this.m_radius = 2.0f
                }
            ).apply { this.userData = it }
        },
        createComponents = {
            listOf(
                BulletComponent(player.id, it)
            )
        }
    )
}
