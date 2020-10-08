

package fr.sirs.core.model;

import java.util.List;

public interface AvecObservations extends  Element {

    public List<? extends AbstractObservation> getObservations();

}

