package fr.sirs;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import fr.sirs.core.Repository;
import fr.sirs.core.SirsCore;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.Element;
import fr.sirs.theme.ui.AbstractFXElementPane;
import fr.sirs.theme.ui.FXElementContainerPane;
import fr.sirs.util.SirsStringConverter;
import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ModifiableObservableListBase;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gui.javafx.util.ComboBoxCompletion;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Constants used for project.
 *
 * @author Johann Sorel
 */
public final class SIRS extends SirsCore {

    public static final CoordinateReferenceSystem CRS_WGS84 = CommonCRS.WGS84.normalizedGeographic();

    /** Cette géométrie sert de base pour tous les nouveaux troncons */
    public static final Geometry DEFAULT_TRONCON_GEOM_WGS84;
    static {
        DEFAULT_TRONCON_GEOM_WGS84 = new GeometryFactory().createLineString(new Coordinate[]{
            new Coordinate(0, 48),
            new Coordinate(5, 48)
        });
        JTS.setCRS(DEFAULT_TRONCON_GEOM_WGS84, CRS_WGS84);
    }

    public static final Image ICON = new Image(SirsCore.class.getResource("/fr/sirs/icon.png").toString());

    public static final Image ICON_ADD_WHITE    = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_PLUS,22,Color.WHITE),null);
    public static final Image ICON_COPY_WHITE   = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_COPY_ALIAS,22,Color.WHITE),null);
    public static final Image ICON_ADD_BLACK    = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_PLUS,16,Color.BLACK),null);
    public static final Image ICON_ARROW_RIGHT_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_ARROW_RIGHT,16,Color.BLACK),null);
    public static final Image ICON_CLOCK_WHITE  = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CLOCK_O,22,Color.WHITE),null);
    public static final Image ICON_SEARCH_WHITE       = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_SEARCH,22,Color.WHITE),null);
    public static final Image ICON_ARCHIVE_WHITE       = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_ARCHIVE,22,Color.WHITE),null);
    public static final Image ICON_TRASH_WHITE        = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_TRASH_O,22,Color.WHITE),null);
    public static final Image ICON_CROSSHAIR_BLACK= SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CROSSHAIRS,22,Color.BLACK),null);
    public static final Image ICON_CARET_UP_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CARET_UP,16,Color.BLACK),null);
    public static final Image ICON_CARET_DOWN_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CARET_DOWN,16,Color.BLACK),null);
    public static final Image ICON_CARET_LEFT = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CARET_LEFT,22,Color.WHITE),null);
    public static final Image ICON_CARET_RIGHT = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CARET_RIGHT,22,Color.WHITE),null);
    public static final Image ICON_FILE_WHITE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_FILE,22,Color.WHITE),null);
    public static final Image ICON_FILE_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_FILE,22,Color.BLACK),null);
    public static final Image ICON_TABLE_WHITE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_TABLE,22,Color.WHITE),null);
    public static final Image ICON_UNDO_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_UNDO, 22, Color.BLACK),null);
    public static final Image ICON_INFO_BLACK_16 = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_INFO, 16, Color.BLACK),null);
    public static final Image ICON_INFO_CIRCLE_BLACK_16 = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_INFO_CIRCLE, 16, Color.BLACK),null);
    public static final Image ICON_EYE_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_EYE, 16, Color.BLACK),null);
    public static final Image ICON_COMPASS_WHITE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_COMPASS, 22, Color.WHITE),null);
    public static final Image ICON_EDIT_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_FILE_O, 16, Color.BLACK),null);
    public static final Image ICON_PRINT_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_PRINT, 16, Color.BLACK),null);
    public static final Image ICON_ROTATE_LEFT_ALIAS = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_ROTATE_LEFT_ALIAS, 16, Color.BLACK),null);
    public static final Image ICON_IMPORT_WHITE  = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_DOWNLOAD,22,Color.WHITE),null);
    public static final Image ICON_EXPORT_WHITE  = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_SAVE_ALIAS,22,Color.WHITE),null);
    public static final Image ICON_VIEWOTHER_WHITE  = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_BARS,22,Color.WHITE),null);
    public static final Image ICON_FILTER_WHITE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_FILTER,22,Color.WHITE),null);

    public static final String COLOR_INVALID_ICON = "#aa0000";
    public static final Image ICON_EXCLAMATION_CIRCLE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_EXCLAMATION_CIRCLE, 16, Color.decode(COLOR_INVALID_ICON)),null);
    public static final String COLOR_VALID_ICON = "#00aa00";
    public static final Image ICON_CHECK_CIRCLE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CHECK_CIRCLE, 16, Color.decode(COLOR_VALID_ICON)),null);
    public static final String COLOR_WARNING_ICON = "#EEB422";
    public static final Image ICON_EXCLAMATION_TRIANGLE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_EXCLAMATION_TRIANGLE, 16, Color.decode(COLOR_WARNING_ICON)),null);
    public static final Image ICON_EXCLAMATION_TRIANGLE_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_EXCLAMATION_TRIANGLE, 16, Color.BLACK),null);

    public static final Image ICON_CHECK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_CHECK, 16, Color.decode(COLOR_VALID_ICON)),null);

    public static final Image ICON_LINK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_EXTERNAL_LINK, 16, Color.BLACK),null);
    public static final Image ICON_WARNING = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_EXCLAMATION_TRIANGLE, 16, Color.BLACK),null);
    public static final Image ICON_REFRESH_WHITE = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_REFRESH,22,Color.WHITE),null);

    public static final String CSS_PATH = "/fr/sirs/theme.css";
    public static final String CSS_PATH_CALENDAR = "/fr/sirs/calendar.css";

    private static AbstractRestartableStage LAUNCHER;
    public static void setLauncher(AbstractRestartableStage currentWindow) {
        LAUNCHER=currentWindow;
    }
    public static AbstractRestartableStage getLauncher() {
        return LAUNCHER;
    }

    public static Loader LOADER;

    private SIRS(){};

    public static void loadFXML(Parent candidate) {
        final Class modelClass = null;
        loadFXML(candidate, modelClass);
    }

    /**
     * Load FXML document matching input controller. If a model class is given,
     * we'll try to load a bundle for text internationalization.
     * @param candidate The controller object to get FXMl for.
     * @param modelClass A class which will be used for bundle loading.
     */
    public static void loadFXML(Parent candidate, final Class modelClass) {
        ResourceBundle bundle = null;
        if (modelClass != null) {
            try{
                bundle = ResourceBundle.getBundle(modelClass.getName(), Locale.FRENCH,
                        Thread.currentThread().getContextClassLoader());
            }catch(MissingResourceException ex){
                LOGGER.log(Level.INFO, "Missing bundle for : {0}", modelClass.getName());
            }
        }
        loadFXML(candidate, bundle);
    }

    public static void loadFXML(Parent candidate, final ResourceBundle bundle) {
        loadFXML(candidate, candidate.getClass(), bundle);
    }

    public static void loadFXML(Parent candidate, final Class fxmlClass, final ResourceBundle bundle) {
        ArgumentChecks.ensureNonNull("JavaFX controller object", candidate);
        final String fxmlpath = "/"+fxmlClass.getName().replace('.', '/')+".fxml";
        final URL resource = fxmlClass.getResource(fxmlpath);
        if (resource == null) {
            throw new RuntimeException("No FXMl document can be found for path : "+fxmlpath);
        }
        final FXMLLoader loader = new FXMLLoader(resource);
        loader.setController(candidate);
        loader.setRoot(candidate);
        //in special environement like osgi or other, we must use the proper class loaders
        //not necessarly the one who loaded the FXMLLoader class
        loader.setClassLoader(fxmlClass.getClassLoader());

        if(bundle!=null) loader.setResources(bundle);

        fxRunAndWait(() -> {
            try {
                loader.load();
            } catch (IOException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        });
    }

    /**
     * Reconstruit une liste d'éléments depuis la liste en entrée et le {@link Repository} donné.
     * Si la liste en paramètre est nulle ou vide, une liste vide est renvoyée.
     * Si elle contient des éléments, elle est renvoyée telle quel.
     * Si c'est une liste d'ID, on construit une liste des élements correspondants.
     *
     * @param sourceList La liste depuis laquelle on doit reconstruire la liste des éléments.
     * @param repo Le repository servant à retrouver les éléments depuis leur ID.
     * @return Une liste d'éléments. Peut être vide, mais jamais nulle.
     */
    public static ObservableList<Element> toElementList(final List sourceList, final AbstractSIRSRepository repo) {
        if (sourceList == null) {
            return FXCollections.observableArrayList();

        } else if (!sourceList.isEmpty() && sourceList.get(0) instanceof Element) {
            if (sourceList instanceof ModifiableObservableListBase) {
                return (ObservableList) sourceList;
            } else {
                return FXCollections.observableArrayList(sourceList);
            }
        } else if (repo == null) {
            return FXCollections.observableArrayList();
        } else {
            // Version de récupération "Bulk" : récupération de l'ensemble des documents dont les IDs sont spécifiés en une requête unique.
//            ViewQuery q = new ViewQuery()
//                      .allDocs()
//                      .includeDocs(true)
//                      .keys(sourceList);
//            return FXCollections.observableArrayList(Injector.getSession().getConnector().queryView(q, repo.getModelClass()));

            // Version de récupération "cache" : fes documents sont récupérés un par un à moins qu'ils ne soient dans le cache du repository
            // Restauration de cette version, car la duplication "Bulk" ne passe pas par le repository et duplique donc les instances déjà dans son cache.
            final ObservableList resultList = FXCollections.observableArrayList();
            final Iterator<String> it = sourceList.iterator();
            while (it.hasNext()) {
                resultList.add(repo.get(it.next()));
            }
            return resultList;
        }
    }

    /**
     * Tente de trouver un éditeur d'élément compatible avec l'objet passé en paramètre.
     * @param pojo
     * @return Un éditeur pour l'objet d'entrée, ou null si aucun ne peut être
     * trouvé. L'éditeur aura déjà été initialisé avec l'objet en paramètre.
     */
    public static AbstractFXElementPane generateEditionPane(final Element pojo) {
        return new FXElementContainerPane((Element) pojo);
    }

    /**
     *
     * @param element
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static AbstractFXElementPane createFXPaneForElement(final Element element)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // Choose the pane adapted to the specific structure.
        final String className = "fr.sirs.theme.ui.FX" + element.getClass().getSimpleName() + "Pane";
        final Class controllerClass = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        final Constructor cstr = controllerClass.getConstructor(element.getClass());
        return (AbstractFXElementPane) cstr.newInstance(element);
    }

    /**
     * initialize ComboBox items using input list. We also activate completion.
     * @param comboBox The combo box to set value on.
     * @param items The items we want into the ComboBox.
     * @param current the element to select by default.
     */
    public static void initCombo(final ComboBox comboBox, final ObservableList items, final Object current) {
        final SirsStringConverter converter = new SirsStringConverter();
        comboBox.setConverter(converter);
        if (items instanceof SortedList) {
            comboBox.setItems(items);
        } else {
            comboBox.setItems(items.sorted((o1, o2) -> converter.toString(o1).compareTo(converter.toString(o2))));
        }
        comboBox.setEditable(true);
        comboBox.getSelectionModel().select(current);
        ComboBoxCompletion.autocomplete(comboBox);
    }

    /**
     * Convert byte number given in parameter in a human readable string. It tries
     * to fit the best unit. Ex : if you've got a number higher than a thousand,
     * input byte number will be expressed in kB. If you've got more than a million,
     * you've got it as MB
     * @param byteNumber Byte quantity to display
     * @return A string displaying byte number converted in fitting unit, along with unit symbol.
     */
    public static String toReadableSize(final long byteNumber) {
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2);
        if (byteNumber < 0) {
            return "inconnu";
        } else if (byteNumber < 1e3) {
            return "" + byteNumber + " octets";
        } else if (byteNumber < 1e6) {
            return "" + format.format(byteNumber / 1e3) + " ko";
        } else if (byteNumber < 1e9) {
            return "" + format.format(byteNumber / 1e6) + " Mo";
        } else if (byteNumber < 1e12) {
            return "" + format.format(byteNumber / 1e9) + " Go";
        } else {
            return "" + (byteNumber / 1e12) + " To";
        }
    }

    public static ObservableList view(ObservableList ... listes){
        return new ViewList(listes);
    }

    private static final class ViewList extends ObservableListBase implements ListChangeListener{

        private final ObservableList[] listes;

        public ViewList(ObservableList ... listes) {
            this.listes = listes;

            for(ObservableList lst : listes){
                lst.addListener(this);
            }
        }

        @Override
        public Object get(int index) {
            for(int i=0;i<listes.length;i++){
                int size = listes[i].size();
                if(size<=index){
                    index -= size;
                }else{
                    return listes[i].get(index);
                }
            }
            throw new ArrayIndexOutOfBoundsException(index);
        }

        @Override
        public int size() {
            int size = 0;
            for (ObservableList liste : listes) {
                size += liste.size();
            }
            return size;
        }

        private int getOffset(ObservableList lst){
            int size = 0;
            for (ObservableList liste : listes) {
                if(lst==liste) break;
                size += liste.size();
            }
            return size;
        }

        @Override
        public void onChanged(ListChangeListener.Change c) {
            final int offset = getOffset(c.getList());

            beginChange();
            while (c.next()) {
                if (c.wasPermutated()) {
                    //permutate
                    beginChange();
                    final int[] perms = new int[c.getTo()-c.getFrom()];
                    for (int i = c.getFrom(),k=0; i < c.getTo(); ++i,k++) {
                        perms[k] = c.getPermutation(i);
                    }
                    nextPermutation(offset+c.getFrom(), offset+c.getTo(), perms);
                    endChange();
                } else if (c.wasUpdated()) {
                    //update item
                    beginChange();
                    nextUpdate(offset+c.getFrom());
                    endChange();
                } else {
                    beginChange();
                    if(c.wasUpdated()){
                        throw new UnsupportedOperationException("Update events not supported.");
                    }else if(c.wasAdded()){
                        nextAdd(offset+c.getFrom(), offset+c.getTo());
                    }else if(c.wasRemoved()){
                        nextReplace(offset+c.getFrom(), offset+c.getTo(), c.getRemoved());
                    }
                    endChange();
                }
            }
            endChange();
        }

    }

    /**
     * Run given task in FX application thread (immediately if we're already in it),
     * and wait for its result before returning.
     *
     * @param <T> Return type of the input task.
     * @param toRun Task to run in JavaFX thread.
     * @return Result of the input task.
     */
    public static <T> T fxRunAndWait(final Callable<T> toRun) {
        return fxRunAndWait(new TaskManager.MockTask<>(toRun));
    }

    /**
     *
     * @param toRun The task to run.
     */
    public static void fxRunAndWait(final Runnable toRun) {
        fxRunAndWait(new TaskManager.MockTask(toRun));
    }

    /**
     * Run given task in FX application thread (immediately if we're already in it),
     * and wait for its result before returning.
     *
     * @param <T> Return type of the input task.
     * @param toRun Task to run in JavaFX thread.
     * @return Result of the input task.
     */
    public static <T> T fxRunAndWait(final Task<T> toRun) {
        return fxRun(true, toRun);
    }

    /**
     * Run given task in FX application thread (immediately if we're already in it).
     * According to input boolean, we will return immediately or wait for the task to
     * be over.
     * @param wait True if we must wait for the task to end before returning, false
     * to return immediately after submission.
     * @param toRun The task to run into JavaFX application thread.
     */
    public static void fxRun(final boolean wait, final Runnable toRun) {
        fxRun(wait, new TaskManager.MockTask(toRun));
    }

    /**
     * Run given task in FX application thread (immediately if we're already in it).
     * According to input boolean, we will return immediately or wait for the task to
     * be over.
     * @param <T> Return type of input task.
     * @param wait True if we must wait for the task to end before returning, false
     * to return immediately after submission.
     * @return The task return value if we must wait, or we're in platform thread. Otherwise null.
     * @param toRun The task to run into JavaFX application thread.
     */
    public static <T> T fxRun(final boolean wait, final Callable<T> toRun) {
        return fxRun(wait, new TaskManager.MockTask<>(toRun));
    }

    /**
     * Run given task in FX application thread (immediately if we're already in it).
     * According to input boolean, we will return immediately or wait for the task to
     * be over.
     * @param <T> Return type of input task.
     * @param wait True if we must wait for the task to end before returning, false
     * to return immediately after submission.
     * @return The task return value if we must wait, or we're in platform thread. Otherwise null.
     * @param toRun The task to run into JavaFX application thread.
     */
    public static <T> T fxRun(final boolean wait, final Task<T> toRun) {
        if (Platform.isFxApplicationThread()) {
            toRun.run();
            return toRun.getValue();
        } else {
            Platform.runLater(toRun);
            if (wait) {
                try {
                    return toRun.get();
                } catch (RuntimeException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new SirsCoreRuntimeException(ex);
                }
            } else return null;
        }
    }

    /**
     * Try to open given file on system.
     * @param toOpen The file open on underlying system.
     * @return True if we succeeded opening file on system, false otherwise.
     */
    public static boolean openFile(final Path toOpen) {
        return openFile(toOpen.toAbsolutePath().toFile());
    }

    /**
     * Try to open given file on system.
     * @param toOpen The file open on underlying system.
     * @return True if we succeeded opening file on system, false otherwise.
     */
    public static boolean openFile(final File toOpen) {
        final Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.EDIT)) {
            TaskManager.INSTANCE.submit("Ouverture d'un fichier", () -> {desktop.edit(toOpen); return true;});
        } else if (desktop.isSupported(Desktop.Action.OPEN)) {
            TaskManager.INSTANCE.submit("Ouverture d'un fichier", () -> {desktop.open(toOpen); return true;});
        } else {
            return false;
        }
        return true;
    }
}
