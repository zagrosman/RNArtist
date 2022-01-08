package io.github.fjossinet.rnartist.gui.editor

import com.google.gson.JsonParser
import io.github.fjossinet.rnartist.Mediator
import io.github.fjossinet.rnartist.core.RnartistConfig
import io.github.fjossinet.rnartist.core.io.parseDSLScript
import io.github.fjossinet.rnartist.core.model.*
import io.github.fjossinet.rnartist.io.awtColorToJavaFX
import io.github.fjossinet.rnartist.io.javaFXToAwt
import io.github.fjossinet.rnartist.model.DrawingLoadedFromScriptEditor
import io.github.fjossinet.rnartist.model.editor.*
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.value.ChangeListener
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Screen
import javafx.stage.Stage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.kordamp.ikonli.javafx.FontIcon
import java.awt.Desktop
import java.awt.geom.Rectangle2D
import java.io.*
import java.net.URL
import javax.script.ScriptEngineManager

abstract class Script(var mediator:Mediator): TextFlow() {

    abstract protected var root:DSLElement
    //this is to avoid to call script init too much time (for example when several elements are added, and then addButtons are fired several times)
    var allowScriptInit = true

    open fun getScriptRoot():DSLElement = this.root

    fun setScriptRoot(root:DSLElement) {
        this.root = root
        this.initScript()
    }

    abstract fun initScript()

    /**
     * remove the secondary structure keyword
     */
    abstract fun removeSecondaryStructure()

}

class ThemeAndLayoutScript(mediator:Mediator):Script(mediator) {

    override var root: DSLElement = RNArtistKw(this)

    init {
        this.prefWidth = Region.USE_COMPUTED_SIZE
        this.initScript()
    }

    override fun getScriptRoot():RNArtistKw = this.root as RNArtistKw

    override fun initScript() {
        if (this.allowScriptInit) {
            this.getScriptRoot()?.let {
                //currentJunctionBehaviors.clear() //we clear the current junction behaviors that can have been tweaked by the user
                //currentJunctionBehaviors.putAll(defaultJunctionBehaviors)
                this.children.clear()
                var nodes = mutableListOf<Node>()
                it.dumpNodes(nodes)
                this.children.addAll(nodes)
                this.layout()
            }
        }
    }

    fun setJunctionLayout(outIds: String, type: String, junctionLocation: Location) {
        this.getScriptRoot()?.getLayoutKw()?.let { layoutKw ->
            layoutKw.addButton.fire()
            layoutKw.children.forEach {
                if (it is JunctionLayoutKw) {
                    println((it as? JunctionLayoutKw)?.inFinalScript)
                    println((it as? JunctionLayoutKw)?.getLocation())
                    println(junctionLocation)
                }
            }
            val junctionLayoutKw =
                layoutKw.searchFirst { it is JunctionLayoutKw && it.inFinalScript && junctionLocation.equals(it.getLocation()) } as JunctionLayoutKw?
            //We have found a junctionKw with the same location, we update it
            junctionLayoutKw?.let {
                it.setOutIds(outIds) //We just need to change the outIds (type and location should be the same)
            } ?: run { //we create a new one
                val junctionLayoutKw = layoutKw.searchFirst { it is JunctionLayoutKw && !it.inFinalScript } as JunctionLayoutKw
                junctionLayoutKw.addButton.fire()
                junctionLayoutKw.setOutIds(outIds)
                junctionLayoutKw.setType(type)
                junctionLayoutKw.setLocation(junctionLocation)
            }
        }
    }

    fun setDetailsLevel(level: String) {
        this.getScriptRoot()?.getThemeKw()?.let { themeKw ->
            themeKw.addButton.fire()
            val selection = if (mediator.canvas2D.getSelectedPositions()
                    .isEmpty()
            ) null else Location(mediator.canvas2D.getSelectedPositions().toIntArray())
            val toUpdates = mutableListOf<DSLElement>()
            themeKw.searchAll(toUpdates) { it is DetailsKw && it.getLocation() == selection  }
            //If we found at least one DetailsKw with the same location if any, we update it
            if (toUpdates.isNotEmpty()) {
                with(toUpdates.first()) {
                    (this as DetailsKw).setlevel(level)
                }

                if (toUpdates.size > 1) {
                    toUpdates.subList(1, toUpdates.size).forEach {
                        (it as DetailsKw).removeButton.fire()
                    }
                } else {

                }

            } else { //nothing found we add a new DetailsKw element
                val detailsKw = themeKw.searchFirst { it is DetailsKw && !it.inFinalScript } as DetailsKw
                detailsKw.setlevel(level)
                selection?.let {
                    detailsKw.setLocation(it)
                }
            }
        }
    }

    fun setColor(types: String, color: String) {
        this.getScriptRoot()?.getThemeKw()?.let { themeKw ->
            themeKw.addButton.fire()
            val selection = if (mediator.canvas2D.getSelectedPositions()
                    .isEmpty()
            ) null else Location(mediator.canvas2D.getSelectedPositions().toIntArray())
            val toUpdates = mutableListOf<DSLElement>()
            themeKw.searchAll(toUpdates) { it is ColorKw && types.equals(it.getTypes()) && it.getLocation() == selection }
            //If we found at least one colorKW with the same types (and location if any), we update it
            if (toUpdates.isNotEmpty()) {

                with(toUpdates.first()) {
                    (this as ColorKw).setColor(color)
                }

                if (toUpdates.size > 1) {
                    toUpdates.subList(1, toUpdates.size).forEach {
                        (it as ColorKw).removeButton.fire()
                    }
                } else {

                }

            } else { //nothing found we add a new ColorKW element
                val colorKw = themeKw.searchFirst { it is ColorKw && !it.inFinalScript } as ColorKw
                colorKw.setColor(color)
                colorKw.setTypes(types)
                selection?.let {
                    colorKw.setLocation(it)
                }
            }

        }
    }

    fun setLineWidth(types: String, width: String) {
        this.getScriptRoot()?.getThemeKw()?.let { themeKw ->
            themeKw.addButton.fire()
            val selection = if (mediator.canvas2D.getSelectedPositions()
                    .isEmpty()
            ) null else Location(mediator.canvas2D.getSelectedPositions().toIntArray())
            val toUpdates = mutableListOf<DSLElement>()
            themeKw.searchAll(toUpdates) { it is LineKw && types.equals(it.getTypes()) && it.getLocation() == selection  }
            //If we found at least one lineKW with the same types (and location if any), we update it
            if (toUpdates.isNotEmpty()) {

                with(toUpdates.first()) {
                    (this as LineKw).setWidth(width)
                }

                if (toUpdates.size > 1) {
                    toUpdates.subList(1, toUpdates.size).forEach {
                        (it as LineKw).removeButton.fire()
                    }
                } else {

                }

            } else { //nothing found we add a new LineKW element
                val lineKw = themeKw.searchFirst { it is LineKw && !it.inFinalScript } as LineKw
                lineKw.setWidth(width)
                lineKw.setTypes(types)
                selection?.let {
                    lineKw.setLocation(it)
                }
            }

        }
    }

    fun setSecondaryStructure(ss:SecondaryStructureKw) {
        ss.increaseIndentLevel()
        (this.root as RNArtistKw).children.add(1, ss)
        this.initScript()
    }

    override fun removeSecondaryStructure() {
        (this.root as RNArtistKw).searchFirst { it is SecondaryStructureInputKw && it.inFinalScript}?.let {
            var optionalElements = mutableListOf<DSLElement>()
            (it as SecondaryStructureInputKw).searchAll(optionalElements, {it is OptionalDSLParameter})
            optionalElements.forEach { (it as OptionalDSLParameter).removeButton.fire() }
            optionalElements.clear()
            it.searchAll(optionalElements, {it is OptionalDSLKeyword})
            optionalElements.forEach { (it as OptionalDSLKeyword).removeButton.fire() }
            it.removeButton.fire()
            this.initScript()
        }
        //if we find a SecondaryStructureKw, this one has been injected from the last run of the script and we need to remove it from the children, since it should not stay as a regular child for RNArtistKw
        (this.root as RNArtistKw).searchFirst {it is SecondaryStructureKw && it.inFinalScript}?.let {
            (this.root as RNArtistKw).children.remove(it)
            this.initScript()
        }

    }

}

class SecondaryStructureScript(mediator:Mediator):Script(mediator) {

    override var root: DSLElement = SecondaryStructureKw(this)

    init {
        this.prefWidth = Region.USE_COMPUTED_SIZE
        this.initScript()
    }

    override fun initScript() {
        if (this.allowScriptInit) {
            this.getScriptRoot()?.let {
                this.children.clear()
                var nodes = mutableListOf<Node>()
                it.dumpNodes(nodes)
                this.children.addAll(nodes)
                this.layout()
            }
        }
    }

    override fun removeSecondaryStructure() {
        (this.root as SecondaryStructureKw).removeButton.fire()
        this.initScript()
    }

    override fun getScriptRoot():SecondaryStructureKw = this.root as SecondaryStructureKw
}

class ScriptEditor(val mediator: Mediator) {

    var currentScriptLocation:File? = null
    val themeAndLayoutScript = ThemeAndLayoutScript(mediator)
    val secondaryStructureScript = SecondaryStructureScript(mediator)
    val stage = Stage()
    val tabPane = TabPane()
    private val run = Button(null, FontIcon("fas-play:15"))

    init {
        stage.title = "Script Editor"
        createScene(stage)
    }

    private fun createScene(stage: Stage) {
        val root = BorderPane()
        val manager = ScriptEngineManager()
        val engine = manager.getEngineByExtension("kts")
        themeAndLayoutScript.style = "-fx-background-color: ${getHTMLColorString(RnartistConfig.backgroundEditorColor)}"
        themeAndLayoutScript.padding = Insets(10.0, 10.0, 10.0, 10.0)
        themeAndLayoutScript.lineSpacing = 10.0
        themeAndLayoutScript.tabSize = 6
        themeAndLayoutScript.layout()

        secondaryStructureScript.style = "-fx-background-color: ${getHTMLColorString(RnartistConfig.backgroundEditorColor)}"
        secondaryStructureScript.padding = Insets(10.0, 10.0, 10.0, 10.0)
        secondaryStructureScript.lineSpacing = 10.0
        secondaryStructureScript.tabSize = 6
        secondaryStructureScript.layout()

        val topToolbar = ToolBar()
        topToolbar.padding = Insets(5.0, 5.0, 5.0, 5.0)

        val loadScriptPane = GridPane()
        loadScriptPane.vgap = 5.0
        loadScriptPane.hgap = 5.0

        var l = Label("Load")
        GridPane.setHalignment(l, HPos.CENTER)
        GridPane.setConstraints(l, 0, 0)
        loadScriptPane.children.add(l)

        val loadScript = MenuButton(null, FontIcon("fas-sign-in-alt:15"))
        val scriptsLibraryMenu = Menu("Scripts Library")

        val newScript = MenuItem("New Script")
        newScript.onAction = EventHandler {
            //we erase the 2D displayed
            mediator.drawingDisplayed.set(null)
            mediator.canvas2D.repaint()
            currentScriptLocation = null
            //we erase the previous scripts
            themeAndLayoutScript.setScriptRoot(RNArtistKw(themeAndLayoutScript))
            secondaryStructureScript.setScriptRoot(SecondaryStructureKw(secondaryStructureScript))
        }

        val loadFile = MenuItem("Load Script..")
        loadFile.onAction = EventHandler {

            val fileChooser = FileChooser()
            fileChooser.initialDirectory = File(mediator.rnartist.getInstallDir(), "samples")
            val file = fileChooser.showOpenDialog(stage)
            file?.let {
                currentScriptLocation = file.parentFile
                loadScript(FileReader(file))
            }
        }

        val loadGist = MenuItem("Load Gist..")
        loadGist.onAction =  EventHandler {
            currentScriptLocation = null
            val gistInput = TextInputDialog()
            gistInput.title = "Enter your Gist ID"
            gistInput.graphic = null
            gistInput.headerText = null
            gistInput.contentText = "Gist ID"
            gistInput.editor.text = "Paste your ID"
            var gistID = gistInput.showAndWait()
            if (gistID.isPresent && !gistID.isEmpty) {
                val gistContent = URL("https://api.github.com/gists/${gistID.get()}").readText()
                val regex = Regex("\"content\":\"(import io.+?)\"},\"rnartist\\.svg\":\\{")
                val match = regex.find(gistContent)
                val scriptContent = match?.groupValues?.get(1)?.
                    replace("\\n",System.lineSeparator())?.
                    replace("\\t", " ")?.
                    replace("\\\"","\"")
                loadScript(StringReader(scriptContent))
            }
        }

        loadScript.getItems().addAll(newScript, loadFile, loadGist, scriptsLibraryMenu)

        val load2D = Menu("Load 2D..")

        scriptsLibraryMenu.items.add(load2D)

        var menuItem = MenuItem("...from bracket notation")
        menuItem.onAction = EventHandler {
            currentScriptLocation = null
            val file = File(mediator.rnartist.getInstallDir(), "/samples/scripts/from_bn.kts")
            loadScript(FileReader(file))
        }

        load2D.items.add(menuItem)

        menuItem = MenuItem("...from bracket notation with data")
        menuItem.setOnAction {
            currentScriptLocation = null
            val file = File(mediator.rnartist.getInstallDir(), "/samples/scripts/from_bn_with_data.kts")
            loadScript(FileReader(file))
        }

        load2D.items.add(menuItem)

        val fromLocalFilesMenu = Menu("...from Local Files")
        val fromDatabasesMenu = Menu("...from Databases")
        load2D.items.addAll(fromLocalFilesMenu, fromDatabasesMenu)

        menuItem = MenuItem("Vienna Format")
        menuItem.onAction = EventHandler {
            currentScriptLocation = null
            val file = File(mediator.rnartist.getInstallDir(), "/samples/scripts/from_vienna_file.kts")
            loadScript(FileReader(file))
        }
        fromLocalFilesMenu.items.add(menuItem)

        menuItem = MenuItem("CT Format")
        menuItem.onAction = EventHandler {
            currentScriptLocation = null
            val file = File(mediator.rnartist.getInstallDir(), "/samples/scripts/from_ct_file.kts")
            loadScript(FileReader(file))
        }
        fromLocalFilesMenu.items.add(menuItem)

        menuItem = MenuItem("BPSeq Format")
        menuItem.onAction = EventHandler {
            currentScriptLocation = null
            val file = File(mediator.rnartist.getInstallDir(), "/samples/scripts/from_bpseq_file.kts")
            loadScript(FileReader(file))
        }
        fromLocalFilesMenu.items.add(menuItem)

        menuItem = MenuItem("Stockholm Format")
        menuItem.onAction = EventHandler {
            currentScriptLocation = null
            val file = File(mediator.rnartist.getInstallDir(), "/samples/scripts/from_stockholm_file.kts")
            loadScript(FileReader(file))
        }
        fromLocalFilesMenu.items.add(menuItem)

        menuItem = MenuItem("Rfam DB")
        menuItem.onAction = EventHandler {
            currentScriptLocation = null
            val file = File(mediator.rnartist.getInstallDir(), "/samples/scripts/from_rfam_entry.kts")
            loadScript(FileReader(file))
        }
        fromDatabasesMenu.items.add(menuItem)

        menuItem = MenuItem("PDB")
        menuItem.onAction = EventHandler {
            currentScriptLocation = null
            val file = File(mediator.rnartist.getInstallDir(), "/samples/scripts/from_pdb.kts")
            loadScript(FileReader(file))
        }
        fromDatabasesMenu.items.add(menuItem)

        val themes = Menu("Create Theme..")
        scriptsLibraryMenu.items.add(themes)

        val layout = Menu("Create Layout..")
        scriptsLibraryMenu.items.add(layout)

        GridPane.setConstraints(loadScript, 0, 1)
        loadScriptPane.children.add(loadScript)

        val exportScriptPane = GridPane()
        exportScriptPane.vgap = 5.0
        exportScriptPane.hgap = 5.0

        l = Label("Export")
        GridPane.setHalignment(l, HPos.CENTER)
        GridPane.setConstraints(l, 0, 0)
        exportScriptPane.children.add(l)

        val saveScript = MenuButton(null, FontIcon("fas-sign-out-alt:15"))

        val saveAsFile = MenuItem("Export in File..")
        saveAsFile.onAction = EventHandler<ActionEvent?> {
            val fileChooser = FileChooser()
            fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("RNArtist Scripts", "*.kts"))
            val file = fileChooser.showSaveDialog(stage)
            if (file != null) {
                if (file.name.endsWith(".kts")) {
                    fileChooser.initialDirectory = file.parentFile

                    themeAndLayoutScript.getScriptRoot().getSecondaryStructureKw().let { ssKw ->
                        val inputFiles = mutableListOf<DSLElement>()
                        ssKw.searchAll(inputFiles) { it is OptionalDSLKeyword && it.inFinalScript && it.text.text.trim() in listOf("pdb", "vienna", "stockholm", "ct", "bpseq") }
                        if (inputFiles.isNotEmpty()) {
                            //the script loaded the 2D from local files, we will rather store the 2D as a script
                            secondaryStructureScript.getScriptRoot().addButton.fire()
                        }
                    }
                    //now we save the script...
                    var writer: PrintWriter
                    try {
                        writer = PrintWriter(file)
                        writer.println(getScriptAsText())
                        writer.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }

        }

        val saveAsGist = MenuItem("Publish as GitHub Gist..")
        saveAsGist.onAction = EventHandler {
            val token = ""
            if (token.trim().isNotEmpty()) {
                mediator.drawingDisplayed.get()?.drawing?.let { drawing ->
                    val dialog = Dialog<String>()
                    dialog.headerText = "Description of your Plot"
                    dialog.initModality(Modality.NONE)
                    dialog.title = "Publish Plot as a GitHub Gist"
                    dialog.contentText = null
                    dialog.dialogPane.content = TextArea()
                    val publish = ButtonType("Publish", ButtonData.OK_DONE)
                    dialog.dialogPane.buttonTypes.add(publish)
                    dialog.setResultConverter { b ->
                        if (b == publish) {
                            (dialog.dialogPane.content as TextArea).text.trim()
                        } else null
                    }
                    val description = dialog.showAndWait()
                    if (description.isPresent && !description.isEmpty) {
                        try {
                            val client = OkHttpClient()
                            val body = """{"description":"${
                                description.get().trim()
                            }", "files":{"rnartist.kts":{"content":"${
                                getScriptAsText().replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t")
                            }"},
                        "rnartist.svg":{"content":"${
                                drawing.asSVG(Rectangle2D.Double(0.0, 0.0, 800.0, 800.0)).replace("\"", "\\\"")
                            }"}}, "public":true}"""
                            println(body)
                            val request = Request.Builder()
                                .url("https://api.github.com/gists")
                                .header("User-Agent", "OkHttp Headers.java")
                                .addHeader("Authorization", "bearer $token")
                                .post(
                                    body.toRequestBody("application/json".toMediaTypeOrNull())
                                )
                                .build()

                            client.newCall(request).execute().use { response ->
                                if (!response.isSuccessful) {
                                    println("Problem!")
                                    println(response.body?.charStream()?.readText())
                                } else {
                                    val root =
                                        JsonParser.parseString(response.body?.charStream()?.readText())
                                            .getAsJsonObject()
                                    val alert = Alert(Alert.AlertType.INFORMATION)
                                    alert.headerText = "Script published Successfully."
                                    alert.graphic = FontIcon("fab-github:15")
                                    alert.buttonTypes.clear()
                                    alert.buttonTypes.add(ButtonType.OK)
                                    alert.buttonTypes.add(ButtonType("Show me", ButtonData.HELP))
                                    var result = alert.showAndWait()
                                    if (result.isPresent && result.get() != ButtonType.OK) { //show me
                                        Desktop.getDesktop().browse(URL(root.get("html_url").asString).toURI())
                                    }
                                }

                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }
                }
            } else {
                val alert = Alert(Alert.AlertType.WARNING)
                alert.headerText = "Gist publication not available."
                alert.contentText = "This feature will be activated soon"
                alert.show()
            }
        }

        saveScript.getItems().addAll(saveAsFile, saveAsGist)

        GridPane.setConstraints(saveScript, 0, 1)
        exportScriptPane.children.add(saveScript)

        val fontPane = GridPane()
        fontPane.vgap = 5.0
        fontPane.hgap = 5.0

        l = Label("Font")
        GridPane.setHalignment(l, HPos.CENTER)
        GridPane.setConstraints(l, 0, 0, 2, 1)
        fontPane.children.add(l)

        val fontFamilies = Font.getFamilies()

        val fontChooser = ComboBox<String>()
        fontChooser.items.addAll(fontFamilies)
        fontChooser.value = RnartistConfig.editorFontName
        GridPane.setConstraints(fontChooser, 0, 1)

        fontChooser.onAction = EventHandler {
            RnartistConfig.editorFontName = fontChooser.value
            val hits = mutableListOf<DSLElement>()
            themeAndLayoutScript.getScriptRoot().searchAll(hits) { it is DSLElement }
            secondaryStructureScript.getScriptRoot().searchAll(hits) { it is DSLElement }
            hits.forEach {
                it.fontName = fontChooser.value
                (it as? OptionalDSLKeyword)?.addButton?.setFontName(fontChooser.value)
                (it as? OptionalDSLParameter)?.addButton?.setFontName(fontChooser.value)
            }
            themeAndLayoutScript.initScript()
            secondaryStructureScript.initScript()
        }

        fontPane.children.add(fontChooser)

        val sizeFont = Spinner<Int>(5, 40, RnartistConfig.editorFontSize)
        sizeFont.isEditable = true
        sizeFont.prefWidth = 75.0
        sizeFont.onMouseClicked = EventHandler {
            RnartistConfig.editorFontSize = sizeFont.value
            val hits = mutableListOf<DSLElement>()
            themeAndLayoutScript.getScriptRoot().searchAll(hits) { it is DSLElement }
            secondaryStructureScript.getScriptRoot().searchAll(hits) { it is DSLElement }
            hits.forEach {
                it.fontSize = sizeFont.value
                (it as? OptionalDSLKeyword)?.addButton?.setFontSize(sizeFont.value)
                (it as? OptionalDSLParameter)?.addButton?.setFontSize(sizeFont.value)
            }
            themeAndLayoutScript.initScript()
            secondaryStructureScript.initScript()
        }

        GridPane.setConstraints(sizeFont, 1, 1)
        fontPane.children.add(sizeFont)

        val runPane = GridPane()
        runPane.vgap = 5.0
        runPane.hgap = 5.0

        l = Label("Run")
        GridPane.setHalignment(l, HPos.CENTER)
        GridPane.setConstraints(l, 0, 0)
        runPane.children.add(l)

        run.onAction = EventHandler {
            Platform.runLater {
                try {
                    when (val result = engine.eval(getScriptAsText())) {
                        is List<*> -> {
                            //first we remove the drawings loaded with the same script id (if any)
                            mediator.drawingsLoaded.removeIf { it is DrawingLoadedFromScriptEditor && it.id.equals(themeAndLayoutScript.getScriptRoot()!!.id) }
                            //then we add the new ones
                            (result as? List<SecondaryStructureDrawing>)?.forEach {
                                mediator.drawingsLoaded.add(
                                    DrawingLoadedFromScriptEditor(
                                        mediator,
                                        it, themeAndLayoutScript.getScriptRoot()!!.id
                                    )
                                )
                                mediator.drawingDisplayed.set(mediator.drawingsLoaded[mediator.drawingsLoaded.size - 1])
                                mediator.canvas2D.fitStructure(null)
                            }

                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            };
        }

        GridPane.setConstraints(run, 0, 1)
        runPane.children.add(run)

        val s1 = Separator()
        s1.padding = Insets(0.0, 5.0, 0.0, 5.0)

        val s2 = Separator()
        s2.padding = Insets(0.0, 5.0, 0.0, 5.0)

        val s3 = Separator()
        s3.padding = Insets(0.0, 5.0, 0.0, 5.0)

        topToolbar.items.addAll(loadScriptPane, s1, exportScriptPane, s2, fontPane, s3, runPane)

        val leftToolbar = VBox()
        leftToolbar.alignment = Pos.TOP_CENTER
        leftToolbar.spacing = 5.0
        leftToolbar.padding = Insets(10.0, 5.0, 10.0, 5.0)

        val decreaseTab = Button(null, FontIcon("fas-outdent:15"))
        decreaseTab.onAction = EventHandler {
            if (themeAndLayoutScript.tabSize > 1) {
                themeAndLayoutScript.tabSize--
            }
            themeAndLayoutScript.layout()
            if (secondaryStructureScript.tabSize > 1) {
                secondaryStructureScript.tabSize--
            }
            secondaryStructureScript.layout()
        }

        val increaseTab = Button(null, FontIcon("fas-indent:15"))
        increaseTab.onAction = EventHandler {
            themeAndLayoutScript.tabSize++
            secondaryStructureScript.tabSize++
        }

        val decreaseLineSpacing = Button(null, FontIcon("fas-compress-alt:15"))
        decreaseLineSpacing.onAction = EventHandler {
            themeAndLayoutScript.lineSpacing--
            secondaryStructureScript.lineSpacing--
        }

        val increaseLineSpacing = Button(null, FontIcon("fas-expand-alt:15"))
        increaseLineSpacing.onAction = EventHandler {
            themeAndLayoutScript.lineSpacing++
            secondaryStructureScript.lineSpacing++
        }

        val expandAll = Button(null, FontIcon("fas-plus:15"))
        expandAll.onAction = EventHandler {
            when (tabPane.selectionModel.selectedIndex) {
                0 -> themeAndLayoutScript.children.filterIsInstance<Collapse>().map {
                    if (it.collapsed)
                        it.fire()
                }
                1 -> secondaryStructureScript.children.filterIsInstance<Collapse>().map {
                    if (it.collapsed)
                        it.fire()
                }
            }
        }

        val collapseAll = Button(null, FontIcon("fas-minus:15"))
        collapseAll.onAction = EventHandler {
            when (tabPane.selectionModel.selectedIndex) {
                0 -> themeAndLayoutScript.children.filterIsInstance<Collapse>().map {
                    if (!it.collapsed)
                        it.fire()
                }
                1 -> secondaryStructureScript.children.filterIsInstance<Collapse>().map {
                    if (!it.collapsed)
                        it.fire()
                }
            }
        }

        val bgColor = ColorPicker()
        bgColor.value = awtColorToJavaFX(RnartistConfig.backgroundEditorColor)
        bgColor.styleClass.add("button")
        bgColor.style = "-fx-color-label-visible: false ;"
        bgColor.onAction = EventHandler {
            themeAndLayoutScript.style = "-fx-background-color: ${getHTMLColorString(javaFXToAwt(bgColor.value))}"
            secondaryStructureScript.style = "-fx-background-color: ${getHTMLColorString(javaFXToAwt(bgColor.value))}"
            RnartistConfig.backgroundEditorColor = javaFXToAwt(bgColor.value)
        }

        val kwColor = ColorPicker()
        kwColor.value = awtColorToJavaFX(RnartistConfig.keywordEditorColor)
        kwColor.styleClass.add("button")
        kwColor.style = "-fx-color-label-visible: false ;"
        kwColor.onAction = EventHandler {
            val hits = mutableListOf<DSLElement>()
            themeAndLayoutScript.getScriptRoot()?.searchAll(hits) { it is DSLKeyword }
            secondaryStructureScript.getScriptRoot()?.searchAll(hits) { it is DSLKeyword }
            hits.forEach {
                it.color = kwColor.value
                (it as DSLKeyword).collapseButton.setColor(kwColor.value)
                (it as? OptionalDSLKeyword)?.addButton?.setColor(kwColor.value)
            }
            RnartistConfig.keywordEditorColor = javaFXToAwt(kwColor.value)
            themeAndLayoutScript.initScript()
            secondaryStructureScript.initScript()
        }

        val bracesColor = ColorPicker()
        bracesColor.value = awtColorToJavaFX(RnartistConfig.bracesEditorColor)
        bracesColor.styleClass.add("button")
        bracesColor.style = "-fx-color-label-visible: false ;"
        bracesColor.onAction = EventHandler {
            val hits = mutableListOf<DSLElement>()
            themeAndLayoutScript.getScriptRoot()?.searchAll(hits) { it is OpenedCurly || it is ClosedCurly }
            secondaryStructureScript.getScriptRoot()?.searchAll(hits) { it is OpenedCurly || it is ClosedCurly }
            hits.forEach {
                it.color = bracesColor.value
            }
            RnartistConfig.bracesEditorColor = javaFXToAwt(bracesColor.value)
            themeAndLayoutScript.initScript()
            secondaryStructureScript.initScript()
        }

        val keyParamColor = ColorPicker()
        keyParamColor.value = awtColorToJavaFX(RnartistConfig.keyParamEditorColor)
        keyParamColor.styleClass.add("button")
        keyParamColor.style = "-fx-color-label-visible: false ;"
        keyParamColor.onAction = EventHandler {
            val hits = mutableListOf<DSLElement>()
            themeAndLayoutScript.getScriptRoot()?.searchAll(hits) { it is DSLParameter }
            secondaryStructureScript.getScriptRoot()?.searchAll(hits) { it is DSLParameter }
            hits.forEach {
                (it as DSLParameter).key.color = keyParamColor.value
                (it as? OptionalDSLParameter)?.addButton?.setColor(keyParamColor.value)
            }
            RnartistConfig.keyParamEditorColor = javaFXToAwt(keyParamColor.value)
            themeAndLayoutScript.initScript()
            secondaryStructureScript.initScript()
        }

        val operatorParamColor = ColorPicker()
        operatorParamColor.value = awtColorToJavaFX(RnartistConfig.operatorParamEditorColor)
        operatorParamColor.styleClass.add("button")
        operatorParamColor.style = "-fx-color-label-visible: false ;"
        operatorParamColor.onAction = EventHandler {
            val hits = mutableListOf<DSLElement>()
            themeAndLayoutScript.getScriptRoot()?.searchAll(hits) { it is DSLParameter }
            secondaryStructureScript.getScriptRoot()?.searchAll(hits) { it is DSLParameter }
            hits.forEach {
                (it as DSLParameter).operator.color = operatorParamColor.value
            }
            RnartistConfig.operatorParamEditorColor = javaFXToAwt(operatorParamColor.value)
            themeAndLayoutScript.initScript()
            secondaryStructureScript.initScript()
        }

        val valueParamColor = ColorPicker()
        valueParamColor.value = awtColorToJavaFX(RnartistConfig.valueParamEditorColor)
        valueParamColor.styleClass.add("button")
        valueParamColor.style = "-fx-color-label-visible: false ;"
        valueParamColor.onAction = EventHandler {
            val hits = mutableListOf<DSLElement>()
            themeAndLayoutScript.getScriptRoot()?.searchAll(hits) { it is DSLParameter }
            secondaryStructureScript.getScriptRoot()?.searchAll(hits) { it is DSLParameter }
            hits.forEach {
                (it as DSLParameter).value.color = valueParamColor.value
            }
            RnartistConfig.valueParamEditorColor = javaFXToAwt(valueParamColor.value)
            themeAndLayoutScript.initScript()
            secondaryStructureScript.initScript()
        }

        val spacer = Region()
        spacer.prefHeight = 20.0
        leftToolbar.children.addAll(
            expandAll,
            collapseAll,
            increaseLineSpacing,
            decreaseLineSpacing,
            increaseTab,
            decreaseTab,
            spacer,
            Label("Bg"),
            bgColor,
            Label("Kw"),
            kwColor,
            Label("{ }"),
            bracesColor,
            Label("Key"),
            keyParamColor,
            Label("Op"),
            operatorParamColor,
            Label("Val"),
            valueParamColor
        )

        root.top = topToolbar
        root.left = leftToolbar
        root.center = tabPane

        var scrollpane = ScrollPane(themeAndLayoutScript)
        scrollpane.isFitToHeight = true
        themeAndLayoutScript.minWidthProperty().bind(scrollpane.widthProperty())
        tabPane.tabs.add(Tab("Theme & Layout", scrollpane))

        scrollpane = ScrollPane(secondaryStructureScript)
        scrollpane.isFitToHeight = true
        secondaryStructureScript.minWidthProperty().bind(scrollpane.widthProperty())
        tabPane.tabs.add(Tab("Secondary Structure", scrollpane))

        tabPane.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        val scene = Scene(root)
        scene.stylesheets.add("io/github/fjossinet/rnartist/gui/css/main.css")
        stage.scene = scene
        val screenSize = Screen.getPrimary().bounds
        val width = (screenSize.width * 0.5).toInt()
        scene.window.width = width.toDouble()
        scene.window.height = screenSize.height
        scene.window.x = screenSize.width - width
        scene.window.y = 0.0
    }

    fun loadScript(scriptContent:Reader) {
        //we erase the 2D displayed
        mediator.drawingDisplayed.set(null)
        mediator.canvas2D.repaint()
        //first we erase the previous scripts
        themeAndLayoutScript.setScriptRoot(RNArtistKw(themeAndLayoutScript))
        secondaryStructureScript.setScriptRoot(SecondaryStructureKw(secondaryStructureScript))
        //to avoid doing initScript() for each addbutton fired
        themeAndLayoutScript.allowScriptInit = false
        secondaryStructureScript.allowScriptInit = false

        //we construct the model
        val (themeAndLayoutScriptRoot, secondaryStructureScriptRoot, issues) = parseScript(scriptContent)
        themeAndLayoutScript.setScriptRoot(themeAndLayoutScriptRoot)
        secondaryStructureScript.setScriptRoot(secondaryStructureScriptRoot)

        //we dump the nodes
        themeAndLayoutScript.allowScriptInit = true
        themeAndLayoutScript.initScript()
        secondaryStructureScript.allowScriptInit = true
        themeAndLayoutScript.initScript()
        if (issues.isNotEmpty()) {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.headerText = "I fixed issues in your script."
            alert.contentText = issues.joinToString(separator = "\n")
            alert.buttonTypes.clear()
            alert.buttonTypes.add(ButtonType.OK)
            alert.buttonTypes.add(ButtonType("Go to Documentation", ButtonData.HELP))
            var result = alert.showAndWait()
            if (result.isPresent && result.get() != ButtonType.OK) { //show documentation
                Desktop.getDesktop()
                    .browse(URL("https://github.com/fjossinet/RNArtistCore/blob/master/Changelog.md").toURI())
            }
        }
    }

    fun runScript() {
        this.run.fire()
    }

    fun getScriptAsText(): String {
        if (secondaryStructureScript.getScriptRoot().inFinalScript) {
            var root = secondaryStructureScript.getScriptRoot()
            //this means that we have a secondary structure defined separately (and then no ss element should be present in the layout & theme script)
            //so we need to inject the ss defined separately in the themeAndLayoutScript
            themeAndLayoutScript.setSecondaryStructure(root)
            var nodes = mutableListOf<Node>()
            themeAndLayoutScript.getScriptRoot().dumpNodes(nodes, true)
            if (nodes.filterIsInstance<DataField>().isNotEmpty())
                println("Missing data")
            var scriptAsText = (nodes.filterIsInstance<Text>().map {
                it.text
            }).joinToString(separator = "")
            scriptAsText = scriptAsText.split("\n").filter { !it.matches(Regex("^\\s*$")) }.joinToString(separator = "\n")
            themeAndLayoutScript.removeSecondaryStructure()
            root.decreaseIndentLevel()
            secondaryStructureScript.initScript() //if i don't do that, the secondaryStructureScript becomes empty due to the previous instruction. Not sure why
            return "import io.github.fjossinet.rnartist.core.*\n\n ${scriptAsText}"
        } else {
            var nodes = mutableListOf<Node>()
            themeAndLayoutScript.getScriptRoot().dumpNodes(nodes, true)
            if (nodes.filterIsInstance<DataField>().isNotEmpty())
                println("Missing data")
            var scriptAsText = (nodes.filterIsInstance<Text>().map {
                it.text
            }).joinToString(separator = "")
            scriptAsText = scriptAsText.split("\n").filter { !it.matches(Regex("^\\s*$")) }.joinToString(separator = "\n")
            return "import io.github.fjossinet.rnartist.core.*\n\n ${scriptAsText}"
        }
    }

    @Throws(java.lang.Exception::class)
    fun parseScript(reader: Reader): Triple<DSLElement, DSLElement, List<String>> {
        var (elements, issues) = parseDSLScript(reader)

        val themeAndLayoutScriptRoot = RNArtistKw(themeAndLayoutScript)
        val secondaryStructureScriptRoot = SecondaryStructureKw(secondaryStructureScript)

        elements.first().children.forEach { element ->
            when (element.name) {
                "ss" -> {
                    val secondaryStructureInputKw = themeAndLayoutScriptRoot.searchFirst { it is SecondaryStructureInputKw } as SecondaryStructureInputKw
                    val secondaryStructureKw = secondaryStructureScriptRoot.searchFirst { it is SecondaryStructureKw } as SecondaryStructureKw
                    element.attributes.forEach { attribute ->
                        val tokens = attribute.split("=")
                        if ("source".equals(tokens.first().trim())) {
                            val tokens = attribute.split("=")
                            val p = secondaryStructureKw.searchFirst { it is OptionalDSLParameter && "source".equals(it.key.text.text.trim())} as OptionalDSLParameter
                            p.value.text.text = tokens.last()
                            p.addButton.fire()
                        }
                    }
                    element.children.forEach { elementChild ->
                        when (elementChild.name) {
                            "bn" -> {
                                if (!secondaryStructureInputKw.inFinalScript)
                                    secondaryStructureInputKw.addButton.fire()
                                val bnKw =
                                    secondaryStructureInputKw.searchFirst { child -> child is BracketNotationKw && !child.inFinalScript } as BracketNotationKw
                                bnKw.addButton.fire()
                                elementChild.attributes.forEach { attribute ->
                                    val tokens = attribute.split("=")
                                    if ("value".equals(tokens.first().trim())) {
                                        val parameter =
                                            (bnKw.searchFirst { it is DSLParameter && "value".equals(it.key.text.text) } as DSLParameter)
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                    if ("seq".equals(tokens.first().trim())) {
                                        val parameter =
                                            (bnKw.searchFirst { it is SequenceBnParameter } as SequenceBnParameter)
                                        parameter.value.text.text = tokens.last().trim()
                                        parameter.addButton.fire()
                                    }
                                }
                            }

                            "vienna" -> {
                                if (!secondaryStructureInputKw.inFinalScript)
                                    secondaryStructureInputKw.addButton.fire()
                                val viennaKw =
                                    (secondaryStructureInputKw.searchFirst { themeChild -> themeChild is ViennaKw && !themeChild.inFinalScript } as ViennaKw)
                                viennaKw.addButton.fire()
                                elementChild.attributes.forEach { attribute ->
                                    val tokens = attribute.split("=")
                                    if ("file".equals(tokens.first().trim())) {
                                        val parameter =
                                            (viennaKw.searchFirst { it is DSLParameter && "file".equals(it.key.text.text) } as DSLParameter)
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                }
                            }

                            "bpseq" -> {
                                if (!secondaryStructureInputKw.inFinalScript)
                                    secondaryStructureInputKw.addButton.fire()
                                val bpseqKw =
                                    (secondaryStructureInputKw.searchFirst { themeChild -> themeChild is BpseqKw && !themeChild.inFinalScript } as BpseqKw)
                                bpseqKw.addButton.fire()
                                elementChild.attributes.forEach { attribute ->
                                    val tokens = attribute.split("=")
                                    if ("file".equals(tokens.first().trim())) {
                                        val parameter =
                                            (bpseqKw.searchFirst { it is DSLParameter && "file".equals(it.key.text.text) } as DSLParameter)
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                }
                            }

                            "ct" -> {
                                if (!secondaryStructureInputKw.inFinalScript)
                                    secondaryStructureInputKw.addButton.fire()
                                val ctKw =
                                    (secondaryStructureInputKw.searchFirst { themeChild -> themeChild is CtKw && !themeChild.inFinalScript } as CtKw)
                                ctKw.addButton.fire()
                                elementChild.attributes.forEach { attribute ->
                                    val tokens = attribute.split("=")
                                    if ("file".equals(tokens.first().trim())) {
                                        val parameter =
                                            (ctKw.searchFirst { it is DSLParameter && "file".equals(it.key.text.text) } as DSLParameter)
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                }
                            }

                            "stockholm" -> {
                                if (!secondaryStructureInputKw.inFinalScript)
                                    secondaryStructureInputKw.addButton.fire()
                                val stockholmKw =
                                    (secondaryStructureInputKw.searchFirst { themeChild -> themeChild is StockholmKw && !themeChild.inFinalScript } as StockholmKw)
                                stockholmKw.addButton.fire()
                                elementChild.attributes.forEach { attribute ->
                                    val tokens = attribute.split("=")
                                    if ("file".equals(tokens.first().trim())) {
                                        val parameter =
                                            (stockholmKw.searchFirst { it is DSLParameter && "file".equals(it.key.text.text) } as DSLParameter)
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                }
                            }

                            "rfam" -> {
                                if (!secondaryStructureInputKw.inFinalScript)
                                    secondaryStructureInputKw.addButton.fire()
                                val rfamKw =
                                    (secondaryStructureInputKw.searchFirst { themeChild -> themeChild is RfamKw && !themeChild.inFinalScript } as RfamKw)
                                rfamKw.addButton.fire()
                                elementChild.attributes.forEach { attribute ->
                                    if (attribute.startsWith("use alignment numbering")) {
                                        val parameter =
                                            rfamKw.searchFirst { it is OptionalDSLParameter && "use".equals(it.key.text.text) && "alignment".equals(it.operator.text.text.trim()) && "numbering".equals(it.value.text.text) } as OptionalDSLParameter
                                        parameter.addButton.fire()
                                    }
                                    val tokens = attribute.split("=")
                                    if ("id".equals(tokens.first().trim())) {
                                        val parameter =
                                            rfamKw.searchFirst { it is DSLParameter && "id".equals(it.key.text.text) } as DSLParameter
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                    if ("name".equals(tokens.first().trim())) {
                                        val parameter =
                                            rfamKw.searchFirst { it is OptionalDSLParameter && "name".equals(it.key.text.text) } as OptionalDSLParameter
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                }
                            }

                            "pdb" -> {
                                if (!secondaryStructureInputKw.inFinalScript)
                                    secondaryStructureInputKw.addButton.fire()
                                val pdbKW =
                                    (secondaryStructureInputKw.searchFirst { themeChild -> themeChild is PDBKw && !themeChild.inFinalScript } as PDBKw)
                                pdbKW.addButton.fire()
                                elementChild.attributes.forEach { attribute ->
                                    val tokens = attribute.split("=")
                                    if ("file".equals(tokens.first().trim())) {
                                        val parameter =
                                            pdbKW.searchFirst { it is OptionalDSLParameter && "file".equals(it.key.text.text) } as OptionalDSLParameter
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                    if ("id".equals(tokens.first().trim())) {
                                        val parameter =
                                            pdbKW.searchFirst { it is OptionalDSLParameter && "id".equals(it.key.text.text) } as OptionalDSLParameter
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                    if ("name".equals(tokens.first().trim())) {
                                        val parameter =
                                            pdbKW.searchFirst { it is OptionalDSLParameter && "name".equals(it.key.text.text) } as OptionalDSLParameter
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                }
                            }

                            "rna" -> {
                                if (!secondaryStructureKw.inFinalScript)
                                    secondaryStructureKw.addButton.fire()
                                val rnaKw =
                                    (secondaryStructureKw.searchFirst { child -> child is RnaKw} as RnaKw)
                                elementChild.attributes.forEach { attribute ->
                                    val tokens = attribute.split("=")
                                    if ("seq".equals(tokens.first().trim())) {
                                        val parameter =
                                            rnaKw.searchFirst { it is OptionalDSLParameter && "seq".equals(it.key.text.text) } as OptionalDSLParameter
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                    if ("length".equals(tokens.first().trim())) {
                                        val parameter =
                                            rnaKw.searchFirst { it is OptionalDSLParameter && "length".equals(it.key.text.text) } as OptionalDSLParameter
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                    if ("name".equals(tokens.first().trim())) {
                                        val parameter =
                                            (rnaKw.searchFirst { it is OptionalDSLParameter && "name".equals(it.key.text.text) } as OptionalDSLParameter)
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                }
                            }
                            "helix" -> {
                                if (!secondaryStructureKw.inFinalScript)
                                    secondaryStructureKw.addButton.fire()
                                val helixKw =
                                    secondaryStructureKw.searchFirst { child -> child is HelixKw && !child.inFinalScript} as HelixKw
                                helixKw.addButton.fire()
                                elementChild.attributes.forEach { attribute ->
                                    val tokens = attribute.split("=")
                                    if ("name".equals(tokens.first().trim())) {
                                        val parameter =
                                            helixKw.searchFirst { it is OptionalDSLParameter && "name".equals(it.key.text.text) } as OptionalDSLParameter
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                }
                                elementChild.children.forEach { elementChildChild ->
                                    when (elementChildChild.name) {
                                        "location" -> {
                                            val blocks = mutableListOf<Block>()
                                            elementChildChild.attributes.forEach { attribute ->
                                                val tokens = attribute.split("to")
                                                blocks.add(Block(tokens.first().trim().toInt(), tokens.last().trim().toInt()))
                                            }
                                            helixKw.setLocation(Location(blocks))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "theme" -> {
                    val themeKw = themeAndLayoutScriptRoot.searchFirst { it is ThemeKw } as ThemeKw
                    themeKw.addButton.fire()
                    element.children.forEach { elementChild ->
                        when (elementChild.name) {
                            "details" -> {
                                val detailsLevelKw =
                                    themeKw.searchFirst { themeChild -> themeChild is DetailsKw && !themeChild.inFinalScript } as DetailsKw
                                detailsLevelKw.addButton.fire()
                                elementChild.attributes.forEach { attribute ->
                                    var tokens = attribute.split("=")
                                    if ("value".equals(tokens.first().trim())) {
                                        val parameter =
                                            (detailsLevelKw.searchFirst { it is DSLParameter && "value".equals(it.key.text.text) } as DSLParameter)
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                    if ("type".equals(tokens.first().trim())) {
                                        val parameter =
                                            (detailsLevelKw.searchFirst { it is OptionalDSLParameter && "type".equals(it.key.text.text) } as OptionalDSLParameter)
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                }
                                elementChild.children.forEach { elementChildChild ->
                                    when (elementChildChild.name) {
                                        "location" -> {
                                            val blocks = mutableListOf<Block>()
                                            elementChildChild.attributes.forEach { attribute ->
                                                val tokens = attribute.split("to")
                                                blocks.add(Block(tokens.first().trim().toInt(), tokens.last().trim().toInt()))
                                            }
                                            detailsLevelKw.setLocation(Location(blocks))
                                        }
                                    }
                                }
                            }
                            "color" -> {
                                val colorKw =
                                    themeKw.searchFirst { themeChild -> themeChild is ColorKw && !themeChild.inFinalScript } as ColorKw
                                colorKw.addButton.fire()
                                elementChild.attributes.forEach { attribute ->
                                    var tokens = attribute.split("=")
                                    if ("value".equals(tokens.first().trim())) {
                                        val parameter =
                                            (colorKw.searchFirst { it is DSLParameter && "value".equals(it.key.text.text) } as DSLParameter)
                                        parameter.value.text.text = tokens.last().trim()
                                        parameter.value.text.fill = Color.web(tokens.last().trim().replace("\"", ""))
                                    }
                                    if ("type".equals(tokens.first().trim())) {
                                        val parameter =
                                            (colorKw.searchFirst { it is OptionalDSLParameter && "type".equals(it.key.text.text) } as OptionalDSLParameter)
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                    if ("to".equals(tokens.first().trim())) {
                                        val parameter =
                                            (colorKw.searchFirst { it is OptionalDSLParameter && "to".equals(it.key.text.text) } as OptionalDSLParameter)
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                        parameter.value.text.fill = Color.web(tokens.last().trim().replace("\"", ""))
                                    }
                                    if (attribute.trim().startsWith("data")) {
                                        tokens = attribute.trim().split(" ")
                                        val parameter =
                                            (colorKw.searchFirst { it is OptionalDSLParameter && "data".equals(it.key.text.text) } as OptionalDSLParameter)
                                        parameter.addButton.fire()
                                        parameter.key.text.text = tokens.first().trim()
                                        parameter.operator.text.text = " ${tokens[1].trim()} "
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                }
                                elementChild.children.forEach { elementChildChild ->
                                    when (elementChildChild.name) {
                                        "location" -> {
                                            val blocks = mutableListOf<Block>()
                                            elementChildChild.attributes.forEach { attribute ->
                                                val tokens = attribute.split("to")
                                                blocks.add(Block(tokens.first().trim().toInt(), tokens.last().trim().toInt()))
                                            }
                                            colorKw.setLocation(Location(blocks))
                                        }
                                    }
                                }
                            }
                            "line" -> {
                                val lineKw =
                                    themeKw.searchFirst { themeChild -> themeChild is LineKw && !themeChild.inFinalScript } as LineKw
                                lineKw.addButton.fire()
                                elementChild.attributes.forEach { attribute ->
                                    var tokens = attribute.split("=")
                                    if ("value".equals(tokens.first().trim())) {
                                        val parameter =
                                            (lineKw.searchFirst { it is DSLParameter && "value".equals(it.key.text.text) } as DSLParameter)
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                    if ("type".equals(tokens.first().trim())) {
                                        val parameter =
                                            (lineKw.searchFirst { it is OptionalDSLParameter && "type".equals(it.key.text.text) } as OptionalDSLParameter)
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                    if (attribute.startsWith("data")) {
                                        tokens = attribute.split(" ")
                                        val parameter =
                                            (lineKw.searchFirst { it is OptionalDSLParameter && "data".equals(it.key.text.text) } as OptionalDSLParameter)
                                        parameter.addButton.fire()
                                        parameter.key.text.text = tokens.first().trim()
                                        parameter.operator.text.text = " ${tokens[1].trim()} "
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                }
                                elementChild.children.forEach { elementChildChild ->
                                    when (elementChildChild.name) {
                                        "location" -> {
                                            val blocks = mutableListOf<Block>()
                                            elementChildChild.attributes.forEach { attribute ->
                                                val tokens = attribute.split("to")
                                                blocks.add(Block(tokens.first().trim().toInt(), tokens.last().trim().toInt()))
                                            }
                                            lineKw.setLocation(Location(blocks))
                                        }
                                    }
                                }
                            }
                            "show" -> {
                                val showKw =
                                    themeKw.searchFirst { themeChild -> themeChild is ShowKw && !themeChild.inFinalScript } as ShowKw
                                showKw.addButton.fire()
                                elementChild.attributes.forEach { attribute ->
                                    var tokens = attribute.split("=")
                                    if ("type".equals(tokens.first().trim())) {
                                        val parameter =
                                            (showKw.searchFirst { it is OptionalDSLParameter && "type".equals(it.key.text.text) } as OptionalDSLParameter)
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                    if (attribute.startsWith("data")) {
                                        tokens = attribute.split(" ")
                                        val parameter =
                                            (showKw.searchFirst { it is OptionalDSLParameter && "data".equals(it.key.text.text) } as OptionalDSLParameter)
                                        parameter.addButton.fire()
                                        parameter.key.text.text = tokens.first().trim()
                                        parameter.operator.text.text = " ${tokens[1].trim()} "
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                }
                                elementChild.children.forEach { elementChildChild ->
                                    when (elementChildChild.name) {
                                        "location" -> {
                                            val blocks = mutableListOf<Block>()
                                            elementChildChild.attributes.forEach { attribute ->
                                                val tokens = attribute.split("to")
                                                blocks.add(Block(tokens.first().trim().toInt(), tokens.last().trim().toInt()))
                                            }
                                            showKw.setLocation(Location(blocks))
                                        }
                                    }
                                }
                            }
                            "hide" -> {
                                val hideKw =
                                    themeKw.searchFirst { themeChild -> themeChild is HideKw && !themeChild.inFinalScript } as HideKw
                                hideKw.addButton.fire()
                                elementChild.attributes.forEach { attribute ->
                                    var tokens = attribute.split("=")
                                    if ("type".equals(tokens.first().trim())) {
                                        val parameter =
                                            (hideKw.searchFirst { it is OptionalDSLParameter && "type".equals(it.key.text.text) } as OptionalDSLParameter)
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                    if (attribute.startsWith("data")) {
                                        tokens = attribute.split(" ")
                                        val parameter =
                                            (hideKw.searchFirst { it is OptionalDSLParameter && "data".equals(it.key.text.text) } as OptionalDSLParameter)
                                        parameter.addButton.fire()
                                        parameter.key.text.text = tokens.first().trim()
                                        parameter.operator.text.text = " ${tokens[1].trim()} "
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                }
                                elementChild.children.forEach { elementChildChild ->
                                    when (elementChildChild.name) {
                                        "location" -> {
                                            val blocks = mutableListOf<Block>()
                                            elementChildChild.attributes.forEach { attribute ->
                                                val tokens = attribute.split("to")
                                                blocks.add(Block(tokens.first().trim().toInt(), tokens.last().trim().toInt()))
                                            }
                                            hideKw.setLocation(Location(blocks))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "layout" -> {
                    val layoutKw = themeAndLayoutScriptRoot.searchFirst { it is LayoutKw } as LayoutKw
                    layoutKw.addButton.fire()
                    element.children.forEach { elementChild ->
                        when (elementChild.name) {
                            "junction" -> {
                                val junctionLayoutKw =
                                    layoutKw.searchFirst { layoutChild -> layoutChild is JunctionLayoutKw && !layoutChild.inFinalScript } as JunctionLayoutKw
                                elementChild.attributes.forEach { attribute ->
                                    val tokens = attribute.split("=")
                                    if ("out_ids".equals(tokens.first().trim())) {
                                        val parameter =
                                            (junctionLayoutKw.searchFirst { it is OptionalDSLParameter && "out_ids".equals(it.key.text.text) } as OptionalDSLParameter)
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                    if ("type".equals(tokens.first().trim())) {
                                        val parameter =
                                            (junctionLayoutKw.searchFirst { it is OptionalDSLParameter && "type".equals(it.key.text.text) } as OptionalDSLParameter)
                                        parameter.addButton.fire()
                                        parameter.value.text.text = tokens.last().trim()
                                    }
                                }
                                elementChild.children.forEach { elementChildChild ->
                                    when (elementChildChild.name) {
                                        "location" -> {
                                            val blocks = mutableListOf<Block>()
                                            elementChildChild.attributes.forEach { attribute ->
                                                val tokens = attribute.split("to")
                                                blocks.add(Block(tokens.first().trim().toInt(), tokens.last().trim().toInt()))
                                            }
                                            junctionLayoutKw.setLocation(Location(blocks))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "data" -> {
                    val dataKw = themeAndLayoutScriptRoot.searchFirst { it is DataKw } as DataKw
                    dataKw.addButton.fire()
                    element.attributes.forEach { attribute ->
                        val tokens = attribute.split(" ")
                        val parameter =
                            (dataKw.searchFirst { it is OptionalDSLParameter && !it.inFinalScript } as OptionalDSLParameter)
                        parameter.addButton.fire()
                        parameter.key.text.text = tokens.first().trim()
                        parameter.operator.text.text = " ${tokens[1].trim()} "
                        parameter.value.text.text = tokens.last().trim()
                    }
                }
            }
        }

        return Triple(themeAndLayoutScriptRoot, secondaryStructureScriptRoot, issues)
    }

}