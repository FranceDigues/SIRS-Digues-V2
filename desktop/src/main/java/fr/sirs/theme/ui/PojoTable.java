package fr.sirs.theme.ui;

import com.sun.javafx.property.PropertyReference;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.Injector;
import fr.sirs.Printable;
import fr.sirs.SIRS;
import static fr.sirs.SIRS.AUTHOR_FIELD;
import static fr.sirs.SIRS.COMMENTAIRE_FIELD;
import static fr.sirs.SIRS.DATE_MAJ_FIELD;
import static fr.sirs.SIRS.FOREIGN_PARENT_ID_FIELD;
import static fr.sirs.SIRS.GEOMETRY_MODE_FIELD;
import static fr.sirs.SIRS.LATITUDE_MAX_FIELD;
import static fr.sirs.SIRS.LATITUDE_MIN_FIELD;
import static fr.sirs.SIRS.LONGITUDE_MAX_FIELD;
import static fr.sirs.SIRS.LONGITUDE_MIN_FIELD;
import static fr.sirs.SIRS.VALID_FIELD;
import static fr.sirs.SIRS.ID_FIELD;
import static fr.sirs.SIRS.REVISION_FIELD;
import static fr.sirs.SIRS.NEW_FIELD;
import fr.sirs.Session;
import fr.sirs.StructBeanSupplier;
import fr.sirs.core.Repository;
import fr.sirs.core.SirsCore;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.AbstractObservation;
import fr.sirs.core.model.AvecBornesTemporelles;
import fr.sirs.core.model.AvecForeignParent;
import fr.sirs.core.model.AvecGeometrie;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.core.model.LigneEau;
import fr.sirs.core.model.PointDZ;
import fr.sirs.core.model.PointXYZ;
import fr.sirs.core.model.PointZ;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.PrZPointImporter;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.ProfilLong;
import fr.sirs.core.model.SystemeEndiguement;
import fr.sirs.map.ExportTask;
import fr.sirs.map.FXMapTab;
import fr.sirs.theme.ColumnOrder;
import fr.sirs.theme.ui.PojoTablePointBindings.DXYZBinding;
import fr.sirs.theme.ui.PojoTablePointBindings.PRXYZBinding;
import fr.sirs.theme.ui.PojoTablePointBindings.PRZBinding;
import fr.sirs.ui.Growl;
import fr.sirs.util.FXReferenceEqualsOperator;
import fr.sirs.util.ReferenceTableCell;
import fr.sirs.util.SEClassementEqualsOperator;
import fr.sirs.util.SirsStringConverter;
import fr.sirs.util.odt.ODTUtils;
import fr.sirs.util.property.Internal;
import fr.sirs.util.property.Reference;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.util.Callback;
import javafx.util.Duration;
import jidefx.scene.control.field.NumberField;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.DefaultAssociationRole;
import org.apache.sis.feature.DefaultAttributeType;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.geotoolkit.data.FeatureStoreFinder;
import org.geotoolkit.data.FileFeatureStoreFactory;
import org.geotoolkit.data.bean.BeanFeatureSupplier;
import org.geotoolkit.data.bean.BeanStore;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.javafx.filter.FXFilterBuilder;
import org.geotoolkit.gui.javafx.util.ButtonTableCell;
import org.geotoolkit.gui.javafx.util.FXBooleanCell;
import org.geotoolkit.gui.javafx.util.FXEnumTableCell;
import org.geotoolkit.gui.javafx.util.FXLocalDateCell;
import org.geotoolkit.gui.javafx.util.FXLocalDateTimeCell;
import org.geotoolkit.gui.javafx.util.FXNumberCell;
import org.geotoolkit.gui.javafx.util.FXStringCell;
import org.geotoolkit.gui.javafx.util.FXTableView;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.odftoolkit.simple.TextDocument;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Filter;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 * @author Samuel Andrés (Geomatys)
 */
public class PojoTable extends BorderPane implements Printable {

    private static final Callback<TableColumn.CellDataFeatures, ObservableValue> DEFAULT_VALUE_FACTORY = param -> new SimpleObjectProperty(param.getValue());
    private static final Predicate DEFAULT_VISIBLE_PREDICATE = o -> o != null;

    protected static final String BUTTON_STYLE = "buttonbar-button";
    private static final Image ICON_SHOWONMAP = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_GLOBE, 16, FontAwesomeIcons.DEFAULT_COLOR),null);

    public static final String[] COLUMNS_TO_IGNORE = new String[]{
        AUTHOR_FIELD, VALID_FIELD, FOREIGN_PARENT_ID_FIELD, LONGITUDE_MIN_FIELD,
        LONGITUDE_MAX_FIELD, LATITUDE_MIN_FIELD, LATITUDE_MAX_FIELD, DATE_MAJ_FIELD,
        COMMENTAIRE_FIELD, GEOMETRY_MODE_FIELD, REVISION_FIELD, ID_FIELD, NEW_FIELD
    };

    protected final Class pojoClass;
    protected final AbstractSIRSRepository repo;
    protected final Session session = Injector.getBean(Session.class);
    private final TableView<Element> uiTable = new FXTableView<>();
    private final LabelMapper labelMapper;

    /**
     * Editabilité du tableau (possibilité d'ajout et de suppression des éléments
     * via la barre d'action sur la droite, plus édition des cellules)
     */
    protected final BooleanProperty editableProperty = new SimpleBooleanProperty(true);
    /** Editabilité des cellules tableau */
    protected final BooleanProperty cellEditableProperty = new SimpleBooleanProperty();
    /** Parcours fiche par fiche */
    protected final BooleanProperty fichableProperty = new SimpleBooleanProperty(true);
    /** Accès à la fiche détaillée d'un élément particulier */
    protected final BooleanProperty detaillableProperty = new SimpleBooleanProperty(true);
    /** Possibilité de faire une recherche sur le contenu de la table */
    protected final BooleanProperty searchableProperty = new SimpleBooleanProperty(true);
    /** Ouvrir l'editeur sur creation d'un nouvel objet */
    protected final BooleanProperty openEditorOnNewProperty = new SimpleBooleanProperty(true);
    /**
     * Créer un nouvel objet à l'ajout.
     *
     * Si cette propriété contient "vrai", l'action sur le bouton d'ajout sera
     * de créer de nouvelles instances ajoutées dans le tableau. Si elle
     * contient "faux", l'action sur le bouton d'ajout sera de proposer l'ajout
     * de liens vers des objets préexistants.
     */
    protected final BooleanProperty createNewProperty = new SimpleBooleanProperty(true);
    /** Importer des points. Default : false */
    protected final BooleanProperty importPointProperty = new SimpleBooleanProperty(false);

    /** Composant de filtrage. Propose de filtrer la liste d'objets actuels en éditant des contraintes sur leur propriété. */
    protected FXFilterBuilder uiFilterBuilder;
//    protected final TitledPane uiFilterPane = new TitledPane();
    private final Button resetFilterBtn = new Button("Réinitialiser");
    private final Button applyFilterBtn = new Button("Filtrer");

    // Icônes de la barre d'action

    // Barre de droite : manipulation du tableau et passage en mode parcours de fiche
    protected final Button uiRefresh = new Button(null, new ImageView(SIRS.ICON_REFRESH_WHITE));
    protected final ToggleButton uiFicheMode = new ToggleButton(null, new ImageView(SIRS.ICON_FILE_WHITE));
    protected final ImageView searchNone = new ImageView(SIRS.ICON_SEARCH_WHITE);
    protected final Button uiSearch = new Button(null, searchNone);
    protected final Button uiAdd = new Button(null, new ImageView(SIRS.ICON_ADD_WHITE));
    protected final Button uiDelete = new Button(null, new ImageView(SIRS.ICON_TRASH_WHITE));
    protected final Button uiImport = new Button(null, new ImageView(SIRS.ICON_IMPORT_WHITE));
    protected final Button uiExport = new Button(null, new ImageView(SIRS.ICON_EXPORT_WHITE));
    protected final ToggleButton uiFilter = new ToggleButton(null, new ImageView(SIRS.ICON_FILTER_WHITE));
    protected final HBox searchEditionToolbar = new HBox(uiRefresh, uiFicheMode, uiImport, uiExport, uiSearch, uiAdd, uiDelete, uiFilter);

    // Barre de gauche : navigation dans le parcours de fiches
    protected FXElementPane elementPane = null;
    private final Button uiPrevious = new Button("",new ImageView(SIRS.ICON_CARET_LEFT));
    private final Button uiNext = new Button("",new ImageView(SIRS.ICON_CARET_RIGHT));
    private final Button uiCurrent = new Button();
    protected final HBox navigationToolbar = new HBox(uiPrevious, uiCurrent, uiNext);

    protected final ProgressIndicator searchRunning = new ProgressIndicator();
    /** Supplier providing table data. Returned list will be used to set {@link #allValues}*/
    private final SimpleObjectProperty<Supplier<ObservableList<Element>>> dataSupplierProperty = new SimpleObjectProperty<>();
    /** Brut values returned by {@link #dataSupplier}. */
    private ObservableList<Element> allValues;
    /** Values from {@link #allValues}, after applying text/property filters. */
    private ObservableList<Element> filteredValues;
    //Cette liste est uniquement pour de la visualisation, elle peut contenir un enregistrement en plus
    //afin d'afficher la barre de scroll horizontale.
    private SortedList<Element> decoratedValues;

    protected final StringProperty currentSearch = new SimpleStringProperty("");
    protected final BorderPane topPane;

    private final VBox filterContent;

    // Colonnes de suppression et d'ouverture d'éditeur.
    protected final DeleteColumn deleteColumn = new DeleteColumn();
    protected final EditColumn editCol = new EditColumn(this::editPojo);

    /** The element to set as parent for any created element using {@linkplain #createPojo() }. */
    protected final ObjectProperty<Element> parentElementProperty = new SimpleObjectProperty<>();
    /** The element to set as owner for any created element using {@linkplain #createPojo() }.
     On the contrary to the parent, the owner purpose is not to contain the created pojo, but to reference it.*/
    protected final ObjectProperty<Element> ownerElementProperty = new SimpleObjectProperty<>();

    //Partie basse pour les commentaires et photos
    private final FXCommentPhotoView commentPhotoView = new FXCommentPhotoView();

    /** Task object designed for asynchronous update of the elements contained in the table. */
    protected final SimpleObjectProperty<Task> tableUpdaterProperty = new SimpleObjectProperty<>();

    protected final StackPane notifier = new StackPane();

    protected final StringProperty titleProperty = new SimpleStringProperty();

    public PojoTable(final Class pojoClass, final String title) {
        this(pojoClass, title, null);
    }

    public PojoTable(final AbstractSIRSRepository repo, final String title) {
        this(repo.getModelClass(), title, repo);
    }

    private PojoTable(final Class pojoClass, final String title, final AbstractSIRSRepository repo) {
        if (pojoClass == null && repo == null) {
            throw new IllegalArgumentException("Pojo class to expose and Repository parameter are both null. At least one of them must be valid.");
        }

        SIRS.setColumnResize(uiTable);

        setFocusTraversable(true);

        dataSupplierProperty.addListener(this::updateTableItems);

        if (pojoClass == null) {
            this.pojoClass = repo.getModelClass();
        } else {
            this.pojoClass = pojoClass;
        }

        this.labelMapper = LabelMapper.get(this.pojoClass);
        if (repo == null) {
            AbstractSIRSRepository tmpRepo;
            try {
                tmpRepo = session.getRepositoryForClass(pojoClass);
            } catch (IllegalArgumentException e) {
                SIRS.LOGGER.log(Level.FINE, e.getMessage());
                tmpRepo = null;
            }
            this.repo = tmpRepo;
        } else {
            this.repo = repo;
        }
        searchRunning.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        searchRunning.setPrefSize(22, 22);
        searchRunning.setStyle("-fx-progress-color: white;");

        uiRefresh.managedProperty().bind(uiRefresh.visibleProperty());
        uiFicheMode.managedProperty().bind(uiFicheMode.visibleProperty());
        uiSearch.managedProperty().bind(uiSearch.visibleProperty());
        uiAdd.managedProperty().bind(uiAdd.visibleProperty());
        uiDelete.managedProperty().bind(uiDelete.visibleProperty());
        uiImport.managedProperty().bind(uiImport.visibleProperty());
        uiExport.managedProperty().bind(uiExport.visibleProperty());

        uiTable.setRowFactory((TableView<Element> param) ->  new TableRow<Element>() {

            @Override
            protected void updateItem(Element item, boolean empty) {
                super.updateItem(item, empty);

                if(item!=null && !item.getValid()){
                    getStyleClass().add("invalidRow");
                }
                else{
                    getStyleClass().removeAll("invalidRow");
                }
                disableProperty().unbind();
                disableProperty().bind(itemProperty().isNull());
            }
        });
        uiTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        /* We cannot bind visible properties of those columns, because TableView
         * will set their value when user will request to hide them.
         */
        editableProperty.addListener((
                ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    deleteColumn.setVisible(newValue);
                    //editCol.setVisible(newValue && detaillableProperty.get());
                });
        cellEditableProperty.bind(editableProperty);
        detaillableProperty.addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    editCol.setVisible(newValue);
                });

        uiTable.getColumns().add(deleteColumn);
        uiTable.getColumns().add((TableColumn) editCol);
//        uiTable.getColumns().add((TableColumn) new DuplicateColumn());

        if(AvecGeometrie.class.isAssignableFrom(pojoClass)){
            uiTable.getColumns().add(new ShowOnMapColumn());
        }

        try {
            //contruction des colonnes editable
            final HashMap<String, PropertyDescriptor> properties = SIRS.listSimpleProperties(this.pojoClass);

            // On enlève les propriétés inutiles pour l'utilisateur
            for (final String key : COLUMNS_TO_IGNORE) {
                properties.remove(key);
            }

            final ArrayList<String> colNames = new ArrayList<>(properties.keySet());
            final List<TableColumn> cols = new ArrayList<>();

            // On donne toutes les informations de position.
            if (Positionable.class.isAssignableFrom(this.pojoClass)) {
                final Set<String> positionableKeys = SIRS.listSimpleProperties(Positionable.class).keySet();
                positionableKeys.remove(SIRS.DESIGNATION_FIELD);
                final ArrayList<TableColumn> positionColumns = new ArrayList<>();
                for (final String key : positionableKeys) {
                    getPropertyColumn(properties.remove(key)).ifPresent(column -> {
                        cols.add(column);
                        positionColumns.add(column);
                    });
                }

                // On permet de cacher toutes les infos de position d'un coup.
                final ImageView viewOn = new ImageView(SIRS.ICON_COMPASS_WHITE);
                final ToggleButton uiPositionVisibility = new ToggleButton(null, viewOn);
                uiPositionVisibility.setSelected(true); // Prepare to be forced to change.
                uiPositionVisibility.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    if (newValue == null) return;
                    for (final TableColumn col : positionColumns) {
                        col.setVisible(newValue);
                    }
                    if (newValue) {
                        uiPositionVisibility.setTooltip(new Tooltip("Cacher les informations de position"));
                    } else {
                        uiPositionVisibility.setTooltip(new Tooltip("Afficher les informations de position"));
                    }
                });
                uiPositionVisibility.visibleProperty().bind(uiFicheMode.selectedProperty().not());
                uiPositionVisibility.managedProperty().bind(uiPositionVisibility.visibleProperty());
                uiPositionVisibility.getStyleClass().add(BUTTON_STYLE);
                uiPositionVisibility.setSelected(false);// Force to change.
                searchEditionToolbar.getChildren().add(uiPositionVisibility);
            }

            for (final PropertyDescriptor desc : properties.values()) {
                getPropertyColumn(desc).ifPresent(column -> cols.add(column));
            }

            //on trie les colonnes
            final List<String> order = ColumnOrder.sort(this.pojoClass.getSimpleName(),colNames);
            for(String colName : order){
                final TableColumn column = getColumn(colName, cols);
                if(column!=null) uiTable.getColumns().add(column);
            }

        } catch (IntrospectionException ex) {
            SIRS.LOGGER.log(Level.WARNING, "property columns cannot be created.", ex);
        }

        uiTable.editableProperty().bind(editableProperty);

        /* barre d'outils. Si on a un accesseur sur la base, on affiche des
         * boutons de création / suppression.
         */
        uiSearch.textProperty().bind(currentSearch);
        uiSearch.getStyleClass().add(BUTTON_STYLE);
        uiSearch.setOnAction((ActionEvent event) -> searchText());
        uiSearch.getStyleClass().add("label-header");
        uiSearch.disableProperty().bind(searchableProperty.not());

        titleProperty.set(title==null? labelMapper == null? null : labelMapper.mapClassName() : title);
        final Label uiTitle = new Label();
        uiTitle.textProperty().bind(titleProperty);
        uiTitle.getStyleClass().add("pojotable-header");
        uiTitle.setAlignment(Pos.CENTER);

        searchEditionToolbar.getStyleClass().add("buttonbar");

        uiRefresh.getStyleClass().add(BUTTON_STYLE);
        uiRefresh.setOnAction((ActionEvent event) -> updateTableItems(dataSupplierProperty, null, dataSupplierProperty.get()));

        uiAdd.getStyleClass().add(BUTTON_STYLE);
        uiAdd.setOnAction((ActionEvent event) -> {
            final Element p;
            if(createNewProperty.get()){
                p = createPojo();
                if (p != null && openEditorOnNewProperty.get()) {
                    editPojo(p);
                }
            }
            else {
                final PojoTableChoiceStage<Element> stage = new ChoiceStage();
                stage.showAndWait();
                p = stage.getRetrievedElement().get();
                if (p!=null) {
                    if(getAllValues().contains(p)){
                        final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Le lien que vous souhaitez ajouter est déjà présent dans la table.");
                        alert.setResizable(true);
                        alert.showAndWait();
                    } else {
                        getAllValues().add(p);
                    }
                } else {
                    final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Aucune entrée ne peut être créée.");
                    alert.setResizable(true);
                    alert.showAndWait();
                }
            }
        });
        uiAdd.disableProperty().bind(editableProperty.not());

        uiDelete.getStyleClass().add(BUTTON_STYLE);
        uiDelete.setOnAction((ActionEvent event) -> {
            final Element[] elements = ((List<Element>) uiTable.getSelectionModel().getSelectedItems()).toArray(new Element[0]);
            if (elements.length > 0) {
                final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer la suppression ?", ButtonType.NO, ButtonType.YES);
                alert.setResizable(true);
                final Optional<ButtonType> res = alert.showAndWait();
                if (res.isPresent() && ButtonType.YES.equals(res.get())) {
                    deletePojos(elements);
                }
            } else {
                final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Aucune entrée sélectionnée. Pas de suppression possible.");
                alert.setResizable(true);
                alert.showAndWait();
            }
        });
        uiDelete.disableProperty().bind(editableProperty.not());

        uiTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        uiTable.setMaxWidth(Double.MAX_VALUE);
        uiTable.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        uiTable.setPlaceholder(new Label(""));
        uiTable.setTableMenuButtonVisible(true);
        // Load all elements only if the user gave us the repository.
        if (repo != null) {
            setTableItems(()-> SIRS.observableList(this.repo.getAll()));
        }

        commentPhotoView.valueProperty().bind(uiTable.getSelectionModel().selectedItemProperty());
        commentPhotoView.visibleProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                updateView();
            }
        });

        //
        // NAVIGATION FICHE PAR FICHE
        //
        navigationToolbar.getStyleClass().add("buttonbarleft");

        uiCurrent.setFont(Font.font(16));
        uiCurrent.getStyleClass().add(BUTTON_STYLE);
        uiCurrent.setAlignment(Pos.CENTER);
        uiCurrent.setTextFill(Color.WHITE);
        uiCurrent.setOnAction(this::goTo);

        uiPrevious.getStyleClass().add(BUTTON_STYLE);
        uiPrevious.setOnAction((ActionEvent event) -> {
            uiTable.getSelectionModel().selectPrevious();
        });

        uiNext.getStyleClass().add(BUTTON_STYLE);
        uiNext.setOnAction((ActionEvent event) -> {
            uiTable.getSelectionModel().selectNext();
        });
        navigationToolbar.visibleProperty().bind(uiFicheMode.selectedProperty());

        uiFicheMode.getStyleClass().add(BUTTON_STYLE);

        // Update counter when we change selected element.
        final ChangeListener<Number> selectedIndexListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            uiCurrent.setText(""+(newValue.intValue()+1) + " / " + uiTable.getItems().size());
        };
        uiFicheMode.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    uiTable.getSelectionModel().selectedIndexProperty().addListener(selectedIndexListener);
                } else {
                    uiTable.getSelectionModel().selectedIndexProperty().removeListener(selectedIndexListener);
                }
                updateView();
            }
        });
        uiFicheMode.disableProperty().bind(fichableProperty.not());

        uiImport.getStyleClass().add(BUTTON_STYLE);
        uiImport.disableProperty().bind(editableProperty.not());
        uiImport.visibleProperty().bind(importPointProperty);
        uiImport.managedProperty().bind(importPointProperty);
        uiImport.setOnAction(event -> {
            final FXAbstractImportPointLeve importCoord;
            if(PointXYZ.class.isAssignableFrom(pojoClass)) importCoord = new FXImportXYZ(PojoTable.this);
            else importCoord = new FXImportDZ(PojoTable.this);

            final Dialog dialog = new Dialog();
            final DialogPane pane = new DialogPane();
            pane.getButtonTypes().add(ButtonType.CLOSE);
            pane.setContent(importCoord);
            dialog.setDialogPane(pane);
            dialog.setResizable(true);
            dialog.setTitle("Import de points");
            dialog.setOnCloseRequest(event1 -> dialog.hide());
            dialog.show();
        });

        uiExport.getStyleClass().add(BUTTON_STYLE);
        uiExport.disableProperty().bind(Bindings.isNull(uiTable.getSelectionModel().selectedItemProperty()));
        uiExport.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                final DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle(GeotkFX.getString(org.geotoolkit.gui.javafx.contexttree.menu.ExportItem.class, "folder"));
                final File folder = chooser.showDialog(null);

                if(folder!=null){
                    try{
                        final BeanFeatureSupplier sup = new StructBeanSupplier(pojoClass, () -> new ArrayList(uiTable.getSelectionModel().getSelectedItems()));
                        final BeanStore store = new BeanStore(sup);
                        final FeatureMapLayer layer = MapBuilder.createFeatureLayer(store.createSession(false)
                                .getFeatureCollection(QueryBuilder.all(store.getNames().iterator().next())));
                        layer.setName(store.getNames().iterator().next().tip().toString());

                        FileFeatureStoreFactory factory = (FileFeatureStoreFactory) FeatureStoreFinder.getFactoryById("csv");
                        TaskManager.INSTANCE.submit(new ExportTask(layer, folder, factory));
                    } catch (Exception ex) {
                        Dialog d = new Alert(Alert.AlertType.ERROR, "Impossible de créer le fichier CSV", ButtonType.OK);
                        d.setResizable(true);
                        d.showAndWait();
                        throw new UnsupportedOperationException("Failed to create csv store : " + ex.getMessage(), ex);
                    }
                }
            }
        });


        if(PointZ.class.isAssignableFrom(pojoClass)){
            uiTable.getColumns().add(new DistanceComputedPropertyColumn());
        }

        final HBox titleBoxing = new HBox(uiTitle);
        titleBoxing.setAlignment(Pos.CENTER);
        final VBox titleAndFilterBox = new VBox(titleBoxing);


        uiFilter.getStyleClass().add(BUTTON_STYLE);
        uiFilter.managedProperty().bind(uiFilter.visibleProperty());
        uiFilter.disableProperty().bind(uiFicheMode.selectedProperty());
        resetFilterBtn.getStyleClass().addAll("label-header", "buttonbar-button", "white-rounded");
        applyFilterBtn.getStyleClass().addAll("label-header", "buttonbar-button", "white-rounded");
        final Separator separator = new Separator(Orientation.VERTICAL);
        separator.setMaxWidth(Double.MAX_VALUE);
        separator.setVisible(false);
        final HBox confirmationBox = new HBox(5, separator, resetFilterBtn, applyFilterBtn);
        HBox.setHgrow(separator, Priority.ALWAYS);
        filterContent = new VBox(10);
        filterContent.getStyleClass().add("filter-root");
        filterContent.visibleProperty().bind(uiFilter.selectedProperty().and(uiFilter.visibleProperty()).and(uiFilter.disableProperty().not()));
        filterContent.managedProperty().bind(filterContent.visibleProperty());

        try {
            initFilterBuilder();
            filterContent.getChildren().addAll(uiFilterBuilder, confirmationBox);

            applyFilterBtn.managedProperty().bind(filterContent.visibleProperty());
            resetFilterBtn.managedProperty().bind(filterContent.visibleProperty());
            uiFilterBuilder.managedProperty().bind(filterContent.visibleProperty());

            resetFilterBtn.setOnAction(event -> resetFilter(filterContent));
            applyFilterBtn.setOnAction(event -> updateTableItems(dataSupplierProperty, null, dataSupplierProperty.get()));
        } catch (Exception e) {
            SIRS.LOGGER.log(Level.WARNING, "Filter panel cannot be initialized !", e);
        }

        titleAndFilterBox.setFillWidth(true);
        topPane = new BorderPane(notifier, titleAndFilterBox, searchEditionToolbar, filterContent, navigationToolbar);
        setTop(topPane);

        /*
         * TOOLTIPS
         */
        uiFicheMode.setTooltip(new Tooltip("Passer en mode de parcours des fiches"));
        uiSearch.setTooltip(new Tooltip("Rechercher un terme dans la table"));
        uiImport.setTooltip(new Tooltip("Importer des points"));
        uiExport.setTooltip(new Tooltip("Sauvegarder en CSV"));
        uiPrevious.setTooltip(new Tooltip("Fiche précédente"));
        uiNext.setTooltip(new Tooltip("Fiche suivante"));
        uiRefresh.setTooltip(new Tooltip("Recharger la table"));
        uiAdd.setTooltip(new Tooltip(createNewProperty.get()? "Créer un nouvel élément" : "Ajouter un élément existant"));
        createNewProperty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                uiAdd.setTooltip(new Tooltip("Créer un nouvel élément"));
            } else {
                uiAdd.setTooltip(new Tooltip("Ajouter un élément existant"));
            }
        });
        uiCurrent.setTooltip(new Tooltip("Aller au numéro..."));
        uiDelete.setTooltip(new Tooltip("Supprimer les éléments sélectionnés"));
        uiFilter.setTooltip(new Tooltip("Filtrer les données"));

        updateView();
    }

    public StringProperty titleProperty() {
        return titleProperty;
    }

    protected final ObservableList<TableColumn<Element, ?>> getColumns(){
        return uiTable.getColumns();
    }

    public final ObservableList<Element> getSelectedItems(){
        return uiTable.getSelectionModel().getSelectedItems();
    }

    public final ReadOnlyObjectProperty<? extends Element> selectedItemProperty(){
        return getTable().getSelectionModel().selectedItemProperty();
    }

    protected final TableView<Element> getTable() {
        return uiTable;
    }

    private void updateView(){

        if(uiFicheMode.isSelected()){
            // If there's no selection, initialize on first element.
            if (uiTable.getSelectionModel().getSelectedIndex() < 0) {
                uiTable.getSelectionModel().select(0);
            }

            // Prepare editor and bind its content to selection model.
            if (elementPane == null) {
                elementPane = SIRS.generateEditionPane(uiTable.getSelectionModel().getSelectedItem());
            }
            elementPane.elementProperty().bind(uiTable.getSelectionModel().selectedItemProperty());

            uiFicheMode.setTooltip(new Tooltip("Passer en mode de tableau synoptique."));

            uiCurrent.setText("" + (uiTable.getSelectionModel().getSelectedIndex()+1) + " / " + uiTable.getItems().size());
            setCenter((Node) elementPane);
        }else{
            // Deconnect editor.
            if (elementPane != null) {
                elementPane.elementProperty().unbind();
            }
            uiFicheMode.setTooltip(new Tooltip("Passer en mode de parcours des fiches."));

            if(commentPhotoView.isVisible()){
                final SplitPane sPane = new SplitPane();
                sPane.setOrientation(Orientation.VERTICAL);
                sPane.getItems().addAll(uiTable, commentPhotoView);
                sPane.setDividerPositions(0.9);
                setCenter(sPane);
            }else{
                setCenter(uiTable);
            }
        }
    }

    /**
     * Définit l'élément en paramètre comme parent de tout élément créé via cette table.
     *
     * Note : Ineffectif dans le cas où les éléments de la PojoTable sont créés
     * et listés directement depuis un repository couchDB, ou que l'élément créé
     * est déjà un CouchDB document.
     * @param parentElement L'élément qui doit devenir le parent de tout objet créé via
     * la PojoTable.
     */
    public void setParentElement(final Element parentElement) {
        parentElementProperty.set(parentElement);
    }

    /**
     *
     * @return L'élément à affecter en tant que parent de tout élément créé via
     * cette table. Peut être nul.
     */
    public Element getParentElement() {
        return parentElementProperty.get();
    }

    /**
     *
     * @return La propriété contenant l'élément à affecter en tant que parent de
     *  tout élément créé via cette table. Jamais nulle, mais peut-être vide.
     */
    public ObjectProperty<Element> parentElementProperty() {
        return parentElementProperty;
    }

    /**
     * Définit l'élément en paramètre comme principal référent de tout élément créé via cette table.
     *
     * @param parentElement L'élément qui doit devenir le principal référent de tout objet créé via
     * la PojoTable.
     */
    public void setOwnerElement(final Element parentElement) {
        ownerElementProperty.set(parentElement);
    }

    /**
     *
     * @return L'élément principal référent de tout élément créé via
     * cette table. Peut être nul.
     */
    public Element getOwnerElement() {
        return ownerElementProperty.get();
    }

    /**
     *
     * @return La propriété contenant l'élément à affecter en tant que principal référent de
     *  tout élément créé via cette table. Jamais nulle, mais peut-être vide.
     */
    public ObjectProperty<Element> ownerElementProperty() {
        return ownerElementProperty;
    }

    public synchronized ObservableList<Element> getAllValues(){
        return allValues;
    }

    public BooleanProperty editableProperty(){
        return editableProperty;
    }
    public BooleanProperty cellEditableProperty(){
        return cellEditableProperty;
    }
    public BooleanProperty detaillableProperty(){
        return detaillableProperty;
    }
    public BooleanProperty fichableProperty(){
        return fichableProperty;
    }
    public BooleanProperty searchableProperty(){
        return searchableProperty;
    }
    public BooleanProperty openEditorOnNewProperty() {
        return openEditorOnNewProperty;
    }
    public BooleanProperty createNewProperty() {
        return createNewProperty;
    }
    public BooleanProperty importPointProperty() {
        return importPointProperty;
    }
    public BooleanProperty commentAndPhotoProperty() {
        return commentPhotoView.visibleProperty();
    }
    public BooleanProperty exportVisibleProperty() {
        return uiExport.visibleProperty();
    }
    public BooleanProperty searchVisibleProperty() {
        return uiSearch.visibleProperty();
    }
    public BooleanProperty ficheModeVisibleProperty() {
        return uiFicheMode.visibleProperty();
    }

    public BooleanProperty filterVisibleProperty() {
        return uiFilter.visibleProperty();
    }

    public ObjectProperty<Supplier<ObservableList<Element>>> dataSupplierProperty(){
        return dataSupplierProperty;
    }

    public VBox getFilterUI(){
        return filterContent;
    }

    protected void initFilterBuilder() throws IntrospectionException {
        if (uiFilterBuilder == null) {
            uiFilterBuilder = new FXFilterBuilder() {
                @Override
                protected String getTitle(PropertyType candidate) {
                    if (candidate == null || candidate.getName() == null || candidate.getName().head() == null) return "";

                    if (new SEClassementEqualsOperator().canHandle(candidate)) {
                        return "classe";
                    }

                    final String pName = candidate.getName().head().toString();
                    if (labelMapper != null) {
                        final String pTitle = labelMapper.mapPropertyName(pName);
                        if (pTitle != null) {
                            return pTitle;
                        }
                    }
                    return pName;
                }
            };
        }

        // If no property has been given for filtering, we analyze model class to find them.
        final ObservableList<PropertyType> props = uiFilterBuilder.getAvailableProperties();
        if (props.isEmpty()) {
            final BeanInfo info = Introspector.getBeanInfo(pojoClass);
            for (final PropertyDescriptor desc : info.getPropertyDescriptors()) {
                final Method readMethod = desc.getReadMethod();
                if (readMethod != null) {

                    // Do not filter on java standard property like getClass(), etc.
                    if (readMethod.getAnnotation(Internal.class) != null
                            || readMethod.getDeclaringClass().equals(Object.class))
                        continue;

                    final HashMap identification = new HashMap(1);
                    identification.put(AbstractIdentifiedType.NAME_KEY, desc.getName());

                    // If we've got a reference to another document, property is declared as an association.
                    final Reference annot = readMethod.getAnnotation(Reference.class);
                    if (annot != null) {
                        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
                        builder.setName(desc.getName());
                        builder.add(FXReferenceEqualsOperator.CLASS_ATTRIBUTE, annot.ref());
                        props.add(new DefaultAssociationRole(
                                identification, builder.buildFeatureType(), 0, 1));
                        if (SystemeEndiguement.class.equals(annot.ref())) {
                            builder.reset();
                            builder.setName("classement");
                            builder.add(SEClassementEqualsOperator.CLASSEMENT_ATTRIBUTE, String.class);
                            final HashMap tmpIdent = new HashMap(1);
                            tmpIdent.put(AbstractIdentifiedType.NAME_KEY, "classement");
                            props.add(new DefaultAssociationRole(identification, builder.buildFeatureType(), 0, 1));
                        }
                    } else {
                        props.add(new DefaultAttributeType(
                                identification, desc.getPropertyType(), 0, 1, null, null));
                    }
                }
            }
        }
    }

    /**
     * Called when user click on the search icon. Prepare the popup with the textfield
     * to type research into.
     */
    protected void searchText(){
        if(uiSearch.getGraphic()!= searchNone){
            //une recherche est deja en cours
            return;
        }

        final Popup popup = new Popup();
        final TextField textField = new TextField(currentSearch.get());
        popup.getContent().add(textField);
        popup.setAutoHide(true);
        textField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                currentSearch.set(textField.getText());
                popup.hide();
                updateTableItems(dataSupplierProperty, null, dataSupplierProperty.get());
            }
        });
        final Point2D sc = uiSearch.localToScreen(0, 0);
        popup.show(uiSearch, sc.getX(), sc.getY());
    }

    protected void goTo(ActionEvent event){
        final Popup popup = new Popup();
        NumberField indexEditor = new NumberField(NumberField.NumberType.Integer);

        popup.getContent().add(indexEditor);
        popup.setAutoHide(false);

        indexEditor.setOnAction((ActionEvent event1) -> {
            final Number indexToSelect = indexEditor.valueProperty().get();
            if (indexToSelect != null) {
                final int index = indexToSelect.intValue();
                if (index >= 0 && index < uiTable.getItems().size()) {
                    uiTable.getSelectionModel().select(index);
                }
            }
            popup.hide();
        });
        final Point2D sc = uiCurrent.localToScreen(0, 0);
        popup.show(uiSearch, sc.getX(), sc.getY());
    }

    /**
     * @return {@link TableView} used for element display.
     */
    public TableView getUiTable() {
        return uiTable;
    }

    /**
     * Reset the filter pane at its initial state.
     *
     * @param filterContent
     */
    public void resetFilter(final VBox filterContent) {
        final ObservableList<Node> vBoxChildren = filterContent.getChildren();
        final int indexOfBuilder = vBoxChildren.indexOf(uiFilterBuilder);
        if (vBoxChildren.size() > indexOfBuilder) {
            vBoxChildren.remove(indexOfBuilder);
            uiFilterBuilder = null;
            try {
                initFilterBuilder();
                vBoxChildren.add(indexOfBuilder, uiFilterBuilder);
            } catch (Exception e) {
                GeotkFX.newExceptionDialog("Une erreur inattendue est survenue.", e).show();
            }
            applyFilterBtn.fire();
        }
    }

    /**
     * Get the filter which will be applied on the pojo table values. Subclasses can override this method
     * to extend the filter used.
     *
     * @return The filter to use, or {@code null} if none.
     */
    public Filter getFilter() {
        // Apply filter on properties
        Filter tmpFilter = null;
        if (uiFilterBuilder != null) {
            try {
                tmpFilter = uiFilterBuilder.getFilter();
            } catch (Exception e) {
                SIRS.LOGGER.log(Level.FINE, "No filter can be built for pojo table on "+pojoClass, e);
            }
        }
        return tmpFilter;
    }

    /**
     * Start an asynchronous task which will update table content with the elements
     * provided by input supplier.
     * @param producer Data provider.
     */
    public void setTableItems(Supplier<ObservableList<Element>> producer) {
        dataSupplierProperty.set(producer);
    }

    protected final void updateTableItems(
            final ObservableValue<? extends Supplier<ObservableList<Element>>> obs,
            final Supplier<ObservableList<Element>> oldSupplier,
            final Supplier<ObservableList<Element>> newSupplier) {

        if (tableUpdaterProperty.get() != null && !tableUpdaterProperty.get().isDone()) {
            tableUpdaterProperty.get().cancel();
        }

        final Task updater = new TaskManager.MockTask("Recherche...", (Runnable)() -> {
            synchronized (PojoTable.this) {
                try {
                    if (newSupplier == null) {
                        allValues = FXCollections.observableArrayList();
                    } else {
                        allValues = newSupplier.get();
                    }
                } catch (Throwable ex) {
                    allValues = FXCollections.observableArrayList();
                    filteredValues = allValues.filtered((Element t) -> true);
                    decoratedValues = new SortedList<>(filteredValues);
                    decoratedValues.comparatorProperty().bind(uiTable.comparatorProperty());
                    throw ex;
                }
                if (allValues == null) {
                    allValues = FXCollections.observableArrayList();
                }
            }
            if (allValues.isEmpty()) {
                Platform.runLater(() -> {
                    uiSearch.setGraphic(searchNone);
                });
            }

            // Apply filter on properties
            final Filter firstFilter = getFilter();

            final Thread currentThread = Thread.currentThread();
            if (currentThread.isInterrupted()) {
                return;
            }

            // Apply "Plain text" filter
            final String str = currentSearch.get();
            if ((str == null || str.isEmpty()) && firstFilter == null) {
                filteredValues = allValues.filtered((Element t) -> true);
            } else {
                final Set<String> result = new HashSet<>();
                SearchResponse search = Injector.getElasticSearchEngine().search(QueryBuilders.queryString(str));
                Iterator<SearchHit> iterator = search.getHits().iterator();
                while (iterator.hasNext() && !currentThread.isInterrupted()) {
                    result.add(iterator.next().getId());
                }

                if (currentThread.isInterrupted()) {
                    return;
                }

                final Predicate<Element> filterPredicate;
                if (firstFilter == null) {
                    filterPredicate = element -> element == null || result.contains(element.getId());
                } else if (str == null || str.isEmpty()) {
                    filterPredicate = element -> element == null || firstFilter.evaluate(element);
                } else {
                    filterPredicate = element -> element == null || result.contains(element.getId()) && firstFilter.evaluate(element);
                }
                filteredValues = allValues.filtered(filterPredicate);
            }

                //list contenant zero ou un element null en fonction du contenue de la liste filtrée
            //NOTE : bug javafx ici, la premiere ligne n'est plus editable a cause de ca
            // probleme avec la selection/focus qui cause trop d'événement
            //            final ObservableList<Element> emptyRecord = FXCollections.observableArrayList();
            //            filteredValues.addListener(new ListChangeListener<Element>() {
            //                @Override
            //                public void onChanged(ListChangeListener.Change<? extends Element> c) {
            //                    if(filteredValues.isEmpty()){
            //                        if(emptyRecord.isEmpty()) emptyRecord.add(null);
            //                    }else{
            //                        emptyRecord.clear();
            //                    }
            //                }
            //            });
            //            if(filteredValues.isEmpty()) emptyRecord.add(null);
            //            decoratedValues = SIRS.view(filteredValues,emptyRecord);
            decoratedValues = new SortedList<>(filteredValues);
            decoratedValues.comparatorProperty().bind(uiTable.comparatorProperty());
        });


        updater.stateProperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
            if (Worker.State.SUCCEEDED.equals(newValue)) {
                Platform.runLater(() -> {
                    uiTable.setItems(decoratedValues);
                    uiSearch.setGraphic(searchNone);
                });
            } else if (Worker.State.FAILED.equals(newValue) || Worker.State.CANCELLED.equals(newValue)) {
                final Throwable ex = updater.getException();
                if(ex!=null){
                    SIRS.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                }
                Platform.runLater(() -> {
                    uiSearch.setGraphic(searchNone);
                });
            } else if (Worker.State.RUNNING.equals(newValue)) {
                Platform.runLater(() -> uiSearch.setGraphic(searchRunning));
            }
        });

        tableUpdaterProperty.set(TaskManager.INSTANCE.submit("Recherche...", updater));
    }

//    protected final Lock lock = new ReentrantReadWriteLock().readLock();
    /**
     * Check if the input element can be deleted by current user. If not, an
     * alert is displyed on screen.
     * @param pojo The element we want to delete.
     * @return True if we can delete the element in parameter, false otherwise.
     */
    protected boolean authoriseElementDeletion(final Element pojo) {
        if (Boolean.TRUE.equals(session.needValidationProperty().get())) {
            if (session.getUtilisateur() == null || session.getUtilisateur().getId() == null || !session.getUtilisateur().getId().equals(pojo.getAuthor())
                    || pojo.getValid()) {
                final Alert alert = new Alert(Alert.AlertType.INFORMATION, "En tant qu'utilisateur externe, vous ne pouvez supprimer que des éléments invalidés dont vous êtes l'auteur.", ButtonType.OK);
                alert.setResizable(true);
                alert.showAndWait();
                return false;
            }
        }
        return true;
    }

    /**
     * Delete the elements given in parameter. They are suppressed from the table
     * list, and if a {@link Repository} exists for the current table, elements
     * are also suppressed from database. If a parent element has been set using
     * {@linkplain #setParentElement(fr.sirs.core.model.Element) } method, we will
     * try to remove them from the parent as well.
     * @param pojos The {@link Element}s to delete.
     */
    protected void deletePojos(Element... pojos) {
        final ObservableList<Element> items = getAllValues();
        for (Element pojo : pojos) {
            // Si l'utilisateur est un externe, il faut qu'il soit l'auteur de
            // l'élément et que celui-ci soit invalide, sinon, on court-circuite
            // la suppression.
            if(authoriseElementDeletion(pojo)){
                deletor.accept(pojo);
                items.remove(pojo);
            }
        }
    }

    // Default deletor
    private Consumer deletor = new Consumer<Element>() {

        @Override
        public void accept(Element pojo) {
            if (repo != null && createNewProperty.get()) {
                repo.remove(pojo);
            }

            if (parentElementProperty.get() != null) {
                parentElementProperty.get().removeChild(pojo);
            }else if(ownerElementProperty.get() != null){
                ownerElementProperty.get().removeChild(pojo);
            }
        }
    };

    // Change the default deletor
    public void setDeletor(final Consumer deletor){
        this.deletor = deletor;
    }

    /**
     * Try to find and display a form to edit input object.
     * @param pojo The object we want to edit.
     */
    protected void editPojo(Object pojo){
        editElement(pojo);
    }

    /**
     * A method called when an element displayed in the table has been modified
     * in the table.
     * @param event The table event refering to the edition action.
     */
    protected void elementEdited(TableColumn.CellEditEvent<Element, Object> event){
        if (repo != null) {
            final Element obj = event.getRowValue();
            if(obj == null) return;
            repo.update(obj);
        }
    }

    /**
     * Create a new element and add it to table items. If the table {@link Repository}
     * is not null, we also add the element to the database. We also set its parent
     * if it's not a contained element and the table {@linkplain #parentElementProperty}
     * is set.
     * @return The newly created object.
     */
    protected Element createPojo() {
        return createPojo(null);
    }

    protected Element createPojo(final Element foreignParent) {
        Element result = null;

        if (repo != null) {
            result = (Element) repo.create();
            if(foreignParent!=null && result instanceof AvecForeignParent) ((AvecForeignParent) result).setForeignParentId(foreignParent.getId());
            repo.add(result);
        }
        else if (pojoClass != null) {
            try {
                result = session.getElementCreator().createElement(pojoClass);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // TODO : check and set date début
        if (result instanceof Element) {
            final Element newlyCreated = (Element) result;

            AvecBornesTemporelles timePeriod = null;
            final Element parent = parentElementProperty.get();
            final Element owner = ownerElementProperty.get();

            /* Dans le cas où on a un parent, il n'est pas nécessaire de faire
            addChild(), car la liste des éléments de la table est directement
            cette liste d'éléments enfants, sur laquelle on fait un add().*/
            if (parent != null) {
                // this should do nothing for new
                newlyCreated.setParent(parent);
                if (parent instanceof AvecBornesTemporelles) {
                    timePeriod = (AvecBornesTemporelles) parent;

                }
            }

            /* Mais dans le cas où on a un référant principal, il faut faire un
            addChild(), car la liste des éléments de la table n'est pas une
            liste d'éléments enfants. Le référant principal n'a qu'une liste
            d'identifiants qu'il convient de mettre à jour avec addChild().*/
            else if(owner != null){
                owner.addChild(newlyCreated);
                if (owner instanceof AvecBornesTemporelles) {
                    timePeriod = (AvecBornesTemporelles) owner;
                }
            }

            // Force les bornes temporelles à respecter celles du parent.
            if (timePeriod != null) {
                final AvecBornesTemporelles fTimePeriod = timePeriod;
                if (newlyCreated instanceof AvecBornesTemporelles) {
                    final AvecBornesTemporelles child = (AvecBornesTemporelles) newlyCreated;
                    child.date_debutProperty().addListener((obs, oldValue, newValue) -> {
                        if (newValue != null && fTimePeriod.getDate_fin() != null && fTimePeriod.getDate_fin().isBefore(newValue)) {
                            child.setDate_debut(oldValue);
                            SIRS.fxRun(false, () -> new Growl(Growl.Type.WARNING, "Impossible d'affecter une date de début après la fin de validité du parent.").showAndFade());
                        }
                    });
                    child.date_finProperty().addListener((obs, oldValue, newValue) -> {
                        if (newValue != null && fTimePeriod.getDate_debut()!= null && fTimePeriod.getDate_debut().isAfter(newValue)) {
                            child.setDate_debut(oldValue);
                            SIRS.fxRun(false, () -> new Growl(Growl.Type.WARNING, "Impossible d'affecter une date de fin antérieure à la validité du parent.").showAndFade());
                        }
                    });
                } else if (newlyCreated instanceof AbstractObservation) {
                    final AbstractObservation observation = (AbstractObservation) newlyCreated;
                    observation.dateProperty().addListener((obs, oldValue, newValue) -> {
                        final LocalDate parentDebut = fTimePeriod.getDate_debut();
                        final LocalDate parentFin = fTimePeriod.getDate_fin();
                        if (newValue != null) {
                            if (parentDebut != null && parentDebut.isAfter(newValue)) {
                                observation.setDate(oldValue);
                                SIRS.fxRun(false, () -> new Growl(Growl.Type.WARNING, "Impossible d'affecter une date antérieure à la validité du parent.").showAndFade());

                            } else if (parentFin != null && parentFin.isBefore(newValue)) {
                                observation.setDate(oldValue);
                                SIRS.fxRun(false, () -> new Growl(Growl.Type.WARNING, "Impossible d'affecter une date plus récente que la fin de validité du parent.").showAndFade());
                            }
                        }
                    });
                }
            }

            synchronized(this) {
                allValues.add(newlyCreated);
            }
        } else {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Aucune entrée ne peut être créée.");
            alert.setResizable(true);
            alert.showAndWait();
        }
        return (Element) result;
    }

    public static void editElement(Object pojo) {
        try {
            Injector.getSession().showEditionTab(pojo);
        } catch (Exception ex) {
            Dialog d = new Alert(Alert.AlertType.ERROR, "Impossible d'afficher un éditeur", ButtonType.OK);
            d.setResizable(true);
            d.showAndWait();
            throw new UnsupportedOperationException("Failed to load panel : " + ex.getMessage(), ex);
        }
    }

    private static TableColumn getColumn(final String name, Collection<TableColumn> cols) {
        for(TableColumn col : cols){
            if(name.equals(col.getId())){
                return col;
            }
        }
        return null;
    }


    public Optional<TableColumn> getPropertyColumn(final PropertyDescriptor desc) {
        if (desc != null) {
            final TableColumn col;
            if (desc.getReadMethod().getReturnType().isEnum()) {
                col = new EnumColumn(desc);
            } else {
                col = new PropertyColumn(desc);
                col.sortableProperty().bind(importPointProperty.not());
            }
            col.setId(desc.getName());
            return Optional.of(col);
        }
        return Optional.empty();
    }

    @Override
    public String getPrintTitle() {
        return titleProperty.get();
    }

    @Override
    public ObjectProperty getPrintableElements() {
        final List selection = uiTable.getSelectionModel().getSelectedItems();

        return new SimpleObjectProperty(selection.isEmpty()? new ArrayList<>(filteredValues) : new ArrayList(selection));
    }

    @Override
    public boolean print() {
        boolean nothingToPrint = filteredValues.isEmpty();
        final ArrayList<String> propertyNames = new ArrayList<>();
        for (final TableColumn c : getColumns()) {
            if (c.isVisible() && (c instanceof PropertyColumn)) {
                propertyNames.add(((PropertyColumn) c).name);
            }
        }

        nothingToPrint = nothingToPrint || propertyNames.isEmpty();
        if (nothingToPrint) {
            SIRS.fxRun(false, new TaskManager.MockTask<>(() -> new Growl(Growl.Type.WARNING, "Aucun contenu à imprimer.").showAndFade()));
            return true;
        }

        final String title = titleProperty.get();
        // Choose output file
        final Path outputFile = SIRS.fxRunAndWait(() -> {
            final FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Document OpenOffice", "*.odt");
            final FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(extFilter);
            fileChooser.setSelectedExtensionFilter(extFilter);
            final File result = fileChooser.showSaveDialog(PojoTable.this.getScene().getWindow());
            if (result == null) {
                return null;
            } else {
                return result.toPath();
            }
        });

        if (outputFile == null)
            return true; // Printing aborted. Return true to avoid another component to print instead of us.

        final Task<Boolean> printTask = new Task() {
            @Override
            protected Object call() throws Exception {
                updateTitle("Impression : " + title);
                try (final TextDocument doc = TextDocument.newTextDocument()) {
                    doc.addParagraph(title).applyHeading();

                    final long totalWork = filteredValues.size();
                    final AtomicLong work = new AtomicLong(0);
                    ODTUtils.appendTable(doc, filteredValues.stream().peek(e -> updateProgress(work.getAndIncrement(), totalWork)).iterator(), propertyNames);

                    try (final OutputStream out = Files.newOutputStream(outputFile)) {
                        doc.save(out);
                    }
                }

                return true;
            }
        };

        // Events to launch when task finishes
        SIRS.fxRun(false, new TaskManager.MockTask(() -> {
            printTask.setOnFailed(event -> Platform.runLater(() -> GeotkFX.newExceptionDialog("Impossible d'imprimer la table \"" + title + "\"", printTask.getException()).show()));
            printTask.setOnCancelled(event -> Platform.runLater(() -> new Growl(Growl.Type.WARNING, "L'impression de la table \"" + title + "\" a été annulée").showAndFade()));
            printTask.setOnSucceeded(event -> Platform.runLater(() -> {
                new Growl(Growl.Type.INFO, "L'impression de la table \"" + title + "\" la carte est terminée").showAndFade();
                if (!SIRS.openFile(outputFile)) {
                    new Growl(Growl.Type.WARNING, "Impossible de trouver un programme pour ouvrir la table \"" + title + "\"").showAndFade();
                }
            }));
        }));

        // Print map.
        TaskManager.INSTANCE.submit(printTask);
        return true;
    }

////////////////////////////////////////////////////////////////////////////////
//
// INTERNAL CLASSES
//
////////////////////////////////////////////////////////////////////////////////

    private class EnumColumn extends TableColumn<Element, Object> {

        private EnumColumn(final PropertyDescriptor desc) {
            super(labelMapper.mapPropertyName(desc.getDisplayName()));
            setEditable(false);
            setCellValueFactory(new PropertyValueFactory<>(desc.getName()));
            setCellFactory((TableColumn<Element, Object> param) -> {
                final TableCell<Element, Object> cell = new FXEnumTableCell(desc.getReadMethod().getReturnType(), new SirsStringConverter());
                editableProperty().bind(cellEditableProperty);
                return cell;
            });
            addEventHandler(TableColumn.editCommitEvent(), (CellEditEvent<Element, Object> event) -> {
                final Object rowElement = event.getRowValue();
                new PropertyReference<>(rowElement.getClass(), desc.getName()).set(rowElement, event.getNewValue());
                elementEdited(event);
            });
        }
    }

    private static class SimpleCell extends TableCell<Element, Object> {

        private final SirsStringConverter converter = new SirsStringConverter();

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else if (item instanceof String) {
                setText((String) item);
            } else {
                setText(converter.toString(item));
            }
        }
    }

    private static final Callback<TableColumn<Element, Double>, TableCell<Element, Double>> DOUBLE_CELL_FACTORY = param -> new FXNumberCell(Double.class);

    private class DistanceComputedPropertyColumn extends TableColumn<Element, Double>{

        private boolean titleSet = false;

        public DistanceComputedPropertyColumn(){
            setCellFactory(DOUBLE_CELL_FACTORY);
            setEditable(false);
            setText("Valeur calculée");
            setCellValueFactory((CellDataFeatures<Element, Double> param) -> {
                if(param.getValue() instanceof PointXYZ){
                    // Cas des XYZ de profils en long et des lignes d'eau avec PR calculé
                    if(parentElementProperty().get() instanceof ProfilLong
                            || parentElementProperty().get() instanceof LigneEau){
                        if(!titleSet){setText("PR calculé");titleSet=true;}
                        return new PRXYZBinding((PointXYZ) param.getValue(), (Positionable) parentElementProperty().get()).asObject();
                    }
                    // Cas des XYZ de levés de profils en travers avec distance calculée
                    else {
                        final Element origine = getTableView().getItems().get(0);
                        if(origine instanceof PointXYZ){
                            if(!titleSet){setText("Distance calculée");titleSet=true;}
                            return new DXYZBinding((PointXYZ) param.getValue(), (PointXYZ) origine).asObject();
                        }
                        else{
                            // Sinon la colonne ne sert à rien et on la retire dès que possible.
                            if(uiTable.getColumns().contains(DistanceComputedPropertyColumn.this)) uiTable.getColumns().remove(DistanceComputedPropertyColumn.this);
                            return null;
                        }
                    }
                }

                // Das des PrZ de profils en long ou lignes d'eau avec PR saisi converti en PR calculé dans le SR par défaut
                else if(param.getValue() instanceof PointDZ
                        && parentElementProperty().get() instanceof PrZPointImporter
                        && parentElementProperty().get() instanceof Positionable){
                    if(!titleSet){setText("PR calculé"); titleSet=true;}
                    return new PRZBinding((PointDZ) param.getValue(), (Positionable) parentElementProperty().get()).asObject();
                }
                else {
                    // Sinon la colonne ne sert à rien et on la retire dès que possible.
                    if(uiTable.getColumns().contains(DistanceComputedPropertyColumn.this)) uiTable.getColumns().remove(DistanceComputedPropertyColumn.this);
                    return null;
                }
            });
        }
    }

    /**
     * Column used to display / edit simple attributes or references of an element.
     */
    public final class PropertyColumn extends TableColumn<Element, Object>{

        private final Reference ref;
        private final String name;

        public String getName(){return name;}

        public PropertyColumn(final PropertyDescriptor desc) {
            super();
            this.name = desc.getName();

            String pName = labelMapper == null ? null : labelMapper.mapPropertyName(name);
            if (pName != null)
                setText(pName);
            else
                setText(name);

            //choix de l'editeur en fonction du type de données
            boolean isEditable = true;
            this.ref = desc.getReadMethod().getAnnotation(Reference.class);
            if (ref != null) {
                //reference vers un autre objet
                setCellFactory((TableColumn<Element, Object> param) -> new ReferenceTableCell(ref.ref()));
                try {
                    final Method propertyAccessor = ref.ref().getMethod(name+"Property");
                    setCellValueFactory((CellDataFeatures<Element, Object> param) -> {
                        if(param!=null && param.getValue()!=null && propertyAccessor!=null){
                            try {
                                return (ObservableValue) propertyAccessor.invoke(param.getValue());
                            } catch (Exception ex) {
                                SirsCore.LOGGER.log(Level.WARNING, null, ex);
                                return null;
                            }
                        }else{
                            return null;
                        }
                    });
                } catch (Exception ex) {
                    setCellValueFactory(new PropertyValueFactory<>(name));
                }

            } else {
                setCellValueFactory(new PropertyValueFactory<>(name));
                final Class type = desc.getReadMethod().getReturnType();
                if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXBooleanCell());
                } else if (String.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXStringCell());
                } else if (Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXNumberCell(Integer.class));
                } else if (Float.class.isAssignableFrom(type) || float.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXNumberCell.Float());
                } else if (Double.class.isAssignableFrom(type) || double.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXNumberCell(Double.class));
                } else if (LocalDateTime.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXLocalDateTimeCell());
                } else if (LocalDate.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXLocalDateCell());
                } else if (Point.class.isAssignableFrom(type)) {
                    setCellFactory((TableColumn<Element, Object> param) -> new FXPointCell());
                } else {
                    setCellFactory((TableColumn<Element, Object> param) -> new SimpleCell());
                    isEditable = false;
                }
            }

            setEditable(isEditable);
            if (isEditable) {
                editableProperty().bind(cellEditableProperty);
                setOnEditCommit((CellEditEvent<Element, Object> event) -> {
                    /*
                     * We try to update data. If it's a failure, we store exception
                     * to give more information to user. In all cases, a notification
                     * is requested to inform user if its modification has succeded
                     * or not.
                     */
                    Exception tmpError = null;
                    final Object rowElement = event.getRowValue();
                    if (rowElement == null) return;
                    final PropertyReference<Object> propertyReference = new PropertyReference<>(rowElement.getClass(), name);
                    Object oldValue = propertyReference.get(rowElement);
                    try {
                        propertyReference.set(rowElement, event.getNewValue());
                        elementEdited(event);
                    } catch (Exception e) {
                        SIRS.LOGGER.log(Level.WARNING, "Cannot update field.", e);
                        tmpError = e;
                        // rollback value in case of error.
                        propertyReference.set(rowElement, oldValue);
                    }
                    final Exception error = tmpError;
                    final String message = (error == null)?
                            "Le champs "+getText()+" a été modifié avec succès"
                            : "Erreur pendant la mise à jour du champs "+getText();
                    final ImageView graphic = new ImageView(error == null ? SIRS.ICON_CHECK_CIRCLE : SIRS.ICON_EXCLAMATION_TRIANGLE);
                    final Label messageLabel = new Label(message, graphic);
                    if (error == null) {
                        showNotification(messageLabel);
                    } else {
                        final Hyperlink errorLink = new Hyperlink("Voir l'erreur");
                        errorLink.setOnMouseClicked(linkEvent -> GeotkFX.newExceptionDialog(message, error).show());
                        final HBox container = new HBox(5, messageLabel, errorLink);
                        container.setAlignment(Pos.CENTER);
                        container.setPadding(Insets.EMPTY);
                        showNotification(container);
                    }
                });
            }
        }

        public Reference getReference() {
            return ref;
        }
    }

    /**
     * Display input node into the notification popup. All previous content will
     * be removed from popup.
     * @param toShow The node to show in notification popup.
     */
    public void showNotification(final Node toShow) {
        if (toShow == null)
            showNotification(Collections.EMPTY_LIST);
        else
            showNotification(Collections.singletonList(toShow));
    }

    /**
     * Display input nodes into a popup stack pane. All previous content will
     * be removed from popup.
     * @param toShow nodes to display in notification popup.
     */
    public void showNotification(final List<Node> toShow) {
        if (toShow == null || toShow.isEmpty()) {
            notifier.getChildren().clear();
        } else {
            notifier.getChildren().setAll(toShow);
        }
        // transition allows to see a difference when two identic message are queried in line.
        final FadeTransition transition = new FadeTransition(new Duration(1000), notifier);
        transition.setFromValue(0);
        transition.setToValue(1);
        transition.play();
    }

    public class DuplicateColumn<T extends Element> extends TableColumn<T, T> {

        public DuplicateColumn() {
            super("Dupliquer");
            setSortable(false);
            setResizable(false);
            setPrefWidth(24);
            setMinWidth(24);
            setMaxWidth(24);
            setGraphic(new ImageView(GeotkFX.ICON_DUPLICATE));

            setCellValueFactory((Callback) DEFAULT_VALUE_FACTORY);

            if(pojoClass!=null && repo!=null){
                setCellFactory(new Callback<TableColumn<T, T>, TableCell<T, T>>() {

                    @Override
                    public TableCell call(TableColumn param) {
                        return new ButtonTableCell(
                                false, new ImageView(GeotkFX.ICON_DUPLICATE),
                                DEFAULT_VISIBLE_PREDICATE, (Object t) -> {
                                    if (pojoClass.isAssignableFrom(t.getClass())) {
                                        final Element newElement = ((T) t).copy();
                                        repo.add(newElement);
                                    }
                                    return t;
                                });
                    }
                });
            }
            else setVisible(false);
        }
    }

    /**
     * A column allowing to delete the {@link Element} of a row. Two modes possible :
     * - Concrete deletion, which remove the element from database
     * - unlink mode, which dereference element from current list and parent element.
     */
    public class DeleteColumn extends TableColumn<Element,Element>{

        public DeleteColumn() {
            super("Suppression");
            setSortable(false);
            setResizable(false);
            setPrefWidth(24);
            setMinWidth(24);
            setMaxWidth(24);
            setGraphic(new ImageView(GeotkFX.ICON_DELETE));

            final Tooltip deleteTooltip = new Tooltip("Supprimer l'élement");
            final Tooltip unlinkTooltip = new Tooltip("Dissocier l'élement");
            setCellValueFactory((Callback)DEFAULT_VALUE_FACTORY);
            setCellFactory((TableColumn<Element, Element> param) -> {
                final boolean realDelete = createNewProperty.get();
                final ButtonTableCell<Element, Element> button = new ButtonTableCell<>(false,
                        realDelete ? new ImageView(GeotkFX.ICON_DELETE) : new ImageView(GeotkFX.ICON_UNLINK),
                        DEFAULT_VISIBLE_PREDICATE,
                        (Element t) -> {
                            final Alert confirm;
                            if (realDelete) {
                                confirm = new Alert(Alert.AlertType.WARNING, "Vous allez supprimer DEFINITIVEMENT l'entrée de la base de données. Êtes-vous sûr ?", ButtonType.NO, ButtonType.YES);
                            } else {
                                confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer le lien ?", ButtonType.NO, ButtonType.YES);
                            }
                            confirm.setResizable(true);
                            final Optional<ButtonType> res = confirm.showAndWait();
                            if (res.isPresent() && ButtonType.YES.equals(res.get())) {
                                deletePojos(t);
                                return null;
                            } else {
                                return t;
                            }
                        });

                if (realDelete) {
                    button.setTooltip(deleteTooltip);
                } else {
                    button.setTooltip(unlinkTooltip);
                }
                return button;
            });
        }
    }

    /**
     * A column allowing to show the {@link Element} of a row on the map
     */
    public class ShowOnMapColumn extends TableColumn<Element,Element>{

        public ShowOnMapColumn() {
            super("Afficher sur la carte");
            setSortable(false);
            setResizable(false);
            setPrefWidth(24);
            setMinWidth(24);
            setMaxWidth(24);
            setGraphic(new ImageView(ICON_SHOWONMAP));

            final Tooltip tooltip = new Tooltip("Voir l'élément sur la carte");
            setCellValueFactory((Callback)DEFAULT_VALUE_FACTORY);
            setCellFactory((TableColumn<Element, Element> param) -> {
                final ButtonTableCell<Element, Element> button = new ButtonTableCell<>(false,
                        new ImageView(ICON_SHOWONMAP),
                        DEFAULT_VISIBLE_PREDICATE,
                        (Element t) -> {
                            final FXMapTab tab = session.getFrame().getMapTab();
                            tab.getMap().focusOnElement(t);
                            tab.show();
                            return t;
                        });
                button.setTooltip(tooltip);
                return button;
            });
        }
    }

    public static class EditColumn extends TableColumn {

        public EditColumn(Consumer editFct) {
            super("Edition");
            setSortable(false);
            setResizable(false);
            setPrefWidth(24);
            setMinWidth(24);
            setMaxWidth(24);
            setGraphic(new ImageView(SIRS.ICON_EDIT_BLACK));

            final Tooltip tooltip = new Tooltip("Ouvrir la fiche de l'élément");

            setCellValueFactory(DEFAULT_VALUE_FACTORY);

            setCellFactory(new Callback<TableColumn, TableCell>() {

                @Override
                public TableCell call(TableColumn param) {
                    ButtonTableCell button = new ButtonTableCell(
                            false, new ImageView(SIRS.ICON_EDIT_BLACK),
                            DEFAULT_VISIBLE_PREDICATE, (Object t) -> {
                                editFct.accept(t);
                                return t;
                            });
                    button.setTooltip(tooltip);
                    return button;
                }
            });
        }
    }


    /* Rechercher les objets dans un tronçon donné (sert uniquement si on ne
    crée pas les objets à l'ajout, mais si on cherche des objets préexisants.
    Cette propriété sert alors à limiter la recherche à un tronçon donné (de
    manière à ne relier entre eux que des "objets" du même tronçon.*/
    protected final StringProperty tronconSourceProperty = new SimpleStringProperty(null);

    public StringProperty tronconSourceProperty() {
        return tronconSourceProperty;
    }

    private class ChoiceStage extends PojoTableComboBoxChoiceStage<Element, Preview> {


        private final ObjectBinding<Element> elementBinding = new ObjectBinding<Element>() {

            {
                bind(comboBox.getSelectionModel().selectedItemProperty());
            }

            @Override
            protected Element computeValue() {
                if(comboBox.valueProperty()!=null){
                    final Preview preview = comboBox.valueProperty().get();
                    if(preview!=null){
                        return (Element) repo.get(preview.getDocId());
                    }
                }
                return null;
            }
        };

        private ChoiceStage(){
            super();
            setTitle("Choix de l'élément");

            if(tronconSourceProperty.get()==null){
                comboBox.setItems(SIRS.observableList(session.getPreviews().getByClass(pojoClass)).sorted());
            }
            else{
                comboBox.setItems(SIRS.observableList(session.getPreviews().getByClass(pojoClass)).sorted()
                        .filtered((Preview t) -> { return tronconSourceProperty.get().equals(t.getDocId());}));
            }

            retrievedElement.bind(elementBinding);
        }
    }
}
