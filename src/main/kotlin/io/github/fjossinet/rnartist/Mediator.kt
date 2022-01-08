package io.github.fjossinet.rnartist

import io.github.fjossinet.rnartist.core.RnartistConfig
import io.github.fjossinet.rnartist.core.model.*
import io.github.fjossinet.rnartist.io.EmbeddedDB
import io.github.fjossinet.rnartist.gui.*
import io.github.fjossinet.rnartist.gui.editor.ScriptEditor
import io.github.fjossinet.rnartist.io.ChimeraDriver
import io.github.fjossinet.rnartist.io.ChimeraXDriver
import io.github.fjossinet.rnartist.model.*
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.scene.control.MenuItem
import java.io.File

class Mediator(val rnartist: RNArtist) {

    val drawingsLoaded = FXCollections.observableArrayList<DrawingLoaded>()
    var drawingDisplayed: SimpleObjectProperty<DrawingLoaded?> = SimpleObjectProperty<DrawingLoaded?>(null)

    var scope = RNArtist.SCOPE.BRANCH
    var ignoreTertiaries = false

    val embeddedDB = EmbeddedDB()
    var chimeraDriver = if (RnartistConfig.isChimeraX)
                            ChimeraXDriver(this)
                        else
                            ChimeraDriver(this)
    var scriptEditor = ScriptEditor(this)
    val settings = Settings(this)
    //val explorer = Explorer(this)
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
                when (oldValue) {
                    is DrawingLoadedFromFile -> {
                        previousFile =
                            oldValue.file.absolutePath //if the upcoming previous 2D was loaded from a file, we keep its path to avoid to reload twice a PDB file
                    }
                    is DrawingLoadedFromRNArtistDB -> {
                    }
                }
                if (oldValue != null && !this.chimeraDriver.tertiaryStructures.isEmpty() && this.chimeraDriver.pdbFile != null) { //we store a temporary chimera session to restore it when the user come back to this 2D loaded
                    val tmpPdbFile = createTempFile(suffix = ".pdb")
                    val tmpSessionFile = createTempFile(suffix = ".py")
                    oldValue.tmpChimeraSession = Pair(tmpSessionFile, tmpPdbFile)
                    this.chimeraDriver.saveSession(tmpSessionFile, tmpPdbFile)
                }
                when (newValue) {
                    is DrawingLoadedFromFile -> {
                        if (newValue.tmpChimeraSession != null) {
                            this.chimeraDriver.closeSession()
                            Thread.sleep(1000)
                            this.chimeraDriver.restoreSession(
                                newValue.tmpChimeraSession!!.first,
                                newValue.tmpChimeraSession!!.second
                            )
                            this.rnartist.focus3D.isDisable = false
                            this.rnartist.reload3D.isDisable = false
                        } else {
                            if (newValue.file.absolutePath.matches(Regex(".+\\.pdb[0-9]?")) && !newValue.file.absolutePath.equals(previousFile)) { //we could load a 2D for an RNA molecule from the same PDB (not necessary to reload the PDB)
                                this.chimeraDriver.closeSession()
                                Thread.sleep(1000)
                                this.chimeraDriver.loadTertiaryStructure(newValue.file)
                                this.rnartist.focus3D.isDisable = false
                                this.rnartist.reload3D.isDisable = false
                            } else if (newValue.file.absolutePath.matches(Regex(".+\\.json"))) {
                                this.chimeraDriver.closeSession()
                                Thread.sleep(1000)
                                val pdbFile = File(newValue.file.parentFile, "${newValue.file.name.split(".").first()}.pdb")
                                val chimeraSession = File(newValue.file.parentFile, "${newValue.file.name.split(".").first()}.py")
                                if (chimeraSession.exists() && pdbFile.exists()) {
                                    this.chimeraDriver.restoreSession(chimeraSession, pdbFile)
                                } else if (pdbFile.exists()) {
                                    this.chimeraDriver.loadTertiaryStructure(pdbFile)
                                    this.rnartist.focus3D.isDisable = false
                                    this.rnartist.reload3D.isDisable = false
                                }
                            }
                            else if (!newValue.file.absolutePath.matches(Regex(".+\\.(pdb|json)[0-9]?")) ) { //we close the session to avoid to save the previous PDB file (if any) and to link it to this file
                                this.chimeraDriver.closeSession()
                                Thread.sleep(1000)
                            }
                        }
                        rnartist.saveProject.isDisable = true
                    }
                    is DrawingLoadedFromRNArtistDB -> {
                        //println("new 2D loaded from DB")
                        if (newValue.tmpChimeraSession != null) {
                            this.chimeraDriver.closeSession()
                            Thread.sleep(1000)
                            this.chimeraDriver.restoreSession(
                                newValue.tmpChimeraSession!!.first,
                                newValue.tmpChimeraSession!!.second
                            )
                            this.rnartist.focus3D.isDisable = false
                            this.rnartist.reload3D.isDisable = false
                        } else {
                            val sessionFile = newValue.getChimeraSession()
                            val pdbFile = newValue.getPdbFile()
                            if (sessionFile != null && pdbFile != null) {//we can have a project without any 3D displayed (2D from CT, Vienna, BPSEQ, Stockholm,...)
                                this.chimeraDriver.closeSession()
                                Thread.sleep(1000)
                                this.chimeraDriver.restoreSession(sessionFile, pdbFile)
                                this.rnartist.focus3D.isDisable = false
                                this.rnartist.reload3D.isDisable = false
                            }
                        }
                        rnartist.saveProject.isDisable = false
                    }
                }
                this.canvas2D.repaint();
            }
        }

        this.drawingsLoaded.addListener(ListChangeListener { change ->
            while (change.next()) {
                if (change.wasAdded()) {
                    for (drawingLoaded in change.addedSubList) {
                        val item = MenuItem(drawingLoaded.toString())
                        item.setMnemonicParsing(false)
                        item.userData = drawingLoaded
                        item.setOnAction {
                            this.drawingDisplayed.set(item.userData as DrawingLoaded)
                            if ((item.userData as DrawingLoaded).drawing.viewX == 0.0 && (item.userData as DrawingLoaded).drawing.viewY == 0.0 && (item.userData as DrawingLoaded).drawing.zoomLevel == 1.0) {
                                //it seems it is a first opening, then we fit to the display
                                canvas2D.fitStructure(null)
                            }
                        }
                        rnartist.allStructuresAvailable.items.add(0, item)
                    }
                } else if (change.wasRemoved()) {
                    val toDelete = mutableListOf<MenuItem>()
                    for (menuItem in rnartist.allStructuresAvailable.items.toList()) {
                        for (drawingLoaded in change.removed) {
                            if (menuItem.userData == drawingLoaded) {
                                toDelete.add(menuItem)
                            }
                        }
                    }
                    rnartist.allStructuresAvailable.items.removeAll(toDelete)
                }
            }
            if (!this.drawingsLoaded.isEmpty()) {
                rnartist.clearAll2DsItem.isDisable = false
                rnartist.clearAll2DsExceptCurrentItem.isDisable = false
            }
            else {
                this.drawingDisplayed.set(null)
                rnartist.clearAll2DsItem.isDisable = true
                rnartist.clearAll2DsExceptCurrentItem.isDisable = true
                canvas2D.repaint()
            }
        })

    }

    public fun focusInChimera() {
        this.drawingDisplayed.get()?.let { drawingDisplayed ->
            chimeraDriver.setFocus(
                canvas2D.getSelectedPositions()
            )
        }
    }

    public fun pivotInChimera() {
        this.drawingDisplayed.get()?.let { drawingDisplayed->
            chimeraDriver.setPivot(
                canvas2D.getSelectedPositions()
            )
        }
    }

}