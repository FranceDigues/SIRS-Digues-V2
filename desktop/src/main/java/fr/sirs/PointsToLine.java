
package fr.sirs;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.util.logging.Level;
import org.geotoolkit.filter.function.AbstractFunction;
import org.geotoolkit.geometry.jts.JTS;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Try to convert two Points feature's parameters in a {@link LineString}.
 * If only 1 point is non-null return this point.
 *
 * If one of the parameter is missing or if fail to convert the 2nd point in
 * the 1st point CRS, the first expression {@link #expr1} is returned;
 *
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public final class PointsToLine extends AbstractFunction {

    private static final String NAME = "PointsToLines";

    private static final GeometryFactory GF = new GeometryFactory();

    private final Expression expr1;
    private final Expression expr2;

    public PointsToLine(final Expression expr1, final Expression expr2) {
        super(NAME, new Expression[] {expr1,expr2}, null);
        this.expr1 = expr1;
        this.expr2 = expr2;
    }

    @Override
    public Object evaluate(final Object feature) {

        try {
            final Geometry geom1, geom2;

            try {
                geom1 = parameters.get(0).evaluate(feature, Geometry.class);
                geom2 = parameters.get(1).evaluate(feature, Geometry.class);

            } catch (Exception e) {
                SIRS.LOGGER.log(Level.WARNING, "Invalid function parameter." + parameters.get(0) + " " + parameters.get(1), e);
                return expr1;
            }

            final Point pt1 = getPoint(geom1);
            final Point pt2 = getPoint(geom2);

            if (pt1 == null) {
                if (pt2 == null) {
                    SIRS.LOGGER.log(Level.WARNING, "Invalid function parameter, both input expression evaluations are null.");
                    return expr1;
                } else {
                    return pt2;
                }
            } else if (pt2 == null) {
                return pt1;
            }

            int srid = geom1.getSRID();
            Object userData = geom1.getUserData();
            pt1.setSRID(srid);
            pt1.setUserData(userData);
            CoordinateReferenceSystem startCRS = null;
            CoordinateReferenceSystem endCRS = null;
            try {
                startCRS = JTS.findCoordinateReferenceSystem(geom1);
                endCRS   = JTS.findCoordinateReferenceSystem(geom2);

                if (startCRS!= null) {
                    if (endCRS != null) {
                        JTS.convertToCRS(pt2, startCRS);
                    } else {
                        //On considère que le 2nd point est exprimé dans les même coordonnées que le 1er.
                        SIRS.LOGGER.log(Level.WARNING, "2nd point is assumed in the same CRS than the 1st one");
                        pt2.setSRID(srid);
                        pt2.setUserData(userData);
                    }
                } else if (endCRS != null) {
                    srid = geom2.getSRID();
                    userData = geom2.getUserData();
                    //On considère que le 1er point est exprimé dans les même coordonnées que le 2nd.
                    SIRS.LOGGER.log(Level.WARNING, "1st point is assumed in the same CRS than the 2nd one");
                    pt1.setSRID(srid);
                    pt1.setUserData(userData);
                }

            } catch (Exception e) {
                SIRS.LOGGER.log(Level.WARNING, "Fail to convert the 2nd geometry " + parameters.get(1) + " in the same CRS than the 1st one " + parameters.get(0) + " ;\n srid = " + srid + "\n CRS = " + startCRS, e);
                return expr1;
            }

            final LineString lineString = GF.createLineString(new Coordinate[]{
                pt1.getCoordinate(),
                pt2.getCoordinate()
            });
            lineString.setSRID(srid);
            lineString.setUserData(userData);

            return lineString;
        } catch(Exception e) {
            SIRS.LOGGER.log(Level.WARNING, "Fail to compute center for the assessed feature line. Return first position.", e);
            return expr1;
        }
    }

    static Point getPoint(final Geometry geom){
        if(geom == null) {
            return null;
        }
        if(geom instanceof LineString){
            return ((LineString)geom).getStartPoint();
        }else if(geom instanceof Point){
            return (Point) ((Point)geom).clone();
        }else if(geom instanceof Polygon){
            return getPoint( ((Polygon)geom).getExteriorRing());
        }else if(geom instanceof GeometryCollection){
            final int nb = ((GeometryCollection)geom).getNumGeometries();
            if(nb!=0){
                return getPoint(((GeometryCollection)geom).getGeometryN(0));
            }else{
                 return null;
            }
        }else{
            return null;
        }
    }

    public Expression getExpr1() {
        return expr1;
    }

    public Expression getExpr2() {
        return expr2;
    }

}

