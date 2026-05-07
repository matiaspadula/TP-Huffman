// Nodo del arbol de Huffman. Puede ser hoja (caracter != null) o nodo interno (caracter == null)

public class NodoHuffman {
    public double probabilidad;
    public Character caracter;
    public NodoHuffman izquierdo;
    public NodoHuffman derecho;
    public boolean esFusionado; //sirve para ver si es una combinacion de dos o mas nodos

    public NodoHuffman() {
        this.probabilidad = 0.0;
        this.caracter = null;
        this.izquierdo = null;
        this.derecho = null;
        this.esFusionado = false;
    }
}
