package de.blazemcworld.fireflow.node.impl

import de.blazemcworld.fireflow.node.*

class ValueLiteralNode<T : Any>(val type: ValueType<T>) : BaseNode(type.name + " Literal", type.material) {
    private val value = output("Value", type)

    override fun setup(ctx: NodeContext) {
        ctx[value].defaultHandler = { ctx.component.valueLiteral?.let { type.parse(it, ctx.global.space) } }
    }

    companion object {
        val all = setOf(
            ValueLiteralNode(NumberType),
            ValueLiteralNode(TextType),
            ValueLiteralNode(MessageType),
            ValueLiteralNode(ConditionType),
        )
    }
}