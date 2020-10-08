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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import fr.sirs.Injector;
import fr.sirs.core.model.OuvrageVoirieDependance;
import fr.sirs.util.SIRSAreaComputer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.geotoolkit.display.MeasureUtilities;

import java.text.NumberFormat;
import org.apache.sis.measure.Units;

/**
 *
 * @author Olivier Nouguier (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class FXOuvrageVoirieDependancePane extends FXOuvrageVoirieDependancePaneStub {

    @FXML FXPositionDependancePane uiPosition;

    @FXML GridPane uiGridAttributes;

    private final Label lblGeomSize = new Label();
    private final Label geomSize = new Label();

    /**
     * Constructor. Initialize part of the UI which will not require update when element edited change.
     */
    private FXOuvrageVoirieDependancePane() {
        super();
		/*
		 * Disabling rules.
		 */
        uiPosition.disableFieldsProperty().bind(disableFieldsProperty());

        uiPosition.dependanceProperty().bind(elementProperty);

        uiGridAttributes.add(lblGeomSize, 2, 0);
        uiGridAttributes.add(geomSize, 3, 0);
    }

    public FXOuvrageVoirieDependancePane(final OuvrageVoirieDependance ouvrageVoirieDependance){
        this();
        this.elementProperty().set(ouvrageVoirieDependance);
    }

    private void setGeometrySize(final Geometry geometry) {
        if (geometry != null) {
            if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
                lblGeomSize.setText("Surface");
                geomSize.setText(NumberFormat.getNumberInstance().format(
                        SIRSAreaComputer.calculateArea(geometry, Injector.getSession().getProjection(), Units.SQUARE_METRE)) +" m2");
            } else {
                lblGeomSize.setText("Longueur");
                geomSize.setText(NumberFormat.getNumberInstance().format(
                        MeasureUtilities.calculateLenght(geometry,
                                Injector.getSession().getProjection(), Units.METRE)) +" m");
            }
        }
    }

    @Override
    protected void initFields(ObservableValue<? extends OuvrageVoirieDependance> observableElement, OuvrageVoirieDependance oldElement, OuvrageVoirieDependance newElement) {
        super.initFields(observableElement, oldElement, newElement);

        final Geometry geometry = elementProperty.get().getGeometry();
        setGeometrySize(geometry);
        this.elementProperty.get().geometryProperty().addListener(new ChangeListener<Geometry>() {
            @Override
            public void changed(ObservableValue<? extends Geometry> observable, Geometry oldValue, Geometry newValue) {
                setGeometrySize(newValue);
            }
        });
    }
}
