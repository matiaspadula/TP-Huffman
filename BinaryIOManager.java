import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

//basicamente mete varios caracteres en un byte
public class BinaryIOManager {

    /*
     * Comprime un archivo de texto a formato .huf
     * Estructura del archivo .huf:
     *   [8 bytes] long  → cantidad total de caracteres originales
     *   [4 bytes] int   → tamaño del alfabeto
     *   Por cada símbolo: [2 bytes] char + [4 bytes] int frecuencia
     *   [resto]          → cuerpo compactado con acumulador de bits
     */
    public void compress(File inputFile, File outputFile, HuffmanEngine engine) throws IOException {
        String text;
        // lee todo el archivo de una vez y lo convierte en un string UTF-8
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inputFile))) {
            byte[] rawBytes = bis.readAllBytes();
            text = new String(rawBytes, StandardCharsets.UTF_8);
        }

        if (text.isEmpty()) {
            throw new IOException("El archivo de entrada está vacío.");
        }

        // conseguir frecuencias y códigos
        Map<Character, Integer> frequencies = engine.countFrequencies(text);
        Map<Character, String> codes = engine.buildCodes(frequencies);

        // escribir archivo .huf
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputFile)))) {

            //header
            dos.writeLong(text.length());            // cuantos caracteres habian (8bytes)
            dos.writeInt(frequencies.size());         // cuantos simbolos unicos hay(4 bytes)

            // tabla de frecuencias, por cada simbolo se escribe...
            for (Map.Entry<Character, Integer> entry : frequencies.entrySet()) {
                dos.writeChar(entry.getKey());        // carácter
                dos.writeInt(entry.getValue());       // frecuencia
            }

            int buffer = 0; //contenedor temporal de bits
            int bitCount = 0; //lleva la cuenta de cuantos bits se acumularon

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                String code = codes.get(c);

                /*aca es donde se comprimen varios caracteres en un bit
                para "1", "000", "001" (texto "ADB"):
                1     → buffer=1,        bitCount=1
                0     → buffer=10,       bitCount=2
                0     → buffer=100,      bitCount=3
                0     → buffer=1000,     bitCount=4
                0     → buffer=10000,    bitCount=5
                0     → buffer=100000,   bitCount=6
                1     → buffer=1000001,  bitCount=7*/
                for (int j = 0; j < code.length(); j++) {
                    int bit = (code.charAt(j) == '1') ? 1 : 0;
                    buffer = (buffer << 1) | bit;
                    bitCount++;

                    if (bitCount == 8) {
                        dos.write(buffer);
                        buffer = 0;
                        bitCount = 0;
                    }
                }
            }

            // completamos el último byte con ceros a la derecha
            if (bitCount > 0) {
                buffer = buffer << (8 - bitCount);
                dos.write(buffer);
            }
        }
    }

    /*
     * Descomprime un archivo .huf a texto plano .dhu.
     * Lee el header para reconstruir la tabla de frecuencias,
     * reconstruye el árbol de Huffman, y decodifica el cuerpo
     * deteniéndose exactamente al alcanzar originalCharCount caracteres.
     */
    public void decompress(File hufFile, File outputFile, HuffmanEngine engine) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(hufFile)))) {

            // lee header
            long originalCharCount = dis.readLong();   // 8 bytes
            int alphabetSize = dis.readInt();           // 4 bytes

            //reconstruye tabla de frecuencias
            Map<Character, Integer> frequencies = new LinkedHashMap<>();
            for (int i = 0; i < alphabetSize; i++) {
                char c = dis.readChar();                // 2 bytes
                int freq = dis.readInt();               // 4 bytes
                frequencies.put(c, freq);
            }

            // reconstruye arbol de Huffman con buildtree() y obtiene el mismo arbol que uso para comprimir
            HuffmanNode root = engine.buildTree(frequencies);

            // lee los bytes desp del header y los pasa a decode con el arbol y la cant de caracteres a recuperar
            byte[] bodyBytes = dis.readAllBytes();
            String decoded = engine.decode(root, bodyBytes, originalCharCount);

            // Escribe archivo de salida
            try (BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(outputFile))) {
                bos.write(decoded.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
