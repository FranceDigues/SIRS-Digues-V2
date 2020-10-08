/**
 * This file is part of SIRS-Digues 2.
 *
 * Copyright (C) 2019, FRANCE-DIGUES,
 *
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.theme.ui.pojotable;

import javafx.scene.control.Alert;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public class CopyElementException extends Exception {

    public CopyElementException() {
        super();
    }

    public CopyElementException(String message) {
        super(message);
    }
    
    public CopyElementException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Permet d'afficher le message de la CopieElementException dans une fenêtre
     * JavaFx.
     */
    public void openAlertWindow() {
        final Alert alert = new Alert(Alert.AlertType.WARNING, this.getMessage());
        alert.setResizable(true);
        alert.showAndWait();
    }
    
    

}
