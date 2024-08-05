package de.blazemcworld.fireflow.node

import de.blazemcworld.fireflow.gui.IOComponent
import net.minestom.server.item.Material

class FunctionCallNode(private val fnInputs: FunctionInputsNode, private val fnOutputs: FunctionOutputsNode) : BaseNode(fnInputs.fn, fnInputs.material) {
    val fn = fnInputs.fn

    init {
        for (i in fnInputs.outputs) {
            input(i.name, i.type)
        }
        for (i in fnOutputs.inputs) {
            output(i.name, i.type)
        }
    }

    override fun setup(ctx: NodeContext) {
        for (input in inputs) {
            ctx[input].signalListener = listen@{
                val matching = fnInputs.outputs.find(input.name::equals) as Output<Unit>? ?: return@listen
                val otherCtx = ctx.global.nodeContexts[fnInputs.component] ?: return@listen

                it.functionStack.add(ctx.component)
                try {
                    it.emit(otherCtx[matching], now = true)
                } finally {
                    it.functionStack.pop()
                }
            }
        }
        for (output in outputs) {
            val matching = fnOutputs.inputs.find { it.name == output.name } ?: continue
            val otherCtx = ctx.global.nodeContexts[fnOutputs.component] ?: continue
            ctx[output as Output<Any>].defaultHandler = {
                it.functionStack.add(ctx.component)
                try {
                    it[otherCtx[matching]]
                } finally {
                    it.functionStack.pop()
                }
            }
        }
    }

}

class FunctionInputsNode(val fn: String) : BaseNode("$fn Inputs", Material.PRISMARINE_SHARD) {
    val component = newComponent()

    fun add(name: String, type: ValueType<*>) {
        component.outputs += IOComponent.Output(output(name, type), component)
    }
    fun remove(name: String) {
        component.outputs.removeIf {
            if (it.io.name.equals(name, ignoreCase = true)) {
                it.disconnectAll()
                it.remove()
                return@removeIf true
            }
            return@removeIf false
        }
        outputs.removeIf { it.name.equals(name, ignoreCase = true) }
    }

    override fun setup(ctx: NodeContext) {
        for (output in outputs) {
            ctx[output as Output<Any>].defaultHandler = listen@{
                val matching = it.functionStack.peek().node.inputs.find { i -> i.name == output.name } ?: return@listen null
                val matchingCtx = ctx.global.nodeContexts[it.functionStack.peek()] ?: return@listen null
                return@listen it[matchingCtx[matching]]
            }
        }
    }

}

class FunctionOutputsNode(val fn: String) : BaseNode("$fn Outputs", Material.PRISMARINE_SHARD) {
    val component = newComponent()

    fun add(name: String, type: ValueType<*>) {
        component.inputs += IOComponent.Input(input(name, type), component)
    }
    fun remove(name: String) {
        component.inputs.removeIf {
            if (it.io.name.equals(name, ignoreCase = true)) {
                it.disconnectAll()
                it.remove()
                return@removeIf true
            }
            return@removeIf false
        }
        inputs.removeIf { it.name.equals(name, ignoreCase = true) }
    }

    override fun setup(ctx: NodeContext) {
        for (input in inputs) {
            ctx[input as Input<Any>].signalListener = listen@{
                val matching = it.functionStack.peek().node.outputs.find { o -> o.name == input.name } as Output<Unit>? ?: return@listen
                val matchingCtx = ctx.global.nodeContexts[it.functionStack.peek()] ?: return@listen
                it.emit(matchingCtx[matching])
            }
        }
    }

}