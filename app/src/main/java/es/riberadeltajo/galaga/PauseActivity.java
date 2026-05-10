package es.riberadeltajo.galaga;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class PauseActivity extends AppCompatActivity {

    public static final int RESULT_REANUDAR = 1;
    public static final int RESULT_MENU     = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pause);

        // Reanudar: devuelve código 1 a MainActivity y cierra
        findViewById(R.id.btnReanudar).setOnClickListener(v -> {
            setResult(RESULT_REANUDAR);
            finish();
        });

        // Menú: devuelve código 2 a MainActivity, que se encarga de parar el bucle
        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            setResult(RESULT_MENU);
            finish();
        });
    }

    // Botón atrás del sistema = reanudar
    @Override
    public void onBackPressed() {
        setResult(RESULT_REANUDAR);
        finish();
    }
}