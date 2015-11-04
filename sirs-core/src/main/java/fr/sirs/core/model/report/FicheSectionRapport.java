package fr.sirs.core.model.report;

import fr.sirs.core.model.Element;
import fr.sirs.core.model.ElementCreator;
import fr.sirs.util.property.Reference;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Detailed element printing.
 *
 * @author Alexis Manin (Geomatys)
 */
public class FicheSectionRapport extends AbstractSectionRapport {

    private final SimpleIntegerProperty nbPhotos = new SimpleIntegerProperty(0);

    /**
     *
     * @return The number of photos to print for a given element.
     */
    public IntegerProperty nbPhotosProperty() {
        return nbPhotos;
    }

    public int getNbPhotos() {
        return nbPhotos.get();
    }

    public void setNbPhotos(int newValue) {
        nbPhotos.set(newValue);
    }

    private final SimpleStringProperty modeleElementId = new SimpleStringProperty();

    /**
     *
     * @return ODT template used to print each element given in this section.
     */
    @Reference(ref=ModeleElement.class)
    public StringProperty ModeleElementIdProperty() {
        return modeleElementId;
    }

    public String getModeleElementId() {
        return modeleElementId.get();
    }

    public void setModeleElementId(final String newValue) {
        modeleElementId.set(newValue);
    }

    @Override
    public Element copy() {
        final FicheSectionRapport rapport = ElementCreator.createAnonymValidElement(FicheSectionRapport.class);
        super.copy(rapport);

        rapport.setNbPhotos(getNbPhotos());
        rapport.setModeleElementId(getModeleElementId());

        return rapport;
    }

    @Override
    public boolean removeChild(Element toRemove) {
        return false;
    }

    @Override
    public boolean addChild(Element toAdd) {
        return false;
    }

    @Override
    public Element getChildById(String toSearch) {
        if (toSearch != null && toSearch.equals(getId()))
            return this;
        return null;
    }

//    @Override
//    public void print(TextDocument target, Object sourceData) {
//        if (modeleElementId.get() == null)
//            throw new IllegalStateException("No model set for printing !");
//        final SessionCore session = InjectorCore.getBean(SessionCore.class);
//        ModeleElement model = session.getRepositoryForClass(ModeleElement.class).get(modeleElementId.get());
//        final String targetClassName = model.getTargetClass();
//        if (targetClassName == null)
//            throw new IllegalStateException("No data type specified by model !");
//
//        final Class targetClass = Thread.currentThread().getContextClassLoader().loadClass(targetClassName);
//
//        if (sourceData instanceof Iterable) {
//            print(target, (Iterable)sourceData);
//        } else {
//            if (sourceData instanceof Element) {
//                ODTUtils.reportFromTemplate(model.getOdt(), sourceData);
//            }
//
//            if (sourceData instanceof Feature) {
//
//            } else {
//                throw new IllegalArgumentException()
//            }
//        }
//    }
}
