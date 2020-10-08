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
package fr.sirs;


import static fr.sirs.SIRS.hexaMD5;
import fr.sirs.core.SirsCore;
import fr.sirs.core.component.DatabaseRegistry;
import fr.sirs.core.component.UtilisateurRepository;
import fr.sirs.core.model.Role;
import fr.sirs.core.model.Utilisateur;
import fr.sirs.core.plugins.PluginLoader;
import fr.sirs.util.SirsStringConverter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.DialogEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import org.apache.sis.util.ArgumentChecks;
import org.controlsfx.dialog.ExceptionDialog;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.image.jai.Registry;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.lang.Setup;
import org.geotoolkit.sld.xml.JAXBSLDUtilities;
import org.geotoolkit.sld.xml.StyleXmlIO;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class Loader extends Application {

    private final Stage splashStage;
    private final String databaseName;
    private final DatabaseRegistry localRegistry;

    public Loader(String databaseName, final DatabaseRegistry localRegistry) {
        ArgumentChecks.ensureNonEmpty("Database name", databaseName);
        ArgumentChecks.ensureNonNull("Database registry", localRegistry);

        this.databaseName = databaseName;
        // Initialize splash screen
        splashStage = new Stage(StageStyle.TRANSPARENT);
        splashStage.setTitle("SIRS-Digues V2");
        splashStage.getIcons().add(SIRS.ICON);
        splashStage.initStyle(StageStyle.TRANSPARENT);
        this.localRegistry = localRegistry;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        if(primaryStage!=null) primaryStage.getIcons().add(SIRS.ICON);
        // perform initialization and plugin loading tasks
        final Task initTask = new LoadingTask();
        showLoadingStage(initTask);
        TaskManager.INSTANCE.submit(initTask);
    }

    public void showSplashStage() {
        splashStage.show();
    }

    /**
     * Display splash screen.
     *
     * @param task
     * @throws IOException
     */
    private void showLoadingStage(Task task) throws IOException {

        final FXMLLoader loader = new FXMLLoader(getClass().getResource("/fr/sirs/FXSplashscreen.fxml"));
        final GridPane root = loader.load();
        final FXSplashscreen controller = loader.getController();
        controller.uiCancel.setVisible(false);
        controller.uiProgressLabel.textProperty().bind(task.messageProperty());
        controller.uiProgressBar.progressProperty().bind(task.progressProperty());
        controller.analyzeDatabase(localRegistry, databaseName);

        final Scene scene = new Scene(root);
        scene.getStylesheets().add("/fr/sirs/splashscreen.css");
        scene.setFill(new Color(0, 0, 0, 0));

        splashStage.setScene(scene);
        splashStage.show();

        task.stateProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (newValue == Worker.State.SUCCEEDED) {
                    splashStage.toFront();
                    controller.uiLoadingPane.setVisible(false);
                    controller.uiLoginPane.setVisible(true);
                } else if (newValue == Worker.State.CANCELLED) {
                    controller.uiProgressLabel.getStyleClass().remove("label");
                    controller.uiProgressLabel.getStyleClass().add("label-error");
                    controller.uiCancel.setVisible(true);
                }
            }
        });

        controller.uiPassword.setOnAction((ActionEvent e)-> controller.uiConnexion.fire());
        controller.uiConnexion.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                final Session session = Injector.getBean(Session.class);
                final UtilisateurRepository utilisateurRepository = (UtilisateurRepository) session.getRepositoryForClass(Utilisateur.class);

                controller.uiLogInfo.setText("Recherche…");
                final List<Utilisateur> candidateUsers = utilisateurRepository.getByLogin(controller.uiLogin.getText());

                if (candidateUsers.isEmpty()) {
                    controller.uiLogInfo.setText("Identifiants erronés.");
                    controller.uiPassword.setText("");
                    return;
                }

                // Database passwords are encrypted, so we encrypt input password to compare both.
                final String passwordText = controller.uiPassword.getText();
                final String encryptedPassword;
                if (passwordText == null || passwordText.isEmpty()) {
                    encryptedPassword = null;
                } else {
                    encryptedPassword = hexaMD5(passwordText);
                }

                Utilisateur user = null;
                for (final Utilisateur candidate : candidateUsers) {
                    if (Objects.equals(encryptedPassword, candidate.getPassword())) {
                        user = candidate;
                        break;
                    }
                }

                if (user == null) {
                    controller.uiLogInfo.setText("Identifiants erronés.");
                    controller.uiPassword.setText("");

                } else {
                    controller.uiConnexion.setDisable(true);
                    session.setUtilisateur(user);
                    controller.uiLogInfo.setText("Identifiants valides.");
                    final FadeTransition fadeSplash = new FadeTransition(Duration.seconds(1.2), root);
                    fadeSplash.setFromValue(1.0);
                    fadeSplash.setToValue(0.0);
                    fadeSplash.setOnFinished(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent actionEvent) {
                            splashStage.hide();
                            root.setOpacity(1.0);
                            try {
                                createMainStage();
                                controller.uiConnexion.setDisable(false);
                            } catch (Throwable ex) {
                                try {
                                    SIRS.LOGGER.log(Level.WARNING, "Erreur inattendue lors de l'initialisation du panneau principal.", ex);
                                    ExceptionDialog exDialog = GeotkFX.newExceptionDialog("L'application a rencontré une erreur inattendue et doit fermer.", ex);
                                    exDialog.setOnHidden((DialogEvent de) -> System.exit(1));
                                    exDialog.show();

                                } catch (Throwable e) {
                                    SIRS.LOGGER.log(Level.WARNING, "Cannot show error dialog to user", e);
                                    System.exit(1);
                                }
                            }
                        }
                    });
                    fadeSplash.play();
                }
            }
        });
    }

    /**
     * Display the main frame.
     */
    private static synchronized void createMainStage() throws IOException {
        final Session session = Injector.getSession();
        final FXMainFrame frame = new FXMainFrame();
        session.setFrame(frame);
        Scene mainScene = new Scene(frame);

        final Stage mainStage = new Stage();
        mainStage.getIcons().add(SIRS.ICON);
        mainStage.titleProperty().bind(Bindings.createStringBinding(() -> {
            StringBuilder builder = new StringBuilder("SIRS-Digues 2");
            final String version = SIRS.getVersion();
            if (version != null && !version.isEmpty()) {
                builder.append(" v").append(version);
            }
            builder.append(" - Utilisateur ");
            Utilisateur user = session.utilisateurProperty().get();
            if (user == null || user.equals(UtilisateurRepository.GUEST_USER)) {
                builder.append("invité");
            } else {
                builder.append(user.getLogin());
            }
            builder.append(" (rôle ");
            if (user == null || user.getRole() == null) {
                builder.append(new SirsStringConverter().toString(Role.GUEST));
            } else {
                builder.append(new SirsStringConverter().toString(user.getRole()));
            }
            builder.append(") sur la base ")
                    .append(session.getConnector().getDatabaseName());
            return builder.toString();
        }, session.utilisateurProperty()));

        mainStage.setScene(mainScene);
        mainStage.setMinWidth(800);
        mainStage.setMinHeight(600);
        mainStage.setMaximized(true);
        mainStage.setOnCloseRequest((WindowEvent event) -> {
            System.exit(0);
        });

        mainStage.show();
        frame.getMapTab().show();

        // Display alerts on startup.
        frame.showAlertPopup();
    }

    private final class LoadingTask extends Task {

        @Override
        protected Object call() throws InterruptedException {
            try {
                updateMessage("Recherche des plugins");
                int inc = 0;

                final ClassLoader scl = ClassLoader.getSystemClassLoader();
                if (scl instanceof PluginLoader) {
                    ((PluginLoader) scl).loadPlugins();
                }

                final Plugin[] plugins = Plugins.getPlugins();
                final int total = 9 + plugins.length;

                // EPSG DATABASE ///////////////////////////////////////////////
                updateProgress(inc++, total);
                updateMessage("Creation de la base EPSG...");
                // try to create it, won't do anything if already exist
                SirsCore.initEpsgDB();

                // IMAGE ///////////////////////////////////////////////////////
                updateProgress(inc++, total);
                updateMessage("Chargement des lecteurs d'images...");
                Registry.setDefaultCodecPreferences();
                // global initialization
                ImageIO.scanForPlugins();

                // GEOTK ///////////////////////////////////////////////////////
                updateProgress(inc++, total);
                updateMessage("Chargement de Geotoolkit...");
                // work in lazy mode, do your best for lenient datum shift
                Hints.putSystemDefault(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);
                // Geotoolkit startup. We specify that we don't want to use Java preferences,
                // because it does not work on most systems anyway.
                final Properties noJavaPrefs = new Properties();
                noJavaPrefs.put("platform", "server");
                Setup.initialize(noJavaPrefs);

                // DATABASE ////////////////////////////////////////////////////
                updateProgress(inc++, total);
                updateMessage("Chargement des pilotes pour base de données...");
                // loading drivers, some plugin systems requiere this call ,
                // like netbeans RCP
                Class.forName("org.postgresql.Driver").newInstance();
                Class.forName("org.h2.Driver").newInstance();

                // JAXB ////////////////////////////////////////////////////////
                updateProgress(inc++, total);
                updateMessage("Chargement des parseurs XML/JSON...");
                // force loading marshallers
                JAXBSLDUtilities.getMarshallerPoolSLD110();
                JAXBSLDUtilities.getMarshallerPoolSLD100();
                final StyleXmlIO io = new StyleXmlIO();

                // LOAD SIRS DATABASE //////////////////////////////////////////
                updateProgress(inc++, total);
                updateMessage("Chargement et création des index ...");
                localRegistry.connectToSirsDatabase(databaseName, true, true, true);

                // LOAD PLUGINS ////////////////////////////////////////////////
                for (Plugin plugin : plugins) {
                    updateProgress(inc++, total);
                    try {
                        updateMessage("Chargement du plugin ".concat(plugin.getLoadingMessage().getValue()));
                        plugin.load();
                    } catch (Exception e) { // If we fail loading plugin, we just deactivate it.
                        final String errorMsg = "Le chargement du plugin "+plugin.getTitle()+" a échoué. Il sera désactivé.";
                        updateMessage(errorMsg);
                        SIRS.LOGGER.log(Level.WARNING, errorMsg, e);
                        // Send error notification in main window
                        TaskManager.INSTANCE.submit("Chargement d'un plugin", (Callable) () -> {throw e;});
                    }
                }

                // MAP INITIALISATION //////////////////////////////////////////
                //Affiche le contexte carto et le déplace à la date du jour
                updateProgress(inc++, total);
                updateMessage("Initialisation de la carte");
                Injector.getSession().getMapContext().getAreaOfInterest();

                // VÉRIFICATION DES RÉFÉRENCES
                updateProgress(inc++, total);
                updateMessage("Synchronisation des listes de références");
                Injector.getSession().getTaskManager().submit(Injector.getSession().getReferenceChecker());

                // COPIE LOCALE DES REQUÊTES PRÉPROGRAMMÉES
                updateProgress(inc++, total);
                updateMessage("Copie des requêtes préprogrammées");
                Injector.getSession().getTaskManager().submit(Injector.getSession().getQueryChecker());

                // OVER
                updateProgress(total, total);
                updateMessage("Chargement terminé.");
                Thread.sleep(400);
            } catch (Throwable ex) {
                updateProgress(-1, -1);
                updateMessage("Erreur inattendue : "
                        + ex.getLocalizedMessage());
                SIRS.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                cancel();
            }
            return null;
        }
    }
}
