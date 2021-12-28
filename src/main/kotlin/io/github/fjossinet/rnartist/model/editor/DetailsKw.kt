package io.github.fjossinet.rnartist.model.editor

import io.github.fjossinet.rnartist.core.model.Location
import io.github.fjossinet.rnartist.gui.editor.ScriptEditor

class DetailsKw(editor: ScriptEditor, indentLevel:Int, inFinalScript:Boolean = false): OptionalDSLKeyword(editor, " details", indentLevel, inFinalScript) {

    override fun addToFinalScript(add: Boolean) {
        super.addToFinalScript(add)
        if (add) {
            this.children.add(1, LocationKw(editor, this.indentLevel + 1))
            this.children.add(1, OptionalDSLParameter(editor, null, StringWithoutQuotes(editor,"type"), Operator(editor,"="), TypeField(editor,"click me", listOf("helix", "single_strand", "junction")), this.indentLevel + 1))
            this.children.add(1, DSLParameter(editor, StringWithoutQuotes(editor,"value"), Operator(editor,"="), StringWithoutQuotes(editor, "1", editable = true), this.indentLevel+1))
            this.children.add(DetailsKw(editor, indentLevel))
        }
    }

    fun getLocation(): Location? = (this.searchFirst { it is LocationKw } as LocationKw?)?.location

    /**
     * No argument needed. The function addToFinalScript (fired with the button) uses the current selection
     */
    fun setLocation() {
        if (!this.inFinalScript)
            this.addButton.fire()
        val l = (this.searchFirst { it is LocationKw } as LocationKw?)!!
        if (editor.mediator.canvas2D.getSelection().isNotEmpty() && !l.inFinalScript) //if there is a selection, the location needs to be added to the script
            l.addButton.fire()
    }

    fun setlevel(level:String) {
        if (!this.inFinalScript)
            this.addButton.fire()
        val parameter = this.searchFirst { it is DSLParameter && "value".equals(it.key.text.text.trim())} as DSLParameter
        parameter.value.text.text = level
    }

}