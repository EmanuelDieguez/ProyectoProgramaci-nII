package gt.edu.umg.sistema.ventas;
//Nooooooo
import gt.edu.umg.sistema.ventas.vista.VentanaPrincipal;

import javax.swing.*;
import java.awt.*;

/** Punto de entrada del sistema. */
public class SistemaVentas {

    public static void main(String[] args) {
        configurarApariencia();
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }

    private static void configurarApariencia() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.put("nimbusBase", new Color(30, 58, 110));
                    UIManager.put("nimbusBlueGrey", new Color(170, 182, 200));
                    UIManager.put("control", new Color(244, 246, 250));
                    UIManager.put("nimbusSelectionBackground", new Color(37, 99, 235));
                    UIManager.put("nimbusFocus", new Color(96, 165, 250));
                    UIManager.put("Table.alternateRowColor", new Color(241, 245, 249));
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Si Nimbus no está disponible se usa la apariencia por defecto
        }
    }
}
