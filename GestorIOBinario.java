import java.io.*;
import java.util.*;

//basicamente mete varios caracteres en un byte
public class GestorIOBinario {

    /*
     * Comprime un archivo a formato .huf (opera a nivel de byte)
     * Estructura del archivo .huf:
     *   [8 bytes] long  → cantidad total de bytes originales
     *   [4 bytes] int   → tamaño del alfabeto (N, max 256)
     *   Por cada símbolo: [1 byte] valor + [4 bytes] int frecuencia
     *   [resto]          → cuerpo compactado con acumulador de bits
     */
    public void comprimir(File archivoEntrada, File archivoSalida, MotorHuffman motor) throws IOException {
        byte[] datosOriginales;
        // lee todo el archivo de una vez como bytes crudos
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archivoEntrada))) {
            datosOriginales = bis.readAllBytes();
        }

        if (datosOriginales.length == 0) {
            throw new IOException("El archivo de entrada está vacío.");
        }

        // conseguir frecuencias y códigos
        Map<Byte, Integer> frecuencias = motor.contarFrecuencias(datosOriginales);
        Map<Byte, String> codigos = motor.construirCodigos(frecuencias);

        // escribir archivo .huf
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(archivoSalida)))) {

            //header
            dos.writeLong(datosOriginales.length);       // cuantos bytes habian (8 bytes)
            dos.writeInt(frecuencias.size());             // cuantos simbolos unicos hay (4 bytes)

            // tabla de frecuencias, por cada simbolo se escribe...
            for (Map.Entry<Byte, Integer> entrada : frecuencias.entrySet()) {
                dos.writeByte(entrada.getKey());           // byte del símbolo (1 byte)
                dos.writeInt(entrada.getValue());          // frecuencia (4 bytes)
            }

            int buffer = 0; //contenedor temporal de bits
            int contadorBits = 0; //lleva la cuenta de cuantos bits se acumularon

            for (byte b : datosOriginales) {
                String codigo = codigos.get(b);

                /*aca es donde se comprimen varios bytes en menos bits
                para "1", "000", "001" (datos con bytes A, D, B):
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
     * Descomprime un archivo .huf a su formato original .dhu.
     * Lee el header para reconstruir la tabla de frecuencias,
     * reconstruye el árbol de Huffman, y decodifica el cuerpo
     * deteniéndose exactamente al alcanzar cantidadBytesOriginal bytes.
     */
    public void descomprimir(File archivoHuf, File archivoSalida, MotorHuffman motor) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(archivoHuf)))) {

            // lee header
            long cantidadBytesOriginal = dis.readLong();    // 8 bytes
            int tamanoAlfabeto = dis.readInt();              // 4 bytes

            //reconstruye tabla de frecuencias
            Map<Byte, Integer> frecuencias = new LinkedHashMap<>();
            for (int i = 0; i < tamanoAlfabeto; i++) {
                byte b = dis.readByte();                    // 1 byte
                int frec = dis.readInt();                   // 4 bytes
                frecuencias.put(b, frec);
            }

            // reconstruye arbol de Huffman con construirArbol() y obtiene el mismo arbol que uso para comprimir
            NodoHuffman raiz = motor.construirArbol(frecuencias);

            // lee los bytes desp del header y los pasa a decodificar con el arbol y la cant de bytes a recuperar
            byte[] bytesCuerpo = dis.readAllBytes();
            byte[] decodificado = motor.decodificar(raiz, bytesCuerpo, cantidadBytesOriginal);

            // Escribe archivo de salida 
            try (BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(archivoSalida))) {
                bos.write(decodificado);
            }
        }
    }
}
