import javax.swing.*;

// ejecuta el test y luego lanza la interfaz grafica
public class Main {
    public static void main(String[] args) {
        HuffmanEngine.selfTest();        //prueba de validación interna
        SwingUtilities.invokeLater(() -> {
            HuffmanEngine engine = new HuffmanEngine();
            BinaryIOManager ioManager = new BinaryIOManager();
            UserInterface ui = new UserInterface(engine, ioManager);
            ui.setVisible(true);
        });
    }
}
