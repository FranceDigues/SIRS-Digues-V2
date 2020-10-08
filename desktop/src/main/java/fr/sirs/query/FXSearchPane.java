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
package fr.sirs.query;

import fr.sirs.CorePlugin;
import fr.sirs.Injector;
import fr.sirs.Printable;
import fr.sirs.SIRS;
import fr.sirs.Session;
import static fr.sirs.core.SirsCore.MODEL_PACKAGE;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.ReferenceType;
import fr.sirs.core.model.Role;
import fr.sirs.core.model.SQLQueries;
import fr.sirs.core.model.SQLQuery;
import fr.sirs.index.ElasticSearchEngine;
import fr.sirs.index.ElementHit;
import fr.sirs.theme.ui.ObjectTable;
import fr.sirs.ui.Growl;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.sis.storage.DataStoreException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStoreUtilities;
import org.geotoolkit.data.csv.CSVFeatureStore;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.db.FilterToSQL;
import org.geotoolkit.db.JDBCFeatureStore;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.javafx.layer.FXFeatureTable;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.jdbc.html.HtmlBuilder;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.style.MutableStyle;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.util.GenericName;

/**
 * TODO : make printable.
 * @author Johann Sorel (Geomatys)
 */
public class FXSearchPane extends SplitPane {

    public static final Image ICON_SAVE    = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_SAVE_ALIAS,22,Color.WHITE),null);
    public static final Image ICON_OPEN    = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_LIST_UL,22,Color.WHITE),null);
    public static final Image ICON_OPEN_DEFAULT    = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_COGS,22,Color.WHITE),null);
    public static final Image ICON_EXPORT  = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_UPLOAD,22,Color.WHITE),null);
    public static final Image ICON_MODEL   = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_SHARE_ALT_SQUARE,22,Color.WHITE),null);
    public static final Image ICON_CARTO   = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_COMPASS,22,Color.WHITE),null);
    public static final Image ICON_REFRESH = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_REFRESH,22,Color.WHITE),null);

    private static Path htmlIndex;

    @FXML private Button uiSave;
    @FXML private Button uiOpen;
    @FXML private Button uiOpenDefault;
    @FXML private Button uiQueryManagement;
    @FXML private Button uiExportModel;
    @FXML private Button uiRefreshModel;
    @FXML private MenuButton uiViewModel;
    @FXML private Button uiCarto;
    @FXML private Button uiCancel;
    @FXML private Label uiNbResults;

    // 1- Recherche simple
    @FXML private ToggleButton uiToggleSimple;
    @FXML private ToggleGroup simpleRadio;
    @FXML private GridPane uiSimplePane;
    // a) Recherche texte
    @FXML private RadioButton uiRadioPlainText;
    @FXML private GridPane uiPlainTextPane;
    @FXML private TextField uiElasticKeywords;

    // b) Recherche par désignation
    @FXML private RadioButton uiRadioDesignation;
    @FXML private GridPane uiDesignationPane;
    @FXML private ComboBox<Class<? extends Element>> uiDesignationClass;
    @FXML private TextField uiDesignation;

    // 2- Recherche SQL
    @FXML private ToggleButton uiToggleSQL;
    @FXML private GridPane uiSQLPane;
    @FXML private GridPane uiSQLModelOptions;
    @FXML private GridPane uiSQLQueryOptions;
    @FXML private GridPane uiAdminOptions;
    @FXML private ComboBox<String> uiTableChoice;
    @FXML private BorderPane uiFilterPane;
    @FXML private TextArea uiSQLText;
    //nom de la requete si affichage sur carte
    private String sqlLibelle;
    private FXSQLFilterEditor uiFilterEditor;

    private final Session session;

    private JDBCFeatureStore h2Store;

    @FXML private BorderPane resultPane;


    /**
     * Définit s'il est nécessaire de lancer le processus d'export RDBMS pour pouvoir faire
     * des recherches SQL, ou s'il a été déjà lancé.
     */
    private static final ObjectProperty<Boolean> needsSQLExportProperty = new SimpleObjectProperty<>(true);

    public FXSearchPane() {
        SIRS.loadFXML(this);
        session = Injector.getSession();

        uiFilterEditor = new FXSQLFilterEditor();
        uiFilterEditor.filterProperty.addListener(this::setSQLText);

        uiFilterPane.setCenter(uiFilterEditor);

        //affichage des panneaux coord/borne
        final ToggleGroup group = new ToggleGroup();
        uiToggleSimple.setToggleGroup(group);
        uiToggleSQL.setToggleGroup(group);
        uiToggleSQL.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                if (needsSQLExportProperty.get()) {
                    showPopupForRDBMSExport(group);
                } else {
                    connectToH2Store();
                }
            }
        });
        group.selectedToggleProperty().addListener((ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) -> {
            if (newValue == null) {
                group.selectToggle(oldValue);
            }
            uiNbResults.setText("");
        });

        final List<Class<? extends Element>> modelClasses = Session.getConcreteSubTypes(Element.class);
        final ObservableList<Class<? extends Element>> modelObs = FXCollections.observableArrayList(modelClasses);
        modelObs.removeIf((Class<? extends Element> t) -> ReferenceType.class.isAssignableFrom(t));

        SIRS.initCombo(uiDesignationClass, modelObs, null);

        uiToggleSimple.setSelected(true);

        uiTableChoice.getSelectionModel().selectedItemProperty().addListener(
            (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if(newValue!=null){
                try {
                    FeatureType type = h2Store.getFeatureType(newValue);
                    uiFilterEditor.setType(type);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            else {
                uiFilterEditor.setType(null);
            }
        });

        uiSave.setGraphic(new ImageView(ICON_SAVE));
        uiOpen.setGraphic(new ImageView(ICON_OPEN));
        uiOpenDefault.setGraphic(new ImageView(ICON_OPEN_DEFAULT));
        uiExportModel.setGraphic(new ImageView(ICON_EXPORT));
        uiRefreshModel.setGraphic(new ImageView(ICON_REFRESH));
        uiViewModel.setGraphic(new ImageView(ICON_MODEL));
        uiViewModel.setTextFill(javafx.scene.paint.Color.WHITE);

        // VISIBILITY RULES
        uiSimplePane.visibleProperty().bind(uiToggleSimple.selectedProperty());
        uiSimplePane.managedProperty().bind(uiSimplePane.visibleProperty());
        simpleRadio.selectedToggleProperty().addListener(
            (ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) -> {
            resultPane.setCenter(null);
            if (newValue == null) simpleRadio.selectToggle(oldValue);
        });

        uiSQLPane.visibleProperty().bind(uiToggleSQL.selectedProperty().and(needsSQLExportProperty.isEqualTo(false)));
        uiSQLPane.managedProperty().bind(uiSQLPane.visibleProperty());

        uiSQLModelOptions.visibleProperty().bind(uiToggleSQL.selectedProperty().and(needsSQLExportProperty.isEqualTo(false)));
        uiSQLModelOptions.managedProperty().bind(uiSQLModelOptions.visibleProperty());

        uiSQLQueryOptions.visibleProperty().bind(uiToggleSQL.selectedProperty().and(needsSQLExportProperty.isEqualTo(false)));
        uiSQLQueryOptions.managedProperty().bind(uiSQLQueryOptions.visibleProperty());

        uiCancel.visibleProperty().bind(new BooleanBinding() {

            {
                super.bind(
                        uiToggleSQL.selectedProperty(),
                        uiToggleSimple.selectedProperty(),
                        uiRadioDesignation.selectedProperty(),
                        uiRadioPlainText.selectedProperty());
            }

            @Override
            protected boolean computeValue() {
                return (uiToggleSimple.isSelected() && (uiRadioDesignation.isSelected() || uiRadioPlainText.isSelected())) || uiToggleSQL.isSelected();
            }
        });

        uiPlainTextPane.visibleProperty().bind(new BooleanBinding() {
            {super.bind(uiRadioPlainText.selectedProperty(), uiToggleSimple.selectedProperty());}

            @Override
            protected boolean computeValue() {
                return uiRadioPlainText.isSelected() && uiToggleSimple.isSelected();
            }
        });
        uiPlainTextPane.managedProperty().bind(uiPlainTextPane.visibleProperty());
        uiDesignationPane.visibleProperty().bind(new BooleanBinding() {

            {super.bind(uiRadioDesignation.selectedProperty(), uiToggleSimple.selectedProperty());}

            @Override
            protected boolean computeValue() {
                return uiRadioDesignation.isSelected() && uiToggleSimple.isSelected();
            }
        });
        uiDesignationPane.managedProperty().bind(uiDesignationPane.visibleProperty());

        final SimpleBooleanProperty isAdmin = new ReadOnlyBooleanWrapper(Role.ADMIN.equals(Injector.getSession().getRole()));
        uiAdminOptions.visibleProperty().bind(uiToggleSQL.selectedProperty().and(isAdmin).and(needsSQLExportProperty.isEqualTo(false)));
        uiAdminOptions.managedProperty().bind(uiAdminOptions.visibleProperty());

        uiCarto.setDisable(true);
        uiCarto.visibleProperty().bind(uiToggleSQL.selectedProperty().and(needsSQLExportProperty.isEqualTo(false)));
        uiCarto.managedProperty().bind(uiCarto.visibleProperty());

        // TOOLTIPS
        uiExportModel.setTooltip(new Tooltip("Voir la structure de la base SQL."));
        uiExportModel.setTooltip(new Tooltip("Exporter l'intégralité de la base SQL."));
        uiOpen.setTooltip(new Tooltip("Choisir une requête SQL parmi celles stockées dans le système."));
        uiOpenDefault.setTooltip(new Tooltip("Choisir une requête SQL préprogrammée."));
        uiSave.setTooltip(new Tooltip("Enregistrer la requête actuelle dans le système local."));
        uiQueryManagement.setTooltip(new Tooltip("Ajouter / supprimer des requêtes en base de données."));

        uiCarto.setTooltip(new Tooltip("Afficher le résultat de la requête sur la carte."));

        // Action on admin button
        uiQueryManagement.setOnAction((ActionEvent e)-> FXAdminQueryPane.showAndWait());

    }

    @FXML
    private void cancel(){

            if(uiToggleSimple.isSelected()){
                if(uiRadioDesignation.isSelected()){
                    uiDesignation.setText("");
                    uiDesignationClass.getSelectionModel().select(null);
                } else if (uiRadioPlainText.isSelected()){
                    uiElasticKeywords.setText("");
                }
            } else if(uiToggleSQL.isSelected()) {
                uiSQLText.setText("");
                uiTableChoice.getSelectionModel().select(null);
            }
    }

    @FXML
    private void openImageModel(ActionEvent event) throws IOException {
        session.getFrame().openModel();
    }

    @FXML
    private void openHTMLModel(ActionEvent event) {
        TaskManager.INSTANCE.submit("Analyse du modèle", () -> {
            if (htmlIndex == null) {
                final HtmlBuilder htmlBuilder = new HtmlBuilder();
                final Path tmpDir = Files.createTempDirectory("htmlSirs");
                htmlIndex = htmlBuilder.setSource(h2Store.getDataSource().getConnection()).setSchema("PUBLIC").setOutput(tmpDir).build();
            }

            return htmlIndex.toUri().toURL();
        }).setOnSucceeded(evt -> SIRS.browseURL((URL)evt.getSource().getValue(), "Modèle SQL"));
    }

    @FXML
    private void refreshModel(ActionEvent event) {
        final Stage stage = new Stage();
        stage.getIcons().add(SIRS.ICON);
        stage.initModality(Modality.NONE);
        stage.setAlwaysOnTop(false);
        stage.setTitle("Rechargement de la base SQL");
        final BorderPane bp = new BorderPane();
        stage.setScene(new Scene(bp));
        stage.show();
        exportToRDBMS(stage, bp);
    }

    private ReadOnlyObjectProperty<Worker.State> connectToH2Store() {
        //h2 connection
        Task<ObservableList> h2Names = TaskManager.INSTANCE.submit("Connexion à la base de données", () -> {
            h2Store = session.getH2Helper().getStore().get();

            final Set<GenericName> names = h2Store.getNames();
            final ObservableList observableNames = FXCollections.observableArrayList();
            for (GenericName n : names) observableNames.add(n.tip().toString());
            Collections.sort(observableNames);

            SIRS.LOGGER.fine("RDBMS CONNEXION FINISHED");
            return observableNames;
        });

        h2Names.setOnSucceeded((WorkerStateEvent e) -> Platform.runLater(() -> uiTableChoice.setItems(h2Names.getValue())));
        return h2Names.stateProperty();
    }

    @FXML
    private void saveSQLQuery(ActionEvent event){
        final Dialog dialog = new Dialog();
        final DialogPane pane = new DialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        final SQLQuery query = new SQLQuery();
        query.sqlProperty().set(uiSQLText.getText());
        pane.setContent(new FXQueryPane(query));

        dialog.setDialogPane(pane);
        dialog.setTitle("Information sur la requête");
        dialog.getDialogPane().setHeader(null);
        final Optional name = dialog.showAndWait();
        if (name.isPresent() && ButtonType.OK.equals(name.get())) {
            try{
                final List<SQLQuery> queries = SQLQueries.getLocalQueries();
                queries.add(query);
                sqlLibelle = query.getLibelle();
                SQLQueries.saveQueriesLocally(queries);
            }catch(IOException ex){
                SIRS.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
        }
    }

    /**
     * Ouverture du panneau des requêtes préprogrammées.
     *
     * Les requêtes sont triées par ordre alphabétique des libellés.
     *
     * @param event
     */
    @FXML
    private void openDefaultSQLQuery(ActionEvent event){
        final List<SQLQuery> queries;
        try {
            queries = SQLQueries.preprogrammedQueries();
        } catch (IOException ex) {
            SIRS.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            GeotkFX.newExceptionDialog("Une erreur s'est produite pendant le chargement des requêtes préprogrammées.", ex).show();
            return;
        }
        showQueryTable(queries, false);
    }

    /**
     * Ouverture du panneau des requêtes utilisateur enregistrées soit localement soit en base document.
     *
     * Les requêtes sont triées par ordre alphabétique des libellés.
     *
     * @param event
     */
    @FXML
    private void openSQLQuery(ActionEvent event){
        final List<SQLQuery> queries;
        try {
            queries = SQLQueries.getLocalQueries();
            queries.addAll(SQLQueries.dbQueries());

            // il faut refaire un tri général des requêtes
            queries.sort(SQLQueries.QUERY_COMPARATOR);
        } catch (IOException ex) {
            SIRS.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            GeotkFX.newExceptionDialog("Une erreur s'est produite pendant la création de la liste des requêtes.", ex).show();
            return;
        }
        showQueryTable(queries, true);
    }

    private void showQueryTable(final List<SQLQuery> queries, final boolean editable){
        Optional<SQLQuery> choice = chooseSQLQuery(queries, editable);
        if (choice.isPresent()) {
            final SQLQuery selected = choice.get();
            sqlLibelle = selected.getLibelle();
            uiSQLText.setText(selected.getSql());
        }
    }

    public static Optional<SQLQuery> chooseSQLQuery(final List<SQLQuery> queries) {
        return chooseSQLQuery(queries, false);
    }

    public static Optional<SQLQuery> chooseSQLQuery(final List<SQLQuery> queries, final boolean editable) {
        if (queries.isEmpty()) {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Aucune requête disponible.", ButtonType.OK);
            alert.setResizable(true);
            alert.showAndWait();
        } else {
            final Dialog dia = new Dialog();
            final FXQueryTable table = new FXQueryTable(queries);
            table.modifiableProperty().set(editable);

            final DialogPane pane = new DialogPane();
            pane.setPrefSize(700, 400);
            final ButtonType bt = new ButtonType("Ouvrir");
            pane.getButtonTypes().addAll(bt, ButtonType.CLOSE);
            pane.setContent(table);
            dia.setDialogPane(pane);
            dia.setTitle("Liste des requêtes");
            dia.setResizable(true);

            final Optional res = dia.showAndWait();
            if (editable) {
                // Save only in editable mode, it may contain changes
                table.save();
            }
            if (res.isPresent() && bt.equals(res.get())) {
                return Optional.ofNullable(table.getSelection());
            }
        }
        return Optional.empty();
    }

    @FXML
    private void exportModel(ActionEvent event) {
        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Dossier d'export");
        final File file = chooser.showDialog(null);

        if (file != null) {
            Task export = session.getH2Helper().dumbSchema(file.toPath());
            export.setOnSucceeded((success) -> Platform.runLater(() -> new Growl(Growl.Type.INFO, "L'export dans le fichier "+file.getAbsolutePath()+" est terminé.").showAndFade()));
            export.setOnCancelled((cancelled) -> Platform.runLater(() -> new Growl(Growl.Type.INFO, "L'export dans le fichier "+file.getAbsolutePath()+" a été annulé.").showAndFade()));
            export.setOnFailed((failed) -> Platform.runLater(() -> GeotkFX.newExceptionDialog("L'export dans le fichier "+file.getAbsolutePath()+" a échoué.", export.getException()).show()));
        }
    }

    @FXML
    private void exportMap(ActionEvent event) throws DataStoreException {
        final String query = getCurrentSQLQuery();
        if(query==null) return;

        final Task<FeatureMapLayer> t = searchSQLLayer(query);
        t.setOnSucceeded(evt -> Platform.runLater(() -> {
            final FeatureMapLayer layer = t.getValue();
            if (layer == null || layer.getCollection().size() < 1)
                return;

            final String name = sqlLibelle == null ? query : sqlLibelle;
            final TextInputDialog d = new TextInputDialog(name);
            d.setTitle("Nom de la couche");
            d.setHeaderText("Titre de la couche affichant les résultats de la requête SQL");
            d.setContentText("");
            d.setGraphic(null);
            final Optional<String> opt = d.showAndWait();
            if (!opt.isPresent())
                return;

            layer.setName(opt.get());
            final MapContext context = session.getMapContext();

            MapItem querygroup = null;
            for (MapItem item : context.items()) {
                if ("Requêtes".equalsIgnoreCase(item.getName())) {
                    querygroup = item;
                }
            }
            if (querygroup == null) {
                querygroup = MapBuilder.createItem();
                querygroup.setName("Requêtes");
                context.items().add(querygroup);
            }

            querygroup.items().add(layer);
            session.getFrame().getMapTab().show();
        }));

        t.setOnFailed(evt -> displayError(t.getException()));
        TaskManager.INSTANCE.submit(t);
    }

    private void setSQLText(final ObservableValue observable, Object oldValue, Object newValue) {
        try {
            uiSQLText.setText(buildSQLQueryFromFilter());
        } catch (DataStoreException ex) {
            SIRS.LOGGER.log(Level.WARNING, "Impossible de construire une requête SQL depuis le panneau de filtres.", ex);
            GeotkFX.newExceptionDialog("Impossible de construire une requête SQL depuis le panneau de filtres.", ex).show();
        }
    }

    private String buildSQLQueryFromFilter() throws DataStoreException {
        final String tableName = uiTableChoice.getValue();
        if (tableName == null) {
            return null;
        }
        final Filter filter = uiFilterEditor.toFilter();

        final FeatureType ft = h2Store.getFeatureType(tableName);
        final FilterToSQL filterToSQL = new SirsFilterToSQL(ft);
        final StringBuilder sb = new StringBuilder();
        filter.accept(filterToSQL, sb);
        final String condition = sb.toString();

        return "SELECT * FROM \"" + tableName + "\" WHERE " + condition;
    }

    private String getCurrentSQLQuery() throws DataStoreException {
        return uiSQLText.getText().trim();
    }

    /**
     * Affichage d'une popup de confirmation signalant que le processus d'export RDBMS peut être long.
     */
    private void showPopupForRDBMSExport(final ToggleGroup group) {
        final Stage stage = new Stage();
        stage.getIcons().add(SIRS.ICON);
        stage.initModality(Modality.NONE);
        stage.setAlwaysOnTop(false);
        stage.setTitle("Initialisation de la recherche");

        final Button okButton = new Button("Valider");
        final Button cancelButton = new Button("Annuler");
        final HBox hboxBtn = new HBox(cancelButton, okButton);
        hboxBtn.setSpacing(10);
        hboxBtn.setAlignment(Pos.CENTER_RIGHT);

        final GridPane gridPane = new GridPane();
        gridPane.setVgap(5);
        gridPane.setPadding(new Insets(10, 10, 20, 10));

        gridPane.add(new Label("La recherche nécessite une étape de préparation qui peut"), 0, 0);
        gridPane.add(new Label("prendre de quelques secondes à plusieurs minutes sur des"), 0, 1);
        gridPane.add(new Label("configurations modestes. Voulez vous continuer ?"), 0, 2);
        gridPane.add(hboxBtn, 0, 6);

        cancelButton.setOnAction(e -> {
            final ObservableList<Toggle> toggles = group.getToggles();
            for (final Toggle toggle : toggles) {
                if (!toggle.isSelected()) {
                    // On sélectionne l'autre onglet, vu que l'on souhaite annuler le chargement de la base SQL.
                    group.selectToggle(toggle);
                    break;
                }
            }
            stage.close();
        });
        okButton.setOnAction(e -> {
            gridPane.getChildren().remove(hboxBtn);
            final BorderPane bp = new BorderPane();
            gridPane.add(bp, 0, 6);
            exportToRDBMS(stage, bp);
        });

        final Scene scene = new Scene(gridPane);
        stage.setScene(scene);
        stage.show();
    }

    private void exportToRDBMS(final Stage progressStage, final BorderPane container) {
        setDisable(true);

        final ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        final Label progressLabel = new Label();
        final Button cancelBtn = new Button("Annuler");
        final HBox bottom = new HBox(cancelBtn);
        bottom.setAlignment(Pos.BOTTOM_RIGHT);
        final VBox vboxProgress = new VBox(5, progressLabel, progressBar, cancelBtn);
        vboxProgress.setAlignment(Pos.CENTER);

        container.setCenter(vboxProgress);
        progressStage.sizeToScene();

        final Task task = session.getH2Helper().export();
        cancelBtn.setOnAction(evt -> {
            task.cancel();
            progressStage.close();
        });

        progressLabel.textProperty().bind(task.messageProperty());
        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(
                e -> connectToH2Store().addListener(
                        (obs, oldState, newState) -> {
                            Platform.runLater(() -> {
                                if (Worker.State.SUCCEEDED.equals(newState)) {
                                    setDisable(false);
                                    needsSQLExportProperty.setValue(false);
                                    progressStage.close();
                                } else if (Worker.State.FAILED.equals(newState)) {
                                    setDisable(false);
                                    progressLabel.textProperty().unbind();
                                    progressLabel.setText("Une erreur est survenue pendant le chargement de la base.");
                                } else if (Worker.State.CANCELLED.equals(newState)) {
                                    setDisable(false);
                                    progressLabel.textProperty().unbind();
                                    progressLabel.setText("Le chargement de la base de donnée a été interrompu.");
                                }
                            });
                        }
                )
        );

        task.setOnCancelled(e -> Platform.runLater(() -> {
            setDisable(false);
            progressLabel.textProperty().unbind();
            progressLabel.setText("Le chargement de la base de donnée a été interrompu.");
            cancelBtn.setText("fermer");
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            setDisable(false);
            progressLabel.textProperty().unbind();
            progressLabel.setText("Une erreur est survenue pendant le chargement de la base.");
        }));
    }

    @FXML
    private void search(ActionEvent event) {

        try{
            if(uiToggleSimple.isSelected()){
                if(uiRadioDesignation.isSelected()){
                    searchDesignation();
                } else if (uiRadioPlainText.isSelected()){
                    searchText();
                }

            } else if(uiToggleSQL.isSelected()) {
                if(h2Store==null) {
                    final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Veuillez attendre que la connexion à la base de donnée SQL soit établie.", ButtonType.OK);
                    alert.setResizable(true);
                    alert.show();
                } else {
                    final String query = getCurrentSQLQuery();
                    searchSQL(query);
                }
            }
        } catch(Exception ex) {
            SIRS.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            // Si une erreur s'est produite pendant la requête, on propose un panneau
            // dépliable pour informer l'utilisateur.
            displayError(ex);
        }
    }

    /**
     * Display given exception in search panel instead of searh result.
     * @param ex Error to display to user.
     */
    private void displayError(final Throwable ex) {
        SIRS.fxRun(false, () -> {
            Label errorLabel = new Label("Une erreur est survenue. Assurez-vous que la syntaxe de votre requête est correcte.");
            errorLabel.setBackground(Background.EMPTY);

            final StringWriter errorStack = new StringWriter();
            ex.printStackTrace(new PrintWriter(errorStack));
            final TitledPane errorPane = new TitledPane("Trace de l'erreur :", new TextArea(errorStack.toString()));
            errorPane.setBackground(Background.EMPTY);
            errorPane.setBorder(Border.EMPTY);

            final Accordion accordion = new Accordion();
            accordion.getPanes().add(errorPane);
            VBox vBox = new VBox(10, errorLabel, accordion);
            vBox.setPadding(new Insets(10));
            resultPane.setCenter(vBox);
        });
    }

    private void searchText(){

        final ElasticSearchEngine engine = Injector.getElasticSearchEngine();
        final QueryBuilder qb = QueryBuilders.simpleQueryStringQuery("*"+uiElasticKeywords.getText()+"*").analyzeWildcard(true).lenient(true);

        final SearchResponse response = engine.search(qb);
        final SearchHits hits = response.getHits();

        final ObservableList<ElementHit> results = FXCollections.observableArrayList();
        final Iterator<SearchHit> ite = hits.iterator();
        while(ite.hasNext()){
            final ElementHit hit = new ElementHit(ite.next());
            // Add result only if we can reach element behind it.
            if (hit.getElementClassName() != null && session.getRepositoryForType(hit.getElementClassName()) != null) {
                results.add(hit);
            }
        }

        final ObjectTable table = new ObjectTable(ElementHit.class, "Résultats");
        table.setTableItems(results);
        uiNbResults.setText(results.size()+" résultat(s).");
        resultPane.setCenter(table);
    }

    private void searchDesignation() {
        final String designation = uiDesignation.getText();
        if (designation == null || designation.isEmpty()) {
            final Label label = new Label("Veuillez spécifier la désignation à chercher");
            label.setTextFill(javafx.scene.paint.Color.DARKRED);
            resultPane.setCenter(label);
            return;
        }

        final Class<? extends Element> typeClass = uiDesignationClass.getValue();
        if (typeClass == null) {
            final Label label = new Label("Veuillez spécifier un type d'objet pour la recherche");
            label.setTextFill(javafx.scene.paint.Color.DARKRED);
            resultPane.setCenter(label);
            return;
        }

        final List<Preview> previews = session.getPreviews().getByClass(typeClass);

        final ObjectTable table = new ObjectTable(Preview.class, "Résultats");
        final ObservableList<Preview> result = SIRS.observableList(previews).filtered(preview ->
            designation.equalsIgnoreCase(preview.getDesignation()));

        table.setTableItems(result);
        uiNbResults.setText(String.valueOf(result.size()).concat(" résultat(s)."));

        resultPane.setCenter(table);
    }

    private void searchSQL(String query) {
        final Task<FeatureMapLayer> t = searchSQLLayer(query);
        t.setOnSucceeded(evt -> Platform.runLater(() -> {
            final FeatureMapLayer layer = t.getValue();
            if (layer == null || layer.getCollection().isEmpty()) {
                resultPane.setCenter(new Label("Pas de résultat pour votre recherche."));
                uiNbResults.setText("0 résultat.");
            } else {
                final CustomizedFeatureTable table = new CustomizedFeatureTable(MODEL_PACKAGE + ".", Locale.getDefault(), Thread.currentThread().getContextClassLoader());
                final Button uiExport = new Button(null, new ImageView(SIRS.ICON_EXPORT_WHITE));
                uiExport.setTooltip(new Tooltip("Sauvegarder le résultat de la requête au format CSV"));
                uiExport.getStyleClass().add("buttonbar");
                uiExport.setOnAction((evt2) -> {
                    export(layer).ifPresent(task -> {
                        uiExport.setDisable(true);
                        task.setOnSucceeded(e -> Platform.runLater(() -> {
                            uiExport.setDisable(false);
                            new Growl(Growl.Type.INFO, "L'export des résultats de la requête est terminé").showAndFade();
                        }));
                        task.setOnFailed(e -> Platform.runLater(() -> {
                            uiExport.setDisable(false);
                            new Growl(Growl.Type.ERROR, "L'export des résultats a échoué").showAndFade();
                        }));
                        task.setOnCancelled(e -> Platform.runLater(() -> {
                            uiExport.setDisable(false);
                            new Growl(Growl.Type.WARNING, "L'export des résultats a été interrompu").showAndFade();
                        }));
                        TaskManager.INSTANCE.submit(task);
                    });
                });
                final HBox box = new HBox(uiExport);
                box.setAlignment(Pos.BOTTOM_RIGHT);
                HBox.setHgrow(uiExport, Priority.NEVER);
                table.setTop(box);
                table.setLoadAll(true);
                table.init(layer);
                resultPane.setCenter(table);
                uiNbResults.setText(layer.getCollection().size() + " résultat(s).");
            }
        }));
        t.setOnFailed(evt -> displayError(t.getException()));
        TaskManager.INSTANCE.submit(t);
    }

    /**
     * Perform given SQL statement, returning result as a map layer.
     * @param query The SQL request to perfform.
     * @return A task (not submitted yet) to perform the request and transform it
     * into map layer.
     */
    private Task<FeatureMapLayer> searchSQLLayer(String query) {
        return new TaskManager.MockTask("Recherche SQL", () -> {
            final Query fsquery = org.geotoolkit.data.query.QueryBuilder.language(
                    JDBCFeatureStore.CUSTOM_SQL, query, NamesExt.create("requete"));
            final FeatureCollection col = h2Store.createSession(false).getFeatureCollection(fsquery);
            final FeatureMapLayer layer = MapBuilder.createFeatureLayer(col, getStyleForType(col.getFeatureType()));
            layer.setName(query);

            // Deactivate map export if no geometry can be found in query result.
            Platform.runLater(() -> uiCarto.setDisable(col.getFeatureType().getGeometryDescriptor() == null || col.isEmpty()));

            return layer;
        });
    }

    private static MutableStyle getStyleForType(final FeatureType fType) {
        PropertyType geomType;
        try {
            geomType = (fType.getProperty("geometry") != null)? fType.getProperty("geometry") : null;
        } catch (IllegalArgumentException e) {
            geomType = null;
        }

        return CorePlugin.createDefaultStyle(Color.GRAY, (geomType == null)? null : geomType.getName().toString(), false);
    }

    private Optional<Task> export(final FeatureMapLayer layer) {
        final FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("Comma Separated Values", "*.csv"));
        chooser.setInitialFileName("resultat.csv");
        File output = chooser.showSaveDialog(getScene().getWindow());
        if (output == null)
            return Optional.empty();

        return Optional.of(new TaskManager.MockTask<>("Export CSV", () -> {
            if (output.exists())
                output.delete();

            try (final CSVFeatureStore csvStore = new CSVFeatureStore(output.toPath(), "no namespace", ';')) {
                final FeatureType ft = layer.getCollection().getFeatureType();
                csvStore.createFeatureType(ft.getName(), ft);
                csvStore.addFeatures(ft.getName(), layer.getCollection());
            }

            return output.toPath();
        }));
    }

    private static class CustomizedFeatureTable extends FXFeatureTable implements Printable {

        CustomizedFeatureTable(final String path, final Locale locale, final ClassLoader classLoader){
            super(path, locale, classLoader);
        }

        @Override
        public ObjectProperty getPrintableElements() {
            ObservableList<Feature> ftrs = table.getSelectionModel().getSelectedItems();
            if(ftrs.isEmpty()){
                ftrs = table.getItems();
            }
            return new SimpleObjectProperty(FeatureStoreUtilities.collection(ftrs.get(0).getType(), ftrs));
        }
    }
}
