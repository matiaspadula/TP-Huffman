// Nodo del arbol de Huffman. Puede ser hoja (character != null) o nodo interno (character == null)

public class HuffmanNode {
    public double probability;
    public Character character;
    public HuffmanNode left;
    public HuffmanNode right;
    public boolean isMerged; //sirve para ver si es una combinacion de dos o mas nodos

    public HuffmanNode() {
        this.probability = 0.0;
        this.character = null;
        this.left = null;
        this.right = null;
        this.isMerged = false;
    }
}
