package fr.sirs.other;

import fr.sirs.FXEditMode;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.model.AvecDateMaj;
import static fr.sirs.core.model.Role.ADMIN;
import static fr.sirs.core.model.Role.EXTERN;
import static fr.sirs.core.model.Role.USER;
import fr.sirs.core.model.ContactOrganisme;
import fr.sirs.core.model.Organisme;
import fr.sirs.theme.ui.AbstractFXElementPane;
import fr.sirs.theme.ui.PojoTable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.geotoolkit.gui.javafx.util.FXDateField;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class FXOrganismePane extends AbstractFXElementPane<Organisme> {
    
    @FXML private FXEditMode uiMode;
    @FXML private TextField uiPseudoId;
    @FXML private FXDateField date_maj;

    @FXML private GridPane uiDescriptionGrid;
    @FXML private GridPane uiAdresseGrid;
    
    @FXML private TextField uiRaisonSocialeTextField;
    @FXML private TextField uiStatutJuridiqueTextField;
    @FXML private TextField uiTelTextField;
    @FXML private TextField uiEmailTextField;
    @FXML private TextField uiAdresseTextField;
    @FXML private TextField uiCodePostalTextField;
    @FXML private TextField uiCommuneTextField;
    
    @FXML private DatePicker uiDebutDatePicker;
    @FXML private DatePicker uiFinDatePicker;
    
    @FXML private Tab uiContactOrganismesTab;
    
    private final PojoTable contactOrganismeTable;

    public FXOrganismePane(Organisme organisme) {
        SIRS.loadFXML(this);
        date_maj.setDisable(true);

        uiMode.setAllowedRoles(ADMIN, USER, EXTERN);
        disableFieldsProperty().bind(uiMode.editionState().not());
        for (final Node child : uiDescriptionGrid.getChildren()) {
            if (!(child instanceof Label)) {
                child.disableProperty().bind(disableFieldsProperty());
            }
        }
        for (final Node child : uiAdresseGrid.getChildren()) {
            if (!(child instanceof Label)) {
                child.disableProperty().bind(disableFieldsProperty());
            }
        }
        
        contactOrganismeTable = new PojoTable(ContactOrganisme.class, "Contacts rattachés");
        contactOrganismeTable.parentElementProperty().bind(elementProperty);
        contactOrganismeTable.editableProperty().bind(uiMode.editionState());
        uiContactOrganismesTab.setContent(contactOrganismeTable);
        
        uiPseudoId.disableProperty().bind(disableFieldsProperty());
        
        elementProperty.addListener(this::initPane);
        setElement(organisme);
    }
    
    private void initPane(ObservableValue<? extends Organisme> observable, Organisme oldValue, Organisme newValue) {
        date_maj.valueProperty().unbind();
        if (oldValue != null) {
            uiRaisonSocialeTextField.textProperty().unbindBidirectional(oldValue.nomProperty());
            uiStatutJuridiqueTextField.textProperty().unbindBidirectional(oldValue.statut_juridiqueProperty());
            uiTelTextField.textProperty().unbindBidirectional(oldValue.telephoneProperty());
            uiEmailTextField.textProperty().unbindBidirectional(oldValue.emailProperty());
            uiAdresseTextField.textProperty().unbindBidirectional(oldValue.adresseProperty());
            uiCodePostalTextField.textProperty().unbindBidirectional(oldValue.code_postalProperty());
            uiCommuneTextField.textProperty().unbindBidirectional(oldValue.communeProperty());
            uiPseudoId.textProperty().unbindBidirectional(oldValue.designationProperty());
        }
        
        final Organisme organisme;
        if (newValue == null) {
            organisme = Injector.getSession().getOrganismeRepository().create();
            uiMode.setSaveAction(()->{organisme.setDateMaj(LocalDateTime.now()); Injector.getSession().getOrganismeRepository().add(organisme);});
        } else {
            organisme = newValue;
            uiMode.setSaveAction(()->{organisme.setDateMaj(LocalDateTime.now()); Injector.getSession().getOrganismeRepository().update(organisme);});
        }
        
        date_maj.valueProperty().bind(organisme.dateMajProperty());
        uiPseudoId.textProperty().bindBidirectional(organisme.designationProperty());
        
        uiRaisonSocialeTextField.textProperty().bindBidirectional(organisme.nomProperty());
        uiStatutJuridiqueTextField.textProperty().bindBidirectional(organisme.statut_juridiqueProperty());
        uiTelTextField.textProperty().bindBidirectional(organisme.telephoneProperty());
        uiEmailTextField.textProperty().bindBidirectional(organisme.emailProperty());
        uiAdresseTextField.textProperty().bindBidirectional(organisme.adresseProperty());
        uiCodePostalTextField.textProperty().bindBidirectional(organisme.code_postalProperty());
        uiCommuneTextField.textProperty().bindBidirectional(organisme.communeProperty());
        
        if (organisme.getDate_debut() != null) {
            uiDebutDatePicker.valueProperty().set(organisme.getDate_debut().toLocalDate());
        }
        uiDebutDatePicker.valueProperty().addListener((ObservableValue<? extends LocalDate> observableDate, LocalDate oldDate, LocalDate newDate) -> {
            if (newDate == null) {
                organisme.date_debutProperty().set(null);
            } else {
                organisme.date_debutProperty().set(LocalDateTime.of(newDate, LocalTime.MIN));
            }
        });
        
        if (organisme.getDate_fin() != null) {
            uiFinDatePicker.valueProperty().set(organisme.getDate_fin().toLocalDate());
        }
        uiFinDatePicker.valueProperty().addListener((ObservableValue<? extends LocalDate> observableDate, LocalDate oldDate, LocalDate newDate) -> {
            if (newDate == null) {
                organisme.date_finProperty().set(null);
            } else {
                organisme.date_finProperty().set(LocalDateTime.of(newDate, LocalTime.MIN));
            }
        });
        
        contactOrganismeTable.setTableItems(()-> (ObservableList) organisme.contactOrganisme);
    }

    @Override
    public void preSave() {
        // nothing to do, all is done by JavaFX bindings.
    }
}
