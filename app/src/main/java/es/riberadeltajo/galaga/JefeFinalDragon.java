package es.riberadeltajo.galaga;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.util.ArrayList;

public class JefeFinalDragon {

    // ══════════════════════════════════════════════════════════════
    // CONFIGURACIÓN — ajusta estos valores a tu gusto
    // ══════════════════════════════════════════════════════════════
    private static final int VIDA_FASE_VERDE   = 10;
    private static final int VIDA_FASE_NARANJA = 10;

    // Frames de la animación de vuelo
    private static final int NUM_FRAMES_VUELO = 4;
    private static final int FRAMES_POR_TICK  = 4; // Velocidad animación

    // Probabilidades de ataque (deben sumar 100)
    private static final int PROB_LINEAL       = 60;
    private static final int PROB_TELEDIRIGIDO = 30;
    private static final int PROB_MELEE        = 10;

    private static final int FRAMES_ENTRE_ATAQUES      = 90; // ~3 seg a 30fps
    private static final int FRAMES_MOSTRAR_TRANSICION = 45; // 1.5 seg a 30fps
    private static final int FRAMES_DURACION_DISPARO   = 30; // Tiempo que mantiene el sprite de disparo


    // ══════════════════════════════════════════════════════════════
    // ESTADO
    // ══════════════════════════════════════════════════════════════
    public enum Fase { VERDE, NARANJA, MUERTO }
    public enum EstadoAtaque { IDLE, DISPARANDO, MELEE_ACERCANDO, MELEE_VOLVIENDO }

    public Fase fase = Fase.VERDE;
    public EstadoAtaque estadoAtaque = EstadoAtaque.IDLE;
    public boolean activo = false;

    private int vidaFaseActual;
    private int contadorAtaque    = 0;
    private int contadorFrame     = 0;
    private int frameActual       = 0;
    private int contadorFramesDisparo = 0;

    // Transición entre fases
    private boolean  mostrandoTransicion  = false;
    private int      contadorTransicion   = 0;
    private Bitmap   frameTransicionActual = null;
    private Runnable accionPostTransicion  = null;


    // ══════════════════════════════════════════════════════════════
    // POSICIÓN Y MOVIMIENTO
    // ══════════════════════════════════════════════════════════════
    public float x, y;
    public int   ancho, alto;

    private float vx, vy;
    private float meleeTargetX, meleeTargetY;
    private float origenX, origenY;
    private int   pantAncho, pantAlto;
    private float barraVidaY=0;


    // ══════════════════════════════════════════════════════════════
    // SPRITES
    // ══════════════════════════════════════════════════════════════
    private Bitmap[] framesVerde;
    private Bitmap[] framesNaranja;
    private Bitmap   spriteDisparoVerde, spriteDePieVerde;
    private Bitmap   spriteDisparoNaranja, spriteDePieNaranja;


    // ══════════════════════════════════════════════════════════════
    // PROYECTILES
    // ══════════════════════════════════════════════════════════════
    public ArrayList<DisparoJefeDragon> disparos = new ArrayList<>();
    private Bitmap spriteLineal;
    private Bitmap spriteTeledirigido;


    // ══════════════════════════════════════════════════════════════
    // EXPLOSIÓN AL MORIR
    // ══════════════════════════════════════════════════════════════
    public boolean explotando = false;
    private int     contadorExplosion = 0;
    private static final int FRAMES_EXPLOSION = 60;
    private float[] explosionX = new float[8];
    private float[] explosionY = new float[8];

    private Juego juego;


    // ══════════════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════════════

    public JefeFinalDragon(Juego juego) {
        this.juego     = juego;
        this.pantAncho = juego.anchoPantalla;
        this.pantAlto  = juego.altoPantalla;
        cargarSprites();
        vidaFaseActual = VIDA_FASE_VERDE;

        // Posición inicial centrada en la parte superior
        x = (pantAncho - ancho) / 2f;
        y = pantAlto * 0.08f;

        // Velocidad inicial aleatoria
        float vel = pantAncho / 180f;
        vx = vel * (Math.random() > 0.5 ? 1 : -1);
        vy = vel * 0.4f * (Math.random() > 0.5 ? 1 : -1);
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  CARGA DE SPRITES
    // ══════════════════════════════════════════════════════════════════════════

    private void cargarSprites() {
        // Ancho base del dragón
        int targetW = pantAncho / 3;

        // Calculamos un ALTO base usando el primer frame para que todos midan lo mismo
        Bitmap base = BitmapFactory.decodeResource(juego.getResources(), R.drawable.dragon_verde_volando_1);
        int targetH = (int)(base.getHeight() * ((float) targetW / base.getWidth()));

        // Guardamos las dimensiones oficiales
        this.ancho = targetW;
        this.alto  = targetH;

        // Carga de sprites fase VERDE (todos forzados al mismo tamaño)
        framesVerde = new Bitmap[NUM_FRAMES_VUELO];
        framesVerde[0] = cargarYEscalarExacto(R.drawable.dragon_verde_volando_1, targetW, targetH);
        framesVerde[1] = cargarYEscalarExacto(R.drawable.dragon_verde_volando_2, targetW, targetH);
        framesVerde[2] = cargarYEscalarExacto(R.drawable.dragon_verde_volando_3, targetW, targetH);
        framesVerde[3] = cargarYEscalarExacto(R.drawable.dragon_verde_volando_4, targetW, targetH);
        spriteDisparoVerde = cargarYEscalarExacto(R.drawable.dragon_verde_disparo, targetW, targetH);
        spriteDePieVerde   = cargarYEscalarExacto(R.drawable.dragon_verde_de_pie, targetW, targetH);

        // Carga de sprites fase NARANJA (mismo tamaño)
        framesNaranja = new Bitmap[NUM_FRAMES_VUELO];
        framesNaranja[0] = cargarYEscalarExacto(R.drawable.dragon_naranja_volando_1, targetW, targetH);
        framesNaranja[1] = cargarYEscalarExacto(R.drawable.dragon_naranja_volando_2, targetW, targetH);
        framesNaranja[2] = cargarYEscalarExacto(R.drawable.dragon_naranja_volando_3, targetW, targetH);
        framesNaranja[3] = cargarYEscalarExacto(R.drawable.dragon_naranja_volando_4, targetW, targetH);
        spriteDisparoNaranja = cargarYEscalarExacto(R.drawable.dragon_naranja_disparando, targetW, targetH);
        spriteDePieNaranja   = cargarYEscalarExacto(R.drawable.dragon_naranja_de_pie, targetW, targetH);

        // Proyectiles (estos sí mantienen su proporción original)
        int proyW = pantAncho / 18;
        spriteLineal       = cargarYEscalarProporcional(R.drawable.disparo_lineal_jefe, proyW);
        spriteTeledirigido = cargarYEscalarProporcional(R.drawable.disparo_teledirigido_jefe, proyW);
    }

    private Bitmap cargarYEscalarExacto(int resId, int targetW, int targetH) {
        Bitmap raw = BitmapFactory.decodeResource(juego.getResources(), resId);
        if (raw == null) return null;
        return Bitmap.createScaledBitmap(raw, targetW, targetH, true);
    }

    private Bitmap cargarYEscalarProporcional(int resId, int targetW) {
        Bitmap raw = BitmapFactory.decodeResource(juego.getResources(), resId);
        if (raw == null) return null;
        int targetH = (int)(raw.getHeight() * ((float) targetW / raw.getWidth()));
        return Bitmap.createScaledBitmap(raw, targetW, targetH, true);
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  LÓGICA PRINCIPAL
    // ══════════════════════════════════════════════════════════════════════════

    public void actualizar() {
        if (!activo || fase == Fase.MUERTO) return;

        // Animación de explosión final
        if (explotando) {
            contadorExplosion++;
            if (contadorExplosion >= FRAMES_EXPLOSION) {
                explotando = false;
                fase       = Fase.MUERTO;
                juego.nivelSuperado();
            }
            return;
        }

        // Frame de transición entre fases: el jefe se congela 1.5 seg
        if (mostrandoTransicion) {
            contadorTransicion++;
            if (contadorTransicion >= FRAMES_MOSTRAR_TRANSICION && accionPostTransicion != null) {
                accionPostTransicion.run();
                accionPostTransicion = null;
            }
            return; // no se mueve ni ataca durante la transición
        }

        avanzarAnimacion();
        moverJefe();

        // Gestionar estado de disparo
        if (estadoAtaque == EstadoAtaque.DISPARANDO) {
            contadorFramesDisparo++;
            if (contadorFramesDisparo >= FRAMES_DURACION_DISPARO) {
                estadoAtaque = EstadoAtaque.IDLE;
                contadorFramesDisparo = 0;
            }
        }

        // Gestionar cadencia de ataques
        contadorAtaque++;
        if (contadorAtaque >= FRAMES_ENTRE_ATAQUES && estadoAtaque == EstadoAtaque.IDLE) {
            contadorAtaque = 0;
            elegirAtaque();
        }

        // Actualizar proyectiles activos
        for (int i = disparos.size() - 1; i >= 0; i--) {
            disparos.get(i).actualizar(
                    juego.naveX + juego.spriteNave.getWidth()  / 2f,
                    juego.naveY + juego.spriteNave.getHeight() / 2f);
            if (disparos.get(i).fueraDePantalla()) disparos.remove(i);
        }
    }

    // Mueve al jefe por la zona superior rebotando en los bordes,
    // o ejecuta el movimiento de ataque melee si corresponde
    private void moverJefe() {
        if (estadoAtaque == EstadoAtaque.MELEE_ACERCANDO) {
            float dx   = meleeTargetX - x;
            float dy   = meleeTargetY - y;
            float dist = (float) Math.sqrt(dx*dx + dy*dy);
            float vel  = pantAlto / (1.5f * 30f);
            if (dist < vel) {
                x = meleeTargetX; y = meleeTargetY;
                estadoAtaque = EstadoAtaque.MELEE_VOLVIENDO;
            } else {
                x += (dx / dist) * vel;
                y += (dy / dist) * vel;
            }
            return;
        }

        if (estadoAtaque == EstadoAtaque.MELEE_VOLVIENDO) {
            float dx   = origenX - x;
            float dy   = origenY - y;
            float dist = (float) Math.sqrt(dx*dx + dy*dy);
            float vel  = pantAlto / (2f * 30f);
            if (dist < vel) {
                x = origenX; y = origenY;
                estadoAtaque = EstadoAtaque.IDLE;
            } else {
                x += (dx / dist) * vel;
                y += (dy / dist) * vel;
            }
            return;
        }

        // Vuelo libre (también mientras dispara)
        x += vx;
        y += vy;
        if (x <= 0)                       { x = 0;                    vx =  Math.abs(vx); }
        if (x + ancho >= pantAncho)       { x = pantAncho - ancho;    vx = -Math.abs(vx); }

        float limiteArriba = juego.getHudY() + 80f; // justo debajo de la barra de vida
        float limiteAbajo  = pantAlto * 0.42f;
        if (y <= limiteArriba)            { y = limiteArriba;          vy =  Math.abs(vy); }
        if (y + alto >= limiteAbajo)      { y = limiteAbajo - alto;    vy = -Math.abs(vy); }
    }

    // Elige el ataque según probabilidades: lineal 60%, teledirigido 30%, melee 10%
    private void elegirAtaque() {
        int rand = (int)(Math.random() * 100);
        if (rand < PROB_LINEAL) {
            lanzarDisparoLineal();
            estadoAtaque = EstadoAtaque.DISPARANDO;
        } else if (rand < PROB_LINEAL + PROB_TELEDIRIGIDO) {
            lanzarDisparoTeledirigido();
            estadoAtaque = EstadoAtaque.DISPARANDO;
        } else {
            iniciarMelee();
        }
    }

    // 3 disparos lineales en abanico hacia abajo
    private void lanzarDisparoLineal() {
        float cx = x + ancho / 2f;
        float cy = y + alto;

        if (fase == Fase.NARANJA) {
            // Fase naranja: 5 proyectiles en abanico más abierto
            disparos.add(new DisparoJefeDragon(cx, cy,  0f,    false, pantAncho, pantAlto, spriteLineal));
            disparos.add(new DisparoJefeDragon(cx, cy, -0.25f, false, pantAncho, pantAlto, spriteLineal));
            disparos.add(new DisparoJefeDragon(cx, cy,  0.25f, false, pantAncho, pantAlto, spriteLineal));
            disparos.add(new DisparoJefeDragon(cx, cy, -0.55f, false, pantAncho, pantAlto, spriteLineal));
            disparos.add(new DisparoJefeDragon(cx, cy,  0.55f, false, pantAncho, pantAlto, spriteLineal));
        } else {
            // Fase verde: 3 proyectiles
            disparos.add(new DisparoJefeDragon(cx, cy,  0f,    false, pantAncho, pantAlto, spriteLineal));
            disparos.add(new DisparoJefeDragon(cx, cy, -0.35f, false, pantAncho, pantAlto, spriteLineal));
            disparos.add(new DisparoJefeDragon(cx, cy,  0.35f, false, pantAncho, pantAlto, spriteLineal));
        }
    }

    // 2 disparos que siguen a la nave del jugador
    private void lanzarDisparoTeledirigido() {
        float cx = x + ancho / 2f;
        float cy = y + alto;
        float naveCX = juego.naveX + juego.spriteNave.getWidth()  / 2f;
        float naveCY = juego.naveY + juego.spriteNave.getHeight() / 2f;

        DisparoJefeDragon d1 = new DisparoJefeDragon(cx,      cy, 0f, true, pantAncho, pantAlto, spriteTeledirigido);
        DisparoJefeDragon d2 = new DisparoJefeDragon(cx - 30, cy, 0f, true, pantAncho, pantAlto, spriteTeledirigido);
        d1.setTarget(naveCX, naveCY);
        d2.setTarget(naveCX, naveCY);
        disparos.add(d1);
        disparos.add(d2);
    }
    // El jefe baja en picado hasta la posición de la nave y luego vuelve
    private void iniciarMelee() {
        origenX      = x;
        origenY      = y;
        meleeTargetX = juego.naveX - ancho / 2f;
        meleeTargetY = juego.naveY - alto  / 2f;
        estadoAtaque = EstadoAtaque.MELEE_ACERCANDO;
    }

    // Animación en bucle continuo
    private void avanzarAnimacion() {
        contadorFrame++;
        if (contadorFrame >= FRAMES_POR_TICK) {
            contadorFrame = 0;
            frameActual   = (frameActual + 1) % NUM_FRAMES_VUELO;
        }
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  DAÑO Y FASES
    // ══════════════════════════════════════════════════════════════════════════

    public void recibirImpacto() {
        if (mostrandoTransicion) return; // inmune durante la transición
        vidaFaseActual--;
        if (vidaFaseActual <= 0) {
            if (fase == Fase.VERDE) {
                frameTransicionActual = spriteDePieVerde; // Usamos sprite de pie para transición
                mostrandoTransicion   = true;
                contadorTransicion    = 0;
                accionPostTransicion  = () -> {
                    fase              = Fase.NARANJA;
                    vidaFaseActual    = VIDA_FASE_NARANJA;
                    frameActual       = 0;
                    mostrandoTransicion = false;
                    estadoAtaque      = EstadoAtaque.IDLE;
                };
            } else if (fase == Fase.NARANJA) {
                frameTransicionActual = spriteDePieNaranja;
                mostrandoTransicion   = true;
                contadorTransicion    = 0;
                accionPostTransicion  = () -> {
                    mostrandoTransicion = false;
                    iniciarExplosion();
                };
            }
        }
    }

    private void iniciarExplosion() {
        explotando        = true;
        contadorExplosion = 0;
        for (int i = 0; i < 8; i++) {
            explosionX[i] = x + (float)(Math.random() * ancho);
            explosionY[i] = y + (float)(Math.random() * alto);
        }
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  COLISIONES
    // ══════════════════════════════════════════════════════════════════════════

    public boolean colisionaCon(Disparo d) {
        // Hitbox reducida: 50% del ancho centrada, 80% del alto desde arriba
        float hitX = x + ancho * 0.25f;
        float hitW = ancho * 0.50f;
        float hitY = y + alto * 0.10f;
        float hitH = alto * 0.80f;

        return d.coordenadaX < hitX + hitW &&
                d.coordenadaX + d.ancho() > hitX &&
                d.coordenadaY < hitY + hitH &&
                d.coordenadaY + d.alto()  > hitY;
    }

    public boolean tocaLaNave() {
        float hitX = x + ancho * 0.25f;
        float hitW = ancho * 0.50f;
        float hitY = y + alto * 0.10f;
        float hitH = alto * 0.80f;

        return estadoAtaque == EstadoAtaque.MELEE_ACERCANDO &&
                hitX < juego.naveX + juego.spriteNave.getWidth()  &&
                hitX + hitW > juego.naveX &&
                hitY < juego.naveY + juego.spriteNave.getHeight() &&
                hitY + hitH > juego.naveY;
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  DIBUJO
    // ══════════════════════════════════════════════════════════════════════════

    public void dibujar(Canvas canvas, Paint paint) {
        if (!activo || fase == Fase.MUERTO) return;

        // Proyectiles por detrás
        for (DisparoJefeDragon d : disparos) d.dibujar(canvas);

        // Explosión final con círculos que crecen desde 0
        if (explotando) {
            Paint pExp = new Paint();
            pExp.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 8; i++) {
                // El radio crece proporcionalmente al contador de explosión
                // Empezando desde 0 y con un desfase para cada círculo
                float radio = Math.max(0, (contadorExplosion * 3.0f) - (i * 7));
                int   alpha = Math.max(0, 255 - contadorExplosion * 4);
                
                // Color naranja/fuego que se desvanece
                pExp.setColor(Color.argb(alpha, 255, 120 + i * 15, 0));
                canvas.drawCircle(explosionX[i], explosionY[i], radio, pExp);
            }
            return;
        }

        // Frame de transición entre fases
        if (mostrandoTransicion && frameTransicionActual != null) {
            canvas.drawBitmap(frameTransicionActual, x, y, paint);
            dibujarBarraVida(canvas);
            return;
        }

        // Selección del sprite según el estado y fase
        Bitmap spriteAMostrar = null;
        if (estadoAtaque == EstadoAtaque.MELEE_ACERCANDO || estadoAtaque == EstadoAtaque.MELEE_VOLVIENDO) {
            spriteAMostrar = (fase == Fase.VERDE) ? spriteDePieVerde : spriteDePieNaranja;
        } else if (estadoAtaque == EstadoAtaque.DISPARANDO) {
            spriteAMostrar = (fase == Fase.VERDE) ? spriteDisparoVerde : spriteDisparoNaranja;
        } else {
            // Vuelo normal
            Bitmap[] frames = (fase == Fase.VERDE) ? framesVerde : framesNaranja;
            if (frames != null && frameActual < frames.length) {
                spriteAMostrar = frames[frameActual];
            }
        }

        if (spriteAMostrar != null) {
            canvas.drawBitmap(spriteAMostrar, x, y, paint);
        }

        dibujarBarraVida(canvas);
    }

    private void dibujarBarraVida(Canvas canvas) {
        int   maxVida = (fase == Fase.VERDE) ? VIDA_FASE_VERDE : VIDA_FASE_NARANJA;
        float pct     = (float) vidaFaseActual / maxVida;
        float barW    = pantAncho * 0.6f;
        float barX    = (pantAncho - barW) / 2f;
        float barY = juego.getHudY() + 60f;
        ;

        Paint p = new Paint();
        p.setColor(Color.DKGRAY);
        canvas.drawRect(barX, barY, barX + barW, barY + 25, p);

        p.setColor(fase == Fase.VERDE ? Color.GREEN : Color.rgb(255, 100, 0));
        canvas.drawRect(barX, barY, barX + barW * pct, barY + 25, p);

        p.setStyle(Paint.Style.STROKE);
        p.setColor(Color.WHITE);
        canvas.drawRect(barX, barY, barX + barW, barY + 25, p);

        p.setStyle(Paint.Style.FILL);
        p.setTextSize(24);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("JEFE FINAL", pantAncho / 2f, barY - 5, p);
    }
}
