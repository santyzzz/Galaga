package es.riberadeltajo.galaga;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ResultadosActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultados);

        int score    = getIntent().getIntExtra("score", 0);
        int vidas    = getIntent().getIntExtra("vidas", 0);
        int disparos = getIntent().getIntExtra("disparos", 0);
        int acertados = getIntent().getIntExtra("acertados", 0);
        int amarillos = getIntent().getIntExtra("amarillos", 0);
        int rojos     = getIntent().getIntExtra("rojos", 0);
        int verdes    = getIntent().getIntExtra("verdes", 0);

        float precision = disparos > 0 ? (acertados * 100f / disparos) : 0f;

        // Recortamos el primer frame de cada spritesheet (8 frames en horizontal)
        ImageView imgAmarillo = findViewById(R.id.imgAmarillo);
        ImageView imgRojo     = findViewById(R.id.imgRojo);
        ImageView imgVerde    = findViewById(R.id.imgVerde);

        imgAmarillo.setImageBitmap(primerFrame(R.drawable.enemigo_amarillo));
        imgRojo.setImageBitmap(primerFrame(R.drawable.enemigo_rojo));
        imgVerde.setImageBitmap(primerFrame(R.drawable.enemigo_verde_2hp));

        ((TextView) findViewById(R.id.txtScore)).setText("SCORE: " + score);
        ((TextView) findViewById(R.id.txtVidas)).setText("VIDAS RESTANTES: " + vidas);
        ((TextView) findViewById(R.id.txtDisparos)).setText("DISPAROS: " + disparos);
        ((TextView) findViewById(R.id.txtPrecision)).setText(String.format("PRECISIÓN: %.1f%%", precision));
        ((TextView) findViewById(R.id.txtAmarillos)).setText("x " + amarillos);
        ((TextView) findViewById(R.id.txtRojos)).setText("x " + rojos);
        ((TextView) findViewById(R.id.txtVerdes)).setText("x " + verdes);

        Button btnVolver = findViewById(R.id.btnVolverMenu);
        btnVolver.setOnClickListener(v -> {
            Intent intent = new Intent(this, MenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    // Recorta el primer frame de un spritesheet de 8 columnas
    private android.graphics.Bitmap primerFrame(int drawableId) {
        android.graphics.Bitmap sheet = android.graphics.BitmapFactory.decodeResource(getResources(), drawableId);
        int frameW = sheet.getWidth() / 8;
        int margen = 3;
        return android.graphics.Bitmap.createBitmap(sheet,
                margen, margen,
                frameW - margen * 2,
                sheet.getHeight() - margen * 2);
    }}