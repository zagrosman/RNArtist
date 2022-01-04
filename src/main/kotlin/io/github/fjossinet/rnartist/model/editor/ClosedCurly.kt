package io.github.fjossinet.rnartist.model.editor

import io.github.fjossinet.rnartist.core.RnartistConfig
import io.github.fjossinet.rnartist.io.awtColorToJavaFX
import io.github.fjossinet.rnartist.gui.editor.Script
import javafx.scene.Node
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text

class ClosedCurly(script: Script, indentLevel:Int): DSLElement(script, "}\n", indentLevel) {
    init {
        this.text.fill = awtColorToJavaFX(RnartistConfig.bracesEditorColor)
        this.text.font = Font.font(RnartistConfig.editorFontName, FontWeight.BOLD, RnartistConfig.editorFontSize.toDouble())
    }

    override fun increaseIndentLevel() {
        this.indentLevel ++
    }

    override fun decreaseIndentLevel() {
        this.indentLevel --
    }
}