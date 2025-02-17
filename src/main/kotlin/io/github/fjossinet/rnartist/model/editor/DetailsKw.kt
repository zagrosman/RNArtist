package io.github.fjossinet.rnartist.model.editor

import io.github.fjossinet.rnartist.core.model.Location
import io.github.fjossinet.rnartist.gui.editor.Script
import javafx.event.EventHandler

class DetailsKw(parent:ThemeKw, script: Script, indentLevel:Int): OptionalDSLKeyword(parent, script, "details", indentLevel) {

    init {
        this.children.add(LocationKw(this, script, this.indentLevel + 1))
        this.children.add(OptionalDSLParameter(this, script, null, StringWithoutQuotes(this, script,"type"), Operator(this, script,"="), TypeField(this, script, restrictedTypes =  listOf("helix", "single_strand", "junction")), this.indentLevel + 1))
        this.children.add(DSLParameter(this, script, StringWithoutQuotes(this, script,"value"), Operator(this, script,"="), StringWithoutQuotes(this, script, "1", editable = true), this.indentLevel+1))

        addButton.mouseReleased = {
            this.inFinalScript = true
            if (this.parent.children.indexOf(this) == this.parent.children.size-1 || this.parent.children.get(this.parent.children.indexOf(this)+1) !is DetailsKw)
                this.parent.children.add(this.parent.children.indexOf(this) + 1, DetailsKw(parent, script, indentLevel))
            script.initScript()
        }

        removeButton.mouseReleased = {
            this.inFinalScript = false
            if (this.parent.children.indexOf(this) <this.parent.children.size-1 && this.parent.children.get(this.parent.children.indexOf(this) + 1) is DetailsKw)
                this.parent.children.remove(this)
            else if (this.parent.children.indexOf(this) > 0) {
                val previous = this.parent.children.get(this.parent.children.indexOf(this) - 1)
                if (previous is DetailsKw && !previous.inFinalScript)
                    this.parent.children.remove(this)
            }
            script.initScript()
        }
    }

    fun setlevel(level:String) {
        val parameter = this.searchFirst { it is DSLParameter && "value".equals(it.key.text.text.trim())} as DSLParameter
        parameter.value.text.text = level
        this.addButton.fire()
    }

    fun getLocation(): Location? = (this.searchFirst { it is LocationKw } as LocationKw?)?.getLocation()

    fun setLocation(location:Location) {
        (this.searchFirst { it is LocationKw } as LocationKw).setLocation(location)
        this.addButton.fire()
    }

}