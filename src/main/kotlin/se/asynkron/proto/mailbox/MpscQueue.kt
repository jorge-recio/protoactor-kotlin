package se.asynkron.proto.mailbox

import org.jctools.queues.atomic.MpscAtomicArrayQueue
import proto.mailbox.*

class MpscQueue(capacity : Int = 1000) : IMailboxQueue {

    private val messages: MpscAtomicArrayQueue<Any> = MpscAtomicArrayQueue(capacity)
    override fun push(message: Any) {
        messages.add(message)
    }

    override fun pop(): Any? {
        return messages.poll()
    }

    override val hasMessages: Boolean
        get() = messages.count() > 0
}
