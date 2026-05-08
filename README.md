# Compresor Huffman — Laboratorio Nº2 Teoría de la Información (UNSL 2026)

Aplicación en Java 21 con Swing que implementa compresión y descompresión de archivos
mediante el **Método de Reducción de Fuente de Huffman por Columnas**.
Opera a nivel de **byte** (no de carácter), lo que permite comprimir cualquier tipo de archivo
y maximiza la eficiencia del header.

---

## Estructura del proyecto

```
Main.java            → Punto de entrada
NodoHuffman.java     → Nodo del árbol
MotorHuffman.java    → Motor del algoritmo
GestorIOBinario.java → Lectura y escritura binaria
UserInterface.java   → Interfaz gráfica Swing
```

---

## Cómo ejecutar

```bash
javac *.java
java Main
```

Al arrancar, el programa ejecuta automáticamente un **self-test** en consola que
verifica que el algoritmo produce los códigos correctos con el ejemplo del pizarrón:

```
[SELF-TEST OK] Todos los códigos coinciden con el ejemplo de validación.
```

Si aparece `[SELF-TEST FAIL]`, hay un bug en la generación de códigos.

---

## Flujo de ejecución

### 1. El usuario carga un archivo

`UserInterface` abre un `JFileChooser` y lee el archivo como **bytes crudos**
(`byte[]`) con `BufferedInputStream`. Los bytes se almacenan internamente para
la compresión y se muestran como texto UTF-8 en el panel izquierdo de la ventana.

---

### 2. El usuario presiona "Compactar"

`UserInterface` llama a `GestorIOBinario.comprimir()`, que hace tres cosas:

#### 2a. Contar frecuencias — `MotorHuffman.contarFrecuencias()`

Recorre el `byte[]` del archivo y cuenta cuántas veces aparece cada valor de byte.

```
Datos: [0x41, 0x41, 0x42, 0x44, 0x43]  ("AABDC" en ASCII)
Resultado: {0x41=2, 0x42=1, 0x44=1, 0x43=1}
```

#### 2b. Construir el árbol — `MotorHuffman.construirArbol()`

Implementa el método de reducción de fuente por columnas, exactamente como en el pizarrón:

1. Convierte frecuencias a probabilidades dividiendo por el total de bytes.
2. Ordena los nodos de mayor a menor probabilidad.
3. En cada paso agarra los **dos de menor probabilidad** (los últimos de la lista),
   los fusiona en un nodo padre y lo reinserta en la lista.
4. Repite hasta que quedan exactamente **2 nodos**.
5. Esos 2 nodos se convierten en los hijos de la raíz.

El árbol se construye **de abajo hacia arriba**: cada fusión crea un nodo padre que
guarda referencias a sus dos hijos. Al final la raíz apunta a toda la estructura.

**Regla de empate:** si un nodo fusionado y un nodo individual tienen la misma
probabilidad, el fusionado va **por encima**. Esto garantiza que el byte más
frecuente reciba el código más corto.

Ejemplo con S={A,B,C,D,E}, P={0.5, 0.1, 0.1, 0.2, 0.1}:

![Ejemplo del pizarrón](ejemplo.png)

```
Paso 1: fusiona C(0.1) + E(0.1) → [0.2]
Paso 2: fusiona D(0.2) + B(0.1) → [0.3]
Paso 3: fusiona [0.3] + [0.2]   → [0.5]

Árbol final:
          RAÍZ
          /  \
       [0.5]  A
       /   \
    [0.3] [0.2]
    /  \   /  \
   D    B C    E
```

#### 2c. Asignar códigos — `MotorHuffman.construirCodigos()` + `asignarCodigos()`

Recorre el árbol desde la raíz. Al hijo izquierdo le concatena "0" y al derecho "1",
recursivamente hasta llegar a las hojas.

```
A  → 1
D  → 000
B  → 001
C  → 010
E  → 011
```

Los nodos más cercanos a la raíz reciben códigos más cortos.
Los más profundos reciben códigos más largos.

#### 2d. Escribir el archivo .huf — `GestorIOBinario.comprimir()`

El archivo .huf tiene dos partes:

**Header** (información para reconstruir el árbol al descomprimir):
```
[8 bytes] long → cantidad total de bytes del archivo original
[4 bytes] int  → cantidad de símbolos únicos (tamaño del alfabeto, max 256)
por cada símbolo:
    [1 byte]  byte → el valor del byte
    [4 bytes] int  → su frecuencia
```

**Cuerpo** (datos comprimidos con acumulador de bits):

En vez de guardar cada byte en 8 bits, guarda su código Huffman.
Los bits se van acumulando en un buffer de 8 bits y se escribe un byte
cada vez que el buffer se llena:

```
Datos [0x41, 0x44] ("AD"):
0x41 → "1"    → bits: 1
0x44 → "000"  → bits: 000

Acumulando: 1 0 0 0 → al llegar a 8 bits → escribe byte 10000000
```

Si al terminar los datos quedan bits sin completar un byte, se rellenan
con ceros a la derecha (**padding**).

---

### 3. El usuario presiona "Descompactar"

`UserInterface` abre un `JFileChooser` para seleccionar el `.huf` y llama
a `GestorIOBinario.descomprimir()`, que hace el proceso inverso:

1. **Lee el header** → reconstruye la tabla de frecuencias.
2. **Reconstruye el árbol** llamando a `construirArbol()` con esas frecuencias.
   Funciona porque el árbol es determinista: las mismas frecuencias siempre
   producen el mismo árbol.
3. **Lee los bytes del cuerpo** y los decodifica bit a bit navegando el árbol:
   - bit `0` → ir al hijo izquierdo
   - bit `1` → ir al hijo derecho
   - al llegar a una hoja → ese es el byte, volver a la raíz
4. **Para exactamente** al recuperar `cantidadBytesOriginal` bytes,
   ignorando los bits de padding del último byte.
5. **Escribe el archivo .dhu** como bytes crudos, idéntico al archivo original.

---

### 4. El usuario presiona "Ver Estadística"

Abre un `JDialog` con una tabla que muestra el tamaño en bytes de los tres
archivos usando `file.length()` y calcula el ratio de compresión vs el original.

---

## Estructura del árbol en memoria

Cada nodo del árbol es un objeto `NodoHuffman` con estos campos:

| Campo | Descripción |
|---|---|
| `probabilidad` | Probabilidad del nodo |
| `valorByte` | El byte que representa. `null` si es nodo interno |
| `izquierdo` | Hijo izquierdo (bit "0") |
| `derecho` | Hijo derecho (bit "1") |
| `esFusionado` | `true` si es resultado de una fusión |

Las **hojas** tienen `valorByte != null` y sin hijos.
Los **nodos internos** tienen `valorByte == null` y dos hijos.

---

## Por qué archivos chicos no comprimen

El header tiene un costo fijo mínimo de:

```
12 bytes base + N × 5 bytes por símbolo único
```

Para texto en español con ~90 valores de byte únicos el header pesa ~462 bytes.
La ganancia de Huffman en español es ~3 bits por byte.
El punto de equilibrio es aproximadamente **600 bytes**.

Por debajo de ese umbral el archivo comprimido pesa más que el original.
Esto es matemáticamente correcto, no es un bug.

---

## Validación interna

Al arrancar, `Main` ejecuta `MotorHuffman.autoTest()` que verifica el ejemplo
del pizarrón usando bytes ASCII y confirma que los códigos son exactamente:

```
0x41 (A) → 1
0x42 (B) → 001
0x43 (C) → 010
0x44 (D) → 000
0x45 (E) → 011
```
