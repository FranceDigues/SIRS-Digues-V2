/**
 * This file is part of SIRS-Digues 2.
 *
 * Copyright (C) 2016, FRANCE-DIGUES,
 *
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.plugin.carto;

import fr.sirs.FXEditMode;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.BookMark;
import fr.sirs.core.model.Role;
import fr.sirs.theme.ui.AbstractFXElementPane;
import fr.sirs.ui.Growl;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.apache.sis.util.collection.Cache;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.javafx.util.FXUtilities;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.map.MapLayer;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXBookMarks extends GridPane {

    private static final Image ICON_SHOWONMAP = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_GLOBE, 16, FontAwesomeIcons.DEFAULT_COLOR),null);

    @FXML private TableView<BookMark> uiTable;
    @FXML private BorderPane uiConfig;
    @FXML private Button uiDelete;
    @FXML private Button uiAdd;
    private final AbstractSIRSRepository<BookMark> repo = Injector.getSession().getRepositoryForClass(BookMark.class);

    // cache des panneaux de favoris
    private final Cache<BookMark, AbstractFXElementPane> cache = new Cache<>();

    // référence vers le dernier favori nouvellement créé
    private BookMark newlyCreated;

    public FXBookMarks(){
        SIRS.loadFXML(this, FXBookMarks.class);

        uiDelete.setGraphic(new ImageView(SIRS.ICON_TRASH_WHITE));
        uiAdd.setGraphic(new ImageView(SIRS.ICON_ADD_WHITE));
        uiDelete.disableProperty().bind(uiTable.getSelectionModel().selectedItemProperty().isNull());
        uiTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        uiTable.getSelectionModel().selectedItemProperty().addListener(this::selectionChanged);
        uiTable.setPlaceholder(new Label(""));

        final TableColumn<BookMark,String> col = new TableColumn<>();
        col.setEditable(false);
        col.setCellValueFactory((TableColumn.CellDataFeatures<BookMark, String> param) -> param.getValue().titreProperty());

        uiTable.getColumns().add(col);
        uiTable.getColumns().add(new ViewColumn());
        FXUtilities.hideTableHeader(uiTable);

        final Session session = Injector.getSession();
        final Role role = session.getRole();
        if(!Role.ADMIN.equals(role)){
            uiDelete.setVisible(false);
            uiAdd.setVisible(false);
        }

        uiTable.setItems(SIRS.observableList(repo.getAll()));
    }

    @FXML
    void deleteBookmark(ActionEvent event) {
        final BookMark bookmark = uiTable.getSelectionModel().getSelectedItem();
        if(bookmark==null) return;

        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                String.format("Confirmer la suppression du favori %s ?", bookmark.getDesignation()),
                ButtonType.YES, ButtonType.NO);
        alert.setResizable(true);
        final ButtonType res = alert.showAndWait().get();
        if (res == ButtonType.YES) {
            repo.remove(bookmark);
            uiConfig.setCenter(null);
        }
    }

    @FXML
    void addBookMark(ActionEvent event) {
        final BookMark bookmark = repo.create();
        bookmark.setDescription("");
        bookmark.setDesignation("");
        bookmark.setTitre("pas de nom");
        repo.add(bookmark);

        // on référence le favori nouvellement créé pour l'ouvrir éventuellement en édition.
        this.newlyCreated = bookmark;

        this.uiTable.getSelectionModel().select(bookmark);
    }

    private void selectionChanged(ObservableValue<? extends BookMark> observable, BookMark oldValue, BookMark newValue){
        uiConfig.setCenter(null);
        if(newValue!=null){

            final AbstractFXElementPane editPane = cache.computeIfAbsent(newValue,
                    new Function<BookMark, AbstractFXElementPane>() {
                @Override
                public AbstractFXElementPane apply(BookMark bm) {
                    final AbstractFXElementPane pane = SIRS.generateEditionPane(bm, b -> b == newlyCreated);

                    final Session session = Injector.getSession();
                    final Role role = session.getRole();
                    final FXEditMode searchEditMode = searchEditMode(pane);

                    // seul l'admin semble habilité à ajouter/supprimer des favoris et à les éditer
                    if(!Role.ADMIN.equals(role)){
                        if(searchEditMode!=null){
                            searchEditMode.setVisible(false);
                            searchEditMode.setManaged(false);
                        }
                    } else if (bm == newlyCreated) {
                        searchEditMode.editionState().set(true);
                    }
                    return pane;
                }
            });

            uiConfig.setCenter(editPane);
        }
    }

    private static FXEditMode searchEditMode(Node node){
        if(node instanceof FXEditMode) return (FXEditMode) node;

        if(node instanceof Parent){
            for(Node child : ((Parent)node).getChildrenUnmodifiable()){
                final FXEditMode cdt = searchEditMode(child);
                if(cdt!=null) return cdt;
            }
        }

        return null;
    }


    private class ViewColumn extends TableColumn<BookMark, BookMark>{

        public ViewColumn() {
            setSortable(false);
            setResizable(false);
            setPrefWidth(44);
            setMinWidth(44);
            setMaxWidth(44);
            setCellValueFactory((TableColumn.CellDataFeatures<BookMark, BookMark> param) -> new SimpleObjectProperty(param.getValue()));
            setCellFactory((TableColumn<BookMark, BookMark> param) -> new ViewCell());
            setEditable(true);
        }
    }

    private class ViewCell extends TableCell<BookMark, BookMark>{

        private final Button button = new Button(null, new ImageView(ICON_SHOWONMAP));

        public ViewCell() {
            setGraphic(button);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            button.visibleProperty().bind(itemProperty().isNotNull());
            button.setOnAction(this::showOnMap);
        }

        private void showOnMap(ActionEvent event) {
            BookMark bookmark = getItem();

            if (bookmark == null)
                return;

            button.setGraphic(new ProgressIndicator());
            Task<List<MapLayer>> loader = new TaskManager.MockTask<>(() -> PluginCarto.listLayers(bookmark));
            loader.setOnFailed(evt -> Platform.runLater(() -> {
                final Throwable ex = evt.getSource().getException();
                final String errorMsg;
                if (ex instanceof MalformedURLException)
                    errorMsg = "L'URL renseignée dans le favori n'est pas valide. Cause :\n" + ex.getLocalizedMessage();
                else if (ex instanceof IOException)
                    errorMsg = "La connexion au service a échouée. Cause :\n" + ex.getLocalizedMessage();
                else
                    errorMsg = ex.getLocalizedMessage();

                new Growl(Growl.Type.ERROR, errorMsg).showAndFade();
            }));

            loader.setOnSucceeded(evt -> Platform.runLater(() -> {
                try {
                    PluginCarto.showOnMap(bookmark.getTitre(), (List) evt.getSource().getValue());
                } catch (Exception ex) {
                    final String errorMsg;
                    if (ex instanceof MalformedURLException)
                        errorMsg = "L'URL renseignée dans le favoris n'est pas valide. Cause :\n" + ex.getLocalizedMessage();
                    else if (ex instanceof IOException)
                        errorMsg = "La connexion au service a échouée. Cause :\n" + ex.getLocalizedMessage();
                    else
                        errorMsg = ex.getLocalizedMessage();

                    new Growl(Growl.Type.ERROR, errorMsg).showAndFade();
                }
            }));

            loader.setOnCancelled(evt -> Platform.runLater(() -> {
                    new Growl(Growl.Type.ERROR, "Le chargement des couches a été interrompu").showAndFade();
            }));

            final EventHandler handler = evt -> SIRS.fxRun(false, () -> button.setGraphic(new ImageView(ICON_SHOWONMAP)));
            loader.addEventHandler(WorkerStateEvent.WORKER_STATE_CANCELLED, handler);
            loader.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, handler);
            loader.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, handler);

            TaskManager.INSTANCE.submit(loader);
        }
    }
}
