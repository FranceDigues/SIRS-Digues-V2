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
package fr.sirs.theme.ui.columns;

import java.io.Serializable;

/**
 *
 * Classe permettant de décrire les préférences d'une colonne (de PojoTable).
 *
 * 
 * @author Matthieu Bastianelli (Geomatys)
 */
public class ColumnState implements Serializable {

    private String name;
    private boolean visible;
    private Integer position;
    private float width;

    /**
     * Constructeur par défaut
     */
    public ColumnState() {
    }

    /**
     * Constructeur de préférences pour une colonne.
     *
     * @param name : nom de la colonne, constant mais pas unique. 
     * Le nom d'une colonne peut être utilisé par plusieurs PojoTable.
     * 
     * @param isVisible : boolean indiquant quand l'utilisateur veut spécifier
     * la visibilité (true) ou non (false) de la colonne.
     * @param position : Integer indiquant la position de la colonne par rapport
     * aux autres colonnes du tableau (0 pour la première colonne).
     * @param width : float largeur de la colonne choisie par l'utilisateur.
     */
    public ColumnState(final String name, boolean isVisible, Integer position, float width) {
        this.name = name;
        this.visible = isVisible;
        this.position = position;
        this.width = width;
    }

//    public ColumnState(TableColumn col) {
//        this.name = col.getN;
//        this.visible = isVisible;
//        this.position = position;
//        this.width = width;
//    }
    
    //Getter :
    public String getName() {
        return name;
    }

    public Integer getPosition() {
        return position;
    }

    public boolean isVisible() {
        return visible;
    }

    public float getWidth() {
        return width;
    }

    //Setter :
    public void setName(String name) {
        this.name = name;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setWidth(float width) {
        this.width = width;
    }

}
