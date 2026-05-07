import java.util.*;

public class MotorHuffman {

    //cuenta frecuencia de cada caracter
    public Map<Character, Integer> contarFrecuencias(String texto) {
        Map<Character, Integer> frecuencias = new LinkedHashMap<>(); //linkedhashmap para mantener orden de insercion
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            frecuencias.merge(c, 1, Integer::sum); //si el caracter no existe lo agrega con valor 1, si existia suma uno
        }
        return frecuencias;
    }

    // construye el arbol en base a las frecuencias
    public NodoHuffman construirArbol(Map<Character, Integer> frecuencias) {
        if (frecuencias == null || frecuencias.isEmpty()) { //si no hay texto mandamos pal lobby
            return null;
        }

        int totalCaracteres = 0;
        for (int frec : frecuencias.values()) {
            totalCaracteres += frec; //calculamos la cantidad total de caracteres del txt
        }

        List<NodoHuffman> nodos = new ArrayList<>();
        for (Map.Entry<Character, Integer> entrada : frecuencias.entrySet()) { // creamos lista inicial de nodos iniciales
            NodoHuffman nodo = new NodoHuffman();
            nodo.caracter = entrada.getKey();
            nodo.probabilidad = (double) entrada.getValue() / totalCaracteres; //sacamos probabilidad de caracter
            nodo.esFusionado = false;//marcamos falso pq el nodo es del caracter en si
            nodos.add(nodo);
        }

        // caso especial: un solo carácter
        if (nodos.size() == 1) {
            NodoHuffman raiz = new NodoHuffman();
            raiz.probabilidad = 1.0;
            raiz.esFusionado = true;
            raiz.izquierdo = nodos.get(0); //al no haber nada para fusionar, dejamos al unico nodo como hoja de una raiz artificial
            raiz.derecho = null;
            return raiz;
        }
        ordenarNodos(nodos); //ordenar descendente por probabilidad
        // Fase de reduccion: repetir hasta que queden exactamente 2 nodos
        while (nodos.size() > 2) {
            int ultIndice = nodos.size() - 1;
            // Tomar los dos de menor probabilidad
            NodoHuffman inferior = nodos.remove(ultIndice);       // último = menor prob
            NodoHuffman superior = nodos.remove(ultIndice - 1);   // penúltimo
            NodoHuffman padre = new NodoHuffman();  // Crear nodo padre fusionado
            padre.probabilidad = superior.probabilidad + inferior.probabilidad;
            padre.esFusionado = true;
            padre.caracter = null;
            padre.izquierdo = superior;
            padre.derecho = inferior;
            // Reinsertar en la lista
            nodos.add(padre);
            ordenarNodos(nodos);
        }
        // creamos raíz vacia con los 2 nodos finales como hijos
        NodoHuffman raiz = new NodoHuffman();
        raiz.probabilidad = 1.0;
        raiz.esFusionado = true;
        raiz.caracter = null;
        raiz.izquierdo = nodos.get(0);   // superior → código "0"
        raiz.derecho = nodos.get(1);  // inferior → código "1"
        return raiz;
    }

    //Ordena nodos de MAYOR a MENOR probabilidad.
    private void ordenarNodos(List<NodoHuffman> nodos) {
        nodos.sort((a, b) -> {
            // 1. Descendente por probabilidad, los mas probables van primero y los menos al último
            int cmp = Double.compare(b.probabilidad, a.probabilidad);
            if (cmp != 0) return cmp;
            // 2. En caso de empate, los nodos fusionados primero
            if (a.esFusionado && !b.esFusionado) return -1;
            if (!a.esFusionado && b.esFusionado) return 1;
            // 3. Si son iguales, ordenamos por carácter
            if (a.caracter != null && b.caracter != null) {
                return Character.compare(a.caracter, b.caracter);
            }
            // 4. Si son ambos fusionados, mantenemos orden
            return 0;
        });
    }

    // punto de entrada al backtracking desde raiz
    public Map<Character, String> construirCodigos(Map<Character, Integer> frecuencias) {
        NodoHuffman raiz = construirArbol(frecuencias);
        Map<Character, String> codigos = new LinkedHashMap<>();
        if (raiz == null) return codigos;

        // si solo hay un solo caracter no tenemos nada que recorrer, asignamos 0 y listo
        if (raiz.derecho == null && raiz.izquierdo != null) {
            codigos.put(raiz.izquierdo.caracter, "0");
            return codigos;
        }

        // asigna 0 al nodo superior (izquierdo) y 1 al inferior (derecho),
        // luego le pasa la pelota a asignarCodigos para seguir recursivamente
        asignarCodigos(raiz.izquierdo, "0", codigos);
        asignarCodigos(raiz.derecho, "1", codigos);

        return codigos;
    }

    //asigna codigos recorriendo recursivamente el arbol, el hijo izquiedo hereda cod de padre + 0
    //el hijo derecho hereda cod de padre + 1
    private void asignarCodigos(NodoHuffman nodo, String codigo, Map<Character, String> codigos) {
        if (nodo == null) return;
        // si es nodo terminal (tiene caracter) guarda codigo acumulado hasta ahi y para
        if (nodo.caracter != null) {
            codigos.put(nodo.caracter, codigo);
            return;
        }
        // si no tiene caracter es una fusion y nodo interno, se llama dos veces
        //una por hijo para concatenar 0 o 1 al codigo que ya traia
        asignarCodigos(nodo.izquierdo, codigo + "0", codigos);
        asignarCodigos(nodo.derecho, codigo + "1", codigos);
    }

    //recorre el archivo bit a bit, cada vez que llega a una hoja anota el caracter
    //asi sucesivamente hasta recuperar el texto original
    public String decodificar(NodoHuffman raiz, byte[] datosComprimidos, long cantidadCaracteresOriginal) {
        if (raiz == null || cantidadCaracteresOriginal == 0) return "";

        StringBuilder sb = new StringBuilder();

        //si tiene un solo carácter (hijo izq), se repite el caracter cantidadCaracteresOriginal veces, no hace falta leer bits
        if (raiz.derecho == null && raiz.izquierdo != null) {
            char unicoCaracter = raiz.izquierdo.caracter;
            for (long i = 0; i < cantidadCaracteresOriginal; i++) {
                sb.append(unicoCaracter);
            }
            return sb.toString();
        }

        NodoHuffman actual = raiz;
        long contadorCaracteres = 0;

        for (int indiceByte = 0; indiceByte < datosComprimidos.length; indiceByte++) {
            int byteNoFirmado = datosComprimidos[indiceByte] & 0xFF; //convertimos a unsigned con 0xff
            for (int indiceBit = 7; indiceBit >= 0; indiceBit--) { //extraemos cada bit del byte
                int bit = (byteNoFirmado >> indiceBit) & 1;

                if (bit == 0) { //navegamos arbol
                    actual = actual.izquierdo;
                } else {
                    actual = actual.derecho;
                }

                // Llegamos a una hoja y guardamos caracter
                if (actual.caracter != null) {
                    sb.append(actual.caracter);
                    contadorCaracteres++;
                    if (contadorCaracteres == cantidadCaracteresOriginal) {
                        return sb.toString();
                    }
                    actual = raiz; // volver a la raíz para seguir
                }
            }
        }

        return sb.toString();
    }

    //lo mismo que construirArbol() pero devuelve el estado de la estructura en cada reduccion para poder mostrarlo
    public List<List<NodoHuffman>> obtenerPasosReduccion(Map<Character, Integer> frecuencias) {
        List<List<NodoHuffman>> pasos = new ArrayList<>();

        if (frecuencias == null || frecuencias.isEmpty()) {
            return pasos;
        }

        int totalCaracteres = 0;
        for (int frec : frecuencias.values()) {
            totalCaracteres += frec;
        }

        // Crear lista inicial de nodos hoja
        List<NodoHuffman> nodos = new ArrayList<>();
        for (Map.Entry<Character, Integer> entrada : frecuencias.entrySet()) {
            NodoHuffman nodo = new NodoHuffman();
            nodo.caracter = entrada.getKey();
            nodo.probabilidad = (double) entrada.getValue() / totalCaracteres;
            nodo.esFusionado = false;
            nodos.add(nodo);
        }

        // Caso especial: un solo carácter
        if (nodos.size() == 1) {
            ordenarNodos(nodos);
            pasos.add(new ArrayList<>(nodos));
            return pasos;
        }

        // Ordenar descendente por probabilidad
        ordenarNodos(nodos);
        // Guardar columna inicial (deep copy)
        pasos.add(new ArrayList<>(nodos));

        // Fase de reducción: repetir hasta que queden exactamente 2 nodos
        while (nodos.size() > 2) {
            int ultimoIndice = nodos.size() - 1;
            NodoHuffman inferior = nodos.remove(ultimoIndice);
            NodoHuffman superior = nodos.remove(ultimoIndice - 1);

            NodoHuffman padre = new NodoHuffman();
            padre.probabilidad = superior.probabilidad + inferior.probabilidad;
            padre.esFusionado = true;
            padre.caracter = null;
            padre.izquierdo = superior;
            padre.derecho = inferior;

            nodos.add(padre);
            ordenarNodos(nodos);

            // Guardar columna (deep copy de la lista, los nodos son los mismos objetos)
            pasos.add(new ArrayList<>(nodos));
        }

        return pasos;
    }

    //guarda codigos de todos los nodos para el diagrama
    public Map<NodoHuffman, String> obtenerCodigosNodo(NodoHuffman raiz) {
        Map<NodoHuffman, String> codigosNodo = new IdentityHashMap<>();
        if (raiz == null) return codigosNodo;

        //un solo carácter
        if (raiz.derecho == null && raiz.izquierdo != null) {
            codigosNodo.put(raiz.izquierdo, "0");
            return codigosNodo;
        }

        asignarCodigosNodo(raiz.izquierdo, "0", codigosNodo);
        asignarCodigosNodo(raiz.derecho, "1", codigosNodo);
        return codigosNodo;
    }

    //guarda todos los codigos asignados, no solo los de las hojas como asignarCodigos()
    private void asignarCodigosNodo(NodoHuffman nodo, String codigo, Map<NodoHuffman, String> codigosNodo) {
        if (nodo == null) return;
        codigosNodo.put(nodo, codigo);
        asignarCodigosNodo(nodo.izquierdo, codigo + "0", codigosNodo);
        asignarCodigosNodo(nodo.derecho, codigo + "1", codigosNodo);
    }

    /**
     * Prueba
     * S={A,B,C,D,E}, P={0.5, 0.1, 0.1, 0.2, 0.1}
     * Esperado: A→"1", D→"000", B→"001", C→"010", E→"011"
     */
    public static void autoTest() {
        MotorHuffman motor = new MotorHuffman();
        Map<Character, Integer> frec = new LinkedHashMap<>();
        frec.put('A', 5);
        frec.put('B', 1);
        frec.put('C', 1);
        frec.put('D', 2);
        frec.put('E', 1);

        Map<Character, String> codigos = motor.construirCodigos(frec);

        Map<Character, String> esperado = new LinkedHashMap<>();
        esperado.put('A', "1");
        esperado.put('D', "000");
        esperado.put('B', "001");
        esperado.put('C', "010");
        esperado.put('E', "011");

        boolean exitoso = true;
        for (Map.Entry<Character, String> entrada : esperado.entrySet()) {
            String obtenido = codigos.get(entrada.getKey());
            if (!entrada.getValue().equals(obtenido)) {
                System.err.println("[SELF-TEST FAIL] " + entrada.getKey()
                        + " esperado=" + entrada.getValue() + " obtenido=" + obtenido);
                exitoso = false;
            }
        }
        if (exitoso) {
            System.out.println("[SELF-TEST OK] Todos los códigos coinciden con el ejemplo de validación.");
        } else {
            System.err.println("[SELF-TEST FAIL] Hay errores en la generación de códigos.");
        }
    }
}
