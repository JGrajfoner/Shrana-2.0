package si.uni_lj.fe.tnuv.shrana;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class PodrobnostiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_podrobnosti);

        // Gumb nazaj zapre ta zaslon in se vrne na seznam receptov
        findViewById(R.id.gumbNazaj).setOnClickListener(v -> finish());

        // Prevzamemo recept, ki nam ga je poslala domača stran
        Recept recept = (Recept) getIntent().getSerializableExtra("recept");

        TextView naslov = findViewById(R.id.naslov);
        TextView opis = findViewById(R.id.opis);
        TextView cas = findViewById(R.id.cas);
        TextView sestavine = findViewById(R.id.sestavine);
        TextView priprava = findViewById(R.id.priprava);

        if (recept != null) {
            naslov.setText(recept.naslov);
            opis.setText(recept.opis);
            cas.setText("🕐 " + recept.cas);
            sestavine.setText(seznamVBesedilo(recept.sestavine));
            priprava.setText(oštevilčeniKoraki(recept.koraki));
        }

        findViewById(R.id.gumbNaSeznam).setOnClickListener(v -> {
            if (recept != null && recept.sestavine != null) {
                RepozitorijSeznama.nalozi(this);
                List<PostavkaSeznama> seznam = RepozitorijSeznama.getPostavke();
                for (String sestavina : recept.sestavine) {
                    seznam.add(new PostavkaSeznama(sestavina));
                }
                RepozitorijSeznama.shrani(this);
                Toast.makeText(this, "Dodano na nakupovalni seznam",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Klik na puščico nazaj zapre ta zaslon in se vrne na prejšnjega
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // Sestavine izpišemo vsako v svojo vrstico z vodilno piko
    private String seznamVBesedilo(List<String> seznam) {
        if (seznam == null || seznam.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (String vrstica : seznam) {
            sb.append("• ").append(vrstica).append("\n");
        }
        return sb.toString().trim();
    }

    // Korake priprave oštevilčimo: 1. ... 2. ...
    private String oštevilčeniKoraki(List<String> seznam) {
        if (seznam == null || seznam.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seznam.size(); i++) {
            sb.append(i + 1).append(". ").append(seznam.get(i)).append("\n");
        }
        return sb.toString().trim();
    }
}