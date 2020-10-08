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
package fr.sirs.util;

import fr.sirs.core.SirsCore;
import static fr.sirs.util.AbstractJDomWriter.NULL_REPLACEMENT;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import org.apache.sis.util.ArgumentChecks;

/**
 * Classe utilitaire réservée aux méthodes utilisées depuis les fichiers JRXML.
 *
 * Afin de ne pas modifier involontairement le résultat des fiches imprimées avec Jasperreport, il est préférable de
 * ne pas utiliser ces méthodes dans du code Java.
 *
 * Attention toutefois : ces méthodes, dont les signatures sont écrites en dur dans les fichiers JRXML, sont susceptibles
 * d'une double utilisation.
 *
 * 1- Elles peuvent être utilisées de manière statique dans les fichiers JRXML écrits en dur.
 *
 * 2- Elles peuvent également être utilisées indirectement (écrites en dur sous forme de chaînes de caractères) par les
 * méthodes modifiant dynamiquement les patrons JRXML (telles que par exemple dans la classe
 * {@link AbstractJDomWriterSingleSpecificSheet}). Ceci expose à un risque d'éparpillement d'écriture en dur des signatures
 * de es méthodes. Pour limiter le risque d'utilisation de ces noms de fonction à l'extérieur de cette classe, il est
 * recommandé de passer par des méthodes spécifiques ({@link JRXMLUtil#dynamicDisplayLabel(java.lang.String)} ,
 * {@link JRXMLUtil#dynamicDisplayLabels(java.lang.String) }…).
 *
 * @author Samuel Andrés (Geomatys) <samuel.andres at geomatys.com>
 */
public class JRXMLUtil {

    private static final NumberFormat PR_FORMAT = new DecimalFormat("#.##");

    private JRXMLUtil(){}

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MÉTHODES CŒUR DES FONCTIONNALITÉS.                                                                             //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static String extractDesignation(final String input){
        if(input==null || input.isEmpty()){
            SirsCore.LOGGER.log(Level.WARNING, "aucune donnée de laquelle extraire une désignation");
            return "";
        }
        else{
            try{
                SirsCore.LOGGER.log(Level.FINEST, "extraction de la désignation de : {0}", input);
                int endSubstring = input.indexOf(SirsStringConverter.LABEL_SEPARATOR);
                endSubstring = endSubstring<0? input.length() :endSubstring; 
                int startSubstring = input.indexOf(SirsStringConverter.DESIGNATION_SEPARATOR);
                if(startSubstring<0){
                    return "";
                }else{
                    startSubstring = startSubstring + SirsStringConverter.DESIGNATION_SEPARATOR.length();
                }
                return input.substring(startSubstring, endSubstring);
            }
            catch(Exception e){
                // SYM-1734 : sécurité dans le cas d'une désignation impossible à extraire
                SirsCore.LOGGER.log(Level.WARNING, "un problème a été rencontré lors de l'extraction de la désignation à partir de {0}"+input, e);
                return "(?)";
            }
        }
    }

    static String extractLabel(final String input){
        ArgumentChecks.ensureNonNull("String input", input);
        
        final int index = input.indexOf(SirsStringConverter.LABEL_SEPARATOR);
        if(index>-1){
            return input.substring(index+SirsStringConverter.LABEL_SEPARATOR.length());
        }
        else {
//            SirsCore.LOGGER.log(Level.WARNING, "Label separator not found : {0}", input);  //Surcharge les logs.
            if ((input.length()>2) && (input.charAt(1) == ')')) {
                //ATTENTION : Bricolage 
                // Permet de ne pas considérer la numérotation ("1) ") lors de l'extraction de certain label.
                // Ces cas ce produisent pas exemple dans la colonne 'Intervenants' des prestations au sein des fiches désordre.
                // Sans cette opération, le "1) " est duppliqué.
                // Je ne connais pas l'origine du "1) ".
                SirsCore.LOGGER.log(Level.INFO, "Substring \"{0}\" extraite lors de l'extraction du label.", input.substring(0, 2));
                return input.substring(2).trim(); 
            }
            return input;
        }
    }

    static String extractReferenceCode(final String input){
        int indexOfSeparator = input.indexOf(SirsStringConverter.LABEL_SEPARATOR);
        if (indexOfSeparator < 0){
                SirsCore.LOGGER.log(Level.WARNING, "Problème lors de l'extraction de référence Code pour la chaîne de caractère : "+input,
                        new IndexOutOfBoundsException("No LABEL_SEPARATOR found"));
                return "";
        }
        return input.substring(0, input.indexOf(SirsStringConverter.LABEL_SEPARATOR));
    }

    /**
     * Affichage d'un champ récupéré comme {@link PrintableArrayList}.
     *
     * @param inputList
     * @param ordered
     * @param startIndex
     * @return
     */
    static String extractLabels(final String inputList, final boolean ordered, final int startIndex){

        if(inputList==null || inputList.isEmpty()) return NULL_REPLACEMENT;

        final String[] split = inputList.split("\n");
        final Collection<String> output = new ArrayList<>();
        for(int i = 0; i<split.length; i++){
            output.add(extractLabel(split[i]));
        }
        return PrinterUtilities.printList(output, ordered, startIndex);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MÉTHODES DESTINÉES À UNE ÉCRITURE STATIQUE DANS LES FICHIERS JRXML.                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String displayField(final String input){
        return input==null ? NULL_REPLACEMENT : input;
    }

    /**
     *
     * @param input
     * @return
     *
     * @see JRXMLUtil#dynamicDisplayLabel(java.lang.String) Pour écrire dynamiquement cette méthode dans les patrons JRXML.
     */
    public static String displayLabel(final String input){
        return input==null ? NULL_REPLACEMENT : extractLabel(input);
    }

    /**
     *
     * @param input
     * @return
     *
     * @see JRXMLUtil#dynamicDisplayReferenceCode(java.lang.String) Pour écrire dynamiquement cette méthode dans les patrons JRXML.
     */
    public static String displayReferenceCode(final String input){
        return input==null ? NULL_REPLACEMENT : extractReferenceCode(input);
    }

    /**
     * Affichage d'un champ récupéré comme {@link PrintableArrayList}.
     *
     * @param inputList
     * @param ordered
     * @param startIndex
     * @return
     *
     * @see JRXMLUtil#dynamicDisplayLabels(java.lang.String) Pour écrire dynamiquement cette méthode dans les patrons JRXML.
     */
    public static String displayLabels(final String inputList, final Boolean ordered, final Integer startIndex){
        return inputList==null ? NULL_REPLACEMENT : extractLabels(inputList, ordered, startIndex);
    }

    public static String displayDesignation(final String input){
        return input==null ? NULL_REPLACEMENT : extractDesignation(input);
    }

    public static String displayAmontAval(final Boolean input){
        return input==null ? NULL_REPLACEMENT : input ? "Amont" : "Aval";
    }

    public static String displayDistance(final Double distance){
        return distance==null ? NULL_REPLACEMENT : (PR_FORMAT.format(distance) + " m");
    }

    public static String displayPR(final Double pr){
        return pr==null ? NULL_REPLACEMENT : PR_FORMAT.format(pr);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MÉTHODES À UTILISER POUR ÉCRIRE DYNAMIQUEMENT DANS LES PATRONS JRXML AFIN DE CENTRALISER LES MODIFICATIONS LE  //
    // CAS ÉCHÉANT.                                                                                                   //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Affichage d'un champ récupéré comme {@link PrintableArrayList}.
     *
     * @param fieldName
     * @return
     * @see JRXMLUtil#displayLabels(java.lang.String, java.lang.Boolean, java.lang.Integer)
     */
    public static String dynamicDisplayLabels(final String fieldName){
        return "($F{"+fieldName+"}==null) ? \""+NULL_REPLACEMENT+"\" : fr.sirs.util.JRXMLUtil.displayLabels($F{"+fieldName+"}.toString(), true, 1)";
    }

    /**
     *
     * @param fieldName
     * @return
     * @see JRXMLUtil#displayLabel(java.lang.String)
     */
    public static String dynamicDisplayLabel(final String fieldName){
        return "fr.sirs.util.JRXMLUtil.displayLabel($F{"+fieldName+"})";
    }

    /**
     *
     * @param fieldName
     * @return
     * @see JRXMLUtil#displayReferenceCode(java.lang.String)
     */
    public static String dynamicDisplayReferenceCode(final String fieldName){
        return "fr.sirs.util.JRXMLUtil.displayReferenceCode($F{"+fieldName+"})";
    }
}
