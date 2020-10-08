/**
 * This file is part of SIRS-Digues 2.
 *
 * Copyright (C) 2016, FRANCE-DIGUES,
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
package fr.sirs.map;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXMapTab extends Tab {
    
    // TabPane auquel se raccrocher…
    private final TabPane tabs;
    private final FXMapPane map;

    public FXMapTab(TabPane tabs) {
        this.tabs = tabs;
        this.map = new FXMapPane();
        setText("Carte");
        setContent(map);
        
        selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if(Boolean.TRUE.equals(newValue)){
                    if(getContent()!=null){
                        getContent().requestFocus();
                    }
                }
            }
        });
    }    

    public FXMapPane getMap() {
        return map;
    }
    
    public synchronized void show(){
        if(!tabs.getTabs().contains(this)){
            tabs.getTabs().add(0,this);
        }
        final int index = tabs.getTabs().indexOf(this);
        tabs.getSelectionModel().clearAndSelect(index);
    }
}
