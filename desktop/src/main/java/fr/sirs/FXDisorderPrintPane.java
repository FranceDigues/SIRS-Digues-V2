package fr.sirs;

import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Observation;
import fr.sirs.core.model.RefTypeDesordre;
import fr.sirs.core.model.RefUrgence;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Tab;
import org.apache.sis.measure.NumberRange;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class FXDisorderPrintPane extends TronconChoicePrintPane {
    
    @FXML private Tab uiDisorderTypeChoice;
    @FXML private Tab uiUrgenceTypeChoice;
    
    @FXML private CheckBox uiOptionPhoto;
    @FXML private CheckBox uiOptionReseauOuvrage;
    @FXML private CheckBox uiOptionVoirie;
    
    @FXML private DatePicker uiOptionDebut;
    @FXML private DatePicker uiOptionFin;
    
    @FXML private CheckBox uiOptionNonArchive;

    @FXML private CheckBox uiOptionArchive;

    @FXML private DatePicker uiOptionDebutArchive;
    @FXML private DatePicker uiOptionFinArchive;

    private final TypeChoicePojoTable disordreTypesTable = new TypeChoicePojoTable(RefTypeDesordre.class, "Types de désordres");
    private final TypeChoicePojoTable urgenceTypesTable = new TypeChoicePojoTable(RefUrgence.class, "Types d'urgences");
    
    public FXDisorderPrintPane(){
        super(FXDisorderPrintPane.class);
        disordreTypesTable.setTableItems(()-> (ObservableList) FXCollections.observableList(Injector.getSession().getRepositoryForClass(RefTypeDesordre.class).getAll()));
        disordreTypesTable.commentAndPhotoProperty().set(false);
        uiDisorderTypeChoice.setContent(disordreTypesTable);
        urgenceTypesTable.setTableItems(()-> (ObservableList) FXCollections.observableList(Injector.getSession().getRepositoryForClass(RefUrgence.class).getAll()));
        urgenceTypesTable.commentAndPhotoProperty().set(false);
        uiUrgenceTypeChoice.setContent(urgenceTypesTable);
        
        uiOptionNonArchive.disableProperty().bind(uiOptionArchive.selectedProperty());
        uiOptionArchive.disableProperty().bind(uiOptionNonArchive.selectedProperty());
        uiOptionNonArchive.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if(newValue){
                    if(uiOptionArchive.isSelected()) uiOptionArchive.setSelected(false);
                    uiOptionDebutArchive.setValue(null);
                    uiOptionFinArchive.setValue(null);
                }
            });
        uiOptionArchive.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if(newValue && uiOptionNonArchive.isSelected()) uiOptionNonArchive.setSelected(false);
            });

        uiOptionDebutArchive.disableProperty().bind(uiOptionNonArchive.selectedProperty());
        uiOptionFinArchive.disableProperty().bind(uiOptionNonArchive.selectedProperty());
    }
    
    @FXML private void cancel(){
        
    }
    
    
    @FXML 
    private void print(){
        Injector.getSession().getTaskManager().submit("Génération de fiches détaillées de désordres",
        () -> {
            
            final List<Desordre> desordres = Injector.getSession().getRepositoryForClass(Desordre.class).getAll();
            
            final List<String> tronconIds = new ArrayList<>();
            for(final Element element : tronconsTable.getSelectedItems()){
                tronconIds.add(element.getId());
            }
            final List<String> typeDesordresIds = new ArrayList<>();
            for(final Element element : disordreTypesTable.getSelectedItems()){
                typeDesordresIds.add(element.getId());
            }
            final List<String> typeUrgencesIds = new ArrayList<>();
            for(final Element element : urgenceTypesTable.getSelectedItems()){
                typeUrgencesIds.add(element.getId());
            }
            
            long minTimeSelected = Long.MIN_VALUE;
            long maxTimeSelected = Long.MAX_VALUE;

            {
                LocalDateTime tmpTimeSelected = uiOptionDebut.getValue()==null ? null : uiOptionDebut.getValue().atTime(LocalTime.MIDNIGHT);
                if (tmpTimeSelected !=null) minTimeSelected = Timestamp.valueOf(tmpTimeSelected).getTime();

                tmpTimeSelected = uiOptionFin.getValue()==null ? null : uiOptionFin.getValue().atTime(LocalTime.MIDNIGHT);
                if (tmpTimeSelected !=null) maxTimeSelected = Timestamp.valueOf(tmpTimeSelected).getTime();
            }

            // Intervalle de temps de présence du désordre
            final NumberRange<Long> selectedRange = NumberRange.create(minTimeSelected, true, maxTimeSelected, true);


            minTimeSelected = Long.MIN_VALUE;
            maxTimeSelected = Long.MAX_VALUE;

            {
                LocalDateTime tmpTimeSelected = uiOptionDebutArchive.getValue()==null ? null : uiOptionDebutArchive.getValue().atTime(LocalTime.MIDNIGHT);
                if (tmpTimeSelected !=null) minTimeSelected = Timestamp.valueOf(tmpTimeSelected).getTime();

                tmpTimeSelected = uiOptionFinArchive.getValue()==null ? null : uiOptionFinArchive.getValue().atTime(LocalTime.MIDNIGHT);
                if (tmpTimeSelected !=null) maxTimeSelected = Timestamp.valueOf(tmpTimeSelected).getTime();
            }
            // Intervalle d'archivage du désordre
            final NumberRange<Long> archiveRange = NumberRange.create(minTimeSelected, true, maxTimeSelected, true);

            final Predicate<Desordre> localPredicate = (Desordre desordre) -> {

                        /*
                        CONDITION PORTANT SUR LES OPTIONS
                        */
                        // 1- Si on a décidé de ne pas générer de fiche pour les désordres archivés.
                        final boolean excludeArchiveCondition = (uiOptionNonArchive.isSelected() && desordre.getDate_fin()!=null);

                        // 2- Si le désordre n'a pas eu lieu durant la période retenue
                        final boolean periodeCondition;

                        long minTime = Long.MIN_VALUE;
                        long maxTime = Long.MAX_VALUE;
                        LocalDateTime tmpTime = desordre.getDate_debut()==null ? null : desordre.getDate_debut().atTime(LocalTime.MIDNIGHT);
                        if (tmpTime != null) minTime = Timestamp.valueOf(tmpTime).getTime();

                        tmpTime = desordre.getDate_fin()==null ? null : desordre.getDate_fin().atTime(LocalTime.MIDNIGHT);
                        if (tmpTime != null) maxTime = Timestamp.valueOf(tmpTime).getTime();

                        final NumberRange<Long> desordreRange = NumberRange.create(minTime, true, maxTime, true);
                        periodeCondition = !selectedRange.intersects(desordreRange);


                        // 3- Si on a décidé de ne générer la fiche que des désordres archivés
                        final boolean onlyArchiveCondition = (uiOptionArchive.isSelected() && desordre.getDate_fin()==null);

                        final boolean periodeArchiveCondition;

                        if(!onlyArchiveCondition){
                            long time = Long.MAX_VALUE;

                            tmpTime = desordre.getDate_fin()==null ? null : desordre.getDate_fin().atTime(LocalTime.MIDNIGHT);
                            if (tmpTime != null) time = Timestamp.valueOf(tmpTime).getTime();

                            final NumberRange<Long> archiveDesordreRange = NumberRange.create(time, true, time, true);
                            periodeArchiveCondition = !archiveRange.intersects(archiveDesordreRange);
                        }else{
                            periodeArchiveCondition=false;
                        }

                        final boolean archiveCondition = onlyArchiveCondition || periodeArchiveCondition;

                        final boolean conditionOptions = excludeArchiveCondition || periodeCondition || archiveCondition;

                        // 4- Si on a décidé de ne générer la fiche que pour un niveau d'urgence particulier;
                        final boolean urgenceOption;

                        if(typeUrgencesIds.isEmpty()){
                            urgenceOption = false;
                        }else{
                            // Recherche de la dernière observation.
                            final List<Observation> observations = desordre.getObservations();
                            Observation derniereObservation = null;
                            for(final Observation obs : observations){
                                if(obs.getDate()!=null){
                                    if(derniereObservation==null) derniereObservation = obs;
                                    else{
                                        if(obs.getDate().isAfter(derniereObservation.getDate())) derniereObservation = obs;
                                    }
                                }
                            }

                            if(derniereObservation!=null){
                                urgenceOption = !typeUrgencesIds.contains(derniereObservation.getUrgenceId());
                            }
                            else urgenceOption=false;
                        }
                        
                        /*
                        Sous-condition de retrait de type : si le désordre est
                        d'un type qui n'est pas sélectionné dans la liste.
                        */
                        final boolean typeSelected; // Si le type du désordre n'est pas parmi les types sélectionnés
                                // Si on n'a sélectionné aucun désordre, on laisse passer a priori quel que soit le type de désordre.
                                if (typeDesordresIds.isEmpty()) typeSelected = false;
                                // Si la liste de sélection des types de désordres n'est pas vide on vérifie de type de désordre
                                else typeSelected = (desordre.getTypeDesordreId()==null
                                        || !typeDesordresIds.contains(desordre.getTypeDesordreId()));

                        return typeSelected || conditionOptions || urgenceOption;

                    };

            // On retire les désordres de la liste dans les cas suivants...
            desordres.removeIf(localPredicate.or(new LocationPredicate<>()));
            
            try {
                if(!desordres.isEmpty()){
                    Injector.getSession().getPrintManager().printDesordres(desordres, uiOptionPhoto.isSelected(), uiOptionReseauOuvrage.isSelected(), uiOptionVoirie.isSelected());
                }
            } catch (Exception ex) {
                SIRS.LOGGER.log(Level.WARNING, null, ex);
            }
        });
    }
}
