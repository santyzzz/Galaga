package es.riberadeltajo.galaga;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

public class DisparoJefeDragon {

    public float x, y;
    private float vx, vy;
    private boolean teledirigido;
    private int pantAncho, pantAlto;

    // ── Sprite animado para el teledirigido ──
    private Bitmap[] frames;         // frames del spritesheet
    private Bitmap   spriteStatic;   // sprite único para el lineal
    private int      frameActual    = 0;
    private int      contadorFrame  = 0;
    private static final int FRAMES_POR_TICK = 4;
    private static final int NUM_FRAMES_TELE = 2; // el spritesheet tiene 2 frames

    // Velocidades
    // Reemplaza las variables de velocidad y añade estas:
    private static final float VEL_LINEAL       = 2.5f;
    private static final float VEL_TELE_INICIAL = 6f;
    private static final float VEL_TELE_MAX     = 3.8f;
    private static final float ACELERACION      = 0.012f;
    private static final float FRICCION         = 0.992f; // <1 = frena gradualmente al alejarse del objetivo
    private float velActual;

    // Añade estas variables de instancia:
    private float targetX, targetY;   // posición de la nave en el momento del disparo
    private boolean llegado = false;  // true cuando pasa cerca del objetivo
    public DisparoJefeDragon(float x, float y, float anguloOffset, boolean teledirigido,
                             int pantAncho, int pantAlto, Bitmap spriteOSheet) {
        this.x            = x;
        this.y            = y;
        this.teledirigido = teledirigido;
        this.pantAncho    = pantAncho;
        this.pantAlto     = pantAlto;

        if (teledirigido) {
            // Cortar el spritesheet en 2 frames horizontales
            frames = new Bitmap[NUM_FRAMES_TELE];
            int fw = spriteOSheet.getWidth() / NUM_FRAMES_TELE;
            int fh = spriteOSheet.getHeight();
            for (int i = 0; i < NUM_FRAMES_TELE; i++) {
                frames[i] = Bitmap.createBitmap(spriteOSheet, i * fw, 0, fw, fh);
            }
            // Velocidad inicial baja — acelerará en actualizar()
            velActual = pantAlto / (VEL_TELE_INICIAL * BucleJuego.MAX_FPS);
            vx = 0;
            vy = velActual; // empieza bajando recto
            velActual = pantAlto / (VEL_TELE_INICIAL * BucleJuego.MAX_FPS);
            vx = 0;
            vy = velActual;


        } else {
            spriteStatic = spriteOSheet;
            float vel = pantAlto / (VEL_LINEAL * BucleJuego.MAX_FPS);
            vx = (float) Math.sin(anguloOffset) * vel;
            vy = vel;
        }
    }

    public void actualizar(float naveCX, float naveCY) {
        if (teledirigido) {
            float vel = pantAlto / (VEL_TELE_MAX * BucleJuego.MAX_FPS);

            // El objetivo es un punto 150px POR ENCIMA de la nave
            // así el disparo pasa por delante y el jugador puede esquivarlo
            float targetCY = naveCY + 150f;
            float distY    = targetCY - y;

            float umbralCongelar = vel * 20f;

            if (distY > umbralCongelar) {
                // Sigue la X de la nave en tiempo real
                float dx   = naveCX - x;
                float giro = 0.01f;
                vx = vx + (dx - vx) * giro;
                vy = vel;
            } else {
                // Congela y sale recto hacia abajo
                vy = vel;
            }

            contadorFrame++;
            if (contadorFrame >= FRAMES_POR_TICK) {
                contadorFrame = 0;
                frameActual   = (frameActual + 1) % NUM_FRAMES_TELE;
            }
        }
// Limitar x para que no salga por los bordes laterales
        if (x + vx < 0)          vx = 0;
        if (x + vx > pantAncho)  vx = 0;
//        if (x + vx < 0 || x + vx > pantAncho) vx = -vx;
        x += vx;
        y += vy;
    }

    public void dibujar(Canvas canvas) {
        if (teledirigido && frames != null) {
            Bitmap f = frames[frameActual];
            canvas.drawBitmap(f, x - f.getWidth() / 2f, y - f.getHeight() / 2f, null);
        } else if (spriteStatic != null) {
            canvas.drawBitmap(spriteStatic,
                    x - spriteStatic.getWidth()  / 2f,
                    y - spriteStatic.getHeight() / 2f, null);
        }
    }
    public void setTarget(float tx, float ty) {
        this.targetX = tx;
        this.targetY = ty;
    }

    public boolean fueraDePantalla() {
        return y > pantAlto || x < 0 || x > pantAncho || y < -50;
    }

    public boolean colisionaConNave(float naveX, float naveY, int naveAncho, int naveAlto) {
        return x > naveX && x < naveX + naveAncho &&
                y > naveY && y < naveY + naveAlto;
    }
}