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
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.model.BookMark;
import fr.sirs.core.model.Role;
import fr.sirs.plugin.carto.PluginCarto;
import fr.sirs.ui.Growl;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.StringConverter;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.javafx.util.FXUtilities;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.security.BasicAuthenticationSecurity;
import org.geotoolkit.security.ClientSecurity;

/**
 *
 * @author Olivier Nouguier (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class FXBookMarkPane extends AbstractFXElementPane<BookMark> {
    private static final Image ICON_SHOWONMAP = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_GLOBE, 16, FontAwesomeIcons.DEFAULT_COLOR), null);

    /**
     * Convert service type into string. DESIGNED FOR VALUE SETTING ONLY. Another
     * converter is used for display.
     */
    private static final StringConverter<PluginCarto.SERVICE> SERVICE_CONVERTER = new StringConverter<PluginCarto.SERVICE>() {
        @Override
        public String toString(PluginCarto.SERVICE object) {
            if (object == null)
                return null;
            return object.name();
        }

        @Override
        public PluginCarto.SERVICE fromString(String string) {
            if (string == null || string.isEmpty())
                return null;
            else
                return PluginCarto.SERVICE.valueOf(string);
        }
    };

    // Propriétés de BookMark
    @FXML private PasswordField ui_motDePasse;
    @FXML private TextField ui_identifiant;
    @FXML private TableView<MapLayer> ui_table;
    @FXML private TextField ui_parametres;
    @FXML private TextArea ui_description;
    @FXML private TextField ui_titre;
    @FXML private ChoiceBox<PluginCarto.SERVICE> ui_service;
    @FXML private Label ui_identifiantLbl;
    @FXML private Label ui_motDePasseLbl;
    @FXML private Label ui_authLbl;
    @FXML private Button ui_refresh;
    @FXML private ProgressIndicator ui_progress;

    final BooleanBinding refreshDisabled;

    public FXBookMarkPane(final BookMark bookMark) {
        this();
        this.elementProperty().set(bookMark);
    }

    /**
     * Constructor. Initialize part of the UI which will not require update when
     * element edited change.
     */
    protected FXBookMarkPane() {
        SIRS.loadFXML(this, BookMark.class);
        elementProperty().addListener(this::initFields);

        /*
         * Disabling rules.
         */
        ui_description.disableProperty().bind(disableFieldsProperty());
        ui_titre.disableProperty().bind(disableFieldsProperty());
        ui_parametres.disableProperty().bind(disableFieldsProperty());
        ui_identifiant.disableProperty().bind(disableFieldsProperty());
        ui_motDePasse.disableProperty().bind(disableFieldsProperty());
        ui_service.disableProperty().bind(disableFieldsProperty());
        ui_service.setItems(FXCollections.observableList(Arrays.asList(PluginCarto.SERVICE.values())));
        ui_service.setConverter(new StringConverter<PluginCarto.SERVICE>() {
            @Override
            public String toString(PluginCarto.SERVICE object) {
                if (object == null)
                    return null;
                return object.title;
            }

            @Override
            public PluginCarto.SERVICE fromString(String string) {
                return PluginCarto.SERVICE.findValue(string);
            }
        });

        ui_table.getColumns().add(new NameColumn());
        ui_table.getColumns().add(new ViewColumn());
        ui_table.setPlaceholder(new Label(""));
        FXUtilities.hideTableHeader(ui_table);

        refreshDisabled = ui_service.valueProperty().isNull()
                .or(ui_parametres.textProperty().isEmpty())
                .or(ui_progress.visibleProperty());

        ui_refresh.disableProperty().bind(refreshDisabled);
    }

    public BookMark getElement() {
        return elementProperty.get();
    }

    /**
     * Initialize fields at element setting.
     */
    protected void initFields(ObservableValue<? extends BookMark > observableElement, BookMark oldElement, BookMark newElement) {
        // Unbind fields bound to previous element.
        if (oldElement != null) {
        // Propriétés de BookMark
            ui_description.textProperty().unbindBidirectional(oldElement.descriptionProperty());
            ui_titre.textProperty().unbindBidirectional(oldElement.titreProperty());
            ui_parametres.textProperty().unbindBidirectional(oldElement.parametresProperty());
            ui_identifiant.textProperty().unbindBidirectional(oldElement.identifiantProperty());
            ui_motDePasse.textProperty().unbindBidirectional(oldElement.motDePasseProperty());
            Bindings.unbindBidirectional(oldElement.typeServiceProperty(), ui_service.valueProperty());
        }

        /*
         * Bind control properties to Element ones.
         */
        if (newElement != null) {
            // Propriétés de BookMark
            // * description
            ui_description.textProperty().bindBidirectional(newElement.descriptionProperty());
            // * titre
            ui_titre.textProperty().bindBidirectional(newElement.titreProperty());
            // * parametres
            ui_parametres.textProperty().bindBidirectional(newElement.parametresProperty());
            // * identifiant
            ui_identifiant.textProperty().bindBidirectional(newElement.identifiantProperty());
            // * motDePasse
            ui_motDePasse.textProperty().bindBidirectional(newElement.motDePasseProperty());

            ui_service.setValue(
                SERVICE_CONVERTER.fromString(newElement.typeServiceProperty().get())
            );
            Bindings.bindBidirectional(newElement.typeServiceProperty(), ui_service.valueProperty(), SERVICE_CONVERTER);
        }

        final Role role = Injector.getBean(Session.class).getRole();
        if(!Role.ADMIN.equals(role)){
            ui_identifiant.setVisible(false);
            ui_motDePasse.setVisible(false);
            ui_identifiantLbl.setVisible(false);
            ui_motDePasseLbl.setVisible(false);
            ui_authLbl.setVisible(false);
        }
    }

    @Override
    public void preSave() {}


    @FXML
    void refreshList(ActionEvent event) {
        final BookMark bookmark = getElement();
        Task<List<MapLayer>> loader = new TaskManager.MockTask<>("Chargement des couches WMS", () -> PluginCarto.listLayers(bookmark));
        ui_progress.visibleProperty().bind(loader.runningProperty());

        loader.setOnFailed(evt -> Platform.runLater(() -> {
            final String errorMsg;
            final Throwable ex = evt.getSource().getException();
            if (ex instanceof MalformedURLException)
                errorMsg = "L'URL renseignée n'est pas valide.";
            else if (ex instanceof IOException)
                errorMsg = "La connection au service a échouée.";
            else
                errorMsg = ex.getLocalizedMessage();

            SIRS.LOGGER.log(Level.WARNING, "Bookmark connection failed", ex);
            ui_table.setPlaceholder(new Label(errorMsg));
            GeotkFX.newExceptionDialog(errorMsg, ex).show();
        }));

        loader.setOnSucceeded(evt -> Platform.runLater(() -> {
            final List<MapLayer> layers = (List<MapLayer>) evt.getSource().getValue();
            if (layers.isEmpty())
                ui_table.setPlaceholder(new Label("Aucune donnée trouvée pour le service parametré."));

            ui_table.setItems(FXCollections.observableList(layers));
        }));

        loader.setOnCancelled(evt -> Platform.runLater(() -> {
            ui_table.setPlaceholder(new Label("Connexion annulée"));
            new Growl(Growl.Type.WARNING, "La connexion au service a été annulée.").showAndFade();
        }));

        TaskManager.INSTANCE.submit(loader);
    }

    public static Optional<ClientSecurity> parseSecurityParameters(String login, String password) {
        if (login == null || (login = login.trim()).isEmpty())
            return Optional.empty();

        if (password == null)
            password = "";
        return Optional.of(new BasicAuthenticationSecurity(login, password));
    }

    private static class NameColumn extends TableColumn<MapLayer, String>{

        public NameColumn() {
            setCellValueFactory((CellDataFeatures<MapLayer, String> param) -> new SimpleStringProperty(param.getValue().getName()));
            setEditable(false);
            setResizable(true);
            setMaxWidth(Double.MAX_VALUE);
        }
    }

    private class ViewColumn extends TableColumn<MapLayer, MapLayer>{

        public ViewColumn() {
            setSortable(false);
            setResizable(false);
            setPrefWidth(44);
            setMinWidth(44);
            setMaxWidth(44);
            setCellValueFactory((CellDataFeatures<MapLayer, MapLayer> param) -> new SimpleObjectProperty(param.getValue()));
            setCellFactory((TableColumn<MapLayer, MapLayer> param) -> new ViewCell());
            setEditable(true);
            setMaxWidth(Double.MAX_VALUE);
        }
    }

    private class ViewCell extends TableCell<MapLayer, MapLayer>{

        private final Button button = new Button(null, new ImageView(ICON_SHOWONMAP));

        public ViewCell() {
            setGraphic(button);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            button.visibleProperty().bind(itemProperty().isNotNull());
            button.setOnAction(this::showOnMap);
        }

        private void showOnMap(ActionEvent event) {
            final MapLayer layer = getItem();
            if(layer==null)
                return;
            final BookMark bookmark = FXBookMarkPane.this.getElement();

            PluginCarto.showOnMap(bookmark.getTitre(), Collections.singleton(layer));
        }
    }
}
