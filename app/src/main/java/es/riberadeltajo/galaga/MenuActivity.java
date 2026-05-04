package es.riberadeltajo.galaga;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {
    //de primeras el volumen esta activo
    private boolean volumenActivo=true;
    private MediaPlayer mediaPlayer;
private SoundPool soundPool;
private int idSonidoBotonJugar;
private boolean sonidoBotonJugarListo=false;

    @Override
    protected void onCreate(Bundle savedInstanceBundle){
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_menu);

        //Inicializo el sound pool
        AudioAttributes attrs=new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool=new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(attrs)
                .build();

        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0) sonidoBotonJugarListo = true;
        });

        idSonidoBotonJugar= soundPool.load(this, R.raw.sonido_disparo_jugador, 1);


        //Iniciar musica
        mediaPlayer=MediaPlayer.create(this,R.raw.soundtrack01);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        Button botonJugar=findViewById(R.id.botonJugar);
        Button botonCreditos=findViewById(R.id.botonCreditos);
        ImageButton botonVolumen=findViewById(R.id.botonVolumen);

        //Cuando se pulsa el boton de jugar
        botonJugar.setOnClickListener(view -> {
            if(sonidoBotonJugarListo){
                soundPool.play(idSonidoBotonJugar,1f,1f,1,0,1f);
            }

            Intent intent =new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        //Cuando se pulsa el boton de los creditos
        botonCreditos.setOnClickListener(view -> {
            Intent intent =new Intent(this, CreditosActivity.class);
            startActivity(intent);
        });

        //Logica para el boton del volumen
        botonVolumen.setOnClickListener(view -> {
            volumenActivo=!volumenActivo;
            if(volumenActivo){
                botonVolumen.setImageResource(R.drawable.volumen_activo);
                mediaPlayer.setVolume(1f, 1f);
            }else{
                botonVolumen.setImageResource(R.drawable.volumen_muteado);
        //Media player esta muteado
                mediaPlayer.setVolume(0f, 0f);
            }
        });

    }

@Override
    protected void onPause(){
    super.onPause();
    if(mediaPlayer!=null && volumenActivo)mediaPlayer.pause();
}
@Override
    protected void onResume(){
        super.onResume();;
        if(mediaPlayer!=null && volumenActivo)mediaPlayer.start();
}
@Override
    protected void onDestroy(){
        super.onDestroy();
        if(mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer=null;
        }
    if (soundPool != null) {
        soundPool.release();
        soundPool = null;
    }
}
}
