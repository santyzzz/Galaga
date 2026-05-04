package es.riberadeltajo.galaga;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class DisparoEnemigo {

    public float x, y;
    private float vx, vy;
    private int pantallaAlto, pantallaAncho;

    public DisparoEnemigo(float x, float y, float angulo, int pantallaAncho, int pantallaAlto) {
        this.x = x;
        this.y = y;
        this.pantallaAncho = pantallaAncho;
        this.pantallaAlto  = pantallaAlto;
        // Velocidad: cruza la pantalla en ~3 segundos
        float velocidad = pantallaAlto / (3f * BucleJuego.MAX_FPS);
        this.vx = (float) Math.sin(angulo) * velocidad;
        this.vy = (float) Math.cos(angulo) * velocidad;
    }

    public void actualizar() {
        x += vx;
        y += vy;
    }

    public void dibujar(Canvas canvas, Paint paint) {
        Paint p = new Paint();
        p.setColor(Color.RED);
        p.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, y, 8, p);
    }

    public boolean fueraDePantalla() {
        return y > pantallaAlto || x < 0 || x > pantallaAncho;
    }

    public boolean colisionaConNave(float naveX, float naveY, int naveAncho, int naveAlto) {
        return x > naveX && x < naveX + naveAncho &&
                y > naveY && y < naveY + naveAlto;
    }
}