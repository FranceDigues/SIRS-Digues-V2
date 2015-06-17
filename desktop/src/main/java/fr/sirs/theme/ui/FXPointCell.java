
package fr.sirs.theme.ui;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gui.javafx.util.FXTableCell;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXPointCell<S> extends FXTableCell<S, Object> {

    private static final NumberFormat FORMAT = new DecimalFormat("#0.00");

    private final Spinner x = new Spinner(new SpinnerValueFactory.DoubleSpinnerValueFactory(Double.MIN_VALUE, Double.MAX_EXPONENT));
    private final Spinner y = new Spinner(new SpinnerValueFactory.DoubleSpinnerValueFactory(Double.MIN_VALUE, Double.MAX_EXPONENT));
    private final VBox vbox = new VBox(x,y);

    private CoordinateReferenceSystem crs = null;

    public FXPointCell() {
        setGraphic(vbox);
        setAlignment(Pos.CENTER);
        setContentDisplay(ContentDisplay.CENTER);
    }

    @Override
    public void terminateEdit() {
        final Point pt = GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(
                ((Number)x.getValue()).doubleValue(),
                ((Number)y.getValue()).doubleValue()));
        if(crs!=null) JTS.setCRS(pt, crs);
        commitEdit(pt);
    }

    @Override
    public void startEdit() {
        Point value = (Point) getItem();
        if(value == null) value = GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(0, 0));

        x.getValueFactory().setValue(value.getCoordinate().x);
        y.getValueFactory().setValue(value.getCoordinate().y);
        super.startEdit();
        setText(null);
        setGraphic(vbox);
        x.requestFocus();
    }

    @Override
    public void commitEdit(Object newValue) {
        itemProperty().set(newValue);
        super.commitEdit(newValue);
        updateItem(newValue, false);
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        updateItem(getItem(), false);
    }

    @Override
    protected void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);
        setText(null);
        setGraphic(null);
        if (item instanceof Point) {
            final Point pt = (Point) item;
            try {
                crs = JTS.findCoordinateReferenceSystem(pt);
            } catch (FactoryException ex) {
                Logger.getLogger(FXPointCell.class.getName()).log(Level.INFO, null, ex);
            }
            setText(FORMAT.format(pt.getCoordinate().x)+" / "+FORMAT.format(pt.getCoordinate().y));
        }
    }

}
