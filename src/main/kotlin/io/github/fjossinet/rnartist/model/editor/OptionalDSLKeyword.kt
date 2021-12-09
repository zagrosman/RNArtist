package io.github.fjossinet.rnartist.io.github.fjossinet.rnartist.model.editor

import io.github.fjossinet.rnartist.io.github.fjossinet.rnartist.gui.editor.Button
import io.github.fjossinet.rnartist.io.github.fjossinet.rnartist.gui.editor.ScriptEditor
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import org.kordamp.ikonli.javafx.FontIcon

open class OptionalDSLKeyword(editor: ScriptEditor, text:String, indentLevel:Int, var inFinalScript:Boolean = false):
    DSLElement(editor,text,indentLevel) {
    val addButton = Button(editor,  "+ $text", null)
    val removeButton = Button(editor, null, FontIcon("fas-trash:15"))

    init {
        this.text.fill = Color.MEDIUMAQUAMARINE
        this.text.font = Font.font("Helvetica", FontWeight.BOLD, FontPosture.REGULAR, 20.0)
        this.children.add(OpenedCurly(editor))
        this.children.add(ClosedCurly(editor, indentLevel))

        this.addToFinalScript(inFinalScript)

        addButton.onMouseClicked = EventHandler {
            this.addToFinalScript(true)
            editor.keywordAddedToScript(this)
        }

        removeButton.onMouseClicked = EventHandler {
            var nodes = mutableListOf<Node>()
            dumpNodes(nodes, false)
            this.addToFinalScript(false)
            editor.keywordRemovedFromScript(this, nodes.size)
        }
    }

    /**
     * Reorganizes the children elements when this element change its status concerning the final script
     */
    open fun addToFinalScript(add:Boolean) {
        this.children.clear()
        if (add) {
            inFinalScript = true
            this.children.add(OpenedCurly(editor))
            this.children.add(ClosedCurly(editor, indentLevel))
        } else {
            inFinalScript = false
            this.children.add(DSLElement(editor, "\n",0))
        }
    }

    override fun dumpNodes(nodes:MutableList<Node>, withTabs:Boolean) {
        if (withTabs)
            (0 until indentLevel).forEach {
                nodes.add(ScriptTab(editor).text)
            }
        if (inFinalScript) {
            nodes.add(this.removeButton)
            nodes.add(this.text)
        }
        else {
            nodes.add(this.addButton)
        }
        this.children.forEach {
            it.dumpNodes(nodes)
        }

    }

}