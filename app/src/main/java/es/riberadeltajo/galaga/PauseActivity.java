package es.riberadeltajo.galaga;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class PauseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pause);

        findViewById(R.id.btnReanudar).setOnClickListener(v -> finish());

        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            Intent i = new Intent(this, MenuActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        finish(); // pulsar atrás = reanudar
    }
}