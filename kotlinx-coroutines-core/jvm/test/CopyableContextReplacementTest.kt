package kotlinx.coroutines

import kotlin.coroutines.CoroutineContext
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CopyableContextReplacementTest {
    private interface TraceContext : CoroutineContext.Element {
        companion object Key : CoroutineContext.Key<TraceContext>
        override val key: CoroutineContext.Key<*> get() = Key
    }

    private object NoTrace : TraceContext

    private class ActiveTrace : TraceContext, CopyableThreadContextElement<Unit> {
        override fun updateThreadContext(context: CoroutineContext) = Unit
        override fun restoreThreadContext(context: CoroutineContext, oldState: Unit) {}
        override fun copyForChild() = ActiveTrace()
        override fun mergeForChild(overwritingElement: CoroutineContext.Element) = overwritingElement
    }

    @Test
    fun testReplacePlainElementWithCopyableElementInWithContext() = runBlocking {
        withContext(NoTrace) {
            val added = ActiveTrace()
            withContext(added) {
                val installed = assertIs<ActiveTrace>(coroutineContext[TraceContext])
                assertNotSame(added, installed)
            }
            assertSame(NoTrace, coroutineContext[TraceContext])
        }
    }
}
