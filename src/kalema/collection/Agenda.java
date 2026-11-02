/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kalema.collection;

import kalema.collection.presentation.AgendaFrame;
import javax.swing.*;

/**
 *
 * @author Ulb-Kalema
 */
public class Agenda {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); }
            catch (Exception ignored) { }
            UIManager.put("defaultFont", new java.awt.Font("Segoe UI", 0, 13));
            UIManager.put("OptionPane.yesButtonText", "Oui");
            UIManager.put("OptionPane.noButtonText", "Non");
            new AgendaFrame().setVisible(true);
        });
    }
    
}
