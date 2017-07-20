package actor.proto.tests

import actor.proto.*
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert

open class BehaviorTests {
    fun spawnActorFromFunc (receive : suspend (Context) -> Unit) : PID = spawn(fromFunc(receive))
    fun can_change_states () {
        runBlocking {
            val testActorProps: Props = fromProducer { -> LightBulb() }
            val actor: PID = spawn(testActorProps)
            Assert.assertEquals("Turning on", actor.requestAwait<String>(PressSwitch()))
            Assert.assertEquals("Hot!", actor.requestAwait<String>(Touch()))
            Assert.assertEquals("Turning off", actor.requestAwait<String>(PressSwitch()))
            Assert.assertEquals("Cold", actor.requestAwait<String>(Touch()))
        }
    }
    fun can_use_global_behaviour () {
        runBlocking {
            val testActorProps: Props = fromProducer { -> LightBulb() }
            val actor: PID = spawn(testActorProps)
            var response: String = actor.requestAwait<String>(PressSwitch())
            Assert.assertEquals("Smashed!", actor.requestAwait<String>(HitWithHammer()))
            Assert.assertEquals("Broken", actor.requestAwait<String>(PressSwitch()))
            Assert.assertEquals("OW!", actor.requestAwait<String>(Touch()))
        }
    }
    suspend fun pop_behavior_should_restore_pushed_behavior () {
        val behavior : Behavior = Behavior()
        behavior.become{ctx -> 
            if (ctx.message is String) {
                behavior.becomeStacked{ctx2 -> 
                    ctx2.respond(42)
                    behavior.unbecomeStacked()
                }

                ctx.respond(ctx.message)
            }
        }

        val pid : PID = spawnActorFromFunc({behavior.receive(it)})
        Assert.assertEquals("number", pid.requestAwait<String>("number"))
        Assert.assertEquals(42,pid.requestAwait<Int>(123))
        Assert.assertEquals("answertolifetheuniverseandeverything", pid.requestAwait<String>("answertolifetheuniverseandeverything"))
    }
}


open class LightBulb : Actor {
    private val _behavior : Behavior = Behavior()
    private var _smashed : Boolean = false
    private suspend fun off (context : Context) {
        when (context.message) {
            is PressSwitch -> {
                context.respond("Turning on")
                _behavior.become{on(it)}
            }
            is Touch -> {
                context.respond("Cold")
            }
        }
    }
    private suspend fun on (context : Context) {
        when (context.message) {
            is PressSwitch -> {
                context.respond("Turning off")
                _behavior.become{off(it)}
            }
            is Touch -> {
                context.respond("Hot!")
            }
        }
    }
    suspend override fun receive (context : Context) {
        when (context.message) {
            is HitWithHammer -> {
                context.respond("Smashed!")
                _smashed = true
                return
            }
            is PressSwitch -> {
                context.respond("Broken")
                return
            }
            is Touch -> {
                context.respond("OW!")
                return
            }
        }
        _behavior.receive(context)
    }

    init {
        _behavior.become{off(it)}
    }
}

open class PressSwitch
open class Touch
open class HitWithHammer