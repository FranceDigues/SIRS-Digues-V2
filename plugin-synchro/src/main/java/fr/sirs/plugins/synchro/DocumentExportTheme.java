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
package fr.sirs.plugins.synchro;

import fr.sirs.plugins.synchro.ui.database.AttachmentUploadPane;
import fr.sirs.plugins.synchro.ui.mount.DocumentExportPane;
import fr.sirs.theme.ui.AbstractPluginsButtonTheme;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class DocumentExportTheme extends AbstractPluginsButtonTheme {

    private static final Image ICON = new Image(DocumentExportTheme.class.getResourceAsStream("documentExport.png"));

    final SynchroPlugin plugin;

    public DocumentExportTheme(final SynchroPlugin plugin) {
        super("Export de documents", "Interface permettant de transférer des documents depuis l'application de bureau vers l'application mobile.", ICON);
        this.plugin = plugin;
    }

    @Override
    public Parent createPane() {
        final BorderPane container = new BorderPane();
        container.setPadding(new Insets(20));
        container.setPrefWidth(600);
        container.setMaxWidth(800);
        initDatabase(container);
        return container;
    }

    private void initDatabase(final BorderPane container) {
        final Hyperlink changeMethod = new Hyperlink("Charger directement sur un disque dur");
        changeMethod.setOnAction(evt -> initUSB(container));
        final HBox hBox = new HBox(5, new Label("Changer de mode :"), changeMethod);
        hBox.setAlignment(Pos.CENTER_LEFT);

        container.setBottom(new VBox(5, new Separator(Orientation.HORIZONTAL), hBox));
        final ScrollPane scroll = new ScrollPane(new AttachmentUploadPane(plugin.getSession(), plugin.getExecutor()));
        scroll.setStyle("-fx-background-color:transparent;");
        scroll.setFitToWidth(true);
        container.setCenter(scroll);
    }

    private void initUSB(final BorderPane container) {
        final Hyperlink changeMethod = new Hyperlink("Envoyer dans la base de données");
        changeMethod.setOnAction(evt -> initDatabase(container));
        final HBox hBox = new HBox(5, new Label("Changer de mode :"), changeMethod);
        hBox.setAlignment(Pos.CENTER_LEFT);

        container.setBottom(new VBox(5, new Separator(Orientation.HORIZONTAL), hBox));
        final ScrollPane scroll = new ScrollPane(new DocumentExportPane());
        scroll.setStyle("-fx-background-color:transparent;");
        scroll.setFitToWidth(true);
        container.setCenter(scroll);
    }
}
