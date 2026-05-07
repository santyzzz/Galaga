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

// Juego es la vista principal donde ocurre todo. Extiende SurfaceView para poder
// dibujar en ella de forma eficiente, e implementa SurfaceHolder.Callback para
// saber cuándo la superficie está lista, cambia o se destruye.
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
    private static final int DURACION_INTRO = 90; // 3 segundos a 30fps
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

    // Tamaños calculados según pantalla
    private int anchoNave, altoNave;
    private int anchoBoton, altoBoton;
    private int anchoDisparo, altoDisparo;

    // ══════════════════════════════════════════════════════════════
    // NAVE DEL JUGADOR — posición y movimiento
    // ══════════════════════════════════════════════════════════════
    public float naveX, naveY;
    private float velocidad; // píxeles por frame

    // ══════════════════════════════════════════════════════════════
    // DISPAROS DEL JUGADOR
    // ══════════════════════════════════════════════════════════════
    private ArrayList<Disparo> lista_disparos = new ArrayList<>();
    private int frames_para_nuevo_disparo = 0;
    private final int MAX_FRAMES_ENTRE_DISPARO = BucleJuego.MAX_FPS / 2; // 4 disparos/seg
    private boolean nuevo_disparo = false;
    private float velocidadDisparo;

    // ══════════════════════════════════════════════════════════════
    // BOTONES TÁCTILES — posiciones y áreas de toque
    // ══════════════════════════════════════════════════════════════
    private int botonIzqX, botonIzqY;
    private int botonDerX, botonDerY;
    private int botonDispX, botonDispY;
    private Rect rectBotonIzq;
    private Rect rectBotonDer;
    private Rect rectBotonDisp;

    // Estado de cada botón (pulsado o no) y qué dedo lo está tocando
    private boolean pulsandoIzquierda = false;
    private boolean pulsandoDerecha   = false;
    private boolean pulsadoDisparador = false;
    private int pointerIzq  = -1;
    private int pointerDer  = -1;
    private int pointerDisp = -1;

    // ══════════════════════════════════════════════════════════════
    // ESTADO DEL JUGADOR — vidas, score y abducción
    // ══════════════════════════════════════════════════════════════
    public int vidas = 3;
    private int topInset=0;
    public int score = 0;
    public int navesCapturadas = 0;      // cuántas naves tiene el verde capturadas (máx 2)
    public boolean controlesBloqueados = false; // se bloquea al ser abducido

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
    // FLAGS INTERNOS
    // ══════════════════════════════════════════════════════════════
    private boolean spritesListos = false; // true cuando ya se escalaron los sprites

    // ══════════════════════════════════════════════════════════════
    // NIVELES Y TRANSICIÓN
    // ══════════════════════════════════════════════════════════════
    public int nivelActual = 1;
    private static final int MAX_NIVELES = 6;

    // Estados de la transición entre niveles
    private enum FaseTransicion { JUGANDO, NAVE_SUBIENDO, ESPERANDO, NAVE_BAJANDO }
    private FaseTransicion faseTransicion = FaseTransicion.JUGANDO;

    private int contadorTransicion = 0;
    private static final int FRAMES_ESPERA_ENTRE_NIVELES = 60; // 2 segundos
    private float velocidadTransicion; // velocidad de subida/bajada de la nave

    // ══════════════════════════════════════════════════════════════
// ESTADÍSTICAS DE LA PARTIDA
// ══════════════════════════════════════════════════════════════
    public int disparosRealizados  = 0;
    public int disparosAcertados   = 0;
    public int enemigosAmarillosMuertos = 0;
    public int enemigosRojosMuertos     = 0;
    public int enemigosVerdesMuertos    = 0;
    private boolean mostrandoEstadisticas = false;





    // ══════════════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR — cargamos recursos y preparamos el SoundPool
    // ══════════════════════════════════════════════════════════════════════════

    // Aquí cargamos todos los bitmaps y sonidos nada más crear la vista.
    // Los bitmaps todavía no se escalan (eso lo hacemos cuando sabemos el tamaño real de pantalla).
    public Juego(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);

        // Cargamos los bitmaps en su tamaño original desde res/drawable
        spriteNave           = BitmapFactory.decodeResource(context.getResources(), R.drawable.nave_basica);
        spriteFlechaIzq      = BitmapFactory.decodeResource(context.getResources(), R.drawable.flecha_izquierda);
        spriteFlechaDer      = BitmapFactory.decodeResource(context.getResources(), R.drawable.flecha_derecha);
        spriteBotonDisparar  = BitmapFactory.decodeResource(context.getResources(), R.drawable.boton_disparo);
        spriteDisparoNave    = BitmapFactory.decodeResource(context.getResources(), R.drawable.disparo);

        // Configuramos el SoundPool con hasta 5 sonidos simultáneos
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attrs)
                .build();

        // Cuando un sonido termina de cargar, marcamos su flag como listo
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0) {
                if (sampleId == idSonidoDisparo)          sonidoListo          = true;
                if (sampleId == idSonidoEnemigoRojo)      sonidoRojoListo      = true;
                if (sampleId == idSonidoEnemigoEliminado) sonidoEliminadoListo = true;
                if (sampleId == idSonidoAbsorcion)        sonidoAbsorcionListo = true;
                if (sampleId == idSonidoNivel)            sonidoNivelListo     = true;
                if (sampleId == idSonidoIntroNivel) sonidoIntroNivelListo = true;
            }
        });

        // Cargamos todos los archivos de sonido
        idSonidoDisparo          = soundPool.load(context, R.raw.sonido_disparo_jugador, 1);
        idSonidoEnemigoRojo      = soundPool.load(context, R.raw.enemigo_rojo_eliminado, 1);
        idSonidoEnemigoEliminado = soundPool.load(context, R.raw.enemigo_eliminado_2,    1);
        idSonidoAbsorcion        = soundPool.load(context, R.raw.absorcion,              1);
        idSonidoNivel            = soundPool.load(context, R.raw.enemigo_ataca,          1);
        idSonidoIntroNivel = soundPool.load(context, R.raw.intro_primer_nivel, 1);


        // Cargamos la fuente pixel art del menú para reutilizarla en el texto de intro
        fuenteMenu = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.fuente_menu);

    }


    // ══════════════════════════════════════════════════════════════════════════
    //  INICIALIZACIÓN — escalar sprites y colocar elementos en pantalla
    // ══════════════════════════════════════════════════════════════════════════

    // Este método solo se ejecuta una vez, la primera vez que se llama a renderizar().
    // En ese momento ya sabemos el ancho y alto reales del canvas, así que
    // podemos escalar los sprites proporcionalmente y calcular posiciones.
    private void inicializarPosiciones(int pantallaAncho, int pantallaAlto) {
        if (spritesListos) return;

        anchoPantalla = pantallaAncho;
        altoPantalla  = pantallaAlto;


// En inicializarPosiciones(), reemplaza todo el bloque del topInset por esto:
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                topInset = getResources().getDimensionPixelSize(resourceId);
            } else {
                topInset = (int)(25 * getResources().getDisplayMetrics().density); // fallback 25dp
            }
        } else {
            topInset = (int)(25 * getResources().getDisplayMetrics().density);
        }
        topInset += (int)(8 * getResources().getDisplayMetrics().density); // margen extra

        // Nave: 10% del ancho de pantalla, alto proporcional
        anchoNave = pantallaAncho / 10;
        altoNave  = (int)(spriteNave.getHeight() * (float) anchoNave / spriteNave.getWidth());
        spriteNave = Bitmap.createScaledBitmap(spriteNave, anchoNave, altoNave, true);

        // Botones: 12.5% del ancho de pantalla
        anchoBoton = pantallaAncho / 6;
        altoBoton  = (int)(spriteFlechaIzq.getHeight() * ((float) anchoBoton / spriteFlechaIzq.getWidth()));
        spriteFlechaIzq     = Bitmap.createScaledBitmap(spriteFlechaIzq,     anchoBoton, altoBoton, true);
        spriteFlechaDer     = Bitmap.createScaledBitmap(spriteFlechaDer,     anchoBoton, altoBoton, true);
        spriteBotonDisparar = Bitmap.createScaledBitmap(spriteBotonDisparar, anchoBoton, altoBoton, true);

        // Disparo: 5% del ancho de pantalla
        anchoDisparo = pantallaAncho / 20;
        altoDisparo  = (int)(spriteDisparoNave.getHeight() * ((float) anchoDisparo / spriteDisparoNave.getWidth()));
        spriteDisparoNave = Bitmap.createScaledBitmap(spriteDisparoNave, anchoDisparo, altoDisparo, true);

        velocidadDisparo = pantallaAlto / 30f;

        // Posición inicial de la nave: centrada horizontalmente, en el 80% de la pantalla
        naveX = (pantallaAncho - anchoNave) / 2f;
        naveY = (float)(pantallaAlto / 1.45);

        // Velocidad: cruza la pantalla completa en ~2 segundos
        velocidad = pantallaAncho / 60f;

        // Posiciones de los botones: pegados al borde inferior con un margen
        int margen = 180;
        botonIzqX  = margen;
        botonIzqY  = pantallaAlto - altoBoton - margen;
        botonDerX  = anchoBoton + margen + 100;
        botonDerY  = pantallaAlto - altoBoton - margen;
        botonDispX = pantallaAncho - anchoBoton - margen;
        botonDispY = pantallaAlto - altoBoton - margen;

        // Rectángulos de colisión para detectar qué botón se toca
        rectBotonIzq  = new Rect(botonIzqX,  botonIzqY,  botonIzqX  + anchoBoton, botonIzqY  + altoBoton);
        rectBotonDer  = new Rect(botonDerX,  botonDerY,  botonDerX  + anchoBoton, botonDerY  + altoBoton);
        rectBotonDisp = new Rect(botonDispX, botonDispY, botonDispX + anchoBoton, botonDispY + altoBoton);

        gestorEnemigos = new GestorEnemigos(this);
        spritesListos  = true;
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  TOUCH — detectar qué botón se pulsa y con qué dedo
    // ══════════════════════════════════════════════════════════════════════════

    // Android puede mandar varios dedos a la vez. Guardamos el ID de cada dedo
    // para saber exactamente cuándo se levanta cada uno sin confundirlos.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!spritesListos) return true;

        int accion    = event.getActionMasked();
        int indice    = event.getActionIndex();
        int pointerId = event.getPointerId(indice);
        int x         = (int) event.getX(indice);
        int y         = (int) event.getY(indice);

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
                pulsandoIzquierda = false;
                pulsandoDerecha   = false;
                pulsadoDisparador = false;
                pointerIzq = pointerDer = pointerDisp = -1;
                break;
        }
        return true;
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  LÓGICA — se llama ~30 veces por segundo desde BucleJuego
    // ══════════════════════════════════════════════════════════════════════════

    // Aquí actualizamos todo el estado del juego: mover la nave, crear disparos,
    // comprobar colisiones... Si los controles están bloqueados (abducción),
    // solo seguimos procesando la lógica de enemigos.
    public void actualizar() {
        if(faseTransicion!= FaseTransicion.JUGANDO){
            switch (faseTransicion){
                case NAVE_SUBIENDO:
                    //La nave sube hasta salir por arriba
                    naveY-= velocidadTransicion;
                    if(naveY+altoNave<0){
                        faseTransicion=FaseTransicion.ESPERANDO;
                        contadorTransicion=0;
                    }
                    break;
                case ESPERANDO:
                    //Se esperan 2 segundos mostrando solo el fondo
                    contadorTransicion++;
                    if(contadorTransicion>= FRAMES_ESPERA_ENTRE_NIVELES){
                        nivelActual++;
                        if(nivelActual>MAX_NIVELES)nivelActual=1;
                        gestorEnemigos.configurarNivel(nivelActual);
                        // Recolocamos la nave abajo del todo fuera de pantalla
                        naveX              = (anchoPantalla - spriteNave.getWidth()) / 2f;
                        naveY              = altoPantalla + altoNave;
                        faseTransicion     = FaseTransicion.NAVE_BAJANDO;
                        contadorTransicion = 0;
                    }
                    break;
                case NAVE_BAJANDO:
                    // La nave entra deslizándose desde abajo hasta su posición de juego
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

        // Durante la intro solo contamos frames y reproducimos el sonido una vez
        if (enFaseIntro) {
            if (!introSonidoReproducido && sonidoIntroNivelListo) {
                soundPool.play(idSonidoIntroNivel, 1f, 1f, 1, 0, 1f);
                introSonidoReproducido = true;
            }
            contadorIntro++;
            if (contadorIntro >= DURACION_INTRO) enFaseIntro = false;
            return; // no movemos nada mientras aparece el texto
        }

        if (!controlesBloqueados) {

            // Comprobar si algún disparo enemigo nos ha dado
            for (int i = gestorEnemigos.disparosEnemigos.size() - 1; i >= 0; i--) {
                DisparoEnemigo d = gestorEnemigos.disparosEnemigos.get(i);
                if (d.colisionaConNave(naveX, naveY, anchoNave, altoNave)) {
                    gestorEnemigos.disparosEnemigos.remove(i);
                    recibirDanio();
                    break;
                }
            }

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

            // Mover la nave según el botón pulsado
            if (pulsandoDerecha)   moverNave(1);
            if (pulsandoIzquierda) moverNave(-1);

            // Gestionar el disparo con cadencia limitada
            if (pulsadoDisparador) {
                nuevo_disparo = true;
                if (frames_para_nuevo_disparo == 0) {
                    if (nuevo_disparo) {
                        crearDisparo();
                        nuevo_disparo = false;
                    }
                    frames_para_nuevo_disparo = MAX_FRAMES_ENTRE_DISPARO;
                }
                frames_para_nuevo_disparo--;
            }
        }

        if (gestorEnemigos != null) {
            gestorEnemigos.actualizar();

            // Comprobar si algún disparo nuestro ha dado a un enemigo
            for (int i = lista_disparos.size() - 1; i >= 0; i--) {
                Disparo d = lista_disparos.get(i);
                for (Enemigo e : gestorEnemigos.enemigos) {
                    if (e.estado != Enemigo.Estado.MUERTO && e.colisionaCon(d)) {
                        e.recibirDanio();
                        lista_disparos.remove(i);

                        // Si matamos al verde que tenía nuestra nave capturada, la liberamos
                        if (e.tipo == Enemigo.Tipo.VERDE && e.estado == Enemigo.Estado.MUERTO && e.esCapturador) {
                            if (navesCapturadas > 0) navesCapturadas--;
                            e.esCapturador = false;
                            if (navesCapturadas == 0) controlesBloqueados = false;
                            naveX = (anchoPantalla - spriteNave.getWidth()) / 2f;
                            naveY = (float)(altoPantalla / 1.40);
                        }
                        break;
                    }
                }
            }
        }

        // Mover los disparos hacia arriba y eliminar los que salen de pantalla
        for (int i = lista_disparos.size() - 1; i >= 0; i--) {
            lista_disparos.get(i).actualizarCoordenadas();
            if (lista_disparos.get(i).fueraDePantalla()) lista_disparos.remove(i);
        }

        // Limitar la nave dentro de los bordes de la pantalla
        if (naveX < 0) naveX = 0;
        if (naveX + anchoNave > bucle.maxX) naveX = bucle.maxX - anchoNave;
    }

    // Creamos un nuevo disparo en la posición actual de la nave y reproducimos su sonido
    private void crearDisparo() {
        lista_disparos.add(new Disparo(this, naveX, naveY - 75));
        disparosRealizados++;
        if (sonidoListo) soundPool.play(idSonidoDisparo, 1f, 1f, 1, 0, 1f);
    }

    public float getHudY() {
        return topInset + 50f + 10f;
    }

    // Movemos la nave en la dirección indicada (1 = derecha, -1 = izquierda)
    public void moverNave(int direccion) {
        naveX += direccion * velocidad;
        if (naveX < 0) naveX = 0;
        if (naveX + anchoNave > bucle.maxX) naveX = bucle.maxX - anchoNave;
    }


    public void nivelSuperado(){
        if(faseTransicion!=FaseTransicion.JUGANDO)return;
        lista_disparos.clear();
        gestorEnemigos.disparosEnemigos.clear();
        if (nivelActual >=MAX_NIVELES) {
            lista_disparos.clear();
            gestorEnemigos.disparosEnemigos.clear();

            // Lanzamos la pantalla de resultados pasando todas las estadísticas
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
        faseTransicion = FaseTransicion.NAVE_SUBIENDO;
        contadorTransicion=0;
        velocidadTransicion=altoPantalla/30f;


    }
    // ══════════════════════════════════════════════════════════════════════════
    //  DIBUJO — se llama ~30 veces por segundo desde BucleJuego
    // ══════════════════════════════════════════════════════════════════════════

    // Pintamos todo el estado actual del juego en el canvas.
    // El orden importa: primero el fondo, luego los enemigos, luego la nave,
    // luego los botones y por último el HUD encima de todo.
    public void renderizar(Canvas canvas) {
        // Inicializamos la primera vez que llegamos aquí
        inicializarPosiciones(canvas.getWidth(), canvas.getHeight());

        // Limpiamos la pantalla con negro
        canvas.drawColor(Color.BLACK);

        // Si estamos en la intro del primer nivel
        if (enFaseIntro) {
            Paint paintIntro = new Paint();
            paintIntro.setAntiAlias(true);
            paintIntro.setColor(Color.WHITE);
            paintIntro.setTextSize(anchoPantalla / 10f);
            paintIntro.setTypeface(fuenteMenu);
            paintIntro.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("NIVEL " + nivelActual, anchoPantalla / 2f, altoPantalla / 2f, paintIntro);
            return;
        }

        // Pantalla de estadísticas finales al superar el último nivel
        if (mostrandoEstadisticas) {
            float precision = disparosRealizados > 0
                    ? (disparosAcertados * 100f / disparosRealizados) : 0f;

            Paint p = new Paint();
            p.setAntiAlias(true);
            p.setColor(Color.WHITE);
            p.setTypeface(fuenteMenu);
            p.setTextAlign(Paint.Align.CENTER);
            float cx = anchoPantalla / 2f;

            // Título
            p.setTextSize(anchoPantalla / 9f);
            canvas.drawText("PARTIDA COMPLETADA", cx, altoPantalla * 0.12f, p);

            p.setTextSize(anchoPantalla / 14f);
            float y = altoPantalla * 0.25f;
            float lineH = altoPantalla * 0.09f;

            // Enemigos por tipo con su sprite
            dibujarLineaEnemigo(canvas, p, gestorEnemigos.getSpriteAmarillo(),
                    "x " + enemigosAmarillosMuertos, cx, y);
            y += lineH;
            dibujarLineaEnemigo(canvas, p, gestorEnemigos.getSpriteRojo(),
                    "x " + enemigosRojosMuertos, cx, y);
            y += lineH;
            dibujarLineaEnemigo(canvas, p, gestorEnemigos.getSpriteVerde(),
                    "x " + enemigosVerdesMuertos, cx, y);
            y += lineH * 1.3f;

            // Resto de estadísticas
            canvas.drawText("DISPAROS: " + disparosRealizados, cx, y, p); y += lineH;
            canvas.drawText(String.format("PRECISION: %.1f%%", precision), cx, y, p); y += lineH;
            float hudY = topInset + p.getTextSize() + 10;
            canvas.drawText("SCORE: " + score, 50, hudY, p);
            canvas.drawText("VIDAS: " + vidas, anchoPantalla - 300, hudY, p);
            return;
        }

        // Si estamos en la pausa entre niveles, mostramos el letrero del nuevo nivel
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

        // Dibujamos la nave del jugador
        canvas.drawBitmap(spriteNave, naveX, naveY, null);

        // Dibujamos todos los disparos activos del jugador
        for (Disparo d : lista_disparos) d.Dibujar(canvas, myPaint);

        // Dibujamos enemigos y sus disparos
        if (gestorEnemigos != null) gestorEnemigos.renderizar(canvas, myPaint);

        // Dibujamos los tres botones de control en su posición fija
        canvas.drawBitmap(spriteFlechaIzq,     botonIzqX,  botonIzqY,  null);
        canvas.drawBitmap(spriteFlechaDer,     botonDerX,  botonDerY,  null);
        canvas.drawBitmap(spriteBotonDisparar, botonDispX, botonDispY, null);

        // HUD: score y vidas encima de todo
        Paint paintHUD = new Paint();
        paintHUD.setColor(Color.WHITE);
        paintHUD.setTextSize(50);
        paintHUD.setAntiAlias(true);
        canvas.drawText("SCORE: " + score, 50, 130, paintHUD);
        canvas.drawText("VIDAS: " + vidas, anchoPantalla - 300, 130, paintHUD);
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  ESTADO DEL JUGADOR — daño, game over
    // ══════════════════════════════════════════════════════════════════════════

    // Cuando recibimos daño descontamos una vida.
    public void recibirDanio() {
        vidas--;
        // Si no estamos abducidos, nos aseguramos que los controles estén LIBRES
        if (navesCapturadas == 0) {
            controlesBloqueados = false;
        }
        if (vidas <= 0) {
            Log.d("pruebas", "GAME OVER");
        }
    }

    // Cuando somos abducidos por un verde, bloqueamos controles.
    public void serAbducido() {
        vidas--;
        if (navesCapturadas < 2) navesCapturadas++;
        controlesBloqueados = true;
        if (vidas <= 0) {
            Log.d("pruebas", "GAME OVER");
        }
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  SONIDOS — métodos públicos para que otras clases reproduzcan sonidos
    // ══════════════════════════════════════════════════════════════════════════

    // Cada método comprueba si el sonido ya terminó de cargar antes de reproducirlo
    public void reproducirSonidoEnemigoRojo()  { if (sonidoRojoListo)      soundPool.play(idSonidoEnemigoRojo,      1f, 1f, 1, 0, 1f); }
    public void reproducirSonidoEliminado()    { if (sonidoEliminadoListo) soundPool.play(idSonidoEnemigoEliminado, 1f, 1f, 1, 0, 1f); }
    public void reproducirSonidoAbsorcion()    { if (sonidoAbsorcionListo) soundPool.play(idSonidoAbsorcion,        1f, 1f, 1, 0, 1f); }
    public void reproducirSonidoNivel()        { if (sonidoNivelListo)     soundPool.play(idSonidoNivel,            1f, 1f, 1, 0, 1f); }


    // ══════════════════════════════════════════════════════════════════════════
    //  CICLO DE VIDA DE LA SURFACE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        // La superficie está lista: arrancamos el hilo del bucle de juego
        bucle = new BucleJuego(getHolder(), this);
        setFocusable(true);
        bucle.start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        // Si la pantalla cambia de tamaño o rotación, aquí lo gestionaríamos
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        // La superficie se destruye al cerrar o minimizar la app.
        // Liberamos la memoria del SoundPool y esperamos a que el hilo termine limpiamente.
        Log.d(TAG, "Juego destruido");

        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }

        boolean retry = true;
        while (retry) {
            try {
                bucle.join();
                retry = false;
            } catch (InterruptedException e) { }
        }
    }
    // Dibuja el sprite del enemigo centrado con el texto a su derecha
    private void dibujarLineaEnemigo(Canvas canvas, Paint paint, Bitmap sprite, String texto, float cx, float y) {
        if (sprite != null) {
            float spriteX = cx - sprite.getWidth() / 2f - 80;
            float spriteY = y - sprite.getHeight() / 2f - 20;
            canvas.drawBitmap(sprite, spriteX, spriteY, null);
        }
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(texto, cx + 20, y, paint);
        paint.setTextAlign(Paint.Align.CENTER);
    }
}
