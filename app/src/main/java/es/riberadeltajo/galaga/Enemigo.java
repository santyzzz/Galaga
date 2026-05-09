package es.riberadeltajo.galaga;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class Enemigo {

    // ── Enumeraciones ─────────────────────────────────────────────────────────
    public enum Tipo   { AMARILLO, ROJO, VERDE }
    public enum Estado { ESPERANDO, ENTRANDO, EN_FORMACION, ATACANDO, ABDUCIENDO, MUERTO }

    // ── Referencia al juego ───────────────────────────────────────────────────
    public Juego juego;

    // ── Posición y destino ────────────────────────────────────────────────────
    public float x, y;
    public float targetX, targetY;
    public float targetNaveX, targetNaveY;

    // ── Estado del enemigo ────────────────────────────────────────────────────
    public int   vida;
    public Tipo  tipo;
    public Estado estado = Estado.ESPERANDO;
    public boolean esCapturador = false;
    public boolean capturoNave  = false;

    // ── Animación del sprite ──────────────────────────────────────────────────
    private Bitmap[] frames;
    private Bitmap[] framesDanado;
    private int  frameActual    = 0;
    private int  contadorFrame  = 0;
    private boolean animAvanzando = true;
    private static final int FRAMES_POR_TICK = 5;
    private static final int TOTAL_FRAMES    = 8;

    // ── Animación del rayo de abducción ───────────────────────────────────────
    public Bitmap[] framesRayo;
    private int frameRayo        = 0;
    private int contadorFrameRayo = 0;

    // ── Tamaño del sprite ─────────────────────────────────────────────────────
    public int ancho, alto;

    // ── Movimiento y velocidades ──────────────────────────────────────────────
    private float velocidadVuelta;
    public  float ataqueVX, ataqueVY;
    public  boolean volviendoFormacion = false;
    private int pantAlto;
    private int pantAncho;

    // ── Abducción ─────────────────────────────────────────────────────────────
    public boolean abduciendoNave   = false;
    public int     contadorAbduccion = 0;
    private static final int FRAMES_ABDUCCION    = 90;
    private static final int FRAMES_MOSTRAR_RAYO = 60;
    public  static final int FRAMES_MOSTRAR_RAYO_PUBLIC = FRAMES_MOSTRAR_RAYO;

    // ── Bézier de entrada ─────────────────────────────────────────────────────
    // Curva Bézier cúbica: punto inicial P0, dos puntos de control P1 y P2,
    // y punto final P3 (= targetX/Y). El parámetro t va de 0.0 a 1.0.
    private float bezP0x, bezP0y; // origen (fuera de pantalla)
    private float bezP1x, bezP1y; // primer punto de control (zona intermedia alta)
    private float bezP2x, bezP2y; // segundo punto de control (giro hacia el destino)
    // P3 = targetX, targetY
    private float bezT       = 0f;    // parámetro actual de la curva [0..1]
    private float bezVelocidad;       // cuánto avanza t por frame

    // ── Constructor ───────────────────────────────────────────────────────────
    public Enemigo(Tipo tipo, float targetX, float targetY, boolean entraPorDerecha,
                   Bitmap[] frames, Bitmap[] framesDanado,
                   int pantallaAncho, int pantallaAlto, Juego juego) {

        this.tipo      = tipo;
        this.targetX   = targetX;
        this.targetY   = targetY;
        this.frames    = frames;
        this.framesDanado = framesDanado;
        this.pantAlto  = pantallaAlto;
        this.pantAncho = pantallaAncho;
        this.juego     = juego;

        vida = (tipo == Tipo.VERDE) ? 2 : 1;

        if (frames != null && frames[0] != null) {
            ancho = frames[0].getWidth();
            alto  = frames[0].getHeight();
        }

        // ── Punto de inicio fuera de pantalla ──────────────────────────────
        // Todos los enemigos salen de uno de los dos lados superiores
        bezP0x = entraPorDerecha ? pantallaAncho + 80f : -80f;
        bezP0y = pantallaAlto * 0.05f;

        // ── Punto de control 1: curva hacia el centro-arriba de la pantalla ──
        // Esto hace que el enemigo entre "barriendo" la parte superior
        bezP1x = entraPorDerecha
                ? pantallaAncho * 0.65f   // viene por la derecha → curva hacia la izquierda
                : pantallaAncho * 0.35f;  // viene por la izquierda → curva hacia la derecha
        bezP1y = -pantallaAlto * 0.1f;   // ligeramente por encima de la pantalla (da profundidad)

        // ── Punto de control 2: justo antes del destino, desde arriba ────────
        // Esto hace que el enemigo "caiga" en picado hacia su posición final
        bezP2x = targetX + ancho / 2f;
        bezP2y = targetY - pantallaAlto * 0.25f;

        // Posición inicial del sprite = punto P0
        this.x = bezP0x;
        this.y = bezP0y;

        // t avanza ~1/60 por frame → la entrada dura ~60 frames (2 segundos a 30fps)
        bezVelocidad  = 1f / 60f;
        velocidadVuelta = pantallaAncho / 40f;
    }

    // ── Métodos públicos ──────────────────────────────────────────────────────

    public void iniciarEntrada() {
        estado = Estado.ENTRANDO;
        bezT   = 0f;
    }

    public void iniciarAtaqueAbduccion(float naveX, float naveY) {
        targetNaveX = naveX;
        targetNaveY = naveY;
    }

    public void recibirDanio() {
        vida--;
        if (vida <= 0) {
            estado = Estado.MUERTO;
            juego.disparosAcertados++;
            switch (tipo) {
                case AMARILLO:
                    juego.score += 100;
                    juego.enemigosAmarillosMuertos++;
                    juego.reproducirSonidoEliminado();
                    break;
                case ROJO:
                    juego.score += 200;
                    juego.enemigosRojosMuertos++;
                    juego.reproducirSonidoEnemigoRojo();
                    break;
                case VERDE:
                    juego.score += 300;
                    juego.enemigosVerdesMuertos++;
                    juego.reproducirSonidoEliminado();
                    break;
            }
        }
    }

    public boolean colisionaCon(Disparo d) {
        return d.coordenadaX < x + ancho &&
                d.coordenadaX + d.ancho() > x &&
                d.coordenadaY < y + alto &&
                d.coordenadaY + d.alto() > y;
    }

    public void actualizar() {
        avanzarAnimacion();

        switch (estado) {
            case ENTRANDO:
                moverPorBezier();
                break;

            case ATACANDO:
                if (volviendoFormacion) {
                    moverHaciaTarget(velocidadVuelta);
                } else {
                    x += ataqueVX;
                    y += ataqueVY;

                    if (tipo == Tipo.VERDE) {
                        float cx     = x + ancho / 2f;
                        float cy     = y + alto  / 2f;
                        float naveCX = juego.naveX + juego.spriteNave.getWidth()  / 2f;
                        float naveCY = juego.naveY + juego.spriteNave.getHeight() / 2f;
                        float dist   = (float) Math.sqrt(
                                (cx - naveCX) * (cx - naveCX) + (cy - naveCY) * (cy - naveCY));

                        if (dist < 80f) {
                            estado            = Estado.ABDUCIENDO;
                            abduciendoNave    = true;
                            contadorAbduccion = 0;
                            ataqueVX = 0;
                            ataqueVY = 0;

                            float areaX1 = x - 60f, areaX2 = x + ancho + 60f;
                            float areaY1 = y - 60f, areaY2 = y + alto  + 60f;

                            if (naveCX >= areaX1 && naveCX <= areaX2 &&
                                    naveCY >= areaY1 && naveCY <= areaY2) {
                                capturoNave  = true;
                                esCapturador = true;
                                juego.serAbducido();
                                juego.reproducirSonidoAbsorcion();
                            }
                            return;
                        }
                    }

                    if (y > pantAlto) {
                        y = -alto;
                        volviendoFormacion = true;
                    }
                }
                break;

            case ABDUCIENDO:
                contadorAbduccion++;
                if (contadorAbduccion >= FRAMES_MOSTRAR_RAYO) {
                    moverHaciaTarget(velocidadVuelta);
                    if (estado == Estado.EN_FORMACION) {
                        juego.controlesBloqueados = false;
                        abduciendoNave = false;
                    }
                }
                break;
        }
    }

    public void dibujar(Canvas canvas, Paint paint) {
        if (estado == Estado.ESPERANDO || estado == Estado.MUERTO) return;

        Bitmap[] sprites = (tipo == Tipo.VERDE && vida == 1 && framesDanado != null)
                ? framesDanado : frames;
        if (sprites != null && frameActual < sprites.length && sprites[frameActual] != null) {
            canvas.drawBitmap(sprites[frameActual], x, y, paint);
        }

        if (estado == Estado.ABDUCIENDO && contadorAbduccion < FRAMES_MOSTRAR_RAYO && framesRayo != null) {
            int   rayoW = framesRayo[0].getWidth();
            float rayoX = x + ancho / 2f - rayoW / 2f;
            float rayoY = y + alto;
            canvas.drawBitmap(framesRayo[frameRayo], rayoX, rayoY, paint);

            contadorFrameRayo++;
            if (contadorFrameRayo >= 4) {
                contadorFrameRayo = 0;
                frameRayo = (frameRayo + 1) % framesRayo.length;
            }
        }

        boolean debesDibujarNave = esCapturador &&
                ((estado == Estado.ABDUCIENDO && contadorAbduccion >= FRAMES_MOSTRAR_RAYO)
                        || estado == Estado.EN_FORMACION);
        if (debesDibujarNave && juego.spriteNave != null) {
            float naveX = x + ancho / 2f - juego.spriteNave.getWidth()  / 2f;
            float naveY = y - juego.spriteNave.getHeight() - 5;
            canvas.drawBitmap(juego.spriteNave, naveX, naveY, paint);
        }
    }

    // ── Métodos privados ──────────────────────────────────────────────────────

    /**
     * Avanza el parámetro t de la curva Bézier y calcula la posición del enemigo.
     * Fórmula Bézier cúbica:
     *   B(t) = (1-t)³·P0 + 3(1-t)²t·P1 + 3(1-t)t²·P2 + t³·P3
     * Cuando t llega a 1.0 el enemigo está exactamente en (targetX, targetY)
     * y pasa a estado EN_FORMACION.
     */
    private void moverPorBezier() {
        bezT += bezVelocidad;
        if (bezT >= 1f) {
            bezT   = 1f;
            x      = targetX;
            y      = targetY;
            estado = Estado.EN_FORMACION;
            return;
        }

        float u  = 1f - bezT;
        float u2 = u  * u;
        float u3 = u2 * u;
        float t2 = bezT * bezT;
        float t3 = t2   * bezT;

        x = u3 * bezP0x
                + 3 * u2 * bezT * bezP1x
                + 3 * u  * t2   * bezP2x
                + t3             * targetX;

        y = u3 * bezP0y
                + 3 * u2 * bezT * bezP1y
                + 3 * u  * t2   * bezP2y
                + t3             * targetY;
    }

    private void avanzarAnimacion() {
        contadorFrame++;
        if (contadorFrame >= FRAMES_POR_TICK) {
            contadorFrame = 0;
            if (animAvanzando) {
                frameActual++;
                if (frameActual >= TOTAL_FRAMES - 1) animAvanzando = false;
            } else {
                frameActual--;
                if (frameActual <= 0) animAvanzando = true;
            }
        }
    }

    private void moverHaciaTarget(float velocidad) {
        float dx   = targetX - x;
        float dy   = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < velocidad) {
            x      = targetX;
            y      = targetY;
            estado = Estado.EN_FORMACION;
            volviendoFormacion = false;
        } else {
            x += (dx / dist) * velocidad;
            y += (dy / dist) * velocidad;
        }
    }
}