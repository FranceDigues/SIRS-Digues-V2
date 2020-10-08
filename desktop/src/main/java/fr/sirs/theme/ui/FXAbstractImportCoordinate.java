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
import static fr.sirs.SIRS.CRS_WGS84;
import fr.sirs.util.SirsStringConverter;
import java.io.File;
import java.util.Collection;
import java.util.prefs.Preferences;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.gui.javafx.layer.FXFeatureTable;
import org.opengis.feature.PropertyType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class FXAbstractImportCoordinate extends BorderPane {

    @FXML protected TextField uiPath;
    @FXML protected TextField uiSeparator;

    @FXML protected ComboBox<CoordinateReferenceSystem> uiCRS;
    @FXML protected FXFeatureTable uiTable;

    @FXML protected GridPane uiPaneConfig;
    @FXML protected GridPane uiPaneImport;

    protected FeatureStore store;

    final SirsStringConverter stringConverter = new SirsStringConverter();

    public FXAbstractImportCoordinate() {
        SIRS.loadFXML(this);

        uiCRS.setItems(FXCollections.observableArrayList(Injector.getSession().getProjection(), CRS_WGS84));
        uiCRS.setConverter(stringConverter);
        uiCRS.getSelectionModel().clearAndSelect(0);

        uiPaneConfig.setDisable(true);
        uiTable.setEditable(false);
        uiTable.setLoadAll(true);

    }

    @FXML
    protected void openFileChooser(ActionEvent event) {
        final FileChooser fileChooser = new FileChooser();
        final File prevPath = getPreviousPath();
        if (prevPath != null) {
            fileChooser.setInitialDirectory(prevPath);
        }
        final File file = fileChooser.showOpenDialog(getScene().getWindow());
        if(file!=null){
            setPreviousPath(file.getParentFile());
            uiPath.setText(file.getAbsolutePath());
        }
    }

    private static File getPreviousPath() {
        final Preferences prefs = Preferences.userNodeForPackage(FXAbstractImportCoordinate.class);
        final String str = prefs.get("path", null);
        if(str!=null){
            final File file = new File(str);
            if(file.isDirectory()){
                return file;
            }
        }
        return null;
    }

    private static void setPreviousPath(final File path) {
        final Preferences prefs = Preferences.userNodeForPackage(FXAbstractImportCoordinate.class);
        prefs.put("path", path.getAbsolutePath());
    }

    protected ObservableList<PropertyType> getPropertiesFromFeatures(final FeatureCollection col) {
        return FXCollections
                .observableArrayList((Collection<PropertyType>) col.getFeatureType().getProperties(true))
                .sorted((o1, o2) -> o1.getName().compareTo(o2.getName()));
    }
}
