package fr.unistra.rnartist.gui;

import fr.unistra.rnartist.model.Project;
import fr.unistra.rnartist.model.RnartistConfig;
import fr.unistra.rnartist.model.SecondaryStructure;
import fr.unistra.rnartist.model.TertiaryStructure;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Callback;
import kotlin.Pair;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.dizitart.no2.Document;
import org.dizitart.no2.NitriteId;

import java.io.File;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.util.Optional;

import static fr.unistra.rnartist.model.io.UtilsKt.createTemporaryFile;

public class ProjectManager {

    private Mediator mediator;
    private ObservableList<Project> projects;
    private GridView<Project> gridview;
    private Stage stage;

    public ProjectManager(Mediator mediator) {
        this.mediator = mediator;
        this.stage = new Stage();
        stage.setTitle("RNArtist Projects");
        stage.setOnCloseRequest(windowEvent -> {
            if (mediator.getSecondaryStructureDrawing() != null) { //the user has decided to cancel its idea to open an other project
                mediator.getRnartist().getStage().show();
                mediator.getRnartist().getStage().toFront();
            }
            else { //nothing displayed? So the user want to exit RNArtist -> confirmation
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.initOwner(this.stage);
                alert.initModality(Modality.WINDOW_MODAL);
                alert.setTitle("Confirm Exit");
                alert.setHeaderText(null);
                alert.setContentText("Are you sure to exit RNArtist?");

                Stage alerttStage = (Stage) alert.getDialogPane().getScene().getWindow();
                alerttStage.setAlwaysOnTop(true);
                alerttStage.toFront();

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    try {
                        Platform.exit();
                        System.exit(0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    windowEvent.consume();
                }
            }
        });

        this.projects = FXCollections.observableArrayList();
        this.projects.add(new NewProject());
        for (Document project: this.mediator.getEmbeddedDB().getProjects().find()) {
            this.projects.add(new Project(project.getId(), (String)project.get("name")));
        }
        this.createScene(stage);
        Rectangle2D screenSize = Screen.getPrimary().getBounds();
        this.stage.setWidth(1360);
        this.stage.setHeight(2*screenSize.getHeight()/3);
        final double newX = (screenSize.getWidth() - this.stage.getWidth()) / 2;
        final double newY = (screenSize.getHeight() - this.stage.getHeight()) / 2;
        this.stage.setX(newX);
        this.stage.setY(newY);
    }

    public Stage getStage() {
        return stage;
    }

    public void addProject(NitriteId id, String name) {
        this.projects.add(new Project(id, name));
    }

    private void createScene(Stage stage) {
        gridview = new GridView<Project>(projects);
        gridview.setPadding( new javafx.geometry.Insets(20, 20, 20, 20));
        gridview.setHorizontalCellSpacing(20);
        gridview.setVerticalCellSpacing(20);
        gridview.setCellWidth(400);
        gridview.setCellHeight(200);
        gridview.setStyle("-fx-background-color: lightgray;");

        gridview.setCellFactory(new Callback<GridView<Project>, GridCell<Project>>() {
            public GridCell<Project> call(GridView<Project> gridView) {
                return new ProjectCell();
            }
        });
        Scene scene = new Scene(gridview);
        stage.setScene(scene);
    }

    class ProjectCell extends GridCell<Project> {

        private ImageView icon = new ImageView();
        private Label name = new Label();
        private VBox content;
        private HBox border;

        public ProjectCell() {
            this.content = new VBox();
            this.content.setSpacing(5);
            this.content.setAlignment(Pos.CENTER);
            border = new HBox();
            border.getChildren().add(icon);
            border.setStyle("-fx-border-color: lightgray; -fx-border-width: 4px;");
            this.content.getChildren().add(border);
            this.content.getChildren().add(name);
            name.setTextFill(Color.BLACK);
            name.setStyle("-fx-font-weight: bold");
            this.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (ProjectCell.this.getItem().name.equals("New Project")) {
                        stage.hide();
                        mediator.getAllStructures().clear();
                        mediator.getRnartist().getStage().show();
                        mediator.getToolbox().getStage().show();
                    } else {
                        stage.hide();
                        fr.unistra.rnartist.model.Project project = mediator.getEmbeddedDB().getProject(ProjectCell.this.getItem().id);
                        //mediator.getAllStructures().add(project.getSecondaryStructure()); //TODO load the secondary structure drawinf
                        //TODO load the theme and the graphicsContext
                        if (project.getTertiaryStructure() != null && mediator.getChimeraDriver() != null) {
                            mediator.setTertiaryStructure(project.getTertiaryStructure());
                            try {
                                File tmpF = createTemporaryFile("ts.pdb");
                                fr.unistra.rnartist.model.io.ParsersKt.writePDB(mediator.getTertiaryStructure(), true, new FileWriter(tmpF));
                                mediator.getChimeraDriver().loadTertiaryStructure(tmpF);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        mediator.getRnartist().getStage().show();
                        mediator.getToolbox().getStage().show();
                    }

                }
            });

            this.setOnMouseEntered(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    border.setStyle("-fx-border-color: darkgray; -fx-border-width: 4px;");
                }
            });

            this.setOnMouseExited(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    border.setStyle("-fx-border-color: lightgray; -fx-border-width: 4px;");
                }
            });
        }

        @Override
        protected void updateItem(Project project, boolean empty) {
            super.updateItem(project, empty);
            setGraphic(null);
            setText(null);
            if (!empty && project != null) {
                name.setText(project.name);
                icon.setImage(project.getImage());
                setGraphic(this.content);
            }
        }
    }

    class Project {

        protected NitriteId id;
        protected String name;

        Project() {

        }

        Project(NitriteId id, String name) {
            this.id = id;
            this.name = name;
        }

        Image getImage() {
            try {
                return new Image(new File(new File(new File(mediator.getEmbeddedDB().getRootDir(),"images"),"user"),id.toString()+".png").toURI().toURL().toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    class NewProject extends Project {
        public NewProject() {
            this.name = "New Project";
        }

        Image getImage() {
            try {
                return new Image(new File(new File(new File(mediator.getEmbeddedDB().getRootDir(),"images"),"user"),"New Project.png").toURI().toURL().toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
