import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.geom.QuadCurve2D;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

/**
 * Interfaz gráfica Swing para el compresor/descompresor de Huffman.
 * Layout: NORTE=botones, CENTRO=JSplitPane con dos JTextArea, SUR=barra de estado.
 */
public class UserInterface extends JFrame {

    private final HuffmanEngine engine;
    private final BinaryIOManager ioManager;

    /** Área de texto para el archivo original (panel izquierdo). */
    private JTextArea textAreaOriginal;

    /** Área de texto para el texto descompactado (panel derecho). */
    private JTextArea textAreaDecompressed;

    /** Etiqueta de estado en la parte inferior. */
    private JLabel statusLabel;

    /** Referencia al archivo de texto cargado. */
    private File loadedFile;

    /** Referencia al archivo .huf generado. */
    private File compressedFile;

    /** Referencia al archivo .dhu generado. */
    private File decompressedFile;

    /** Flag para evitar recursión infinita en sincronización de scroll. */
    private boolean isSyncing = false;

    /**
     * Construye la interfaz gráfica.
     * @param engine    motor de Huffman
     * @param ioManager gestor de E/S binaria
     */
    public UserInterface(HuffmanEngine engine, BinaryIOManager ioManager) {
        this.engine = engine;
        this.ioManager = ioManager;
        initializeUI();
    }

    /**
     * Inicializa todos los componentes de la interfaz.
     */
    private void initializeUI() {
        setTitle("Compresor Huffman — Reducción de Fuente por Columnas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        // ─── NORTE: Panel de botones ───
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolBar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JButton btnLoad = new JButton("Cargar Archivo");
        JButton btnCompress = new JButton("Compactar");
        JButton btnDecompress = new JButton("Descompactar");
        JButton btnStats = new JButton("Ver Estadística");
        JButton btnTree = new JButton("Ver Árbol");

        toolBar.add(btnLoad);
        toolBar.add(btnCompress);
        toolBar.add(btnDecompress);
        toolBar.add(btnStats);
        toolBar.add(btnTree);

        add(toolBar, BorderLayout.NORTH);

        // ─── CENTRO: JSplitPane con dos paneles de texto ───
        textAreaOriginal = new JTextArea();
        textAreaOriginal.setEditable(false);
        textAreaOriginal.setFont(new Font("Monospaced", Font.PLAIN, 13));

        textAreaDecompressed = new JTextArea();
        textAreaDecompressed.setEditable(false);
        textAreaDecompressed.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JScrollPane scrollLeft = new JScrollPane(textAreaOriginal);
        scrollLeft.setBorder(BorderFactory.createTitledBorder("Archivo Original"));

        JScrollPane scrollRight = new JScrollPane(textAreaDecompressed);
        scrollRight.setBorder(BorderFactory.createTitledBorder("Texto Descompactado"));

        // Sincronización de scroll vertical
        scrollLeft.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (!isSyncing) {
                    isSyncing = true;
                    scrollRight.getVerticalScrollBar().setValue(e.getValue());
                    isSyncing = false;
                }
            }
        });
        scrollRight.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (!isSyncing) {
                    isSyncing = true;
                    scrollLeft.getVerticalScrollBar().setValue(e.getValue());
                    isSyncing = false;
                }
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollLeft, scrollRight);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(480);
        add(splitPane, BorderLayout.CENTER);

        // ─── SUR: Barra de estado ───
        statusLabel = new JLabel("  Listo");
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        add(statusLabel, BorderLayout.SOUTH);

        // ─── Acciones de botones ───
        btnLoad.addActionListener(e -> loadFile());
        btnCompress.addActionListener(e -> compressFile());
        btnDecompress.addActionListener(e -> decompressFile());
        btnStats.addActionListener(e -> showStatistics());
        btnTree.addActionListener(e -> showTree());
    }

    /**
     * Abre un JFileChooser para cargar un archivo de texto.
     * Muestra el contenido en el panel izquierdo.
     */
    private void loadFile() {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Seleccionar archivo de texto");
            chooser.setFileFilter(new FileNameExtensionFilter(
                    "Archivos de texto (*.txt, *.doc, *.csv)", "txt", "doc", "csv", "text"));
            chooser.setAcceptAllFileFilterUsed(true);

            int result = chooser.showOpenDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) return;

            loadedFile = chooser.getSelectedFile();
            statusLabel.setText("  Cargando: " + loadedFile.getName() + "...");

            // Leer contenido del archivo
            String content;
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(loadedFile))) {
                byte[] bytes = bis.readAllBytes();
                content = new String(bytes, StandardCharsets.UTF_8);
            }

            textAreaOriginal.setText(content);
            textAreaOriginal.setCaretPosition(0);
            textAreaDecompressed.setText("");

            // Limpiar referencias anteriores
            compressedFile = null;
            decompressedFile = null;

            statusLabel.setText("  Archivo cargado: " + loadedFile.getName()
                    + " (" + loadedFile.length() + " bytes)");

        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this,
                    "Archivo no encontrado:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("  Error al cargar archivo.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error de lectura:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("  Error al cargar archivo.");
        }
    }

    /**
     * Comprime el archivo cargado a formato .huf.
     * Genera el archivo en el mismo directorio con extensión .huf.
     */
    private void compressFile() {
        if (loadedFile == null) {
            JOptionPane.showMessageDialog(this,
                    "No hay archivo cargado. Use 'Cargar Archivo' primero.",
                    "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            statusLabel.setText("  Compactando...");

            // Generar nombre del archivo .huf
            String baseName = getBaseName(loadedFile.getName());
            compressedFile = new File(loadedFile.getParentFile(), baseName + ".huf");

            ioManager.compress(loadedFile, compressedFile, engine);

            statusLabel.setText("  Compactación exitosa: " + compressedFile.getName()
                    + " (" + compressedFile.length() + " bytes)");

            JOptionPane.showMessageDialog(this,
                    "Compactación exitosa.\nArchivo: " + compressedFile.getAbsolutePath()
                            + "\nTamaño: " + compressedFile.length() + " bytes",
                    "Compactación", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al compactar:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("  Error en compactación.");
        }
    }

    /**
     * Abre un JFileChooser para seleccionar un archivo .huf y lo descomprime.
     * Genera un archivo .dhu y muestra su contenido en el panel derecho.
     */
    private void decompressFile() {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Seleccionar archivo .huf");
            chooser.setFileFilter(new FileNameExtensionFilter("Archivos Huffman (*.huf)", "huf"));

            // Si hay un archivo comprimido, empezar en su directorio
            if (compressedFile != null && compressedFile.getParentFile() != null) {
                chooser.setCurrentDirectory(compressedFile.getParentFile());
            } else if (loadedFile != null && loadedFile.getParentFile() != null) {
                chooser.setCurrentDirectory(loadedFile.getParentFile());
            }

            int result = chooser.showOpenDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) return;

            File hufFile = chooser.getSelectedFile();
            statusLabel.setText("  Descompactando: " + hufFile.getName() + "...");

            // Generar nombre del archivo .dhu
            String baseName = getBaseName(hufFile.getName());
            decompressedFile = new File(hufFile.getParentFile(), baseName + ".dhu");

            ioManager.decompress(hufFile, decompressedFile, engine);

            // Leer y mostrar el archivo descompactado
            String decompressedContent;
            try (BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(decompressedFile))) {
                byte[] bytes = bis.readAllBytes();
                decompressedContent = new String(bytes, StandardCharsets.UTF_8);
            }

            textAreaDecompressed.setText(decompressedContent);
            textAreaDecompressed.setCaretPosition(0);

            // Guardar referencia al .huf si no teníamos uno
            if (compressedFile == null) {
                compressedFile = hufFile;
            }

            statusLabel.setText("  Descompactación exitosa: " + decompressedFile.getName()
                    + " (" + decompressedFile.length() + " bytes)");

            JOptionPane.showMessageDialog(this,
                    "Descompactación exitosa.\nArchivo: " + decompressedFile.getAbsolutePath(),
                    "Descompactación", JOptionPane.INFORMATION_MESSAGE);

        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this,
                    "Archivo no encontrado:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("  Error en descompactación.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al descompactar:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("  Error en descompactación.");
        }
    }

    /**
     * Muestra un JDialog modal con una JTable de estadísticas:
     * Nombre, Tamaño (bytes) y Ratio vs Original de cada archivo.
     */
    private void showStatistics() {
        String[] columnNames = {"Archivo", "Nombre", "Tamaño (bytes)", "Ratio vs Original"};
        Object[][] data = new Object[3][4];

        long originalSize = -1;

        // Fila: Archivo Original
        if (loadedFile != null && loadedFile.exists()) {
            originalSize = loadedFile.length();
            data[0] = new Object[]{"Original", loadedFile.getName(),
                    String.valueOf(originalSize), "100.0%"};
        } else {
            data[0] = new Object[]{"Original", "N/A", "N/A", "N/A"};
        }

        // Fila: Archivo Compactado (.huf)
        if (compressedFile != null && compressedFile.exists()) {
            long hufSize = compressedFile.length();
            String ratio = (originalSize > 0)
                    ? String.format("%.1f%%", (double) hufSize / originalSize * 100)
                    : "N/A";
            data[1] = new Object[]{"Compactado", compressedFile.getName(),
                    String.valueOf(hufSize), ratio};
        } else {
            data[1] = new Object[]{"Compactado", "N/A", "N/A", "N/A"};
        }

        // Fila: Archivo Descompactado (.dhu)
        if (decompressedFile != null && decompressedFile.exists()) {
            long dhuSize = decompressedFile.length();
            String ratio = (originalSize > 0)
                    ? String.format("%.1f%%", (double) dhuSize / originalSize * 100)
                    : "N/A";
            data[2] = new Object[]{"Descompactado", decompressedFile.getName(),
                    String.valueOf(dhuSize), ratio};
        } else {
            data[2] = new Object[]{"Descompactado", "N/A", "N/A", "N/A"};
        }

        // Crear tabla no editable
        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);

        // Crear JDialog modal
        JDialog dialog = new JDialog(this, "Estadísticas de Archivos", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnClose = new JButton("Cerrar");
        btnClose.addActionListener(e -> dialog.dispose());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(btnClose);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setSize(550, 220);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Muestra un JDialog modal con el diagrama de reducción de fuente de Huffman.
     */
    private void showTree() {
        if (loadedFile == null || textAreaOriginal.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cargue un archivo primero.",
                    "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String text = textAreaOriginal.getText();
        Map<Character, Integer> frequencies = engine.countFrequencies(text);
        List<List<HuffmanNode>> steps = engine.getReductionSteps(frequencies);
        Map<Character, String> codes = engine.buildCodes(frequencies);

        // Construir el árbol una vez más para obtener códigos por nodo
        HuffmanNode root = engine.buildTree(frequencies);
        Map<HuffmanNode, String> nodeCodes = engine.getNodeCodes(root);

        ReductionPanel panel = new ReductionPanel(steps, codes, nodeCodes);
        JScrollPane scroll = new JScrollPane(panel);
        scroll.getHorizontalScrollBar().setUnitIncrement(20);
        scroll.getVerticalScrollBar().setUnitIncrement(20);

        JDialog dialog = new JDialog(this,
                "Reducción de Fuente — " + loadedFile.getName(), true);
        dialog.setLayout(new BorderLayout());
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.setSize(950, 600);
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Obtiene el nombre base de un archivo (sin extensión).
     * @param fileName nombre del archivo con extensión
     * @return nombre sin extensión
     */
    private String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }

    // ═══════════════════════════════════════════════════════════════
    //  CLASE INTERNA: Panel de dibujo del diagrama de reducción
    // ═══════════════════════════════════════════════════════════════

    /**
     * Panel que dibuja el proceso de reducción de fuente de Huffman por columnas.
     * Cada columna es un paso de la reducción; los nodos se muestran como
     * rectángulos redondeados con flechas de fusión entre columnas adyacentes.
     */
    private static class ReductionPanel extends JPanel {

        private static final int COL_WIDTH  = 140;
        private static final int ROW_HEIGHT = 60;
        private static final int NODE_W     = 110;
        private static final int NODE_H     = 36;
        private static final int MARGIN_X   = 30;
        private static final int MARGIN_Y   = 40;

        private final List<List<HuffmanNode>> steps;
        private final Map<Character, String>  charCodes;
        private final Map<HuffmanNode, String> nodeCodes;

        /**
         * @param steps     columnas de la reducción
         * @param charCodes códigos finales carácter → bits
         * @param nodeCodes códigos asignados a cada nodo del árbol (por identidad)
         */
        ReductionPanel(List<List<HuffmanNode>> steps,
                       Map<Character, String> charCodes,
                       Map<HuffmanNode, String> nodeCodes) {
            this.steps     = steps;
            this.charCodes = charCodes;
            this.nodeCodes = nodeCodes;
            setBackground(Color.WHITE);
        }

        @Override
        public Dimension getPreferredSize() {
            int maxRows = 0;
            for (List<HuffmanNode> col : steps) {
                maxRows = Math.max(maxRows, col.size());
            }
            int width  = MARGIN_X * 2 + steps.size() * COL_WIDTH + NODE_W;
            int height = MARGIN_Y * 2 + maxRows * ROW_HEIGHT + 60;
            return new Dimension(width, height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            if (steps.isEmpty()) return;

            // ── Cabeceras de columnas ──
            Font headerFont = new Font("SansSerif", Font.BOLD, 12);
            g2.setFont(headerFont);
            g2.setColor(new Color(0x555555));
            for (int i = 0; i < steps.size(); i++) {
                String label;
                if (i == 0) {
                    label = "Original";
                } else if (i == steps.size() - 1) {
                    label = "Final";
                } else {
                    label = "Paso " + i;
                }
                int x = MARGIN_X + i * COL_WIDTH + NODE_W / 2;
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, x - fm.stringWidth(label) / 2, MARGIN_Y - 10);
            }

            // ── Dibujar flechas (debajo de los nodos para que no tapen) ──
            drawArrows(g2);

            // ── Dibujar nodos ──
            drawNodes(g2);
        }

        /** Dibuja todos los nodos rectángulo con texto. */
        private void drawNodes(Graphics2D g2) {
            Font mainFont  = new Font("SansSerif", Font.PLAIN, 12);
            Font codeFont  = new Font("SansSerif", Font.BOLD, 10);

            for (int col = 0; col < steps.size(); col++) {
                List<HuffmanNode> column = steps.get(col);
                for (int row = 0; row < column.size(); row++) {
                    HuffmanNode node = column.get(row);
                    int cx = MARGIN_X + col * COL_WIDTH + NODE_W / 2;
                    int cy = MARGIN_Y + row * ROW_HEIGHT + NODE_H / 2;
                    int rx = cx - NODE_W / 2;
                    int ry = cy - NODE_H / 2;

                    // ─ Fondo ─
                    if (col == 0 && node.character != null) {
                        g2.setColor(new Color(0xAED6F1));   // azul claro (hoja original)
                    } else if (node.isMerged) {
                        g2.setColor(new Color(0xFAD7A0));   // naranja claro (fusionado)
                    } else {
                        g2.setColor(new Color(0xD5F5E3));   // verde claro (nodo hoja en col > 0)
                    }
                    g2.fillRoundRect(rx, ry, NODE_W, NODE_H, 10, 10);

                    // ─ Borde ─
                    g2.setColor(new Color(0x555555));
                    g2.drawRoundRect(rx, ry, NODE_W, NODE_H, 10, 10);

                    // ─ Texto principal ─
                    g2.setFont(mainFont);
                    g2.setColor(Color.BLACK);
                    String text = buildNodeText(node, col);
                    FontMetrics fm = g2.getFontMetrics();

                    // Si hay código, subir el texto principal un poco
                    String codeStr = getLeafCode(node);
                    if (codeStr != null) {
                        g2.drawString(text, cx - fm.stringWidth(text) / 2, cy - 2);
                        // Código en rojo debajo
                        g2.setFont(codeFont);
                        g2.setColor(Color.RED);
                        String codeLabel = "→ " + codeStr;
                        FontMetrics fmCode = g2.getFontMetrics();
                        g2.drawString(codeLabel,
                                cx - fmCode.stringWidth(codeLabel) / 2, cy + 12);
                    } else {
                        g2.drawString(text, cx - fm.stringWidth(text) / 2, cy + fm.getAscent() / 2 - 1);
                    }
                }
            }
        }

        /** Construye el texto para mostrar dentro de un nodo. */
        private String buildNodeText(HuffmanNode node, int col) {
            String prob = String.format("%.2f", node.probability);
            if (col == 0 && node.character != null) {
                String ch = charDisplayName(node.character);
                return ch + "  |  " + prob;
            }
            return prob;
        }

        /** Nombre legible de caracteres especiales. */
        private String charDisplayName(char c) {
            if (c == ' ')  return "SP";
            if (c == '\n') return "NL";
            if (c == '\t') return "TAB";
            if (c == '\r') return "CR";
            return String.valueOf(c);
        }

        /**
         * Devuelve el código asignado a un nodo hoja, o null si es nodo interno.
         * Primero busca por identidad en nodeCodes; si no está, usa charCodes.
         */
        private String getLeafCode(HuffmanNode node) {
            // Buscar por identidad de nodo
            String code = nodeCodes.get(node);
            if (code != null && node.character != null) return code;
            // Fallback: si es hoja con carácter, buscar en charCodes
            if (node.character != null) {
                return charCodes.get(node.character);
            }
            return null;
        }

        /** Dibuja las flechas de conexión entre columnas adyacentes. */
        private void drawArrows(Graphics2D g2) {
            Stroke defaultStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(1.5f));
            Font bitFont = new Font("SansSerif", Font.BOLD, 11);

            for (int col = 1; col < steps.size(); col++) {
                List<HuffmanNode> prevCol = steps.get(col - 1);
                List<HuffmanNode> currCol = steps.get(col);

                for (int row = 0; row < currCol.size(); row++) {
                    HuffmanNode node = currCol.get(row);
                    int destX = MARGIN_X + col * COL_WIDTH;
                    int destY = MARGIN_Y + row * ROW_HEIGHT + NODE_H / 2;

                    if (node.isMerged && node.left != null && node.right != null) {
                        // ── Flechas convergentes de fusión ──
                        int leftIdx  = findNodeIndex(prevCol, node.left);
                        int rightIdx = findNodeIndex(prevCol, node.right);

                        if (leftIdx >= 0) {
                            int srcX = MARGIN_X + (col - 1) * COL_WIDTH + NODE_W;
                            int srcY = MARGIN_Y + leftIdx * ROW_HEIGHT + NODE_H / 2;
                            drawCurvedArrow(g2, srcX, srcY, destX, destY,
                                    new Color(0x2E86C1), "0", bitFont);
                        }
                        if (rightIdx >= 0) {
                            int srcX = MARGIN_X + (col - 1) * COL_WIDTH + NODE_W;
                            int srcY = MARGIN_Y + rightIdx * ROW_HEIGHT + NODE_H / 2;
                            drawCurvedArrow(g2, srcX, srcY, destX, destY,
                                    new Color(0xC0392B), "1", bitFont);
                        }
                    } else {
                        // ── Flecha horizontal: nodo que pasa sin cambios ──
                        int srcIdx = findMatchingNodeIndex(prevCol, node);
                        if (srcIdx >= 0) {
                            int srcX = MARGIN_X + (col - 1) * COL_WIDTH + NODE_W;
                            int srcY = MARGIN_Y + srcIdx * ROW_HEIGHT + NODE_H / 2;
                            g2.setColor(new Color(0x888888));
                            g2.drawLine(srcX, srcY, destX, destY);
                            // Punta de flecha
                            drawArrowHead(g2, srcX, srcY, destX, destY);
                        }
                    }
                }
            }
            g2.setStroke(defaultStroke);
        }

        /** Dibuja una flecha curva con etiqueta de bit. */
        private void drawCurvedArrow(Graphics2D g2, int x1, int y1, int x2, int y2,
                                     Color color, String bitLabel, Font font) {
            g2.setColor(color);
            double ctrlX = (x1 + x2) / 2.0;
            double ctrlY = (y1 + y2) / 2.0;
            // Desplazar el punto de control para hacer la curva más visible
            double dy = y2 - y1;
            if (Math.abs(dy) < 5) {
                // Casi horizontal: curva leve hacia arriba
                ctrlY -= 10;
            }
            QuadCurve2D curve = new QuadCurve2D.Double(x1, y1, ctrlX, ctrlY, x2, y2);
            g2.draw(curve);

            // Punta de flecha
            drawArrowHead(g2, (int) ctrlX, (int) ctrlY, x2, y2);

            // Etiqueta de bit
            g2.setFont(font);
            g2.setColor(Color.RED);
            FontMetrics fm = g2.getFontMetrics();
            int labelX = (int) ((x1 + ctrlX) / 2.0);
            int labelY = (int) ((y1 + ctrlY) / 2.0) - 4;
            g2.drawString(bitLabel, labelX - fm.stringWidth(bitLabel) / 2, labelY);
        }

        /** Dibuja una punta de flecha pequeña. */
        private void drawArrowHead(Graphics2D g2, int fromX, int fromY, int toX, int toY) {
            double angle = Math.atan2(toY - fromY, toX - fromX);
            int len = 8;
            double a1 = angle - Math.PI / 6;
            double a2 = angle + Math.PI / 6;
            int[] xPoints = {toX, toX - (int) (len * Math.cos(a1)), toX - (int) (len * Math.cos(a2))};
            int[] yPoints = {toY, toY - (int) (len * Math.sin(a1)), toY - (int) (len * Math.sin(a2))};
            g2.fillPolygon(xPoints, yPoints, 3);
        }

        /** Busca un nodo por identidad de referencia en una columna. */
        private int findNodeIndex(List<HuffmanNode> column, HuffmanNode target) {
            for (int i = 0; i < column.size(); i++) {
                if (column.get(i) == target) return i;
            }
            return -1;
        }

        /**
         * Busca un nodo correspondiente en la columna previa (no fusionado).
         * Coincide por identidad de referencia (mismo objeto Java).
         */
        private int findMatchingNodeIndex(List<HuffmanNode> prevCol, HuffmanNode node) {
            for (int i = 0; i < prevCol.size(); i++) {
                if (prevCol.get(i) == node) return i;
            }
            return -1;
        }
    }
}
