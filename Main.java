import javax.swing.*;

// ejecuta el test y luego lanza la interfaz grafica
public class Main {
    public static void main(String[] args) {
        MotorHuffman.autoTest();        //prueba de validación interna
        SwingUtilities.invokeLater(() -> {
            MotorHuffman motor = new MotorHuffman();
            GestorIOBinario gestorIO = new GestorIOBinario();
            UserInterface ui = new UserInterface(motor, gestorIO);
            ui.setVisible(true);
        });
    }
}
