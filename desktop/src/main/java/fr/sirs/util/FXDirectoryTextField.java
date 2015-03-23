/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.sirs.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.stage.DirectoryChooser;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class FXDirectoryTextField extends AbstractPathTextField {

    @Override
    protected Path choosePath() {
        final DirectoryChooser chooser = new DirectoryChooser();
        String strPath = getText();
        if (strPath != null && !strPath.isEmpty()) {
            final Path tmp = Paths.get(strPath);
            if (Files.isDirectory(tmp)) {
                chooser.setInitialDirectory(tmp.toFile());
            } else if (Files.isDirectory(tmp.getParent())) {
                chooser.setInitialDirectory(tmp.getParent().toFile());
            }
        }
        File returned = chooser.showDialog(null);
        if (returned == null) {
            return null;
        } else {
            return returned.toPath();
        }
    }
    
}