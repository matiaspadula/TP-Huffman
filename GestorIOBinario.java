import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

//basicamente mete varios caracteres en un byte
public class GestorIOBinario {

    /*
     * Comprime un archivo de texto a formato .huf
     * Estructura del archivo .huf:
     *   [8 bytes] long  → cantidad total de caracteres originales
     *   [4 bytes] int   → tamaño del alfabeto
     *   Por cada símbolo: [2 bytes] char + [4 bytes] int frecuencia
     *   [resto]          → cuerpo compactado con acumulador de bits
     */
    public void comprimir(File archivoEntrada, File archivoSalida, MotorHuffman motor) throws IOException {
        String texto;
        // lee todo el archivo de una vez y lo convierte en un string UTF-8
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archivoEntrada))) {
            byte[] bytesOriginales = bis.readAllBytes();
            texto = new String(bytesOriginales, StandardCharsets.UTF_8);
        }

        if (texto.isEmpty()) {
            throw new IOException("El archivo de entrada está vacío.");
        }

        // conseguir frecuencias y códigos
        Map<Character, Integer> frecuencias = motor.contarFrecuencias(texto);
        Map<Character, String> codigos = motor.construirCodigos(frecuencias);

        // escribir archivo .huf
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(archivoSalida)))) {

            //header
            dos.writeLong(texto.length());            // cuantos caracteres habian (8bytes)
            dos.writeInt(frecuencias.size());         // cuantos simbolos unicos hay(4 bytes)

            // tabla de frecuencias, por cada simbolo se escribe...
            for (Map.Entry<Character, Integer> entrada : frecuencias.entrySet()) {
                dos.writeChar(entrada.getKey());        // carácter
                dos.writeInt(entrada.getValue());       // frecuencia
            }

            int buffer = 0; //contenedor temporal de bits
            int contadorBits = 0; //lleva la cuenta de cuantos bits se acumularon

            for (int i = 0; i < texto.length(); i++) {
                char c = texto.charAt(i);
                String codigo = codigos.get(c);

                /*aca es donde se comprimen varios caracteres en un bit
                para "1", "000", "001" (texto "ADB"):
                1     → buffer=1,        contadorBits=1
                0     → buffer=10,       contadorBits=2
                0     → buffer=100,      contadorBits=3
                0     → buffer=1000,     contadorBits=4
                0     → buffer=10000,    contadorBits=5
                0     → buffer=100000,   contadorBits=6
                1     → buffer=1000001,  contadorBits=7*/
                for (int j = 0; j < codigo.length(); j++) {
                    int bit = (codigo.charAt(j) == '1') ? 1 : 0;
                    buffer = (buffer << 1) | bit;
                    contadorBits++;

                    if (contadorBits == 8) {
                        dos.write(buffer);
                        buffer = 0;
                        contadorBits = 0;
                    }
                }
            }

            // completamos el último byte con ceros a la derecha
            if (contadorBits > 0) {
                buffer = buffer << (8 - contadorBits);
                dos.write(buffer);
            }
        }
    }

    /*
     * Descomprime un archivo .huf a texto plano .dhu.
     * Lee el header para reconstruir la tabla de frecuencias,
     * reconstruye el árbol de Huffman, y decodifica el cuerpo
     * deteniéndose exactamente al alcanzar cantidadCaracteresOriginal caracteres.
     */
    public void descomprimir(File archivoHuf, File archivoSalida, MotorHuffman motor) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(archivoHuf)))) {

            // lee header
            long cantidadCaracteresOriginal = dis.readLong();   // 8 bytes
            int tamanoAlfabeto = dis.readInt();           // 4 bytes

            //reconstruye tabla de frecuencias
            Map<Character, Integer> frecuencias = new LinkedHashMap<>();
            for (int i = 0; i < tamanoAlfabeto; i++) {
                char c = dis.readChar();                // 2 bytes
                int frec = dis.readInt();               // 4 bytes
                frecuencias.put(c, frec);
            }

            // reconstruye arbol de Huffman con construirArbol() y obtiene el mismo arbol que uso para comprimir
            NodoHuffman raiz = motor.construirArbol(frecuencias);

            // lee los bytes desp del header y los pasa a decodificar con el arbol y la cant de caracteres a recuperar
            byte[] bytesCuerpo = dis.readAllBytes();
            String decodificado = motor.decodificar(raiz, bytesCuerpo, cantidadCaracteresOriginal);

            // Escribe archivo de salida
            try (BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(archivoSalida))) {
                bos.write(decodificado.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
