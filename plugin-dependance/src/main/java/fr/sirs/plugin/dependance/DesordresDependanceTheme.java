package fr.sirs.plugin.dependance;

import fr.sirs.Injector;
import fr.sirs.core.model.DesordreDependance;
import fr.sirs.theme.ui.AbstractPluginsButtonTheme;
import fr.sirs.theme.ui.PojoTable;
import fr.sirs.util.SimpleFXEditMode;
import javafx.scene.Parent;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Panneau regroupant les désordres pour les dépendances.
 *
 * @author Cédric Briançon (Geomatys)
 */
public final class DesordresDependanceTheme extends AbstractPluginsButtonTheme {
    private static final Image BUTTON_IMAGE = new Image(
            DesordresDependanceTheme.class.getResourceAsStream("images/desordre.png"));

    public DesordresDependanceTheme() {
        super("Désordres", "Désordres", BUTTON_IMAGE);
    }

    /**
     * Création du panneau principal de ce thème qui regroupera tous les éléments.
     *
     * @return Le panneau généré pour ce thème.
     */
    @Override
    public Parent createPane() {
        // Gestion du bouton consultation / édition pour la pojo table
        final Separator separator = new Separator();
        separator.setVisible(false);
        final SimpleFXEditMode editMode = new SimpleFXEditMode();
        final HBox topPane = new HBox(separator, editMode);
        HBox.setHgrow(separator, Priority.ALWAYS);

        final PojoTable dependancesTable = new PojoTable(Injector.getSession().getRepositoryForClass(DesordreDependance.class), "Liste des désordres");
        dependancesTable.editableProperty().bind(editMode.editionState());
        return new BorderPane(dependancesTable, topPane, null, null, null);
    }
}