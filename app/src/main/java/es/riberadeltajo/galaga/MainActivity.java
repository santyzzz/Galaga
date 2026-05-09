package es.riberadeltajo.galaga;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public Juego juegoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        juegoView = new Juego(this);
        setContentView(juegoView);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Botón atrás = pausar y abrir PauseActivity
                if (juegoView != null) {
                    juegoView.pausado = true;
                    startActivityForResult(new Intent(MainActivity.this, PauseActivity.class), 1);
                }
            }
        });
    }

    /**
     * Se llama cuando PauseActivity cierra (el jugador pulsó Reanudar).
     * Simplemente quitamos el flag de pausa para que el bucle retome.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && juegoView != null) {
            juegoView.pausado = false;
        }
    }
}