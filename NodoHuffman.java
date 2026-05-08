// Nodo del arbol de Huffman. Puede ser hoja (valorByte != null) o nodo interno (valorByte == null)

public class NodoHuffman {
    public double probabilidad;
    public Byte valorByte;          // byte del archivo original (null = nodo interno)
    public NodoHuffman izquierdo;
    public NodoHuffman derecho;
    public boolean esFusionado;     //sirve para ver si es una combinacion de dos o mas nodos

    public NodoHuffman() {
        this.probabilidad = 0.0;
        this.valorByte = null;
        this.izquierdo = null;
        this.derecho = null;
        this.esFusionado = false;
    }
}
