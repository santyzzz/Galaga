package es.riberadeltajo.galaga;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.util.Log;

public class Disparo {
    public float coordenadaX, coordenadaY;
    private Juego juego;
    private float velocidad;
    private MediaPlayer mediaPlayer;
    private final float MAX_SEGUNDOS_EN_CRUZAR_PANTALLA=3;

    public Disparo(Juego j, float x, float y) {
        juego =j;
        coordenadaX = x + (j.spriteNave.getWidth() / 2f) - (j.spriteDisparoNave.getWidth() / 2f);
        coordenadaY=y;
        velocidad = j.bucle.maxY/MAX_SEGUNDOS_EN_CRUZAR_PANTALLA/BucleJuego.MAX_FPS; //adaptar la velocidad al alto de la pantalla
    }

    public void actualizarCoordenadas(){coordenadaY-=velocidad;}

    public void Dibujar(Canvas c, Paint p){
        c.drawBitmap(juego.spriteDisparoNave,coordenadaX,coordenadaY,p);
    }


    public int ancho(){return juego.spriteDisparoNave.getWidth();}
    public int alto(){return juego.spriteDisparoNave.getHeight();}

    public boolean fueraDePantalla(){return coordenadaY<0;}
}
