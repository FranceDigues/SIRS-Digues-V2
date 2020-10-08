

package fr.sirs.core.model;

import com.vividsolutions.jts.geom.Geometry;

public interface AvecSettableGeometrie extends  AvecGeometrie {

    void setGeometry(final Geometry geometry);

}

