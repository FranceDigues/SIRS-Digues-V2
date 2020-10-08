
package fr.sirs;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import java.util.logging.Level;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.filter.function.AbstractFunction;

/**
 * Try to convert two Points feature's parameters in its {@linkplain Point center}.
 *
 *
 * @author Matthieu Bastianelli (Geomatys)
 * @throw {@link IllegalArgumentException} if one of the parameter is missing.
 * @throw {@link RuntimeException} if fail to convert the 2nd point in the 1st point CRS.
 */
public final class PointsToCenter extends AbstractFunction {

    private static final String NAME = "PointsToCenter";

    private final PointsToLine pointsToLine;

    /**
     *
     * @param pointToLine : {@link PointsToLine} expression trying to converts
     *                      the 2 Points feature's parameters in a line.
     */
    public PointsToCenter(PointsToLine pointToLine) {
        super(NAME, null, null);
        this.pointsToLine = pointToLine;
    }

    @Override
    public Object evaluate(final Object feature) {
        ArgumentChecks.ensureNonNull("Points to line function", pointsToLine);

        try {

            final Object ptsToLine = pointsToLine.evaluate(feature);
            if (ptsToLine instanceof Point) {
                return ptsToLine;
            } else if (ptsToLine instanceof LineString) {

                final LineString lineString = (LineString) pointsToLine.evaluate(feature);
                if (lineString == null) {
                    return lineString;
                }
                final Point center = lineString.getCentroid();
                center.setSRID(lineString.getSRID());
                center.setUserData(lineString.getUserData());
                return center;

            } else {
                SIRS.LOGGER.log(Level.WARNING, "PointsToLine function was expected to return a Point or a LineString but returned  : {0}.\n Return first expression.", ptsToLine);
                return pointsToLine.getExpr1();
            }
        } catch(Exception e) {
            SIRS.LOGGER.log(Level.WARNING, "Fail to compute center for the assessed feature line. Return first position.", e);
            return pointsToLine.getExpr1();
        }
    }



}

