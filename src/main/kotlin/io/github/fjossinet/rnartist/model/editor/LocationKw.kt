package io.github.fjossinet.rnartist.model.editor

import io.github.fjossinet.rnartist.core.model.Block
import io.github.fjossinet.rnartist.core.model.Location
import io.github.fjossinet.rnartist.gui.editor.Script

open class LocationKw(script: Script, indentLevel:Int, inFinalScript:Boolean = false, var l:Location? = null): OptionalDSLKeyword(script, " location", indentLevel, inFinalScript) {

    var location:Location?
        get() {
            var blocks = mutableListOf<Block>()
            this.children.filter {it is OptionalDSLParameter && it.inFinalScript && "to".equals(it.operator.text.text.trim()) }.forEach { range ->
                (range as? OptionalDSLParameter)?.let {
                    blocks.add(Block(it.key.text.text.toInt(), it.value.text.text.toInt()))
                }
            }
            if (blocks.isEmpty())
                return null
            return Location(blocks)
        }
        private set(value) {
            //remove the previous Parameters making the current location
            this.children.removeIf { it is OptionalDSLParameter && "to".equals(it.operator.text.text.trim())  }
            //adding the new one
            value?.blocks?.forEachIndexed() { index, block ->
                this.children.add(
                    this.children.size-1,
                    OptionalDSLParameter(
                        script,
                        "range",
                        IntegerField(script, block.start),
                        Operator(script, "to"),
                        IntegerField(script, block.end),
                        this.indentLevel + 1,
                        inFinalScript = true,
                        canBeMultiple = index == value.blocks.size-1
                    )
                )
            }
        }

    override fun addToFinalScript(add: Boolean) {
        super.addToFinalScript(add)
        if (add) {
            this.l?.let { location ->
                this.location = Location(location.positions.toIntArray())
            } ?: run {
                if (script.mediator.canvas2D.getSelectedPositions().isNotEmpty())
                    this.location = Location(script.mediator.canvas2D.getSelectedPositions().toIntArray())
                else
                    this.children.add(
                        this.children.size - 1,
                        OptionalDSLParameter(
                            script,
                            "range",
                            IntegerField(script, 1),
                            Operator(script, "to"),
                            IntegerField(script, 10),
                            this.indentLevel + 1,
                            inFinalScript = false,
                            canBeMultiple = true
                        )
                    )
            }
        }
    }
}