/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.sirs.map;

/**
 * Enum indiquant le mode d'édition adopté par les outils d'édition cartographique.
 *
 * Utilisé par exemple pour le contrôle par {@link SIRSEditMouseListen} des
 * actions de la souris.
 *
 * @author Johann Sorel (Geomatys)
 * @author Matthieu Bastianelli (Geomatys)
 */
public enum EditModeObjet {
    PICK_TRONCON, // Sélection de tronçon
    EDIT_OBJET,   // Edition d'un objet existant
    CREATE_OBJET, // Création d'un objet
    NONE
};


//public interface EditMode {};