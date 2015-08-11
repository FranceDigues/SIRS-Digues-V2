
package fr.sirs.plugin.vegetation;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.model.PlanVegetation;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.util.SirsStringConverter;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXPlanDeGestionPane extends BorderPane {

    @FXML private Tab tabPlanification;
    @FXML private Tab tabParametrage;

    private final BorderPane tablePane = new BorderPane();

    public FXPlanDeGestionPane() {
        SIRS.loadFXML(this, FXParametragePane.class);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public void initialize() {
        tablePane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        final Session session = Injector.getSession();

        //plan de gestion actif
        final ChoiceBox<PlanVegetation> planChoiceBox = new ChoiceBox<>();
        planChoiceBox.setMaxWidth(400);
        planChoiceBox.setConverter(new SirsStringConverter());
        final List<PlanVegetation> allPlan = session.getRepositoryForClass(PlanVegetation.class).getAll();
        planChoiceBox.setItems(FXCollections.observableArrayList(allPlan));
        planChoiceBox.valueProperty().bindBidirectional(VegetationSession.INSTANCE.planProperty());
        final Label lblPlan = new Label("Plan de gestion actif : ");
        lblPlan.getStyleClass().add("label-header");
        final VBox planPane = new VBox(10,lblPlan,planChoiceBox);
        planPane.getStyleClass().add("blue-light");
        planPane.setPadding(new Insets(10));

        //troncon actif
        final ChoiceBox<TronconDigue> tronconChoiceBox = new ChoiceBox<>();
        tronconChoiceBox.setMaxWidth(400);
        tronconChoiceBox.setConverter(new SirsStringConverter());
        final List<TronconDigue> allTroncon = session.getRepositoryForClass(TronconDigue.class).getAll();
        allTroncon.add(0, null);
        tronconChoiceBox.setItems(FXCollections.observableArrayList(allTroncon));
        final Label lblTroncon = new Label("Tronçon : ");
        lblTroncon.getStyleClass().add("label-header");
        final VBox tronconPane = new VBox(10,lblTroncon,tronconChoiceBox);
        tronconPane.getStyleClass().add("blue-light");
        tronconPane.setPadding(new Insets(10));
        

        final GridPane pane = new GridPane();
        pane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        pane.setHgap(10);
        pane.setVgap(10);
        pane.setPadding(new Insets(10, 10, 10, 10));
        pane.getColumnConstraints().add(new ColumnConstraints(USE_PREF_SIZE, USE_COMPUTED_SIZE, USE_PREF_SIZE, Priority.NEVER, HPos.LEFT, true));
        pane.getColumnConstraints().add(new ColumnConstraints(USE_PREF_SIZE, USE_COMPUTED_SIZE, USE_PREF_SIZE, Priority.NEVER, HPos.LEFT, true));
        pane.getColumnConstraints().add(new ColumnConstraints(USE_PREF_SIZE, USE_COMPUTED_SIZE, Double.MAX_VALUE, Priority.ALWAYS, HPos.LEFT, true));
        pane.getRowConstraints().add(new RowConstraints(USE_PREF_SIZE, USE_COMPUTED_SIZE, USE_PREF_SIZE, Priority.NEVER, VPos.CENTER, true));
        pane.getRowConstraints().add(new RowConstraints(USE_PREF_SIZE, USE_COMPUTED_SIZE, Double.MAX_VALUE, Priority.ALWAYS, VPos.CENTER, true));
        pane.add(planPane, 0, 0);
        pane.add(tronconPane, 1, 0);
        pane.add(tablePane, 0, 1, 3, 1);

        tabPlanification.setContent(pane);
        tabParametrage.setContent(new FXParametragePane());

        if(planChoiceBox.getValue()!=null){
            tablePane.setCenter(new FXPlanTable(planChoiceBox.getValue(), tronconChoiceBox.getValue(), false));
        }

        //on ecoute les changements de troncon et de plan
        final ChangeListener chgListener = new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                tablePane.setCenter(new FXPlanTable(planChoiceBox.getValue(), tronconChoiceBox.getValue(), false));
            }
        };
        planChoiceBox.valueProperty().addListener(chgListener);
        tronconChoiceBox.valueProperty().addListener(chgListener);

    }


}
