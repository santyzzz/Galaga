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
    public ArrayList<Enemigo>        enemigos         = new ArrayList<>();
    public ArrayList<DisparoEnemigo> disparosEnemigos = new ArrayList<>();

    // ── Sprites de cada tipo de enemigo ──────────────────────────────────────
    private Bitmap[] framesAmarillo, framesRojo, framesVerde2hp, framesVerde1hp, framesRayo;
    private static final int NUM_FRAMES = 8;

    // ── Control de entrada en formación ──────────────────────────────────────
    private int enemigosSoltados = 0;
    private int contadorEntrada  = 0;
    // A partir del nivel 3 los enemigos entran más rápido (menos delay entre ellos)
    private int delayEntreEnemigos = 8;

    // ── Movimiento de la formación ────────────────────────────────────────────
    private float   formacionVelocidad;
    private int     formacionDireccion = 1;
    private boolean formacionCompleta  = false;
    private static final float MARGEN  = 40f;

    // ── Control de ataques ────────────────────────────────────────────────────
    private int contadorAtaque = 0;
    // Cadencia base de ataques: se reduce con el nivel (más ataques a más nivel)
    private int framesEntreAtaques = 150;

    // ── Configuración de niveles ──────────────────────────────────────────────
    // Cada nivel añade más enemigos y más filas.
    // Nivel 1: formación pequeña; nivel 5: pantalla llena; nivel 6: jefe final.
    //
    //                      N1   N2   N3   N4   N5
    private static final int[] VERDES_POR_NIVEL    = {  4,   6,   8,  10,  14 };
    private static final int[] AMARILLOS_POR_NIVEL = {  6,   8,  10,  12,  14 };
    private static final int[] ROJOS_POR_NIVEL     = {  4,   6,   8,  10,  14 };

    public int nivelActual = 1;
    public JefeFinalDragon jefeFinalDragon = null;

    public Bitmap getSpriteAmarillo() { return framesAmarillo != null ? framesAmarillo[0] : null; }
    public Bitmap getSpriteRojo()     { return framesRojo     != null ? framesRojo[0]     : null; }
    public Bitmap getSpriteVerde()    { return framesVerde2hp  != null ? framesVerde2hp[0]  : null; }

    private boolean sonidoIntroReproducido = false;

    // ── Constructor ───────────────────────────────────────────────────────────
    public GestorEnemigos(Juego juego) {
        this.juego = juego;
        cargarSprites();
        configurarNivel(nivelActual);
        formacionVelocidad = juego.anchoPantalla / 250f;
    }

    // ── Lógica principal ──────────────────────────────────────────────────────
    public void actualizar() {
        if (!sonidoIntroReproducido) {
            // La música de introducción solo suena en el nivel 1
            if (nivelActual == 1) juego.reproducirSonidoNivel();
            sonidoIntroReproducido = true;
        }

        // ── Nivel del jefe final ──────────────────────────────────────────────
        if (jefeFinalDragon != null && jefeFinalDragon.activo) {
            jefeFinalDragon.actualizar();

            for (int i = jefeFinalDragon.disparos.size() - 1; i >= 0; i--) {
                DisparoJefeDragon d = jefeFinalDragon.disparos.get(i);
                if (d.colisionaConNave(juego.naveX, juego.naveY,
                        juego.spriteNave.getWidth(), juego.spriteNave.getHeight())) {
                    jefeFinalDragon.disparos.remove(i);
                    juego.recibirDanio();
                    break;
                }
            }

            if (jefeFinalDragon.tocaLaNave()) {
                juego.recibirDanio();
                jefeFinalDragon.estadoAtaque = JefeFinalDragon.EstadoAtaque.MELEE_VOLVIENDO;
            }

            actualizarDisparosRestantes();
            return;
        }

        // ── Soltar enemigos de uno en uno con delay ───────────────────────────
        if (enemigosSoltados < enemigos.size()) {
            contadorEntrada++;
            if (contadorEntrada >= delayEntreEnemigos) {
                contadorEntrada = 0;
                enemigos.get(enemigosSoltados).iniciarEntrada();
                enemigosSoltados++;
            }
        }

        // ── Detectar formación completa ───────────────────────────────────────
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

        for (Enemigo e : enemigos) e.actualizar();

        for (Enemigo e : enemigos) {
            if (e.tipo == Enemigo.Tipo.VERDE
                    && e.estado == Enemigo.Estado.ABDUCIENDO
                    && e.contadorAbduccion == 1) {
                e.esCapturador = true;
                juego.reproducirSonidoAbsorcion();
            }
        }

        actualizarDisparosRestantes();

        // ── Comprobar si todos los enemigos están muertos ─────────────────────
        boolean todosMuertos = true;
        for (Enemigo e : enemigos) {
            if (e.estado != Enemigo.Estado.MUERTO) { todosMuertos = false; break; }
        }
        if (todosMuertos && !enemigos.isEmpty()) {
            juego.nivelSuperado();
        }
    }

    private void actualizarDisparosRestantes() {
        for (int i = disparosEnemigos.size() - 1; i >= 0; i--) {
            disparosEnemigos.get(i).actualizar();
            if (disparosEnemigos.get(i).fueraDePantalla()) disparosEnemigos.remove(i);
        }
    }

    public void renderizar(Canvas canvas, Paint paint) {
        if (jefeFinalDragon != null && jefeFinalDragon.activo) {
            jefeFinalDragon.dibujar(canvas, paint);
            return;
        }
        for (Enemigo e        : enemigos)         e.dibujar(canvas, paint);
        for (DisparoEnemigo d : disparosEnemigos) d.dibujar(canvas, paint);
    }

    // ── Configuración del nivel ───────────────────────────────────────────────
    /**
     * Ajusta la cantidad de enemigos, la velocidad de la formación y la cadencia
     * de ataques según el nivel actual. Escala progresivamente:
     *
     *  Nivel 1 → formación pequeña, ataques lentos, velocidad baja
     *  Nivel 5 → formación grande (3+ filas por tipo), ataques rápidos, velocidad alta
     *  Nivel 6 → jefe final
     */
    public void configurarNivel(int nivel) {
        if (nivel == 6) {
            configurarNivelJefe();
            return;
        }

        enemigos.clear();
        disparosEnemigos.clear();
        enemigosSoltados  = 0;
        contadorEntrada   = 0;
        formacionCompleta = false;
        contadorAtaque    = 0;
        sonidoIntroReproducido = false;

        int idx          = Math.min(nivel - 1, VERDES_POR_NIVEL.length - 1);
        int numVerdes    = VERDES_POR_NIVEL[idx];
        int numAmarillos = AMARILLOS_POR_NIVEL[idx];
        int numRojos     = ROJOS_POR_NIVEL[idx];

        // Velocidad de la formación: crece linealmente del nivel 1 al 5
        // Nivel 1 → anchoPantalla/250   Nivel 5 → anchoPantalla/120
        formacionVelocidad = juego.anchoPantalla / (250f - (nivel - 1) * 26f);

        // Cadencia de ataques: baja de 150 a 70 frames conforme sube el nivel
        // Nivel 1 → ~5 seg   Nivel 5 → ~2.3 seg
        framesEntreAtaques = 150 - (nivel - 1) * 20;

        // Delay entre la entrada de cada enemigo: se reduce en niveles altos
        // para que la formación entre más "en masa" y sea más intimidante
        // Nivel 1 → 8 frames   Nivel 5 → 4 frames
        delayEntreEnemigos = Math.max(4, 8 - (nivel - 1));

        int pantW = juego.anchoPantalla;
        int pantH = juego.altoPantalla;

        int espaciadoX = framesAmarillo[0].getWidth()  + 30;
        int espaciadoY = framesAmarillo[0].getHeight() + 20;

        // Máximo de enemigos por fila calculado según el ancho real de la pantalla.
        // Dejamos MARGEN*2 de espacio a cada lado para que la formación pueda rebotar
        // sin que ningún enemigo salga de la pantalla.
        int anchoDisponible = pantW - (int)(MARGEN * 2);
        int maxPorFila      = Math.max(1, anchoDisponible / espaciadoX);

        // Calculamos cuántas filas ocupa cada tipo para que filaBase se acumule
        // correctamente: si los verdes necesitan 2 filas, los amarillos empiezan en la 2.
        int filasVerdes    = (int) Math.ceil((double) numVerdes    / maxPorFila);
        int filasAmarillos = (int) Math.ceil((double) numAmarillos / maxPorFila);

        int filaBaseVerdes    = 0;
        int filaBaseAmarillos = filaBaseVerdes    + filasVerdes;
        int filaBaseRojos     = filaBaseAmarillos + filasAmarillos;

        agregarFila(Enemigo.Tipo.VERDE,    numVerdes,    filaBaseVerdes,    maxPorFila, espaciadoX, espaciadoY, pantW, pantH);
        agregarFila(Enemigo.Tipo.AMARILLO, numAmarillos, filaBaseAmarillos, maxPorFila, espaciadoX, espaciadoY, pantW, pantH);
        agregarFila(Enemigo.Tipo.ROJO,     numRojos,     filaBaseRojos,     maxPorFila, espaciadoX, espaciadoY, pantW, pantH);
    }

    private void configurarNivelJefe() {
        enemigos.clear();
        disparosEnemigos.clear();
        enemigosSoltados       = 0;
        formacionCompleta      = false;
        contadorAtaque         = 0;
        sonidoIntroReproducido = false;
        jefeFinalDragon        = new JefeFinalDragon(juego);
        jefeFinalDragon.activo = true;
    }

    // ── Construcción de la formación ──────────────────────────────────────────
    /**
     * Añade 'cantidad' enemigos del tipo indicado empezando en la fila 'filaBase'.
     * Si hay más enemigos de los que caben en una fila (maxPorFila) se añaden
     * filas adicionales automáticamente, de forma que en niveles altos la
     * formación puede tener 2 o 3 filas por tipo de enemigo.
     */
    private void agregarFila(Enemigo.Tipo tipo, int cantidad, int filaBase,
                             int maxPorFila, int espaciadoX, int espaciadoY,
                             int pantW, int pantH) {
        int   col    = 0;
        int   fila   = 0;
        float startY = pantH * 0.18f;

        for (int i = 0; i < cantidad; i++) {
            if (col >= maxPorFila) { col = 0; fila++; }

            int   totalEnFila = Math.min(cantidad - fila * maxPorFila, maxPorFila);
            // Centramos dentro del ancho disponible (entre los dos márgenes de rebote)
            // para que ningún enemigo empiece pegado al borde ni sobresalga
            float zonaAncho = pantW - MARGEN * 2;
            float startX    = MARGEN + (zonaAncho - totalEnFila * espaciadoX) / 2f;
            float tx        = startX + col * espaciadoX;
            float ty        = startY + (filaBase + fila) * espaciadoY;

            boolean porDerecha = (col % 2 == 0);

            Enemigo enemigo = new Enemigo(tipo, tx, ty, porDerecha,
                    getFrames(tipo, true), getFrames(tipo, false), pantW, pantH, juego);
            if (tipo == Enemigo.Tipo.VERDE) enemigo.framesRayo = framesRayo;
            enemigos.add(enemigo);
            col++;
        }
    }

    private Bitmap[] getFrames(Enemigo.Tipo tipo, boolean sano) {
        switch (tipo) {
            case AMARILLO: return framesAmarillo;
            case ROJO:     return framesRojo;
            case VERDE:    return sano ? framesVerde2hp : framesVerde1hp;
            default:       return framesAmarillo;
        }
    }

    // ── Movimiento de la formación ────────────────────────────────────────────
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

        for (Enemigo e : enemigos) {
            if (e.estado != Enemigo.Estado.MUERTO && e.estado != Enemigo.Estado.ATACANDO) {
                e.targetX += formacionVelocidad * formacionDireccion;
                if (e.estado == Enemigo.Estado.EN_FORMACION) {
                    e.x += formacionVelocidad * formacionDireccion;
                }
            }
        }
    }

    // ── Gestión de ataques ────────────────────────────────────────────────────
    private void gestionarAtaques() {
        contadorAtaque++;
        if (contadorAtaque < framesEntreAtaques) return;
        contadorAtaque = 0;

        ArrayList<Enemigo> candidatosVerdes = new ArrayList<>();
        ArrayList<Enemigo> candidatosOtros  = new ArrayList<>();
        for (Enemigo e : enemigos) {
            if (e.estado == Enemigo.Estado.EN_FORMACION) {
                if (e.tipo == Enemigo.Tipo.VERDE) candidatosVerdes.add(e);
                else                              candidatosOtros.add(e);
            }
        }

        if (!candidatosVerdes.isEmpty() && Math.random() < 0.5 && juego.navesCapturadas < 2) {
            Enemigo verde = candidatosVerdes.get((int)(Math.random() * candidatosVerdes.size()));
            verde.estado             = Enemigo.Estado.ATACANDO;
            verde.volviendoFormacion = false;
            verde.abduciendoNave     = false;
            verde.targetNaveX        = juego.naveX;
            verde.targetNaveY        = juego.naveY;

            float dx   = juego.naveX - verde.x;
            float dy   = juego.naveY - verde.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float vel  = juego.altoPantalla / (2.5f * BucleJuego.MAX_FPS);
            verde.ataqueVX = (dx / dist) * vel;
            verde.ataqueVY = (dy / dist) * vel;
            return;
        }

        if (candidatosOtros.isEmpty()) return;
        Enemigo atacante = candidatosOtros.get((int)(Math.random() * candidatosOtros.size()));
        atacante.estado             = Enemigo.Estado.ATACANDO;
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

    // ── Carga de sprites ──────────────────────────────────────────────────────
    private void cargarSprites() {
        framesAmarillo = extraerFrames(BitmapFactory.decodeResource(juego.getResources(), R.drawable.enemigo_amarillo));
        framesRojo     = extraerFrames(BitmapFactory.decodeResource(juego.getResources(), R.drawable.enemigo_rojo));
        framesVerde2hp = extraerFrames(BitmapFactory.decodeResource(juego.getResources(), R.drawable.enemigo_verde_2hp));
        framesVerde1hp = extraerFrames(BitmapFactory.decodeResource(juego.getResources(), R.drawable.enemigo_verde_1hp));
        framesRayo     = extraerFramesRayo(BitmapFactory.decodeResource(juego.getResources(), R.drawable.rayo_abduccion));
    }

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
}