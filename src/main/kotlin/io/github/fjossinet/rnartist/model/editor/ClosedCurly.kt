package io.github.fjossinet.rnartist.io.github.fjossinet.rnartist.model.editor

import io.github.fjossinet.rnartist.io.github.fjossinet.rnartist.gui.editor.ScriptEditor
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight

class ClosedCurly(editor: ScriptEditor, indentLevel:Int): DSLElement(editor, "}\n", indentLevel) {
    init {
        this.text.fill = Color.BLANCHEDALMOND
        this.text.font = Font.font("Helvetica", FontWeight.BOLD, FontPosture.REGULAR, 20.0)
    }
}