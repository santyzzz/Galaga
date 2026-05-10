package es.riberadeltajo.galaga;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class Juego extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = Juego.class.getSimpleName();

    // ══════════════════════════════════════════════════════════════
    // SISTEMA / REFERENCIAS
    // ══════════════════════════════════════════════════════════════
    private SurfaceHolder holder;
    private Activity actividad;
    public BucleJuego bucle;
    public GestorEnemigos gestorEnemigos;

    // ══════════════════════════════════════════════════════════════
    // TAMAÑO DE PANTALLA
    // ══════════════════════════════════════════════════════════════
    public int anchoPantalla, altoPantalla;

    // ══════════════════════════════════════════════════════════════
    // INTRO DEL NIVEL
    // ══════════════════════════════════════════════════════════════
    private boolean enFaseIntro            = true;
    private boolean introSonidoReproducido = false;
    private int     contadorIntro          = 0;
    private static final int DURACION_INTRO = 90;
    private android.graphics.Typeface fuenteMenu;

    private int     idSonidoIntroNivel;
    private boolean sonidoIntroNivelListo = false;

    // ══════════════════════════════════════════════════════════════
    // SPRITES
    // ══════════════════════════════════════════════════════════════
    Bitmap spriteNave;
    Bitmap spriteDisparoNave;
    Bitmap spriteFlechaIzq;
    Bitmap spriteFlechaDer;
    Bitmap spriteBotonDisparar;
    Bitmap spriteBotonPausa;
    Bitmap spriteBotonPlay;

    private int anchoNave, altoNave;
    private int pausaSize;
    private int anchoBoton, altoBoton;
    private int anchoDisparo, altoDisparo;

    // ══════════════════════════════════════════════════════════════
    // NAVE DEL JUGADOR
    // ══════════════════════════════════════════════════════════════
    public float naveX, naveY;
    private float velocidad;

    // ══════════════════════════════════════════════════════════════
    // DISPAROS DEL JUGADOR
    // ══════════════════════════════════════════════════════════════
    private ArrayList<Disparo> lista_disparos        = new ArrayList<>();
    private int     frames_para_nuevo_disparo        = 0;
    private final int MAX_FRAMES_ENTRE_DISPARO       = BucleJuego.MAX_FPS / 2;
    private boolean nuevo_disparo                    = false;
    private float   velocidadDisparo;

    // ══════════════════════════════════════════════════════════════
    // BOTONES TÁCTILES
    // ══════════════════════════════════════════════════════════════
    private int  botonIzqX,  botonIzqY;
    private int  botonDerX,  botonDerY;
    private int  botonDispX, botonDispY;
    private Rect rectBotonIzq;
    private Rect rectBotonDer;
    private Rect rectBotonDisp;

    // ── Botón de pausa ────────────────────────────────────────────
    // Se dibuja en la esquina superior derecha del HUD
    private Rect rectBotonPausa;
    private static final int PAUSA_BTN_SIZE = 120; // tamaño del área táctil en px

    // Estado de cada botón
    private boolean pulsandoIzquierda = false;
    private boolean pulsandoDerecha   = false;
    private boolean pulsadoDisparador = false;
    private int pointerIzq  = -1;
    private int pointerDer  = -1;
    private int pointerDisp = -1;

    // ══════════════════════════════════════════════════════════════
    // PAUSA
    // ══════════════════════════════════════════════════════════════
    // Flag leído por BucleJuego: si es true el hilo deja de actualizar y dibujar
    public volatile boolean pausado = false;

    // ══════════════════════════════════════════════════════════════
    // ESTADO DEL JUGADOR
    // ══════════════════════════════════════════════════════════════
    public int     vidas          = 3;
    private int    topInset       = 0;
    public  int    score          = 0;
    public  int    navesCapturadas = 0;
    public  boolean controlesBloqueados = false;

    // ══════════════════════════════════════════════════════════════
    // SONIDOS
    // ══════════════════════════════════════════════════════════════
    private SoundPool soundPool;
    private int idSonidoDisparo;
    private int idSonidoEnemigoRojo;
    private int idSonidoEnemigoEliminado;
    private int idSonidoAbsorcion;
    private int idSonidoNivel;
    private boolean sonidoListo          = false;
    private boolean sonidoRojoListo      = false;
    private boolean sonidoEliminadoListo = false;
    private boolean sonidoAbsorcionListo = false;
    private boolean sonidoNivelListo     = false;

    // ══════════════════════════════════════════════════════════════
    // MEJORAS DE LA NAVE
    // ══════════════════════════════════════════════════════════════
    public enum NivelNave { DEFAULT, DISPARO_RAPIDO, DOBLE_CANON }
    public NivelNave nivelNave = NivelNave.DEFAULT;
    private Bitmap spriteNaveDefault;
    private Bitmap spriteNaveDisparoRapido;
    private Bitmap spriteNaveDobleCanon;

    // ══════════════════════════════════════════════════════════════
    // GAME OVER
    // ══════════════════════════════════════════════════════════════
    private boolean gameOver         = false;
    // Área táctil del botón "VOLVER AL MENÚ" de la pantalla de Game Over
    private Rect    rectBotonGameOver = null;

    // ══════════════════════════════════════════════════════════════
    // FLAGS INTERNOS
    // ══════════════════════════════════════════════════════════════
    private boolean spritesListos = false;

    // ══════════════════════════════════════════════════════════════
    // FONDO ESTRELLADO
    // ══════════════════════════════════════════════════════════════
    private static final int NUM_ESTRELLAS = 120;
    private float[] estrellaX    = new float[NUM_ESTRELLAS];
    private float[] estrellaY    = new float[NUM_ESTRELLAS];
    private float[] estrellaVel  = new float[NUM_ESTRELLAS]; // velocidad de caída
    private int[]   estrellaBrillo = new int[NUM_ESTRELLAS]; // brillo actual
    private int[]   estrellaBrilloDir = new int[NUM_ESTRELLAS]; // dirección parpadeo
    private int[]   estrellaSize = new int[NUM_ESTRELLAS];   // tamaño (1 o 2px)
    private boolean estrellasInicializadas = false;

    // ══════════════════════════════════════════════════════════════
    // NIVELES Y TRANSICIÓN
    // ══════════════════════════════════════════════════════════════
    public int nivelActual = 1;
    private static final int MAX_NIVELES = 6;

    private enum FaseTransicion { JUGANDO, NAVE_SUBIENDO, ESPERANDO, NAVE_BAJANDO }
    private FaseTransicion faseTransicion = FaseTransicion.JUGANDO;

    private int   contadorTransicion = 0;
    private static final int FRAMES_ESPERA_ENTRE_NIVELES = 60;
    private float velocidadTransicion;

    // ══════════════════════════════════════════════════════════════
    // ESTADÍSTICAS DE LA PARTIDA
    // ══════════════════════════════════════════════════════════════
    public int  disparosRealizados       = 0;
    public int  disparosAcertados        = 0;
    public int  enemigosAmarillosMuertos = 0;
    public int  enemigosRojosMuertos     = 0;
    public int  enemigosVerdesMuertos    = 0;
    private boolean mostrandoEstadisticas = false;


    // ══════════════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════════════

    public Juego(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);

        spriteNave              = BitmapFactory.decodeResource(context.getResources(), R.drawable.nave_basica);
        spriteFlechaIzq         = BitmapFactory.decodeResource(context.getResources(), R.drawable.flecha_izquierda);
        spriteFlechaDer         = BitmapFactory.decodeResource(context.getResources(), R.drawable.flecha_derecha);
        spriteBotonDisparar     = BitmapFactory.decodeResource(context.getResources(), R.drawable.boton_disparo);
        spriteDisparoNave       = BitmapFactory.decodeResource(context.getResources(), R.drawable.disparo);
        spriteNaveDefault       = BitmapFactory.decodeResource(context.getResources(), R.drawable.nave_basica);
        spriteNaveDisparoRapido = BitmapFactory.decodeResource(context.getResources(), R.drawable.nave_disparo_rapido);
        spriteNaveDobleCanon    = BitmapFactory.decodeResource(context.getResources(), R.drawable.nave_doble_canion);
        spriteBotonPausa        = BitmapFactory.decodeResource(context.getResources(), R.drawable.pause_button);
        spriteBotonPlay         = BitmapFactory.decodeResource(context.getResources(), R.drawable.play_button);

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attrs)
                .build();

        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0) {
                if (sampleId == idSonidoDisparo)          sonidoListo          = true;
                if (sampleId == idSonidoEnemigoRojo)      sonidoRojoListo      = true;
                if (sampleId == idSonidoEnemigoEliminado) sonidoEliminadoListo = true;
                if (sampleId == idSonidoAbsorcion)        sonidoAbsorcionListo = true;
                if (sampleId == idSonidoNivel)            sonidoNivelListo     = true;
                if (sampleId == idSonidoIntroNivel)       sonidoIntroNivelListo = true;
            }
        });

        idSonidoDisparo          = soundPool.load(context, R.raw.sonido_disparo_jugador, 1);
        idSonidoEnemigoRojo      = soundPool.load(context, R.raw.enemigo_rojo_eliminado, 1);
        idSonidoEnemigoEliminado = soundPool.load(context, R.raw.enemigo_eliminado_2,    1);
        idSonidoAbsorcion        = soundPool.load(context, R.raw.absorcion,              1);
        idSonidoNivel            = soundPool.load(context, R.raw.enemigo_ataca,          1);
        idSonidoIntroNivel       = soundPool.load(context, R.raw.intro_primer_nivel,     1);

        fuenteMenu = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.fuente_menu);
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  INICIALIZACIÓN
    // ══════════════════════════════════════════════════════════════════════════

    private void inicializarPosiciones(int pantallaAncho, int pantallaAlto) {
        if (spritesListos) return;

        anchoPantalla = pantallaAncho;
        altoPantalla  = pantallaAlto;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            topInset = (resourceId > 0)
                    ? getResources().getDimensionPixelSize(resourceId)
                    : (int)(25 * getResources().getDisplayMetrics().density);
        } else {
            topInset = (int)(25 * getResources().getDisplayMetrics().density);
        }
        topInset += (int)(8 * getResources().getDisplayMetrics().density);

        anchoNave  = pantallaAncho / 10;
        altoNave   = (int)(spriteNave.getHeight() * (float) anchoNave / spriteNave.getWidth());
        spriteNave = Bitmap.createScaledBitmap(spriteNave, anchoNave, altoNave, true);

        spriteNaveDefault       = Bitmap.createScaledBitmap(spriteNaveDefault,       anchoNave, altoNave, true);
        spriteNaveDisparoRapido = (spriteNaveDisparoRapido != null)
                ? Bitmap.createScaledBitmap(spriteNaveDisparoRapido, anchoNave, altoNave, true) : spriteNave;
        spriteNaveDobleCanon    = (spriteNaveDobleCanon != null)
                ? Bitmap.createScaledBitmap(spriteNaveDobleCanon,    anchoNave, altoNave, true) : spriteNave;

        anchoBoton = pantallaAncho / 6;
        altoBoton  = (int)(spriteFlechaIzq.getHeight() * ((float) anchoBoton / spriteFlechaIzq.getWidth()));
        spriteFlechaIzq     = Bitmap.createScaledBitmap(spriteFlechaIzq,     anchoBoton, altoBoton, true);
        spriteFlechaDer     = Bitmap.createScaledBitmap(spriteFlechaDer,     anchoBoton, altoBoton, true);
        spriteBotonDisparar = Bitmap.createScaledBitmap(spriteBotonDisparar, anchoBoton, altoBoton, true);

        anchoDisparo      = pantallaAncho / 20;
        altoDisparo       = (int)(spriteDisparoNave.getHeight() * ((float) anchoDisparo / spriteDisparoNave.getWidth()));
        spriteDisparoNave = Bitmap.createScaledBitmap(spriteDisparoNave, anchoDisparo, altoDisparo, true);

        velocidadDisparo = pantallaAlto / 30f;

        naveX = (pantallaAncho - anchoNave) / 2f;
        naveY = (float)(pantallaAlto / 1.45);

        velocidad = pantallaAncho / 60f;

        int margen = 180;
        botonIzqX  = margen;
        botonIzqY  = pantallaAlto - altoBoton - margen;
        botonDerX  = anchoBoton + margen + 100;
        botonDerY  = pantallaAlto - altoBoton - margen;
        botonDispX = pantallaAncho - anchoBoton - margen;
        botonDispY = pantallaAlto  - altoBoton - margen;

        rectBotonIzq  = new Rect(botonIzqX,  botonIzqY,  botonIzqX  + anchoBoton, botonIzqY  + altoBoton);
        rectBotonDer  = new Rect(botonDerX,  botonDerY,  botonDerX  + anchoBoton, botonDerY  + altoBoton);
        rectBotonDisp = new Rect(botonDispX, botonDispY, botonDispX + anchoBoton, botonDispY + altoBoton);

        // Botón de pausa: centrado horizontalmente, misma altura que el HUD (score y vidas)
        pausaSize = pantallaAncho / 10;
        if (spriteBotonPausa != null)
            spriteBotonPausa = Bitmap.createScaledBitmap(spriteBotonPausa, pausaSize, pausaSize, true);
        if (spriteBotonPlay != null)
            spriteBotonPlay  = Bitmap.createScaledBitmap(spriteBotonPlay,  pausaSize, pausaSize, true);
        int pausaLeft = (pantallaAncho - pausaSize) / 2;
        int pausaTop  = topInset + 10;
        rectBotonPausa = new Rect(pausaLeft, pausaTop, pausaLeft + pausaSize, pausaTop + pausaSize);

        // Inicializar posiciones aleatorias de las estrellas
        if (!estrellasInicializadas) {
            java.util.Random rnd = new java.util.Random();
            for (int i = 0; i < NUM_ESTRELLAS; i++) {
                estrellaX[i]       = rnd.nextInt(pantallaAncho);
                estrellaY[i]       = rnd.nextInt(pantallaAlto);
                // 3 capas de profundidad: lentas (fondo), medias, rápidas (primer plano)
                float capa = rnd.nextFloat();
                if      (capa < 0.5f) estrellaVel[i] = 0.4f + rnd.nextFloat() * 0.3f; // fondo
                else if (capa < 0.8f) estrellaVel[i] = 0.9f + rnd.nextFloat() * 0.5f; // medio
                else                  estrellaVel[i] = 1.8f + rnd.nextFloat() * 0.8f; // frente
                estrellaBrillo[i]    = 100 + rnd.nextInt(155);
                estrellaBrilloDir[i] = (rnd.nextBoolean()) ? 3 : -3;
                estrellaSize[i]      = (estrellaVel[i] > 1.5f) ? 2 : 1;
            }
            estrellasInicializadas = true;
        }

        gestorEnemigos = new GestorEnemigos(this);
        spritesListos  = true;
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  TOUCH
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!spritesListos) return true;

        int accion    = event.getActionMasked();
        int indice    = event.getActionIndex();
        int pointerId = event.getPointerId(indice);
        int x         = (int) event.getX(indice);
        int y         = (int) event.getY(indice);

        if (accion == MotionEvent.ACTION_DOWN || accion == MotionEvent.ACTION_POINTER_DOWN) {
            // ── Botón de pausa: congela el bucle y lanza PauseActivity ────────
            if (!gameOver && rectBotonPausa != null && rectBotonPausa.contains(x, y)) {
                pausado = true;
                ((Activity) getContext()).startActivityForResult(
                        new Intent(getContext(), PauseActivity.class), 1);
                return true;
            }
        }

        // Si el juego está pausado o en game over no procesamos más entradas
        if (pausado || gameOver) return true;

        switch (accion) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (rectBotonIzq.contains(x, y))  { pulsandoIzquierda = true; pointerIzq  = pointerId; }
                if (rectBotonDer.contains(x, y))  { pulsandoDerecha   = true; pointerDer  = pointerId; }
                if (rectBotonDisp.contains(x, y)) { pulsadoDisparador = true; pointerDisp = pointerId; }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (pointerId == pointerIzq)  { pulsandoIzquierda = false; pointerIzq  = -1; }
                if (pointerId == pointerDer)  { pulsandoDerecha   = false; pointerDer  = -1; }
                if (pointerId == pointerDisp) { pulsadoDisparador = false; pointerDisp = -1; }
                break;

            case MotionEvent.ACTION_CANCEL:
                pulsandoIzquierda = pulsandoDerecha = pulsadoDisparador = false;
                pointerIzq = pointerDer = pointerDisp = -1;
                break;
        }
        return true;
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  LÓGICA
    // ══════════════════════════════════════════════════════════════════════════

    public void actualizar() {
        if (gameOver) return;

        // Actualizar estrellas siempre, incluso durante transiciones e intro
        if (estrellasInicializadas) actualizarEstrellas();

        // ── Transición entre niveles ──────────────────────────────────────────
        if (faseTransicion != FaseTransicion.JUGANDO) {
            switch (faseTransicion) {
                case NAVE_SUBIENDO:
                    naveY -= velocidadTransicion;
                    if (naveY + altoNave < 0) {
                        faseTransicion     = FaseTransicion.ESPERANDO;
                        contadorTransicion = 0;
                    }
                    break;
                case ESPERANDO:
                    contadorTransicion++;
                    if (contadorTransicion >= FRAMES_ESPERA_ENTRE_NIVELES) {
                        nivelActual++;
                        if (nivelActual > MAX_NIVELES) nivelActual = 1;
                        gestorEnemigos.configurarNivel(nivelActual);
                        naveX          = (anchoPantalla - spriteNave.getWidth()) / 2f;
                        naveY          = altoPantalla + altoNave;
                        faseTransicion = FaseTransicion.NAVE_BAJANDO;
                        contadorTransicion = 0;
                        // Resetear intro del nivel
                        enFaseIntro            = true;
                        introSonidoReproducido = false;
                        contadorIntro          = 0;
                    }
                    break;
                case NAVE_BAJANDO:
                    float destY = (float)(altoPantalla / 1.25);
                    naveY -= velocidadTransicion;
                    if (naveY <= destY) {
                        naveY          = destY;
                        faseTransicion = FaseTransicion.JUGANDO;
                    }
                    break;
            }
            return;
        }

        if (!spritesListos) return;

        // ── Intro del nivel: solo en el nivel 1 ──────────────────────────────
        if (enFaseIntro) {
            if (nivelActual == 1) {
                if (!introSonidoReproducido && sonidoIntroNivelListo) {
                    soundPool.play(idSonidoIntroNivel, 1f, 1f, 1, 0, 1f);
                    introSonidoReproducido = true;
                }
                contadorIntro++;
                if (contadorIntro >= DURACION_INTRO) enFaseIntro = false;
                return;
            } else {
                // En niveles 2+ saltamos la intro directamente, sin sonido ni letrero
                enFaseIntro = false;
            }
        }

        // ── Controles del jugador ─────────────────────────────────────────────
        if (!controlesBloqueados) {

            // Colisión disparos enemigos con la nave
            for (int i = gestorEnemigos.disparosEnemigos.size() - 1; i >= 0; i--) {
                DisparoEnemigo d = gestorEnemigos.disparosEnemigos.get(i);
                if (d.colisionaConNave(naveX, naveY, anchoNave, altoNave)) {
                    gestorEnemigos.disparosEnemigos.remove(i);
                    recibirDanio();
                    break;
                }
            }

            // Colisión disparos del jugador con el jefe
            if (gestorEnemigos.jefeFinalDragon != null && gestorEnemigos.jefeFinalDragon.activo) {
                for (int i = lista_disparos.size() - 1; i >= 0; i--) {
                    Disparo d = lista_disparos.get(i);
                    if (gestorEnemigos.jefeFinalDragon.colisionaCon(d)) {
                        gestorEnemigos.jefeFinalDragon.recibirImpacto();
                        lista_disparos.remove(i);
                        disparosAcertados++;
                        break;
                    }
                }
            }

            if (pulsandoDerecha)   moverNave(1);
            if (pulsandoIzquierda) moverNave(-1);

            if (pulsadoDisparador) {
                nuevo_disparo = true;
                if (frames_para_nuevo_disparo == 0) {
                    if (nuevo_disparo) {
                        crearDisparo();
                        nuevo_disparo = false;
                    }
                    frames_para_nuevo_disparo = getCadenciaDisparo();
                }
                frames_para_nuevo_disparo--;
            }
        }

        // ── Comprobar abducción ───────────────────────────────────────────────
        if (gestorEnemigos != null) {
            for (Enemigo e : gestorEnemigos.enemigos) {
                if (e.tipo == Enemigo.Tipo.VERDE
                        && e.estado == Enemigo.Estado.ABDUCIENDO
                        && e.contadorAbduccion < Enemigo.FRAMES_MOSTRAR_RAYO_PUBLIC
                        && !e.capturoNave) {

                    float areaX1 = e.x - 60f, areaX2 = e.x + e.ancho + 60f;
                    float areaY1 = e.y - 60f, areaY2 = e.y + e.alto  + 60f;
                    float naveCX = naveX + spriteNave.getWidth()  / 2f;
                    float naveCY = naveY + spriteNave.getHeight() / 2f;

                    if (naveCX >= areaX1 && naveCX <= areaX2 &&
                            naveCY >= areaY1 && naveCY <= areaY2) {
                        e.capturoNave  = true;
                        e.esCapturador = true;
                        serAbducido();
                        reproducirSonidoAbsorcion();
                    }
                }
            }
        }

        // ── Lógica de enemigos ────────────────────────────────────────────────
        if (gestorEnemigos != null) {
            gestorEnemigos.actualizar();

            for (int i = lista_disparos.size() - 1; i >= 0; i--) {
                Disparo d = lista_disparos.get(i);
                for (Enemigo e : gestorEnemigos.enemigos) {
                    if (e.estado != Enemigo.Estado.MUERTO && e.colisionaCon(d)) {
                        e.recibirDanio();
                        lista_disparos.remove(i);

                        if (e.tipo == Enemigo.Tipo.VERDE
                                && e.estado == Enemigo.Estado.MUERTO
                                && e.esCapturador) {
                            if (navesCapturadas > 0) navesCapturadas--;
                            e.esCapturador = false;
                            if (navesCapturadas == 0) controlesBloqueados = false;
                            naveX = (anchoPantalla - spriteNave.getWidth()) / 2f;
                            naveY = (float)(altoPantalla / 1.40);
                            mejorarNave();
                        }
                        break;
                    }
                }
            }
        }

        // ── Mover y limpiar disparos del jugador ──────────────────────────────
        for (int i = lista_disparos.size() - 1; i >= 0; i--) {
            lista_disparos.get(i).actualizarCoordenadas();
            if (lista_disparos.get(i).fueraDePantalla()) lista_disparos.remove(i);
        }

        if (naveX < 0) naveX = 0;
        if (naveX + anchoNave > bucle.maxX) naveX = bucle.maxX - anchoNave;
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  DISPARO Y MEJORAS
    // ══════════════════════════════════════════════════════════════════════════

    private void actualizarEstrellas() {
        for (int i = 0; i < NUM_ESTRELLAS; i++) {
            // Mover hacia abajo
            estrellaY[i] += estrellaVel[i];
            if (estrellaY[i] > altoPantalla) {
                estrellaY[i] = 0;
                estrellaX[i] = (float)(Math.random() * anchoPantalla);
            }
            // Parpadeo: el brillo oscila entre 80 y 255
            estrellaBrillo[i] += estrellaBrilloDir[i];
            if (estrellaBrillo[i] >= 255) { estrellaBrillo[i] = 255; estrellaBrilloDir[i] = -3; }
            if (estrellaBrillo[i] <= 80)  { estrellaBrillo[i] = 80;  estrellaBrilloDir[i] =  3; }
        }
    }

    private void dibujarEstrellas(Canvas canvas) {
        android.graphics.Paint p = new android.graphics.Paint();
        for (int i = 0; i < NUM_ESTRELLAS; i++) {
            int b = estrellaBrillo[i];
            p.setColor(android.graphics.Color.rgb(b, b, b));
            int s = estrellaSize[i];
            canvas.drawRect(estrellaX[i], estrellaY[i],
                    estrellaX[i] + s, estrellaY[i] + s, p);
        }
    }

    // ── Fondo por nivel ──────────────────────────────────────────────────────────
    private void dibujarFondoNivel(Canvas canvas) {
        switch (nivelActual) {
            case 1: dibujarFondoNivel1(canvas); break;
            case 2: dibujarFondoNivel2(canvas); break;
            case 3: dibujarFondoNivel3(canvas); break;
            case 4: dibujarFondoNivel4(canvas); break;
            case 5: dibujarFondoNivel5(canvas); break;
            case 6: dibujarFondoNivel6(canvas); break;
        }
    }

    /** Nivel 1 — Planeta azul con anillo (Saturno) en esquina superior izquierda */
    private void dibujarFondoNivel1(Canvas canvas) {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setAntiAlias(true);
        float cx = anchoPantalla * 0.18f, cy = altoPantalla * 0.18f;
        float r  = anchoPantalla * 0.13f;

        // Cuerpo azul
        p.setShader(new android.graphics.RadialGradient(
                cx - r * 0.3f, cy - r * 0.3f, r,
                new int[]{ android.graphics.Color.rgb(120,160,220),
                        android.graphics.Color.rgb( 40, 60,140),
                        android.graphics.Color.rgb( 15, 15, 55) },
                new float[]{ 0f, 0.55f, 1f },
                android.graphics.Shader.TileMode.CLAMP));
        p.setStyle(android.graphics.Paint.Style.FILL);
        canvas.drawCircle(cx, cy, r, p);

        // Anillo inclinado
        p.setShader(null);
        p.setColor(android.graphics.Color.argb(90, 160, 190, 255));
        p.setStyle(android.graphics.Paint.Style.STROKE);
        p.setStrokeWidth(r * 0.18f);
        canvas.save();
        canvas.rotate(-18f, cx, cy);
        canvas.drawOval(new android.graphics.RectF(cx-r*1.55f, cy-r*0.28f, cx+r*1.55f, cy+r*0.28f), p);
        canvas.restore();

        // Borde
        p.setStyle(android.graphics.Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        p.setColor(android.graphics.Color.argb(100, 10, 10, 40));
        canvas.drawCircle(cx, cy, r, p);
    }

    /** Nivel 2 — Luna gris en esquina superior derecha con cráteres */
    private void dibujarFondoNivel2(Canvas canvas) {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setAntiAlias(true);
        float cx = anchoPantalla * 0.82f, cy = altoPantalla * 0.14f;
        float r  = anchoPantalla * 0.11f;

        // Cuerpo gris
        p.setShader(new android.graphics.RadialGradient(
                cx - r * 0.3f, cy - r * 0.3f, r,
                new int[]{ android.graphics.Color.rgb(210,210,210),
                        android.graphics.Color.rgb(130,130,130),
                        android.graphics.Color.rgb( 60, 60, 60) },
                new float[]{ 0f, 0.6f, 1f },
                android.graphics.Shader.TileMode.CLAMP));
        p.setStyle(android.graphics.Paint.Style.FILL);
        canvas.drawCircle(cx, cy, r, p);

        // Cráteres
        p.setShader(null);
        p.setStyle(android.graphics.Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setColor(android.graphics.Color.argb(80, 40, 40, 40));
        canvas.drawCircle(cx - r*0.3f, cy + r*0.2f, r*0.2f, p);
        canvas.drawCircle(cx + r*0.35f, cy - r*0.25f, r*0.13f, p);
        canvas.drawCircle(cx - r*0.1f,  cy - r*0.4f,  r*0.09f, p);
        canvas.drawCircle(cx + r*0.15f, cy + r*0.45f, r*0.07f, p);

        // Borde
        p.setStyle(android.graphics.Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        p.setColor(android.graphics.Color.argb(80, 20, 20, 20));
        canvas.drawCircle(cx, cy, r, p);
    }

    /** Nivel 3 — Planeta rojo (tipo Marte) centrado arriba + constelación */
    private void dibujarFondoNivel3(Canvas canvas) {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setAntiAlias(true);
        float cx = anchoPantalla * 0.5f, cy = altoPantalla * 0.12f;
        float r  = anchoPantalla * 0.10f;

        // Cuerpo rojo
        p.setShader(new android.graphics.RadialGradient(
                cx - r*0.3f, cy - r*0.3f, r,
                new int[]{ android.graphics.Color.rgb(230,130, 80),
                        android.graphics.Color.rgb(180, 60, 30),
                        android.graphics.Color.rgb( 80, 15, 10) },
                new float[]{ 0f, 0.55f, 1f },
                android.graphics.Shader.TileMode.CLAMP));
        p.setStyle(android.graphics.Paint.Style.FILL);
        canvas.drawCircle(cx, cy, r, p);

        // Casquete polar blanco
        p.setShader(null);
        p.setColor(android.graphics.Color.argb(120, 255, 255, 255));
        canvas.drawCircle(cx, cy - r*0.65f, r*0.28f, p);

        // Borde
        p.setStyle(android.graphics.Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        p.setColor(android.graphics.Color.argb(100, 60, 10, 5));
        canvas.drawCircle(cx, cy, r, p);

        // Constelación (triángulo de estrellas brillantes en esquina inferior derecha)
        dibujarConstelacion(canvas,
                new float[]{ anchoPantalla*0.72f, anchoPantalla*0.85f, anchoPantalla*0.90f,
                        anchoPantalla*0.78f, anchoPantalla*0.68f },
                new float[]{ altoPantalla*0.72f,  altoPantalla*0.65f,  altoPantalla*0.80f,
                        altoPantalla*0.82f,  altoPantalla*0.60f });
    }

    /** Nivel 4 — Planeta verde (gas gigante) esquina inferior izquierda + nebulosa */
    private void dibujarFondoNivel4(Canvas canvas) {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setAntiAlias(true);

        // Nebulosa difusa en el centro-derecha
        p.setShader(new android.graphics.RadialGradient(
                anchoPantalla*0.75f, altoPantalla*0.4f, anchoPantalla*0.35f,
                new int[]{ android.graphics.Color.argb(40,  80,  0, 120),
                        android.graphics.Color.argb(20,  40,  0,  80),
                        android.graphics.Color.argb( 0,   0,  0,   0) },
                new float[]{ 0f, 0.5f, 1f },
                android.graphics.Shader.TileMode.CLAMP));
        p.setStyle(android.graphics.Paint.Style.FILL);
        canvas.drawRect(0, 0, anchoPantalla, altoPantalla, p);

        // Planeta verde
        float cx = anchoPantalla * 0.2f, cy = altoPantalla * 0.82f;
        float r  = anchoPantalla * 0.14f;
        p.setShader(new android.graphics.RadialGradient(
                cx - r*0.3f, cy - r*0.3f, r,
                new int[]{ android.graphics.Color.rgb(100,210,120),
                        android.graphics.Color.rgb( 30,130, 60),
                        android.graphics.Color.rgb( 10, 50, 20) },
                new float[]{ 0f, 0.55f, 1f },
                android.graphics.Shader.TileMode.CLAMP));
        p.setStyle(android.graphics.Paint.Style.FILL);
        canvas.drawCircle(cx, cy, r, p);

        // Bandas atmosféricas horizontales
        p.setShader(null);
        p.setStyle(android.graphics.Paint.Style.STROKE);
        p.setStrokeWidth(r * 0.08f);
        p.setColor(android.graphics.Color.argb(60, 20, 80, 30));
        canvas.save();
        canvas.clipRect(cx-r, cy-r, cx+r, cy+r);
        for (float offset : new float[]{ -0.35f, -0.1f, 0.15f, 0.38f }) {
            canvas.drawLine(cx-r, cy+r*offset, cx+r, cy+r*offset, p);
        }
        canvas.restore();

        // Borde
        p.setStyle(android.graphics.Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        p.setColor(android.graphics.Color.argb(100, 5, 30, 10));
        canvas.drawCircle(cx, cy, r, p);
    }

    /** Nivel 5 — Planeta naranja gigante (tipo Júpiter) con tormenta + constelación */
    private void dibujarFondoNivel5(Canvas canvas) {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setAntiAlias(true);
        float cx = anchoPantalla * 0.78f, cy = altoPantalla * 0.20f;
        float r  = anchoPantalla * 0.16f;

        // Cuerpo naranja
        p.setShader(new android.graphics.RadialGradient(
                cx - r*0.3f, cy - r*0.3f, r,
                new int[]{ android.graphics.Color.rgb(240,180, 80),
                        android.graphics.Color.rgb(200,100, 30),
                        android.graphics.Color.rgb( 90, 35,  5) },
                new float[]{ 0f, 0.55f, 1f },
                android.graphics.Shader.TileMode.CLAMP));
        p.setStyle(android.graphics.Paint.Style.FILL);
        canvas.drawCircle(cx, cy, r, p);

        // Bandas de Júpiter
        p.setShader(null);
        p.setStyle(android.graphics.Paint.Style.STROKE);
        p.setStrokeWidth(r * 0.09f);
        p.setColor(android.graphics.Color.argb(70, 120, 50, 10));
        canvas.save();
        canvas.clipRect(cx-r, cy-r, cx+r, cy+r);
        for (float offset : new float[]{ -0.45f, -0.2f, 0.05f, 0.28f, 0.48f }) {
            canvas.drawLine(cx-r, cy+r*offset, cx+r, cy+r*offset, p);
        }
        canvas.restore();

        // Gran mancha roja (tormenta)
        p.setShader(null);
        p.setStyle(android.graphics.Paint.Style.FILL);
        p.setColor(android.graphics.Color.argb(150, 180, 50, 30));
        canvas.drawOval(new android.graphics.RectF(
                cx - r*0.22f, cy + r*0.08f, cx + r*0.22f, cy + r*0.30f), p);

        // Borde
        p.setStyle(android.graphics.Paint.Style.STROKE);
        p.setStrokeWidth(3f);
        p.setColor(android.graphics.Color.argb(100, 60, 20, 5));
        canvas.drawCircle(cx, cy, r, p);

        // Constelación en esquina inferior izquierda
        dibujarConstelacion(canvas,
                new float[]{ anchoPantalla*0.08f, anchoPantalla*0.18f, anchoPantalla*0.12f,
                        anchoPantalla*0.22f, anchoPantalla*0.06f },
                new float[]{ altoPantalla*0.68f,  altoPantalla*0.75f,  altoPantalla*0.83f,
                        altoPantalla*0.62f,  altoPantalla*0.77f });
    }

    /** Nivel 6 (jefe) — Fondo rojo oscuro amenazante, sin planeta */
    private void dibujarFondoNivel6(Canvas canvas) {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setAntiAlias(true);

        // Nebulosa roja difusa centrada
        p.setShader(new android.graphics.RadialGradient(
                anchoPantalla*0.5f, altoPantalla*0.35f, anchoPantalla*0.5f,
                new int[]{ android.graphics.Color.argb(60, 150,  0,  0),
                        android.graphics.Color.argb(30,  80,  0,  0),
                        android.graphics.Color.argb( 0,   0,  0,  0) },
                new float[]{ 0f, 0.6f, 1f },
                android.graphics.Shader.TileMode.CLAMP));
        p.setStyle(android.graphics.Paint.Style.FILL);
        canvas.drawRect(0, 0, anchoPantalla, altoPantalla, p);
    }

    /**
     * Dibuja una constelación: puntos brillantes unidos por líneas tenues.
     * xs[] e ys[] son las coordenadas de cada estrella de la constelación.
     */
    private void dibujarConstelacion(Canvas canvas, float[] xs, float[] ys) {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setAntiAlias(true);

        // Líneas tenues entre estrellas consecutivas
        p.setColor(android.graphics.Color.argb(50, 180, 200, 255));
        p.setStrokeWidth(1.5f);
        for (int i = 0; i < xs.length - 1; i++) {
            canvas.drawLine(xs[i], ys[i], xs[i+1], ys[i+1], p);
        }

        // Estrellas brillantes
        p.setStyle(android.graphics.Paint.Style.FILL);
        p.setColor(android.graphics.Color.argb(220, 220, 230, 255));
        for (int i = 0; i < xs.length; i++) {
            canvas.drawCircle(xs[i], ys[i], 4f, p);
        }
        // Destello central en la primera estrella (la más brillante)
        p.setColor(android.graphics.Color.argb(255, 255, 255, 255));
        canvas.drawCircle(xs[0], ys[0], 5.5f, p);
    }

    private void crearDisparo() {
        switch (nivelNave) {
            case DOBLE_CANON:
                lista_disparos.add(new Disparo(this, naveX - anchoNave * 0.15f, naveY - 75));
                lista_disparos.add(new Disparo(this, naveX + anchoNave * 0.45f, naveY - 75));
                break;
            default:
                lista_disparos.add(new Disparo(this, naveX, naveY - 75));
                break;
        }
        disparosRealizados++;
        if (sonidoListo) soundPool.play(idSonidoDisparo, 1f, 1f, 1, 0, 1f);
    }

    public void mejorarNave() {
        switch (nivelNave) {
            case DEFAULT:
                nivelNave  = NivelNave.DISPARO_RAPIDO;
                spriteNave = spriteNaveDisparoRapido;
                vidas++;
                break;
            case DISPARO_RAPIDO:
                nivelNave  = NivelNave.DOBLE_CANON;
                spriteNave = spriteNaveDobleCanon;
                vidas++;
                break;
            case DOBLE_CANON:
                // Ya en nivel máximo, no se puede mejorar más
                break;
        }
    }

    public void degradarNave() {
        switch (nivelNave) {
            case DOBLE_CANON:
                nivelNave  = NivelNave.DISPARO_RAPIDO;
                spriteNave = spriteNaveDisparoRapido;
                break;
            case DISPARO_RAPIDO:
                nivelNave  = NivelNave.DEFAULT;
                spriteNave = spriteNaveDefault;
                break;
            case DEFAULT:
                break;
        }
    }

    private int getCadenciaDisparo() {
        switch (nivelNave) {
            case DISPARO_RAPIDO: return BucleJuego.MAX_FPS / 4;
            case DOBLE_CANON:    return BucleJuego.MAX_FPS / 3;
            default:             return BucleJuego.MAX_FPS / 2;
        }
    }

    public float getHudY() { return topInset + 50f + 10f; }

    public void moverNave(int direccion) {
        naveX += direccion * velocidad;
        if (naveX < 0) naveX = 0;
        if (naveX + anchoNave > bucle.maxX) naveX = bucle.maxX - anchoNave;
    }

    public void nivelSuperado() {
        if (faseTransicion != FaseTransicion.JUGANDO) return;
        lista_disparos.clear();
        gestorEnemigos.disparosEnemigos.clear();

        if (nivelActual >= MAX_NIVELES) {
            Intent intent = new Intent(getContext(), ResultadosActivity.class);
            intent.putExtra("score",     score);
            intent.putExtra("vidas",     vidas);
            intent.putExtra("disparos",  disparosRealizados);
            intent.putExtra("acertados", disparosAcertados);
            intent.putExtra("amarillos", enemigosAmarillosMuertos);
            intent.putExtra("rojos",     enemigosRojosMuertos);
            intent.putExtra("verdes",    enemigosVerdesMuertos);
            ((android.app.Activity) getContext()).startActivity(intent);
            bucle.JuegoEnEjecucion = false;
            return;
        }

        faseTransicion      = FaseTransicion.NAVE_SUBIENDO;
        contadorTransicion  = 0;
        velocidadTransicion = altoPantalla / 30f;
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  DIBUJO
    // ══════════════════════════════════════════════════════════════════════════

    public void renderizar(Canvas canvas) {
        inicializarPosiciones(canvas.getWidth(), canvas.getHeight());

        canvas.drawColor(Color.BLACK);

        // Fondo estrellado: se dibuja primero para que quede detrás de todo
        if (estrellasInicializadas) dibujarEstrellas(canvas);

        // Fondo específico de cada nivel (planeta, luna, nebulosa...)
        dibujarFondoNivel(canvas);

        // ── Intro del nivel: letrero solo en nivel 1 ─────────────────────────
        if (enFaseIntro && nivelActual == 1 && faseTransicion == FaseTransicion.JUGANDO) {
            Paint paintIntro = new Paint();
            paintIntro.setAntiAlias(true);
            paintIntro.setColor(Color.WHITE);
            paintIntro.setTextSize(anchoPantalla / 10f);
            paintIntro.setTypeface(fuenteMenu);
            paintIntro.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("NIVEL " + nivelActual, anchoPantalla / 2f, altoPantalla / 2f, paintIntro);
            return;
        }



        // ── Estadísticas finales ──────────────────────────────────────────────
        if (mostrandoEstadisticas) {
            dibujarEstadisticas(canvas);
            return;
        }

        // ── Letrero entre niveles ─────────────────────────────────────────────
        if (faseTransicion == FaseTransicion.ESPERANDO) {
            Paint paintNivel = new Paint();
            paintNivel.setAntiAlias(true);
            paintNivel.setColor(Color.WHITE);
            paintNivel.setTextSize(anchoPantalla / 10f);
            paintNivel.setTypeface(fuenteMenu);
            paintNivel.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("NIVEL " + (nivelActual + 1 > MAX_NIVELES ? 1 : nivelActual + 1),
                    anchoPantalla / 2f, altoPantalla / 2f, paintNivel);
            return;
        }

        Paint myPaint = new Paint();
        myPaint.setStyle(Paint.Style.STROKE);
        myPaint.setColor(Color.WHITE);

        canvas.drawBitmap(spriteNave, naveX, naveY, null);
        for (Disparo d : lista_disparos) d.Dibujar(canvas, myPaint);
        if (gestorEnemigos != null) gestorEnemigos.renderizar(canvas, myPaint);

        // Botones de movimiento y disparo
        canvas.drawBitmap(spriteFlechaIzq,     botonIzqX,  botonIzqY,  null);
        canvas.drawBitmap(spriteFlechaDer,     botonDerX,  botonDerY,  null);
        canvas.drawBitmap(spriteBotonDisparar, botonDispX, botonDispY, null);

        // ── HUD ───────────────────────────────────────────────────────────────
        Paint paintHUD = new Paint();
        paintHUD.setColor(Color.WHITE);
        paintHUD.setTextSize(50);
        paintHUD.setAntiAlias(true);
        float hudY = topInset + paintHUD.getTextSize() + 10;
        canvas.drawText("SCORE: " + score, 50, hudY, paintHUD);
        canvas.drawText("VIDAS: " + vidas, anchoPantalla - 300, hudY, paintHUD);

        // ── Botón de pausa ────────────────────────────────────────────────────
        dibujarBotonPausa(canvas, paintHUD);
    }

    /** Dibuja el botón de pausa usando los PNG pause_button / play_button */
    private void dibujarBotonPausa(Canvas canvas, Paint paintBase) {
        if (rectBotonPausa == null) return;
        Bitmap sprite = pausado ? spriteBotonPlay : spriteBotonPausa;
        if (sprite != null) canvas.drawBitmap(sprite, rectBotonPausa.left, rectBotonPausa.top, null);
    }

    /**
     * Pantalla de Game Over con score y botón táctil "VOLVER AL MENÚ".
     * El Rect del botón se calcula aquí para que onTouchEvent pueda detectar el toque.
     */
    private void dibujarPantallaGameOver(Canvas canvas) {
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setTypeface(fuenteMenu);
        p.setTextAlign(Paint.Align.CENTER);

        // Título rojo
        p.setColor(Color.RED);
        p.setTextSize(anchoPantalla / 7f);
        canvas.drawText("GAME OVER", anchoPantalla / 2f, altoPantalla * 0.35f, p);

        // Score
        p.setColor(Color.WHITE);
        p.setTextSize(anchoPantalla / 14f);
        canvas.drawText("SCORE: " + score, anchoPantalla / 2f, altoPantalla * 0.50f, p);

        // ── Botón "VOLVER AL MENÚ" ─────────────────────────────────────────────
        float btnW  = anchoPantalla * 0.55f;
        float btnH  = altoPantalla  * 0.08f;
        float btnX  = (anchoPantalla - btnW) / 2f;
        float btnY  = altoPantalla  * 0.62f;

        // Guardamos el Rect para que onTouchEvent lo detecte
        rectBotonGameOver = new Rect(
                (int) btnX, (int) btnY,
                (int)(btnX + btnW), (int)(btnY + btnH));

        // Fondo del botón
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(220, 200, 0, 0));
        canvas.drawRoundRect(btnX, btnY, btnX + btnW, btnY + btnH, 30, 30, p);

        // Borde blanco
        p.setStyle(Paint.Style.STROKE);
        p.setColor(Color.WHITE);
        p.setStrokeWidth(4);
        canvas.drawRoundRect(btnX, btnY, btnX + btnW, btnY + btnH, 30, 30, p);

        // Texto del botón
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.WHITE);
        p.setTextSize(anchoPantalla / 18f);
        canvas.drawText("VOLVER AL MENÚ",
                anchoPantalla / 2f,
                btnY + btnH / 2f - (p.ascent() + p.descent()) / 2f,
                p);
    }

    private void dibujarEstadisticas(Canvas canvas) {
        float precision = disparosRealizados > 0
                ? (disparosAcertados * 100f / disparosRealizados) : 0f;

        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.WHITE);
        p.setTypeface(fuenteMenu);
        p.setTextAlign(Paint.Align.CENTER);
        float cx = anchoPantalla / 2f;

        p.setTextSize(anchoPantalla / 9f);
        canvas.drawText("PARTIDA COMPLETADA", cx, altoPantalla * 0.12f, p);

        p.setTextSize(anchoPantalla / 14f);
        float y     = altoPantalla * 0.25f;
        float lineH = altoPantalla * 0.09f;

        dibujarLineaEnemigo(canvas, p, gestorEnemigos.getSpriteAmarillo(), "x " + enemigosAmarillosMuertos, cx, y); y += lineH;
        dibujarLineaEnemigo(canvas, p, gestorEnemigos.getSpriteRojo(),     "x " + enemigosRojosMuertos,     cx, y); y += lineH;
        dibujarLineaEnemigo(canvas, p, gestorEnemigos.getSpriteVerde(),    "x " + enemigosVerdesMuertos,    cx, y); y += lineH * 1.3f;

        canvas.drawText("DISPAROS: "   + disparosRealizados,           cx, y, p); y += lineH;
        canvas.drawText(String.format("PRECISION: %.1f%%", precision), cx, y, p); y += lineH;
        canvas.drawText("VIDAS: "      + vidas,                        cx, y, p); y += lineH;
        canvas.drawText("SCORE: "      + score,                        cx, y, p);
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  ESTADO DEL JUGADOR
    // ══════════════════════════════════════════════════════════════════════════

    /** Daño normal (disparos enemigos, melee jefe) — no bloquea controles */
    public void recibirDanio() {
        vidas--;
        degradarNave();
        if (navesCapturadas == 0) controlesBloqueados = false;
        if (vidas <= 0) activarGameOver();
    }

    /** Abducción del verde — bloquea controles y quita vida */
    public void serAbducido() {
        vidas--;
        degradarNave();
        if (navesCapturadas < 2) navesCapturadas++;
        controlesBloqueados = true;
        if (vidas <= 0) activarGameOver();
    }

    /**
     * Activa el estado de Game Over:
     * - Detiene los controles
     * - Cancela cualquier pausa activa (si no, el overlay de pausa taparía el Game Over)
     * - Limpia los disparos activos para que la pantalla quede despejada
     */
    private void activarGameOver() {
        pausado             = false;
        controlesBloqueados = true;
        pulsandoIzquierda   = false;
        pulsandoDerecha     = false;
        pulsadoDisparador   = false;
        lista_disparos.clear();
        if (gestorEnemigos != null) gestorEnemigos.disparosEnemigos.clear();
        bucle.JuegoEnEjecucion = false;

        // Lanzamos GameOverActivity pasando el score
        Intent intent = new Intent(getContext(), GameOverActivity.class);
        intent.putExtra("score", score);
        ((Activity) getContext()).runOnUiThread(() -> {
            ((Activity) getContext()).startActivity(intent);
            ((Activity) getContext()).finish();
        });
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  SONIDOS
    // ══════════════════════════════════════════════════════════════════════════

    public void reproducirSonidoEnemigoRojo()  { if (sonidoRojoListo)      soundPool.play(idSonidoEnemigoRojo,      1f, 1f, 1, 0, 1f); }
    public void reproducirSonidoEliminado()    { if (sonidoEliminadoListo) soundPool.play(idSonidoEnemigoEliminado, 1f, 1f, 1, 0, 1f); }
    public void reproducirSonidoAbsorcion()    { if (sonidoAbsorcionListo) soundPool.play(idSonidoAbsorcion,        1f, 1f, 1, 0, 1f); }
    public void reproducirSonidoNivel()        { if (sonidoNivelListo)     soundPool.play(idSonidoNivel,            1f, 1f, 1, 0, 1f); }


    // ══════════════════════════════════════════════════════════════════════════
    //  CICLO DE VIDA DE LA SURFACE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        bucle = new BucleJuego(getHolder(), this);
        setFocusable(true);
        bucle.start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {}

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        Log.d(TAG, "Juego destruido");
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        boolean retry = true;
        while (retry) {
            try { bucle.join(); retry = false; }
            catch (InterruptedException e) {}
        }
    }

    private void dibujarLineaEnemigo(Canvas canvas, Paint paint, Bitmap sprite,
                                     String texto, float cx, float y) {
        if (sprite != null) {
            float spriteX = cx - sprite.getWidth()  / 2f - 80;
            float spriteY = y  - sprite.getHeight() / 2f - 20;
            canvas.drawBitmap(sprite, spriteX, spriteY, null);
        }
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(texto, cx + 20, y, paint);
        paint.setTextAlign(Paint.Align.CENTER);
    }
}