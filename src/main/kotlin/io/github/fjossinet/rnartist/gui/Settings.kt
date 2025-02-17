package io.github.fjossinet.rnartist.gui

import io.github.fjossinet.rnartist.Mediator
import io.github.fjossinet.rnartist.core.RnartistConfig
import io.github.fjossinet.rnartist.core.RnartistConfig.chimeraHost
import io.github.fjossinet.rnartist.core.RnartistConfig.chimeraPort
import io.github.fjossinet.rnartist.core.RnartistConfig.exportSVGWithBrowserCompatibility
import io.github.fjossinet.rnartist.io.ChimeraXDriver
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.DirectoryChooser
import org.kordamp.ikonli.javafx.FontIcon

class Settings(val mediator: Mediator):VBox() {

    val chimeraXConnectionStatus:Label

    init {
        this.padding = Insets(10.0, 10.0, 10.0, 10.0)
        this.spacing = 5.0

        val projectsFolderPane = GridPane()
        projectsFolderPane.padding = Insets(10.0, 5.0, 15.0, 5.0)
        projectsFolderPane.hgap = 5.0
        projectsFolderPane.vgap = 5.0
        this.children.add(projectsFolderPane)

        val projectsFolderLabel = Label("Projects Folder")
        projectsFolderLabel.style = "-fx-font-weight: bold"
        projectsFolderPane.children.add(projectsFolderLabel)
        GridPane.setConstraints(projectsFolderLabel, 0, 0, 2, 1)
        var s = Separator(Orientation.HORIZONTAL)
        projectsFolderPane.children.add(s)
        GridPane.setConstraints(s, 0, 1, 2, 1)
        val folderField = TextField(RnartistConfig.projectsFolder)
        folderField.isEditable = false
        GridPane.setHgrow(folderField, Priority.ALWAYS)
        projectsFolderPane.children.add(folderField)
        GridPane.setConstraints(folderField, 0, 2)
        val chooseFolder = Button("Choose")
        projectsFolderPane.children.add(chooseFolder)
        GridPane.setConstraints(chooseFolder, 1, 2)
        GridPane.setHgrow(chooseFolder, Priority.NEVER)

        chooseFolder.onAction = EventHandler { e ->
            val dir = DirectoryChooser().showDialog(mediator.rnartist.stage)
            dir?.let {
                folderField.text = dir.absolutePath
                RnartistConfig.projectsFolder = dir.absolutePath
                mediator.projectsPanel.clearProjects()
                mediator.projectsPanel.loadProjects()
            }
        }

        val chimeraPane = GridPane()
        chimeraPane.padding = Insets(10.0, 5.0, 15.0, 5.0)
        chimeraPane.hgap = 5.0
        chimeraPane.vgap = 5.0
        this.children.add(chimeraPane)

        val chimeraLabel = Label("UCSF ChimeraX")
        chimeraLabel.style = "-fx-font-weight: bold"
        chimeraPane.children.add(chimeraLabel)
        GridPane.setConstraints(chimeraLabel, 0, 0, 6, 1)
        s = Separator(Orientation.HORIZONTAL)
        chimeraPane.children.add(s)
        GridPane.setConstraints(s, 0, 1, 6, 1)
        val hostLabel = Label("Host")
        chimeraPane.children.add(hostLabel)
        GridPane.setConstraints(hostLabel, 0, 2)
        val hostValue = TextField(chimeraHost)
        chimeraPane.children.add(hostValue)
        GridPane.setConstraints(hostValue, 1, 2)
        val portLabel = Label("Port")
        chimeraPane.children.add(portLabel)
        GridPane.setConstraints(portLabel, 2, 2)
        val portValue = TextField("$chimeraPort")
        chimeraPane.children.add(portValue)
        GridPane.setConstraints(portValue, 3, 2)
        val connect2ChimeraRest = Button("Connect")
        chimeraPane.children.add(connect2ChimeraRest)
        GridPane.setConstraints(connect2ChimeraRest, 4, 2)
        connect2ChimeraRest.onMouseClicked = EventHandler {
            try {
                chimeraHost = hostValue.text.trim { it <= ' ' }
                chimeraPort = portValue.text.trim { it <= ' ' }.toInt()
                val sessionFile = mediator.chimeraDriver.sessionFile
                mediator.chimeraDriver = ChimeraXDriver(mediator)
                mediator.chimeraDriver.sessionFile = sessionFile
                mediator.chimeraDriver.connectToRestServer()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        this.chimeraXConnectionStatus = Label(null, FontIcon("fas-exclamation-circle:15"))
        (this.chimeraXConnectionStatus.graphic as FontIcon).fill = Color.RED
        chimeraPane.children.add(this.chimeraXConnectionStatus)
        GridPane.setConstraints(this.chimeraXConnectionStatus, 5, 2)

        val optionsPane = GridPane()
        optionsPane.padding = Insets(10.0, 5.0, 15.0, 5.0)
        optionsPane.hgap = 5.0
        optionsPane.vgap = 5.0
        this.children.add(optionsPane)

        var l = Label("SVG Export")
        l.style = "-fx-font-weight: bold"
        optionsPane.children.add(l)
        GridPane.setConstraints(l, 0, 0)
        s = Separator(Orientation.HORIZONTAL)
        optionsPane.children.add(s)
        GridPane.setConstraints(s, 0, 1, 5, 1)
        l = Label("Apply Browser Compatibility")
        optionsPane.children.add(l)
        GridPane.setConstraints(l, 0, 2)
        val svgBrowserFix = CheckBox()
        svgBrowserFix.isSelected = exportSVGWithBrowserCompatibility()
        svgBrowserFix.onAction = EventHandler { actionEvent: ActionEvent? ->
            exportSVGWithBrowserCompatibility(svgBrowserFix.isSelected)
        }
        optionsPane.children.add(svgBrowserFix)
        GridPane.setConstraints(svgBrowserFix, 1, 2)
    }

}