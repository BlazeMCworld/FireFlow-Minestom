package de.blazemcworld.fireflow.node

import de.blazemcworld.fireflow.FireFlow
import de.blazemcworld.fireflow.gui.NodeComponent
import java.util.*
import java.util.function.Supplier

class EvaluationContext(val global: GlobalNodeContext, val varStore: MutableMap<String, Any> = mutableMapOf()) {
    private val store = mutableMapOf<NodeContext.BoundOutput<*>, Supplier<*>>()
    private val tasks = Stack<Runnable>()
    val functionStack = Stack<NodeComponent>()

    operator fun <T : Any> set(output: NodeContext.BoundOutput<T>, value: Supplier<T>) {
        store[output] = value
    }

    operator fun <T : Any> get(input: NodeContext.BoundInput<T>): T? {
        var out: T? = null
        global.measureCode {
            if (input is NodeContext.BoundInsetInput && input.insetVal != null) {
                out = input.insetVal
                return@measureCode
            }

            val output = input.connected.singleOrNull() ?: return@measureCode
            if (store.containsKey(output)) {
                out = store[output]!!.get() as? T
                return@measureCode
            }
            try {
                out = output.defaultHandler(this) as? T
            } catch (e: Exception) {
                FireFlow.LOGGER.error(e) { "Exception calling defaultHandler for ${output.nodeContext().component.node.title}!" }
                for (connectedInput in output.nodeContext().inputs()) {
                    if (connectedInput.v.type == SignalType) continue
                    FireFlow.LOGGER.error { "${connectedInput.v.name} -> ${this[connectedInput]}" }
                }
            }
        }
        return out
    }

    fun emit(output: NodeContext.BoundOutput<Unit>, now: Boolean = false) {
        tasks.add {
            try {
                output.connected.singleOrNull()?.signalListener?.invoke(this)
            } catch (e: Exception) {
                val connected = output.connected.singleOrNull() ?: return@add

                FireFlow.LOGGER.error(e) { "Exception calling signalListener for ${output.nodeContext().component.node.title}!" }
                for (input in connected.nodeContext().inputs()) {
                    if (input.v.type == SignalType) continue
                    FireFlow.LOGGER.error { "${input.v.name} -> ${this[input]}" }
                }
            }
        }
        if (now) taskLoop()
    }

    private fun taskLoop() {
        while (tasks.isNotEmpty()) {
            global.measureCode {
                tasks.pop().run()
            }
            if (global.cpuLimit()) break
        }
    }

    fun child(shareLocals: Boolean): EvaluationContext {
        val c = if (shareLocals) EvaluationContext(global, varStore) else EvaluationContext(global)
        if (functionStack.isNotEmpty()) c.functionStack.push(functionStack.peek())
        for ((k, v) in store) c.store[k] = v
        return c
    }
}
