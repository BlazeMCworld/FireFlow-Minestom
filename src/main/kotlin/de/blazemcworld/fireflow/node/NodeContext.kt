package de.blazemcworld.fireflow.node

import de.blazemcworld.fireflow.gui.ExtractedNodeComponent
import de.blazemcworld.fireflow.gui.IOComponent
import de.blazemcworld.fireflow.gui.NodeComponent

class NodeContext(val global: GlobalNodeContext, val component: NodeComponent) {

    private val store = HashMap<BaseNode.IO<*>, Bound<*>>()

    operator fun <T : Any> get(v: BaseNode.Output<T>) = store[v] as BoundOutput<T>
    operator fun <T : Any> get(v: BaseNode.Input<T>) = store[v] as BoundInput<T>

    init {
        if (component is ExtractedNodeComponent) {
            store[component.extraction.output] = ExtractedOutput(component.extraction.input, component.extraction.output)
            store[component.extraction.input] = BoundInput(component.extraction.input)
        } else {
            for (i in component.inputs) {
                store[i.io] = if (i.io.type.insetable && i is IOComponent.InsetInput<*> && i.insetVal != null)
                    BoundInsetInput(i) else BoundInput(i.io)
            }
            for (o in component.outputs) store[o.io] = BoundOutput(o.io)
        }
    }

    fun computeConnections() {
        for (v in store.values) v.computeConnections()
    }

    fun inputs() = store.values.filterIsInstance<BoundInput<*>>()
    fun outputs() = store.values.filterIsInstance<BoundInput<*>>()

    abstract inner class Bound<T : BaseNode.IO<*>>(val v: T) {
        abstract fun computeConnections()
        fun nodeContext() = this@NodeContext
    }

    open inner class BoundOutput<T : Any>(v: BaseNode.Output<T>) : Bound<BaseNode.Output<T>>(v) {
        lateinit var connected: Set<BoundInput<*>>
        open var defaultHandler: (EvaluationContext) -> T? = { null }

        override fun computeConnections() {
            connected = component.outputs.find { it.io == v }?.connections?.map { global.nodeContexts[it.node]!![it.io] }?.toSet() ?: emptySet()
        }
    }

    inner class ExtractedOutput<I : Any, O : Any>(inp: BaseNode.Input<I>, v: BaseNode.Output<O>) : BoundOutput<O>(v) {
        override var defaultHandler: (EvaluationContext) -> O? = {
            val extraction: TypeExtraction<I, O>? = (component as? ExtractedNodeComponent)?.extraction as? TypeExtraction<I, O>?
            val value = it[get(inp)]
            if (value != null) extraction?.extract(value) else null
        }
    }

    open inner class BoundInput<T : Any>(v: BaseNode.Input<T>) : Bound<BaseNode.Input<T>>(v) {
        var signalListener: (EvaluationContext) -> Unit = {}

        open lateinit var connected: Set<BoundOutput<*>>
        override fun computeConnections() {
            connected = component.inputs.find { it.io == v }?.connections?.map { global.nodeContexts[it.output.node]!![it.output.io] }?.toSet() ?: emptySet()
        }
    }

    inner class BoundInsetInput<T : Any>(insetValInp: IOComponent.InsetInput<T>) : BoundInput<T>(insetValInp.io as BaseNode.Input<T>) {
        var insetVal: T? = insetValInp.insetVal
        override var connected = emptySet<BoundOutput<*>>()
    }
}