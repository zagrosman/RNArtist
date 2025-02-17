package io.github.fjossinet.rnartist.model.editor

import io.github.fjossinet.rnartist.core.model.Block
import io.github.fjossinet.rnartist.core.model.Location
import io.github.fjossinet.rnartist.gui.editor.Script

class HelixLocationKw(parent:HelixKw, script: Script, indentLevel: Int) : OptionalDSLKeyword(parent, script, "location", indentLevel) {

    init {
        val p = OptionalDSLParameter(
            this,
            script,
            "range",
            StringWithoutQuotes(this, script, editable = true),
            Operator(this, script, "to"),
            StringWithoutQuotes(this, script, editable = true),
            this.indentLevel + 1,
            canBeMultiple = true
        )
        this.children.add(
            p
        )
    }

    fun setLocation(location: Location) {
        location.blocks.forEachIndexed() { index, block ->
            val p = this.searchFirst { it is OptionalDSLParameter && !it.inFinalScript && "to".equals(it.operator.text.text.trim()) } as OptionalDSLParameter
            (p.key as StringWithoutQuotes).setText(block.start.toString())
            (p.value as StringWithoutQuotes).setText(block.end.toString())
            p.addButton.fire()
        }
        this.addButton.fire()
    }

    fun getLocation(): Location? {
        var blocks = mutableListOf<Block>()
        this.children.filter { it is OptionalDSLParameter && it.inFinalScript && "to".equals(it.operator.text.text.trim()) }
            .forEach { range ->
                (range as? OptionalDSLParameter)?.let {
                    blocks.add(Block(it.key.text.text.toInt(), it.value.text.text.toInt()))
                }
            }
        if (blocks.isEmpty())
            return null
        return Location(blocks)
    }
}