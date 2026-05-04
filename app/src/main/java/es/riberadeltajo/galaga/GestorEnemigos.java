package es.riberadeltajo.galaga;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.ArrayList;

public class GestorEnemigos {

    // ── Referencia al juego ───────────────────────────────────────────────────
    private Juego juego;

    // ── Listas de entidades activas ───────────────────────────────────────────
    public ArrayList<Enemigo>       enemigos        = new ArrayList<>();
    public ArrayList<DisparoEnemigo> disparosEnemigos = new ArrayList<>();

    // ── Sprites de cada tipo de enemigo ──────────────────────────────────────
    private Bitmap[] framesAmarillo, framesRojo, framesVerde2hp, framesVerde1hp, framesRayo;
    private static final int NUM_FRAMES = 8;

    // ── Control de entrada en formación ──────────────────────────────────────
    private int enemigosSoltados          = 0;
    private int contadorEntrada           = 0;
    private static final int DELAY_ENTRE_ENEMIGOS = 8; // frames entre cada enemigo

    // ── Movimiento de la formación ────────────────────────────────────────────
    private float   formacionVelocidad;
    private int     formacionDireccion  = 1;
    private boolean formacionCompleta   = false;
    private static final float MARGEN   = 40f;

    // ── Control de ataques ────────────────────────────────────────────────────
    private int contadorAtaque = 0;
    private static final int FRAMES_ENTRE_ATAQUES = 150; // ~5 seg a 30fps

    // ── Configuración de niveles ──────────────────────────────────────────────
// Aquí defines cuántos enemigos de cada tipo hay en cada nivel.
// Cambia estos arrays para ajustar la dificultad sin tocar nada más.
    private static final int[] VERDES_POR_NIVEL   = { 5, 6, 4, 8, 10 };
    private static final int[] AMARILLOS_POR_NIVEL = { 5, 6, 8, 8, 10 };
    private static final int[] ROJOS_POR_NIVEL     = { 5, 4, 8, 9, 10 };
    public int nivelActual=1;

    // Devuelven el primer frame de cada tipo para usarlo en las estadísticas
    public Bitmap getSpriteAmarillo() { return framesAmarillo != null ? framesAmarillo[0] : null; }
    public Bitmap getSpriteRojo()     { return framesRojo     != null ? framesRojo[0]     : null; }
    public Bitmap getSpriteVerde()    { return framesVerde2hp  != null ? framesVerde2hp[0]  : null; }


    // ── Otros ─────────────────────────────────────────────────────────────────
    private boolean sonidoIntroReproducido = false;

//====================================================================================
    //                  PROGRAMACION
//====================================================================================





    // ── Constructor ───────────────────────────────────────────────────────────

    // Inicializa el gestor, carga los sprites y prepara la formación del nivel 1
    public GestorEnemigos(Juego juego) {
        this.juego = juego;
        cargarSprites();
        configurarNivel(1);
        formacionVelocidad = juego.anchoPantalla / 250f;
    }


    // ── Métodos públicos ──────────────────────────────────────────────────────
    // Bucle principal del gestor: controla entradas, formación, ataques y disparos
    public void actualizar() {
        if (!sonidoIntroReproducido) {
            juego.reproducirSonidoNivel();
            sonidoIntroReproducido = true;
        }

        // Suelta los enemigos de uno en uno con un pequeño retardo entre cada uno
        if (enemigosSoltados < enemigos.size()) {
            contadorEntrada++;
            if (contadorEntrada >= DELAY_ENTRE_ENEMIGOS) {
                contadorEntrada = 0;
                enemigos.get(enemigosSoltados).iniciarEntrada();
                enemigosSoltados++;
            }
        }

        // Marca la formación como completa cuando todos han llegado a su sitio
        if (!formacionCompleta && enemigosSoltados == enemigos.size()) {
            formacionCompleta = true;
            for (Enemigo e : enemigos) {
                if (e.estado == Enemigo.Estado.ENTRANDO) { formacionCompleta = false; break; }
            }
        }

        moverFormacion();

        if (formacionCompleta) {
            gestionarAtaques();
        }

        // Actualiza la lógica de cada enemigo individualmente
        for (Enemigo e : enemigos) e.actualizar();

        // Detecta cuando un verde entra en modo abducción y avisa al juego
        for (Enemigo e : enemigos) {
            if (e.tipo == Enemigo.Tipo.VERDE
                    && e.estado == Enemigo.Estado.ABDUCIENDO
                    && e.contadorAbduccion == 1) {
                juego.recibirDanio();
                e.esCapturador = true;
                juego.reproducirSonidoAbsorcion();
            }
        }

        // Mueve los disparos enemigos y elimina los que salen de pantalla
        for (int i = disparosEnemigos.size() - 1; i >= 0; i--) {
            disparosEnemigos.get(i).actualizar();
            if (disparosEnemigos.get(i).fueraDePantalla()) disparosEnemigos.remove(i);
        }

        boolean todosMuertos=true;
        for(Enemigo e: enemigos){
            if(e.estado!=Enemigo.Estado.MUERTO){
                todosMuertos=false;
                break;
            }
        }
        if(todosMuertos&& !enemigos.isEmpty()){
            juego.nivelSuperado();
        }
    }

    // Dibuja todos los enemigos y sus disparos en el canvas
    public void renderizar(Canvas canvas, Paint paint) {
        for (Enemigo e        : enemigos)         e.dibujar(canvas, paint);
        for (DisparoEnemigo d : disparosEnemigos) d.dibujar(canvas, paint);
    }


    // ── Métodos privados ──────────────────────────────────────────────────────

    // Carga los spritesheets desde los recursos y los corta en frames individuales
    private void cargarSprites() {
        framesAmarillo = extraerFrames(BitmapFactory.decodeResource(juego.getResources(), R.drawable.enemigo_amarillo));
        framesRojo     = extraerFrames(BitmapFactory.decodeResource(juego.getResources(), R.drawable.enemigo_rojo));
        framesVerde2hp = extraerFrames(BitmapFactory.decodeResource(juego.getResources(), R.drawable.enemigo_verde_2hp));
        framesVerde1hp = extraerFrames(BitmapFactory.decodeResource(juego.getResources(), R.drawable.enemigo_verde_1hp));
        framesRayo     = extraerFramesRayo(BitmapFactory.decodeResource(juego.getResources(), R.drawable.rayo_abduccion));
    }

    // Corta el spritesheet del rayo en 4 frames y los escala al tamaño de pantalla
    private Bitmap[] extraerFramesRayo(Bitmap sheet) {
        int numFrames = 4;
        Bitmap[] frames = new Bitmap[numFrames];
        int fw      = sheet.getWidth() / numFrames;
        int fh      = sheet.getHeight();
        int targetW = juego.anchoPantalla / 14;
        int targetH = (int)(fh * ((float) targetW / fw));
        for (int i = 0; i < numFrames; i++) {
            Bitmap raw = Bitmap.createBitmap(sheet, i * fw, 0, fw, fh);
            frames[i]  = Bitmap.createScaledBitmap(raw, targetW, targetH, false);
        }
        return frames;
    }

    // Corta un spritesheet de 8 frames, elimina el margen de cada celda y escala el resultado
    private Bitmap[] extraerFrames(Bitmap sheet) {
        Bitmap[] frames = new Bitmap[NUM_FRAMES];
        int fw     = sheet.getWidth() / NUM_FRAMES;
        int fh     = sheet.getHeight();
        int margen = 3;
        int targetW = juego.anchoPantalla / 12;
        int targetH = (int)(fh * ((float) targetW / fw));
        for (int i = 0; i < NUM_FRAMES; i++) {
            Bitmap raw = Bitmap.createBitmap(sheet,
                    i * fw + margen, margen,
                    fw - margen * 2, fh - margen * 2,
                    null, false);
            frames[i] = Bitmap.createScaledBitmap(raw, targetW, targetH, true);
        }
        return frames;
    }

    // Construye la formación del nivel indicado según los arrays de configuración
    public void configurarNivel(int nivel) {
        enemigos.clear();
        disparosEnemigos.clear();
        enemigosSoltados  = 0;
        contadorEntrada   = 0;
        formacionCompleta = false;
        contadorAtaque    = 0;

        int idx       = nivel - 1; // los arrays empiezan en 0
        int numVerdes   = VERDES_POR_NIVEL[idx];
        int numAmarillos = AMARILLOS_POR_NIVEL[idx];
        int numRojos    = ROJOS_POR_NIVEL[idx];

        int pantW = juego.anchoPantalla;
        int pantH = juego.altoPantalla;

        int espaciadoX = framesAmarillo[0].getWidth()  + 30;
        int espaciadoY = framesAmarillo[0].getHeight() + 20;

        // Calculamos cuántas filas y columnas necesitamos
        int maxPorFila = 7; // máximo de enemigos por fila
        agregarFila(Enemigo.Tipo.VERDE,    numVerdes,   0, maxPorFila, espaciadoX, espaciadoY, pantW, pantH);
        agregarFila(Enemigo.Tipo.AMARILLO, numAmarillos, 1, maxPorFila, espaciadoX, espaciadoY, pantW, pantH);
        agregarFila(Enemigo.Tipo.ROJO,     numRojos,    2, maxPorFila, espaciadoX, espaciadoY, pantW, pantH);
    }

    // Añade una fila de enemigos del tipo indicado, distribuyéndolos en filas de maxPorFila
    private void agregarFila(Enemigo.Tipo tipo, int cantidad, int filaBase,
                             int maxPorFila, int espaciadoX, int espaciadoY,
                             int pantW, int pantH) {
        int col  = 0;
        int fila = 0;
        float startY = pantH * 0.18f;

        for (int i = 0; i < cantidad; i++) {
            if (col >= maxPorFila) { col = 0; fila++; }

            int totalEnFila  = Math.min(cantidad - fila * maxPorFila, maxPorFila);
            float startX     = (pantW - totalEnFila * espaciadoX) / 2f;
            float tx         = startX + col * espaciadoX;
            float ty         = startY + (filaBase + fila) * espaciadoY;
            boolean porDerecha = (col % 2 == 0);

            Enemigo enemigo = new Enemigo(tipo, tx, ty, porDerecha,
                    getFrames(tipo, true), getFrames(tipo, false), pantW, pantH, juego);
            if (tipo == Enemigo.Tipo.VERDE) enemigo.framesRayo = framesRayo;
            enemigos.add(enemigo);
            col++;
        }
    }

    // Devuelve los frames correspondientes al tipo de enemigo y a si está sano o dañado
    private Bitmap[] getFrames(Enemigo.Tipo tipo, boolean sano) {
        switch (tipo) {
            case AMARILLO: return framesAmarillo;
            case ROJO:     return framesRojo;
            case VERDE:    return sano ? framesVerde2hp : framesVerde1hp;
            default:       return framesAmarillo;
        }
    }

    // Desplaza toda la formación de lado a lado rebotando en los bordes de pantalla
    private void moverFormacion() {
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        for (Enemigo e : enemigos) {
            if (e.estado == Enemigo.Estado.EN_FORMACION) {
                minX = Math.min(minX, e.x);
                maxX = Math.max(maxX, e.x + e.ancho);
            }
        }
        if (minX == Float.MAX_VALUE) return;

        if (maxX >= juego.anchoPantalla - MARGEN) formacionDireccion = -1;
        if (minX <= MARGEN)                        formacionDireccion =  1;

        // Actualiza tanto la posición actual como el target para no perder el hueco en la formación
        for (Enemigo e : enemigos) {
            if (e.estado != Enemigo.Estado.MUERTO && e.estado != Enemigo.Estado.ATACANDO) {
                e.targetX += formacionVelocidad * formacionDireccion;
                if (e.estado == Enemigo.Estado.EN_FORMACION) {
                    e.x += formacionVelocidad * formacionDireccion;
                }
            }
        }
    }

    // Cada cierto tiempo elige un enemigo al azar para atacar;
    // los verdes intentan abducir la nave, los otros bajan disparando en abanico
    private void gestionarAtaques() {
        contadorAtaque++;
        if (contadorAtaque < FRAMES_ENTRE_ATAQUES) return;
        contadorAtaque = 0;

        ArrayList<Enemigo> candidatosVerdes = new ArrayList<>();
        ArrayList<Enemigo> candidatosOtros  = new ArrayList<>();
        for (Enemigo e : enemigos) {
            if (e.estado == Enemigo.Estado.EN_FORMACION) {
                if (e.tipo == Enemigo.Tipo.VERDE) candidatosVerdes.add(e);
                else                              candidatosOtros.add(e);
            }
        }

        // El verde tiene un 50% de probabilidad de atacar si hay naves disponibles para capturar
        if (!candidatosVerdes.isEmpty() && Math.random() < 0.5 && juego.navesCapturadas < 2) {
            Enemigo verde = candidatosVerdes.get((int)(Math.random() * candidatosVerdes.size()));
            verde.estado           = Enemigo.Estado.ATACANDO;
            verde.volviendoFormacion = false;
            verde.abduciendoNave    = false;
            verde.targetNaveX       = juego.naveX;
            verde.targetNaveY       = juego.naveY;

            float dx   = juego.naveX - verde.x;
            float dy   = juego.naveY - verde.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float vel  = juego.altoPantalla / (2.5f * BucleJuego.MAX_FPS);
            verde.ataqueVX = (dx / dist) * vel;
            verde.ataqueVY = (dy / dist) * vel;
            return; // un solo ataque por turno
        }

        // Si no ataca ningún verde, lanza un enemigo normal con 3 disparos en abanico
        if (candidatosOtros.isEmpty()) return;
        Enemigo atacante = candidatosOtros.get((int)(Math.random() * candidatosOtros.size()));
        atacante.estado            = Enemigo.Estado.ATACANDO;
        atacante.volviendoFormacion = false;

        float dx   = juego.naveX - atacante.x;
        float dy   = juego.naveY - atacante.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float vel  = juego.altoPantalla / (2.5f * BucleJuego.MAX_FPS);
        atacante.ataqueVX = (dx / dist) * vel;
        atacante.ataqueVY = (dy / dist) * vel;

        float angulo = (float) Math.atan2(dx, dy);
        float cx     = atacante.x + atacante.ancho / 2f;
        float cy     = atacante.y + atacante.alto;
        disparosEnemigos.add(new DisparoEnemigo(cx, cy, angulo - 0.3f, juego.anchoPantalla, juego.altoPantalla));
        disparosEnemigos.add(new DisparoEnemigo(cx, cy, angulo,        juego.anchoPantalla, juego.altoPantalla));
        disparosEnemigos.add(new DisparoEnemigo(cx, cy, angulo + 0.3f, juego.anchoPantalla, juego.altoPantalla));
    }
}