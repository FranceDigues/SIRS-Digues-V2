package fr.sirs.theme.ui;

import fr.sirs.core.model.Positionable;

/**
 * Edition des coordonées géographique d'un {@link Positionable}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXPositionableCoordMode extends FXPositionableAbstractCoordMode {

    private static final String MODE = "COORD";

    public FXPositionableCoordMode() {
        super();
    }

    @Override
    public String getID() {
        return MODE;
    }
}
