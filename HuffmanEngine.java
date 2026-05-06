import java.util.*;

public class HuffmanEngine {

    //cuenta frecuencia de cada caracter
    public Map<Character, Integer> countFrequencies(String text) {
        Map<Character, Integer> frequencies = new LinkedHashMap<>(); //linkedhashmap para mantener orden de insercion
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            frequencies.merge(c, 1, Integer::sum); //si el caracter no existe lo agrega con valor 1, si existia suma uno
        }
        return frequencies;
    }

    // construye el arbol en base a las frecuencias
    public HuffmanNode buildTree(Map<Character, Integer> frequencies) {
        if (frequencies == null || frequencies.isEmpty()) { //si no hay texto mandamos pal lobby
            return null;
        }

        int totalChars = 0;
        for (int freq : frequencies.values()) {
            totalChars += freq; //calculamos la cantidad total de caracteres del txt
        }

        List<HuffmanNode> nodes = new ArrayList<>();
        for (Map.Entry<Character, Integer> entry : frequencies.entrySet()) { // creamos lista inicial de nodos iniciales
            HuffmanNode node = new HuffmanNode();
            node.character = entry.getKey();
            node.probability = (double) entry.getValue() / totalChars; //sacamos probabilidad de caracter
            node.isMerged = false;//marcamos falso pq el nodo es del caracter en si
            nodes.add(node);
        }

        // caso especial: un solo carácter
        if (nodes.size() == 1) {
            HuffmanNode root = new HuffmanNode();
            root.probability = 1.0;
            root.isMerged = true;
            root.left = nodes.get(0); //al no haber nada para fusionar, dejamos al unico nodo como hoja de una raiz artificial
            root.right = null;
            return root;
        }
        sortNodes(nodes); //ordenar descendente por probabilidad
        // Fase de reduccion: repetir hasta que queden exactamente 2 nodos
        while (nodes.size() > 2) {
            int ultIndice = nodes.size() - 1;
            // Tomar los dos de menor probabilidad
            HuffmanNode lower = nodes.remove(ultIndice);       // último = menor prob
            HuffmanNode upper = nodes.remove(ultIndice - 1);   // penúltimo
            HuffmanNode parent = new HuffmanNode();  // Crear nodo padre fusionado
            parent.probability = upper.probability + lower.probability;
            parent.isMerged = true;
            parent.character = null;
            parent.left = upper;
            parent.right = lower;
            // Reinsertar en la lista
            nodes.add(parent);
            sortNodes(nodes);
        }
        // creamos raíz vacia con los 2 nodos finales como hijos
        HuffmanNode root = new HuffmanNode();
        root.probability = 1.0;
        root.isMerged = true;
        root.character = null;
        root.left = nodes.get(0);   // superior → código "0"
        root.right = nodes.get(1);  // inferior → código "1"
        return root;
    }

    //Ordena nodos de MAYOR a MENOR probabilidad.
    private void sortNodes(List<HuffmanNode> nodes) {
        nodes.sort((a, b) -> {
            // 1. Descendente por probabilidad, los mas probables van primero y los menos al último
            int cmp = Double.compare(b.probability, a.probability);
            if (cmp != 0) return cmp;
            // 2. En caso de empate, los nodos fusionados primero
            if (a.isMerged && !b.isMerged) return -1;
            if (!a.isMerged && b.isMerged) return 1;
            // 3. Si son iguales, ordenamos por carácter
            if (a.character != null && b.character != null) {
                return Character.compare(a.character, b.character);
            }
            // 4. Si son ambos fusionados, mantenemos orden
            return 0;
        });
    }

    // punto de entrada al backtracking desde raiz
    public Map<Character, String> buildCodes(Map<Character, Integer> frequencies) {
        HuffmanNode root = buildTree(frequencies);
        Map<Character, String> codes = new LinkedHashMap<>();
        if (root == null) return codes;

        // si solo hay un solo caracter no tenemos nada que recorrer, asignamos 0 y listo
        if (root.right == null && root.left != null) {
            codes.put(root.left.character, "0");
            return codes;
        }

        // asigna 0 al nodo superior (left) y 1 al inferior (right),
        // luego le pasa la pelota a assigncodes para seguir recursivamente
        assignCodes(root.left, "0", codes);
        assignCodes(root.right, "1", codes);

        return codes;
    }

    //asigna codigos recorriendo recursivamente el arbol, el hijo izquiedo hereda cod de padre + 0
    //el hijo derecho hereda cod de padre + 1
    private void assignCodes(HuffmanNode node, String code, Map<Character, String> codes) {
        if (node == null) return;
        // si es nodo terminal (tiene caracter) guarda codigo acumulado hasta ahi y para
        if (node.character != null) {
            codes.put(node.character, code);
            return;
        }
        // si no tiene caracter es una fusion y nodo interno, se llama dos veces
        //una por hijo para concatenar 0 o 1 al codigo que ya traia
        assignCodes(node.left, code + "0", codes);
        assignCodes(node.right, code + "1", codes);
    }

    //recorre el archivo bit a bit, cada vez que llega a una hoja anota el caracter
    //asi sucesivamente hasta recuperar el texto original
    public String decode(HuffmanNode root, byte[] compressedData, long originalCharCount) {
        if (root == null || originalCharCount == 0) return "";

        StringBuilder sb = new StringBuilder();

        //si tiene un solo carácter (hijo izq), se repite el caracter originalCharCount veces, no hace falta leer bits
        if (root.right == null && root.left != null) {
            char singleChar = root.left.character;
            for (long i = 0; i < originalCharCount; i++) {
                sb.append(singleChar);
            }
            return sb.toString();
        }

        HuffmanNode current = root;
        long charCount = 0;

        for (int byteIdx = 0; byteIdx < compressedData.length; byteIdx++) {
            int unsignedByte = compressedData[byteIdx] & 0xFF; //convertimos a unsigned con 0xff
            for (int bitIdx = 7; bitIdx >= 0; bitIdx--) { //extraemos cada bit del byte
                int bit = (unsignedByte >> bitIdx) & 1;

                if (bit == 0) { //navegamos arbol
                    current = current.left;
                } else {
                    current = current.right;
                }

                // Llegamos a una hoja y guardamos caracter
                if (current.character != null) {
                    sb.append(current.character);
                    charCount++;
                    if (charCount == originalCharCount) {
                        return sb.toString();
                    }
                    current = root; // volver a la raíz para seguir
                }
            }
        }

        return sb.toString();
    }

    //lo mismo que buildtree() pero devuelve el estado de la estructura en cada reduccion para poder mostrarlo
    public List<List<HuffmanNode>> getReductionSteps(Map<Character, Integer> frequencies) {
        List<List<HuffmanNode>> steps = new ArrayList<>();

        if (frequencies == null || frequencies.isEmpty()) {
            return steps;
        }

        int totalChars = 0;
        for (int freq : frequencies.values()) {
            totalChars += freq;
        }

        // Crear lista inicial de nodos hoja
        List<HuffmanNode> nodes = new ArrayList<>();
        for (Map.Entry<Character, Integer> entry : frequencies.entrySet()) {
            HuffmanNode node = new HuffmanNode();
            node.character = entry.getKey();
            node.probability = (double) entry.getValue() / totalChars;
            node.isMerged = false;
            nodes.add(node);
        }

        // Caso especial: un solo carácter
        if (nodes.size() == 1) {
            sortNodes(nodes);
            steps.add(new ArrayList<>(nodes));
            return steps;
        }

        // Ordenar descendente por probabilidad
        sortNodes(nodes);
        // Guardar columna inicial (deep copy)
        steps.add(new ArrayList<>(nodes));

        // Fase de reducción: repetir hasta que queden exactamente 2 nodos
        while (nodes.size() > 2) {
            int lastIdx = nodes.size() - 1;
            HuffmanNode lower = nodes.remove(lastIdx);
            HuffmanNode upper = nodes.remove(lastIdx - 1);

            HuffmanNode parent = new HuffmanNode();
            parent.probability = upper.probability + lower.probability;
            parent.isMerged = true;
            parent.character = null;
            parent.left = upper;
            parent.right = lower;

            nodes.add(parent);
            sortNodes(nodes);

            // Guardar columna (deep copy de la lista, los nodos son los mismos objetos)
            steps.add(new ArrayList<>(nodes));
        }

        return steps;
    }

    //guarda codigos de todos los nodos para el diagrama
    public Map<HuffmanNode, String> getNodeCodes(HuffmanNode root) {
        Map<HuffmanNode, String> nodeCodes = new IdentityHashMap<>();
        if (root == null) return nodeCodes;

        //un solo carácter
        if (root.right == null && root.left != null) {
            nodeCodes.put(root.left, "0");
            return nodeCodes;
        }

        assignNodeCodes(root.left, "0", nodeCodes);
        assignNodeCodes(root.right, "1", nodeCodes);
        return nodeCodes;
    }

    //guarda todos los codigos asignados, no solo los de las hojas como assigncodes()
    private void assignNodeCodes(HuffmanNode node, String code, Map<HuffmanNode, String> nodeCodes) {
        if (node == null) return;
        nodeCodes.put(node, code);
        assignNodeCodes(node.left, code + "0", nodeCodes);
        assignNodeCodes(node.right, code + "1", nodeCodes);
    }

    /**
     * Prueba
     * S={A,B,C,D,E}, P={0.5, 0.1, 0.1, 0.2, 0.1}
     * Esperado: A→"1", D→"000", B→"001", C→"010", E→"011"
     */
    public static void selfTest() {
        HuffmanEngine engine = new HuffmanEngine();
        Map<Character, Integer> freq = new LinkedHashMap<>();
        freq.put('A', 5);
        freq.put('B', 1);
        freq.put('C', 1);
        freq.put('D', 2);
        freq.put('E', 1);

        Map<Character, String> codes = engine.buildCodes(freq);

        Map<Character, String> expected = new LinkedHashMap<>();
        expected.put('A', "1");
        expected.put('D', "000");
        expected.put('B', "001");
        expected.put('C', "010");
        expected.put('E', "011");

        boolean passed = true;
        for (Map.Entry<Character, String> entry : expected.entrySet()) {
            String actual = codes.get(entry.getKey());
            if (!entry.getValue().equals(actual)) {
                System.err.println("[SELF-TEST FAIL] " + entry.getKey()
                        + " esperado=" + entry.getValue() + " obtenido=" + actual);
                passed = false;
            }
        }
        if (passed) {
            System.out.println("[SELF-TEST OK] Todos los códigos coinciden con el ejemplo de validación.");
        } else {
            System.err.println("[SELF-TEST FAIL] Hay errores en la generación de códigos.");
        }
    }
}
