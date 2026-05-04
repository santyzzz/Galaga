package es.riberadeltajo.galaga;

import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Juego juegoView; // variable para guardar la referencia al juego

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        juegoView = new Juego(this); // guardamos la referencia
        setContentView(juegoView);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (juegoView != null && juegoView.bucle != null) {
                    juegoView.bucle.JuegoEnEjecucion = false;
                }
                finish(); // cierra el juego y vuelve al menú
            }
        });
    }
}