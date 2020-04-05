package io.fu.covidio

import org.jbox2d.callbacks.ContactListener
import org.jbox2d.collision.AABB
import org.jbox2d.collision.shapes.Shape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.Fixture
import org.jbox2d.dynamics.World

class Physics {
    private val world = World(Vec2())

    fun setContactListener(listener: ContactListener) {
        world.setContactListener(listener)
    }

    fun addBody(
        bodyDef: BodyDef,
        shape: Shape
    ): Body {
        val body = world.createBody(bodyDef)
        body.createFixture(shape, 5.0f)
        return body
    }

    fun removeBody(
        body: Body
    ) {
        world.destroyBody(body)
    }

    fun update(delta: Float, state: GameState) {
        // TODO
        world.step(delta, 6, 2)
    }

    fun getVisibleObjects(go: GameObject): List<Fixture> {
        // TODO
        val visible = mutableListOf<Fixture>()

        world.queryAABB({
            visible.add(it)
            true
        }, AABB())

        return visible
    }
}
