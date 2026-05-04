package es.riberadeltajo.galaga;


import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

// BucleJuego extiende Thread: esto significa que se ejecuta en un hilo
// separado al hilo principal de Android, para no bloquear la UI
public class BucleJuego extends Thread{

    //Definimos cuantos fotogramas por segundo queremos renderizar
    public final static int MAX_FPS=30;

    //Si vmaos lentos podemos saltar frames de renderizado para seguir
    public final static int MAX_FRAMES_SALTADOS=5;

    //Cuantos milisegundos deberia durar cada frame (1000ms / 30fps ≈ 33ms)
    private final static int TIEMPO_FRAME=1000/MAX_FPS;

    //Referencia al objeto juego para llamar a sus metodos
    private Juego juego;

    public int iteraciones;
    public long tiempoTotal;
    public boolean JuegoEnEjecucion=true;
    private static final String TAG = Juego.class.getSimpleName(); // Etiqueta para los logs ("Juego")
    private SurfaceHolder surfaceHolder; // Controla el acceso al Canvas de dibujo

    public int maxX, maxY; // Ancho y alto de la pantalla en píxeles

    // Constructor: recibe el SurfaceHolder y el objeto Juego
    BucleJuego(SurfaceHolder sh, Juego s) {
        juego = s;           // Guardamos la referencia al juego
        surfaceHolder = sh;  // Guardamos la referencia al holder

        // Obtenemos las dimensiones reales de la pantalla bloqueando el canvas momentáneamente
        Canvas c = sh.lockCanvas();   // "Bloqueamos" el canvas para tener acceso exclusivo
        maxX = c.getWidth();          // Guardamos el ancho en píxeles
        maxY = c.getHeight();         // Guardamos el alto en píxeles
        sh.unlockCanvasAndPost(c);    // Liberamos el canvas (y lo publicamos para que se muestre)
    }

    public void run(){
        Canvas canvas;
        Log.d(TAG, "Comienza el game loop"); // Mensaje de debug en el Logcat

        long tiempoComienzo;    // Momento (en ms) en que empieza cada iteración del bucle
        long tiempoDiferencia;  // Cuánto tardó en ejecutarse la iteración
        int tiempoDormir;       // Cuánto debe "dormir" el hilo para mantener los 30fps
        int framesASaltar;      // Cuántos frames nos estamos saltando por ir lentos

        tiempoDormir = 0;

        //BUCLE PRINCIPAL DEL JUEGO: se repite mientras el juego este en ejecucion
        while(JuegoEnEjecucion){
            canvas=null;

            try{
                //Bloqueamos el canvas, nadie mas lo puede tocar
                canvas=this.surfaceHolder.lockCanvas();

                synchronized (surfaceHolder){
                    tiempoComienzo=System.currentTimeMillis();
                    framesASaltar=0;

                    // --- LÓGICA DEL JUEGO ---
                    juego.actualizar(); // Actualiza posiciones, colisiones, puntuación, etc.

                    // --- DIBUJADO ---
                    juego.renderizar(canvas); // Dibuja el estado actual del juego en el canvas

                    iteraciones++; // Incrementamos el contador de frames procesados

                    //Calculamos cuanto tardo todo el proceso de este frame
                    tiempoDiferencia=System.currentTimeMillis()-tiempoComienzo;

                    // Calculamos cuánto tiempo "sobra" hasta el próximo frame
                    // Si es positivo: vamos bien. Si es negativo: vamos lentos
                    tiempoDormir = (int)(TIEMPO_FRAME - tiempoDiferencia);

                    // Acumulamos el tiempo total (tiempo real + tiempo durmiendo = 1 frame)
                    tiempoTotal += tiempoDiferencia + tiempoDormir;


                    if(tiempoDormir>0){
                        //Si el tiempo de dormir es mas de 0 el hilo se duerme
                        try{
                            Thread.sleep(tiempoDormir);
                        }catch (InterruptedException e ){}
                    }//if


                    while(tiempoDormir<0 && framesASaltar < MAX_FRAMES_SALTADOS){
                        juego.actualizar();             //Actualizamos la logica si pintar
                        tiempoDormir+=TIEMPO_FRAME;     //Recuperamos el tiempo de un frame
                        framesASaltar++;                //Contamos el frame saltado
                    }//while
                }//synchronized
            }finally {
                //EL bloque finally simepre se ejecuta
                if(canvas!=null){
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
            Log.d(TAG, "Nueva iteración!"); // Debug: confirma que el bucle sigue vivo

        }

    }

}
