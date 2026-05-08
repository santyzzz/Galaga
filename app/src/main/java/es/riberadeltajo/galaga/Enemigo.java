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
    public float x, y;           // posición actual del enemigo en pantalla
    public float targetX, targetY;       // posición destino en la formación
    public float targetNaveX, targetNaveY; // posición de la nave (para abducción)

    // ── Estado del enemigo ────────────────────────────────────────────────────
    public int vida;
    public Tipo tipo;
    public Estado estado = Estado.ESPERANDO;
    public boolean esCapturador = false;

    // ── Animación del sprite ──────────────────────────────────────────────────
    private Bitmap[] frames;           // frames del sprite normal
    private Bitmap[] framesDanado;     // frames cuando tiene 1 vida (solo VERDE)
    private int frameActual = 0;
    private int contadorFrame = 0;
    private boolean animAvanzando = true;
    private static final int FRAMES_POR_TICK = 5;
    private static final int TOTAL_FRAMES    = 8;

    // ── Animación del rayo de abducción ───────────────────────────────────────
    public Bitmap[] framesRayo;
    private int frameRayo = 0;
    private int contadorFrameRayo = 0;

    // ── Tamaño del sprite ─────────────────────────────────────────────────────
    public int ancho, alto;

    // ── Movimiento y velocidades ──────────────────────────────────────────────
    private float velocidadEntrada;  // velocidad al entrar en formación
    private float velocidadVuelta;   // velocidad al volver tras atacar
    public float ataqueVX, ataqueVY; // vector de movimiento durante el ataque
    public boolean volviendoFormacion = false;
    private int pantAlto;
    private int pantAncho;

    // ── Abducción ─────────────────────────────────────────────────────────────
    public boolean abduciendoNave = false;
    public int contadorAbduccion  = 0;
    private static final int FRAMES_ABDUCCION    = 90;
    private static final int FRAMES_MOSTRAR_RAYO = 60;


    // ── Constructor ───────────────────────────────────────────────────────────

    // Crea un enemigo con su tipo, posición destino en la formación y por qué lado entra
    public Enemigo(Tipo tipo, float targetX, float targetY, boolean entraPorDerecha,
                   Bitmap[] frames, Bitmap[] framesDanado, int pantallaAncho, int pantallaAlto, Juego juego) {
        this.tipo      = tipo;
        this.targetX   = targetX;
        this.targetY   = targetY;
        this.frames    = frames;
        this.framesDanado = framesDanado;
        this.pantAlto  = pantallaAlto;
        this.juego     = juego;

        vida = (tipo == Tipo.VERDE) ? 2 : 1;

        if (frames != null && frames[0] != null) {
            ancho = frames[0].getWidth();
            alto  = frames[0].getHeight();
        }

        // Aparece fuera de pantalla por la izquierda o la derecha
        this.x = entraPorDerecha ? pantallaAncho + 100 : -100f;
        this.y = pantallaAlto * 0.15f;

        velocidadEntrada = pantallaAncho / 100f;
        velocidadVuelta  = pantallaAncho / 40f;
    }


    // ── Métodos públicos ──────────────────────────────────────────────────────

    // Arranca el movimiento de entrada hacia la formación
    public void iniciarEntrada() {
        estado = Estado.ENTRANDO;
    }

    // Guarda la posición de la nave como objetivo para la abducción
    public void iniciarAtaqueAbduccion(float naveX, float naveY) {
        targetNaveX = naveX;
        targetNaveY = naveY;
    }

    // Reduce la vida del enemigo, lo mata si llega a 0 y suma puntos al marcador
    public void recibirDanio() {
        vida--;
        if (vida <= 0) {
            estado = Estado.MUERTO;
            juego.disparosAcertados++;
            switch (tipo) {
                case AMARILLO:
                    juego.score+=100;
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

            if (tipo == Tipo.ROJO)     juego.reproducirSonidoEnemigoRojo();
            if (tipo == Tipo.AMARILLO) juego.reproducirSonidoEliminado();
        }
    }

    // Devuelve true si el disparo del jugador toca al enemigo
    public boolean colisionaCon(Disparo d) {
        return d.coordenadaX < x + ancho &&
                d.coordenadaX + d.ancho() > x &&
                d.coordenadaY < y + alto &&
                d.coordenadaY + d.alto() > y;
    }

    // Actualiza la lógica del enemigo cada frame: animación, movimiento y abducción
    public void actualizar() {
        avanzarAnimacion();

        switch (estado) {
            case ENTRANDO:
                moverHaciaTarget(velocidadEntrada);
                break;

            case ATACANDO:
                if (volviendoFormacion) {
                    moverHaciaTarget(velocidadVuelta);
                } else {
                    x += ataqueVX;
                    y += ataqueVY;

                    // Si es verde y llega cerca de la nave, inicia la abducción
                    if (tipo == Tipo.VERDE) {
                        float cx     = x + ancho / 2f;
                        float cy     = y + alto / 2f;
                        float naveCX = targetNaveX + 25f;
                        float naveCY = targetNaveY + 25f;
                        float dist   = (float) Math.sqrt((cx - naveCX) * (cx - naveCX) + (cy - naveCY) * (cy - naveCY));
                        if (dist < 80f) {
                            estado            = Estado.ABDUCIENDO;
                            abduciendoNave    = true;
                            contadorAbduccion = 0;
                            ataqueVX = 0;
                            ataqueVY = 0;

                            // Área de abducción: rectángulo centrado en el verde
                            // Solo bloquea si la nave está dentro de este rectángulo en este momento
                            float areaX1 = x - 60f;
                            float areaX2 = x + ancho + 60f;
                            float areaY1 = y - 60f;
                            float areaY2 = y + alto + 60f;

                             naveCX = juego.naveX + juego.spriteNave.getWidth()  / 2f;
                             naveCY = juego.naveY + juego.spriteNave.getHeight() / 2f;

                            if (naveCX >= areaX1 && naveCX <= areaX2 &&
                                    naveCY >= areaY1 && naveCY <= areaY2) {
                                juego.serAbducido(); // nave dentro del área → bloquea
                            }
                            // Si la nave se movió fuera del área → no bloquea, el verde sube solo
                            return;
                        }
                    }

                    // Si sale por abajo, reaparece arriba y vuelve a la formación
                    if (y > pantAlto) {
                        y = -alto;
                        volviendoFormacion = true;
                    }
                }
                break;

            case ABDUCIENDO:
                contadorAbduccion++;

                // Fase 2: el verde sube de vuelta a la formación arrastrando la nave
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

    // Dibuja el enemigo, el rayo de abducción y la nave capturada si corresponde
    public void dibujar(Canvas canvas, Paint paint) {
        if (estado == Estado.ESPERANDO || estado == Estado.MUERTO) return;

        // Sprite del enemigo (cambia si es verde con 1 vida)
        Bitmap[] sprites = (tipo == Tipo.VERDE && vida == 1 && framesDanado != null)
                ? framesDanado : frames;
        if (sprites != null && frameActual < sprites.length && sprites[frameActual] != null) {
            canvas.drawBitmap(sprites[frameActual], x, y, paint);
        }

        // Rayo animado durante la fase 1 de abducción
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

        // Nave capturada: se dibuja encima del verde durante la fase 2 y en formación
        boolean debesDibujarNave = esCapturador &&
                ((estado == Estado.ABDUCIENDO && contadorAbduccion >= FRAMES_MOSTRAR_RAYO)
                        || estado == Estado.EN_FORMACION);
        if (debesDibujarNave && juego.spriteNave != null) {
            float naveX = x + ancho / 2f - juego.spriteNave.getWidth() / 2f;
            float naveY = y - juego.spriteNave.getHeight() - 5;
            canvas.drawBitmap(juego.spriteNave, naveX, naveY, paint);
        }

        if (esCapturador) {
            android.util.Log.d("ABDUCCION", "estado=" + estado +
                    " contadorAbduccion=" + contadorAbduccion +
                    " esCapturador=" + esCapturador);
        }
    }


    // ── Métodos privados ──────────────────────────────────────────────────────

    // Hace ping-pong entre el primer y último frame para animar el sprite
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

    // Mueve al enemigo suavemente hacia su posición destino; cuando llega, pasa a EN_FORMACION
    private void moverHaciaTarget(float velocidad) {
        float dx   = targetX - x;
        float dy   = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < velocidad) {
            x     = targetX;
            y     = targetY;
            estado = Estado.EN_FORMACION;
            volviendoFormacion = false;
        } else {
            x += (dx / dist) * velocidad;
            y += (dy / dist) * velocidad;
        }
    }
}