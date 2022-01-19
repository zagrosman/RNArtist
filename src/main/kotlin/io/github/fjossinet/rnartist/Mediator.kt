package io.github.fjossinet.rnartist

import io.github.fjossinet.rnartist.core.RnartistConfig
import io.github.fjossinet.rnartist.core.model.*
import io.github.fjossinet.rnartist.io.EmbeddedDB
import io.github.fjossinet.rnartist.gui.*
import io.github.fjossinet.rnartist.gui.editor.ScriptEditor
import io.github.fjossinet.rnartist.io.ChimeraDriver
import io.github.fjossinet.rnartist.io.ChimeraXDriver
import io.github.fjossinet.rnartist.io.github.fjossinet.rnartist.gui.DrawingLoaded
import io.github.fjossinet.rnartist.io.github.fjossinet.rnartist.gui.DrawingsLoadedPanel
import io.github.fjossinet.rnartist.io.github.fjossinet.rnartist.gui.SideWindow
import javafx.beans.property.SimpleObjectProperty

class Mediator(val rnartist: RNArtist) {

    var drawingDisplayed: SimpleObjectProperty<DrawingLoaded?> = SimpleObjectProperty<DrawingLoaded?>(null)

    var ignoreTertiaries = false

    val embeddedDB = EmbeddedDB()
    var chimeraDriver = if (RnartistConfig.isChimeraX)
                            ChimeraXDriver(this)
                        else
                            ChimeraDriver(this)
    val scriptEditor = ScriptEditor(this)
    val drawingsLoadedPanel = DrawingsLoadedPanel(this)
    val settings = Settings(this)
    var sideWindow = SideWindow(this)
    val projectsPanel = ProjectsPanel(this)
    lateinit var canvas2D: Canvas2D

    //++++++ some shortcuts
    private val secondaryStructure: SecondaryStructure?
        get() {
            return this.drawingDisplayed.get()?.drawing?.secondaryStructure
        }
    val rna: RNA?
        get() {
            return this.secondaryStructure?.rna
        }
    val workingSession: WorkingSession?
        get() {
            return this.drawingDisplayed.get()?.drawing?.workingSession
        }

    val viewX: Double?
        get() {
            return this.workingSession?.viewX
        }

    val viewY: Double?
        get() {
            return this.workingSession?.viewY
        }

    val zoomLevel: Double?
        get() {
            return this.workingSession?.zoomLevel
        }


    init {

        this.drawingDisplayed.addListener {
                observableValue, oldValue, newValue ->

            this.rnartist.focus3D.isDisable = true
            this.rnartist.reload3D.isDisable = true
            this.rnartist.paintSelectionin3D.isDisable = true

            if (newValue == null) { //this means that the menu of 2Ds loaded has been cleared
                this.chimeraDriver.closeSession()
            }
            else {
                var previousFile: String? = null
                var previousPDBId: String? = null
                if (oldValue != null && !this.chimeraDriver.tertiaryStructures.isEmpty() && this.chimeraDriver.pdbFile != null) { //we store a temporary chimera session to restore it when the user come back to this 2D loaded
                    val tmpPdbFile = createTempFile(suffix = ".pdb")
                    val tmpSessionFile = createTempFile(suffix = ".py")
                    oldValue.tmpChimeraSession = Pair(tmpSessionFile, tmpPdbFile)
                    this.chimeraDriver.saveSession(tmpSessionFile, tmpPdbFile)
                }
                this.canvas2D.repaint();
            }
        }

    }

    fun focusInChimera() {
        this.drawingDisplayed.get()?.let { drawingDisplayed ->
            chimeraDriver.setFocus(
                canvas2D.getSelectedPositions()
            )
        }
    }

    fun pivotInChimera() {
        this.drawingDisplayed.get()?.let { drawingDisplayed->
            chimeraDriver.setPivot(
                canvas2D.getSelectedPositions()
            )
        }
    }

}