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

        // Botón atrás = pausar y abrir PauseActivity
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (juegoView != null) {
                    juegoView.pausado = true;
                    startActivityForResult(
                            new Intent(MainActivity.this, PauseActivity.class), 1);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == PauseActivity.RESULT_MENU) {
                // El jugador eligió volver al menú: paramos el bucle aquí
                if (juegoView != null && juegoView.bucle != null) {
                    juegoView.bucle.JuegoEnEjecucion = false;
                }
                Intent i = new Intent(this, MenuActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
            } else {
                // Reanudar: quitamos la pausa y el bucle retoma solo
                if (juegoView != null) juegoView.pausado = false;
            }
        }
    }
}