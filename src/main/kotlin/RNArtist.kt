package io.github.fjossinet.rnartist

import io.github.fjossinet.rnartist.core.RnartistConfig
import io.github.fjossinet.rnartist.core.RnartistConfig.getRnartistRelease
import io.github.fjossinet.rnartist.core.RnartistConfig.save
import io.github.fjossinet.rnartist.gui.*
import io.github.fjossinet.rnartist.io.github.fjossinet.rnartist.gui.*
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.SimpleFloatProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.embed.swing.SwingNode
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.CacheHint
import javafx.scene.Scene
import javafx.scene.chart.AreaChart
import javafx.scene.chart.NumberAxis
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.*
import org.kordamp.ikonli.javafx.FontIcon
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.io.*
import java.util.stream.Collectors

class RNArtist : Application() {

    val mediator: Mediator
    lateinit var stage: Stage
    private var scrollCounter = 0
    private val statusBar: FlowPane
    val swingNode = SwingNode()
    private val root: BorderPane
    val junctionSelectionKnob: JunctionKnob

    companion object {
        val RNArtistGUIColor = Color(51.0 / 255.0, 51.0 / 255.0, 51.0 / 255.0, 1.0)
    }


    fun getInstallDir(): String {
        return File(
            this::class.java.protectionDomain.codeSource.location
                .toURI()
        ).parentFile.parent
    }

    init {
        RnartistConfig.load()
        this.mediator = Mediator(this)

        this.root = BorderPane()

        val menuBar = MenuBar()
        menuBar.background = Background(BackgroundFill(RNArtistGUIColor, CornerRadii.EMPTY, Insets.EMPTY))
        menuBar.menus.add(Menu("File"))

        root.top = menuBar

        //Layouts Panel
        val layoutsPanel = VBox()
        layoutsPanel.alignment = Pos.TOP_CENTER
        layoutsPanel.minWidth = 180.0
        layoutsPanel.prefWidth = 180.0
        layoutsPanel.maxWidth = 180.0
        layoutsPanel.padding = Insets(20.0, 10.0, 20.0, 10.0)
        layoutsPanel.background = Background(BackgroundFill(RNArtistGUIColor, CornerRadii.EMPTY, Insets.EMPTY))

        var l = Label("2D Actions")
        l.textFill = Color.WHITE
        l.maxWidth = 180.0
        layoutsPanel.children.add(l)
        var s = Separator()
        s.maxWidth = 180.0
        layoutsPanel.children.add(s)
        layoutsPanel.children.add(Actions2DButtonsPanel(mediator))

        l = Label("3D Actions")
        l.textFill = Color.WHITE
        l.maxWidth = 180.0
        layoutsPanel.children.add(l)
        s = Separator()
        s.maxWidth = 180.0
        layoutsPanel.children.add(s)
        layoutsPanel.children.add(Actions3DButtonsPanel(mediator))

        l = Label("Details Level")
        l.textFill = Color.WHITE
        l.maxWidth = 180.0
        layoutsPanel.children.add(l)
        s = Separator()
        s.maxWidth = 180.0
        layoutsPanel.children.add(s)
        layoutsPanel.children.add(DetailsLevelButtonsPanel(mediator))

        l = Label("Junctions Layout")
        l.textFill = Color.WHITE
        l.maxWidth = 180.0
        layoutsPanel.children.add(l)
        s = Separator()
        s.maxWidth = 180.0
        layoutsPanel.children.add(s)
        this.junctionSelectionKnob = JunctionKnob("Selection", mediator)
        layoutsPanel.children.add(this.junctionSelectionKnob)

        var layoutsPanelScrollPane = ScrollPane(layoutsPanel)
        layoutsPanelScrollPane.isFitToWidth = true
        layoutsPanelScrollPane.isFitToHeight = true
        layoutsPanelScrollPane.vbarPolicy = ScrollPane.ScrollBarPolicy.ALWAYS

        //Colors Panel
        val colorsPanel = VBox()
        colorsPanel.alignment = Pos.TOP_LEFT
        colorsPanel.minWidth = 200.0
        colorsPanel.prefWidth = 200.0
        colorsPanel.maxWidth = 200.0
        colorsPanel.padding = Insets(20.0, 10.0, 20.0, 10.0)
        colorsPanel.background = Background(BackgroundFill(RNArtistGUIColor, CornerRadii.EMPTY, Insets.EMPTY))
        var colorsPanelScrollPane = ScrollPane(colorsPanel)
        colorsPanelScrollPane.isFitToWidth = true
        colorsPanelScrollPane.isFitToHeight = true
        colorsPanelScrollPane.vbarPolicy = ScrollPane.ScrollBarPolicy.ALWAYS

        l = Label("Structural Domains")
        l.textFill = Color.WHITE
        l.maxWidth = 200.0
        colorsPanel.children.add(l)
        s = Separator()
        s.maxWidth = 200.0
        colorsPanel.children.add(s)
        colorsPanel.children.add(StructuralDomainColorPicker(mediator))

        l = Label("Interactions")
        l.textFill = Color.WHITE
        l.maxWidth = 200.0
        colorsPanel.children.add(l)
        s = Separator()
        s.maxWidth = 200.0
        colorsPanel.children.add(s)
        colorsPanel.children.add(InteractionsColorPicker(mediator))

        l = Label("Residue Shapes")
        l.textFill = Color.WHITE
        l.maxWidth = 200.0
        GridPane.setHalignment(l, HPos.LEFT)
        colorsPanel.children.add(l)
        s = Separator()
        s.maxWidth = 200.0
        colorsPanel.children.add(s)
        colorsPanel.children.add(ResidueShapesColorPicker(mediator))

        l = Label("Residue Characters")
        l.textFill = Color.WHITE
        l.maxWidth = 200.0
        GridPane.setHalignment(l, HPos.LEFT)
        colorsPanel.children.add(l)
        s = Separator()
        s.maxWidth = 200.0
        colorsPanel.children.add(s)
        colorsPanel.children.add(ResidueCharactersColorPicker(mediator))

        val topToolBar2D = HBox()
        topToolBar2D.background =
            Background(BackgroundFill(RNArtistGUIColor, CornerRadii(0.0, 0.0, 10.0, 10.0, false), Insets.EMPTY))
        topToolBar2D.alignment = Pos.CENTER
        topToolBar2D.padding = Insets(10.0)
        topToolBar2D.spacing = 25.0

        l = Label("Font")
        l.textFill = Color.WHITE

        val fontNames = ComboBox(
            FXCollections.observableList(Font.getFamilies().stream().distinct().collect(Collectors.toList()))
        )
        fontNames.onAction = EventHandler {
            mediator.drawingDisplayed.get()?.let { drawingLoaded ->
                drawingLoaded.drawing.workingSession.fontName = fontNames.value
                mediator.canvas2D.repaint()
            }
        }
        fontNames.maxWidth = Double.MAX_VALUE

        var g = HBox()
        g.alignment = Pos.CENTER
        g.spacing = 5.0
        g.children.addAll(l, fontNames)
        topToolBar2D.children.add(g)

        val deltaXRes = Spinner<Int>(Int.MIN_VALUE, Int.MAX_VALUE, 0)
        deltaXRes.prefWidth = 50.0
        deltaXRes.valueProperty().addListener { observable, oldValue, newValue ->
            mediator.drawingDisplayed.get()?.let { drawingLoaded ->
                drawingLoaded.drawing.workingSession.deltafontx = deltaXRes.value
                mediator.canvas2D.repaint()
            }
        }
        l = Label("x")
        l.textFill = Color.WHITE
        g = HBox()
        g.alignment = Pos.CENTER
        g.spacing = 5.0
        g.children.addAll(l, deltaXRes)
        topToolBar2D.children.add(g)

        val deltaYRes = Spinner<Int>(Int.MIN_VALUE, Int.MAX_VALUE, 0)
        deltaYRes.prefWidth = 50.0
        deltaYRes.valueProperty().addListener { observable, oldValue, newValue ->
            mediator.drawingDisplayed.get()?.let { drawingLoaded ->
                drawingLoaded.drawing.workingSession.deltafonty = deltaYRes.value
                mediator.canvas2D.repaint()
            }
        }
        l = Label("y")
        l.textFill = Color.WHITE
        g = HBox()
        g.alignment = Pos.CENTER
        g.spacing = 5.0
        g.children.addAll(l, deltaYRes)
        topToolBar2D.children.add(g)

        val deltaFontSize = Spinner<Int>(Int.MIN_VALUE, Int.MAX_VALUE, 0)
        deltaFontSize.prefWidth = 50.0
        deltaFontSize.valueProperty().addListener { observable, oldValue, newValue ->
            mediator.drawingDisplayed.get()?.let { drawingLoaded ->
                drawingLoaded.drawing.workingSession.deltafontsize = deltaFontSize.value
                mediator.canvas2D.repaint()
            }
        }
        l = Label("size")
        l.textFill = Color.WHITE
        g = HBox()
        g.alignment = Pos.CENTER
        g.spacing = 5.0
        g.children.addAll(l, deltaFontSize)
        topToolBar2D.children.add(g)

        //++++++ Canvas2D
        swingNode.onMouseMoved = EventHandler { mouseEvent: MouseEvent? ->
            mediator.drawingDisplayed.get()?.drawing?.let {
                it.quickDraw = false //a trick if after the scroll event the quickdraw is still true
            }
        }
        swingNode.onMouseClicked = EventHandler { mouseEvent: MouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                mediator.drawingDisplayed.get()?.let mouseClicked@{ drawingLoaded ->
                    val at = AffineTransform()
                    at.translate(
                        drawingLoaded.drawing.workingSession.viewX,
                        drawingLoaded.drawing.workingSession.viewY
                    )
                    at.scale(
                        drawingLoaded.drawing.workingSession.zoomLevel,
                        drawingLoaded.drawing.workingSession.zoomLevel
                    )
                    for (h in drawingLoaded.drawing.workingSession.helicesDrawn) {
                        var shape = h.selectionFrame
                        if (shape != null && at.createTransformedShape(shape)
                                .contains(mouseEvent.x, mouseEvent.y)
                        ) {
                            for (r in h.residues) {
                                shape = r.selectionFrame
                                if (shape != null && at.createTransformedShape(shape)
                                        .contains(mouseEvent.x, mouseEvent.y)
                                ) {
                                    if (!mediator.canvas2D.isSelected(r) && !mediator.canvas2D.isSelected(r.parent) && !mediator.canvas2D.isSelected(
                                            r.parent!!.parent
                                        )
                                    ) {
                                        mediator.canvas2D.addToSelection(r)
                                        return@mouseClicked
                                    } else if (!mediator.canvas2D.isSelected(r.parent) && !mediator.canvas2D.isSelected(
                                            r.parent!!.parent
                                        )
                                    ) {
                                        mediator.canvas2D.removeFromSelection(r)
                                        mediator.canvas2D.addToSelection(r.parent)
                                        return@mouseClicked
                                    } else if (!mediator.canvas2D.isSelected(r.parent!!.parent)) {
                                        mediator.canvas2D.removeFromSelection(r.parent)
                                        mediator.canvas2D.addToSelection(r.parent!!.parent)
                                        return@mouseClicked
                                    }
                                }
                            }
                            for (interaction in h.secondaryInteractions) {
                                shape = interaction.selectionFrame
                                if (shape != null && at.createTransformedShape(shape)
                                        .contains(mouseEvent.x, mouseEvent.y)
                                ) {
                                    if (!mediator.canvas2D.isSelected(interaction) && !mediator.canvas2D.isSelected(
                                            interaction.parent
                                        )
                                    ) {
                                        mediator.canvas2D.addToSelection(interaction)
                                        return@mouseClicked
                                    } else if (!mediator.canvas2D.isSelected(interaction.parent)) {
                                        mediator.canvas2D.removeFromSelection(interaction)
                                        mediator.canvas2D.addToSelection(interaction.parent)
                                        return@mouseClicked
                                    }
                                }
                            }
                            if (!mediator.canvas2D.isSelected(h)) {
                                mediator.canvas2D.addToSelection(h)
                                return@mouseClicked
                            } else {
                                var p = h.parent
                                while (p != null && mediator.canvas2D.isSelected(p)) {
                                    p = p.parent
                                }
                                if (p == null) {
                                    mediator.canvas2D.addToSelection(h)
                                } else {
                                    mediator.canvas2D.addToSelection(p)
                                }
                                return@mouseClicked
                            }
                        }
                    }
                    for (j in drawingLoaded.drawing.workingSession.junctionsDrawn) {
                        var shape = j.selectionFrame
                        if (shape != null && at.createTransformedShape(shape)
                                .contains(mouseEvent.x, mouseEvent.y)
                        ) {
                            for (r in j.residues) {
                                shape = r.selectionFrame
                                if (shape != null && at.createTransformedShape(shape)
                                        .contains(mouseEvent.x, mouseEvent.y)
                                ) {
                                    if (!mediator.canvas2D.isSelected(r) && !mediator.canvas2D.isSelected(r.parent)) {
                                        mediator.canvas2D.addToSelection(r)
                                        return@mouseClicked
                                    } else if (!mediator.canvas2D.isSelected(r.parent)) {
                                        mediator.canvas2D.removeFromSelection(r)
                                        mediator.canvas2D.addToSelection(r.parent)
                                        return@mouseClicked
                                    }
                                }
                            }
                            if (!mediator.canvas2D.isSelected(j)) {
                                mediator.canvas2D.addToSelection(j)
                                return@mouseClicked
                            } else {
                                var p = j.parent
                                while (p != null && mediator.canvas2D.isSelected(p)) {
                                    p = p.parent
                                }
                                if (p == null) {
                                    mediator.canvas2D.addToSelection(j)
                                } else {
                                    mediator.canvas2D.addToSelection(p)
                                }
                                return@mouseClicked
                            }
                        }
                    }
                    for (ss in drawingLoaded.drawing.workingSession.singleStrandsDrawn) {
                        var shape = ss.selectionFrame
                        if (shape != null && at.createTransformedShape(shape)
                                .contains(mouseEvent.x, mouseEvent.y)
                        ) {
                            for (r in ss.residues) {
                                shape = r.selectionFrame
                                if (shape != null && at.createTransformedShape(shape)
                                        .contains(mouseEvent.x, mouseEvent.y)
                                ) {
                                    if (!mediator.canvas2D.isSelected(r) && !mediator.canvas2D.isSelected(r.parent)) {
                                        mediator.canvas2D.addToSelection(r)
                                        return@mouseClicked
                                    } else if (!mediator.canvas2D.isSelected(r.parent)) {
                                        mediator.canvas2D.removeFromSelection(r)
                                        mediator.canvas2D.addToSelection(r.parent)
                                        return@mouseClicked
                                    }
                                }
                            }
                        }
                    }
                    if (mouseEvent.clickCount == 2) {
                        //no selection
                        mediator.canvas2D.clearSelection()
                    }
                }
            }
        }
        swingNode.onMouseDragged = EventHandler { mouseEvent: MouseEvent ->
            if (mouseEvent.button == MouseButton.SECONDARY) {
                mediator.drawingDisplayed.get()?.let { drawingLoaded ->
                    drawingLoaded.drawing.quickDraw = true
                    val transX: Double = mouseEvent.x - mediator.canvas2D.translateX
                    val transY: Double = mouseEvent.y - mediator.canvas2D.translateY
                    drawingLoaded.drawing.workingSession.moveView(transX, transY)
                    mediator.canvas2D.translateX = mouseEvent.x
                    mediator.canvas2D.translateY = mouseEvent.y
                    mediator.canvas2D.repaint()
                }
            }
        }
        swingNode.onMouseReleased = EventHandler { mouseEvent: MouseEvent ->
            if (mouseEvent.button == MouseButton.SECONDARY) {
                mediator.drawingDisplayed.get()?.let { drawingLoaded ->
                    drawingLoaded.drawing.quickDraw = false
                    mediator.canvas2D.translateX = 0.0
                    mediator.canvas2D.translateY = 0.0
                    mediator.canvas2D.repaint()
                }
            }
        }
        swingNode.onMousePressed = EventHandler { mouseEvent: MouseEvent ->
            if (mouseEvent.button == MouseButton.SECONDARY) {
                mediator.drawingDisplayed.get()?.let { drawingLoaded ->
                    mediator.canvas2D.translateX = mouseEvent.x
                    mediator.canvas2D.translateY = mouseEvent.y
                }
            }
        }
        swingNode.onScroll = EventHandler { scrollEvent: ScrollEvent ->
            mediator.drawingDisplayed.get()?.let { drawingLoaded ->
                drawingLoaded.drawing.quickDraw = true
                scrollCounter++
                val th = Thread {
                    try {
                        Thread.sleep(100)
                        if (scrollCounter == 1) {
                            drawingLoaded.drawing.quickDraw = false
                            mediator.canvas2D.repaint()
                        }
                        scrollCounter--
                    } catch (e1: Exception) {
                        e1.printStackTrace()
                    }
                }
                th.isDaemon = true
                th.start()
                val realMouse =
                    Point2D.Double(
                        (scrollEvent.x - drawingLoaded.drawing.workingSession.viewX) / drawingLoaded.drawing.workingSession.zoomLevel,
                        (scrollEvent.y - drawingLoaded.drawing.workingSession.viewY) / drawingLoaded.drawing.workingSession.zoomLevel
                    )
                val notches = scrollEvent.deltaY
                if (notches < 0) drawingLoaded.drawing.workingSession.setZoom(1.25)
                if (notches > 0) drawingLoaded.drawing.workingSession.setZoom(1.0 / 1.25)
                val newRealMouse =
                    Point2D.Double(
                        (scrollEvent.x - drawingLoaded.drawing.workingSession.viewX) / drawingLoaded.drawing.workingSession.zoomLevel,
                        (scrollEvent.y - drawingLoaded.drawing.workingSession.viewY) / drawingLoaded.drawing.workingSession.zoomLevel
                    )
                drawingLoaded.drawing.workingSession.moveView(
                    (newRealMouse.getX() - realMouse.getX()) * drawingLoaded.drawing.workingSession.zoomLevel,
                    (newRealMouse.getY() - realMouse.getY()) * drawingLoaded.drawing.workingSession.zoomLevel
                )
                mediator.canvas2D.repaint()
            }
        }
        createSwingContent(swingNode)

        val verticalSplitPane = SplitPane()
        verticalSplitPane.orientation = Orientation.VERTICAL

        root.center = verticalSplitPane

        val panel3D = GridPane()
        val rConstraints = RowConstraints()
        rConstraints.vgrow = Priority.ALWAYS
        val _rConstraints = RowConstraints()
        _rConstraints.vgrow = Priority.NEVER
        panel3D.rowConstraints.addAll(rConstraints, _rConstraints)
        //horizontalSplitPane.items.add(panel3D)
        val cConstraints = ColumnConstraints()
        cConstraints.hgrow = Priority.ALWAYS
        panel3D.columnConstraints.add(cConstraints)
        panel3D.add(Canvas3D(), 0, 0)

        val panel2D = GridPane()
        val rConstraints1 = RowConstraints()
        rConstraints1.vgrow = Priority.NEVER
        val rConstraints2 = RowConstraints()
        rConstraints2.vgrow = Priority.ALWAYS
        val rConstraints3 = RowConstraints()
        rConstraints3.vgrow = Priority.NEVER

        val cConstraints1 = ColumnConstraints()
        cConstraints1.hgrow = Priority.NEVER
        val cConstraints2 = ColumnConstraints()
        cConstraints2.hgrow = Priority.ALWAYS
        val cConstraints3 = ColumnConstraints()
        cConstraints3.hgrow = Priority.NEVER

        panel2D.rowConstraints.addAll(rConstraints1, rConstraints2, rConstraints3)
        panel2D.columnConstraints.addAll(cConstraints1, cConstraints2, cConstraints3)

        panel2D.add(layoutsPanelScrollPane, 0, 0, 1, 3)
        panel2D.add(topToolBar2D, 1, 0)
        panel2D.add(swingNode, 1, 1)
        panel2D.add(colorsPanelScrollPane, 2, 0, 1, 3)
        colorsPanelScrollPane.isFitToWidth = true

        val lowerHorizontalSplitPane = SplitPane()
        lowerHorizontalSplitPane.orientation = Orientation.HORIZONTAL
        val tabPane = TabPane()
        tabPane.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        class Residue(val position: Int, vararg values: Float) {
            val values = mutableListOf<ObservableValue<Float>>()

            init {
                this.values.addAll(values.map { SimpleFloatProperty(it) as ObservableValue<Float> })
            }
        }

        val table = TableView<Residue>()
        val positions = TableColumn<Residue, Int>("Position")
        positions.setCellValueFactory(PropertyValueFactory("Position"))
        val dataset1 = TableColumn<Residue, Float>("DataSet #1")
        dataset1.setCellValueFactory { r -> r.value.values.get(0) }
        val dataset2 = TableColumn<Residue, Float>("DataSet #2")
        dataset2.setCellValueFactory { r -> r.value.values.get(1) }
        val dataset3 = TableColumn<Residue, Float>("DataSet #3")
        dataset3.setCellValueFactory { r -> r.value.values.get(2) }
        val dataset4 = TableColumn<Residue, Float>("DataSet #4")
        dataset4.setCellValueFactory { r -> r.value.values.get(3) }
        table.columns.addAll(positions, dataset1, dataset2, dataset3, dataset4)

        (1..50).forEach {
            table.items.add(
                Residue(
                    it,
                    kotlin.random.Random.nextFloat(),
                    kotlin.random.Random.nextFloat(),
                    kotlin.random.Random.nextFloat(),
                    kotlin.random.Random.nextFloat()
                )
            )
        }

        lowerHorizontalSplitPane.items.add(table)
        SplitPane.setResizableWithParent(table, false)
        val xAxis = NumberAxis()
        xAxis.label = "Position"

        val yAxis = NumberAxis()
        yAxis.label = "Values"
        lowerHorizontalSplitPane.items.add(AreaChart(xAxis, yAxis))
        lowerHorizontalSplitPane.setDividerPositions(0.3)
        tabPane.tabs.add(Tab("Saved Projects", mediator.projectsPanel))
        tabPane.tabs.add(Tab("Working Session", mediator.drawingsLoadedPanel))
        tabPane.tabs.add(Tab("Data", lowerHorizontalSplitPane))
        tabPane.tabs.add(Tab("Script", this.mediator.scriptEditor))

        verticalSplitPane.items.add(panel2D)
        verticalSplitPane.items.add(tabPane)
        verticalSplitPane.setDividerPositions(0.7)

        //### Status Bar
        this.statusBar = FlowPane()
        root.bottom = this.statusBar
        statusBar.setAlignment(Pos.CENTER_RIGHT)
        statusBar.setPadding(Insets(5.0, 10.0, 5.0, 10.0))
        statusBar.setHgap(20.0)

        val release = Label(getRnartistRelease())
        statusBar.getChildren().add(release)

        val shutdown = Button(null, FontIcon("fas-power-off:15"))
        shutdown.tooltip = Tooltip("Exit RNArtist")
        shutdown.onAction = EventHandler { actionEvent: ActionEvent ->
            val alert =
                Alert(Alert.AlertType.CONFIRMATION)
            alert.initOwner(stage)
            alert.initModality(Modality.WINDOW_MODAL)
            alert.title = "Confirm Exit"
            alert.headerText = null
            alert.contentText = "Are you sure to exit RNArtist?"
            val alerttStage = alert.dialogPane.scene.window as Stage
            alerttStage.isAlwaysOnTop = true
            alerttStage.toFront()
            val result = alert.showAndWait()
            if (result.get() == ButtonType.OK) {
                try {
                    Platform.exit()
                    save()
                    System.exit(0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                actionEvent.consume()
            }
        }
        statusBar.getChildren().add(shutdown)

        val windowsBar = FlowPane()
        windowsBar.alignment = Pos.CENTER_LEFT
        windowsBar.padding = Insets(5.0, 10.0, 5.0, 10.0)
        windowsBar.hgap = 10.0

        val bar = GridPane()
        var cc = ColumnConstraints()
        cc.hgrow = Priority.ALWAYS
        bar.columnConstraints.addAll(cc, ColumnConstraints())

        bar.add(windowsBar, 0, 0)
        GridPane.setFillWidth(windowsBar, true)
        GridPane.setHalignment(windowsBar, HPos.LEFT)
        bar.add(this.statusBar, 1, 0)
        GridPane.setHalignment(this.statusBar, HPos.RIGHT)

    }

    override fun start(stage: Stage) {
        this.stage = stage
        this.stage.setOnCloseRequest(EventHandler { windowEvent: WindowEvent ->
            val alert =
                Alert(Alert.AlertType.CONFIRMATION)
            alert.initOwner(stage)
            alert.initModality(Modality.WINDOW_MODAL)
            alert.title = "Confirm Exit"
            alert.headerText = null
            alert.contentText = "Are you sure to exit RNArtist?"
            val alerttStage = alert.dialogPane.scene.window as Stage
            alerttStage.isAlwaysOnTop = true
            alerttStage.toFront()
            val result = alert.showAndWait()
            if (result.get() == ButtonType.OK) {
                try {
                    Platform.exit()
                    save()
                    System.exit(0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                windowEvent.consume()
            }
        })

        this.stage.widthProperty().addListener { obs: ObservableValue<out Number?>?, oldVal: Number?, newVal: Number? ->
            //mediator.canvas2D.updateKnobs()
            mediator.canvas2D.repaint()
        }

        this.stage.heightProperty()
            .addListener { obs: ObservableValue<out Number?>?, oldVal: Number?, newVal: Number? ->
                //mediator.canvas2D.updateKnobs()
                mediator.canvas2D.repaint()
            }

        this.stage.fullScreenProperty()
            .addListener { obs: ObservableValue<out Boolean?>?, oldVal: Boolean?, newVal: Boolean? ->
                //mediator.canvas2D.updateKnobs()
                mediator.canvas2D.repaint()
            }

        this.stage.maximizedProperty()
            .addListener { obs: ObservableValue<out Boolean?>?, oldVal: Boolean?, newVal: Boolean? ->
                //mediator.canvas2D.updateKnobs()
                mediator.canvas2D.repaint()
            }

        val screen = Screen.getPrimary()

        val scene = Scene(this.root, screen.bounds.width, screen.bounds.height)
        stage.scene = scene
        stage.title = "RNArtist"

        val screenSize = Screen.getPrimary().bounds
        scene.window.width = screenSize.width
        scene.window.height = screenSize.height
        scene.window.x = 0.0
        scene.window.y = 0.0
        scene.stylesheets.add("io/github/fjossinet/rnartist/gui/css/main.css")
        SplashWindow(this.mediator)
    }

    private fun createSwingContent(swingNode: SwingNode) {
        Platform.runLater {
            val canvas = Canvas2D(mediator)
            swingNode.content = canvas
            swingNode.isCache = true
            swingNode.cacheHint = CacheHint.SPEED
        }
    }

    fun main() {
        launch();
    }


}
