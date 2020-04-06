package io.fu.covidio

import org.dyn4j.collision.AxisAlignedBounds
import org.dyn4j.dynamics.Body
import org.dyn4j.dynamics.World
import org.dyn4j.geometry.Vector2

class Physics {
    private val world = World(AxisAlignedBounds(1000.0, 1000.0))
        .apply { this.gravity = Vector2() }

    fun addBody(
        body: Body
    ) {
        world.addBody(body)
    }

    fun removeBody(
        body: Body
    ) {
        world.removeBody(body)
    }

    fun update(delta: Float, state: GameState) {
        state.gameObjects().forEach {
            it.position.add(
                it.direction.copy()
                    .multiply(delta.toDouble() * it.moveSpeed)
            )
        }
    }
}
