package io.github.fjossinet.rnartist.model.editor

import io.github.fjossinet.rnartist.gui.editor.DataField
import io.github.fjossinet.rnartist.gui.editor.Script
import io.github.fjossinet.rnartist.gui.editor.TextField
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.paint.Color

open class StringWithoutQuotes(script: Script, value:String = "", editable: Boolean = false): ParameterField(script, value, editable) {

    init {
        if (editable) {
            this.text.onMouseClicked = EventHandler {
                val index = script.children.indexOf(this.text)
                val textField = TextField(script, this.text)
                textField.prefWidth = script.width
                textField.focusedProperty().addListener { observableValue, oldValue, newValue ->
                    if (!newValue) {
                        this.text.text = textField.text.trim()
                        script.children.removeAt(index)
                        script.children.add(index, this.text)
                        script.initScript() //necessary if the new text is empty, then will be replaced witth the datafield
                    }
                }
                script.children.removeAt(index)
                script.children.add(index, textField)
                textField.requestFocus()
            }
        }
    }

    fun setText(text:String) {
        this.text.text = text
    }

    override fun clone():StringWithoutQuotes = StringWithoutQuotes(script, this.text.text, this.editable)

    override fun dumpNodes(nodes: MutableList<Node>, enterInCollapsedNode: Boolean) {
        if (editable) {
            if (this.text.text.isEmpty()) {
                val button = DataField(script)
                button.onMouseClicked = EventHandler {
                    val index = script.children.indexOf(button)
                    val textField = TextField(script, this.text)
                    textField.prefWidth = script.width
                    textField.focusedProperty().addListener { observableValue, oldValue, newValue ->
                        if (!newValue) {
                            this.text.text = textField.text.trim()
                            script.children.removeAt(index)
                            script.children.add(index, this.text)
                            script.initScript()
                        }
                    }
                    script.children.removeAt(index)
                    script.children.add(index, textField)
                    textField.requestFocus()
                }
                nodes.add(button)
            } else
                nodes.add(this.text)
        } else
            super.dumpNodes(nodes, enterInCollapsedNode)
    }
}