package si.uni_lj.fe.tnuv.shrana;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class PodrobnostiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_podrobnosti);

        findViewById(R.id.gumbNazaj).setOnClickListener(v -> finish());

        Recept recept = (Recept) getIntent().getSerializableExtra("recept");

        ImageView slika = findViewById(R.id.slika);
        TextView naslov = findViewById(R.id.naslov);
        TextView opis = findViewById(R.id.opis);
        TextView casPriprave = findViewById(R.id.casPriprave);
        TextView casKuhanja = findViewById(R.id.casKuhanja);
        TextView kalorije = findViewById(R.id.kalorijePrikaz);
        TextView sestavine = findViewById(R.id.sestavine);
        TextView priprava = findViewById(R.id.priprava);
        ChipGroup chipGroupOznake = findViewById(R.id.chipGroupOznakePrikaz);

        if (recept != null) {
            if (recept.slikaUri != null) {
                slika.setImageURI(Uri.parse(recept.slikaUri));
            } else {
                slika.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            naslov.setText(recept.naslov);
            opis.setText(recept.opis);
            
            casPriprave.setText("🥣 Priprava: " + formatirajCas(recept.casPriprave));
            casKuhanja.setText("🔥 Kuhanje: " + formatirajCas(recept.casKuhanja));
            kalorije.setText("⚡ " + recept.kalorije + " kcal");
            
            sestavine.setText(seznamSestavinVBesedilo(recept.sestavine));
            priprava.setText(recept.priprava);

            if (recept.oznake != null) {
                for (String oznaka : recept.oznake) {
                    Chip chip = new Chip(this);
                    chip.setText(oznaka);
                    chipGroupOznake.addView(chip);
                }
            }
        }

        findViewById(R.id.gumbNaSeznam).setOnClickListener(v -> {
            if (recept != null && recept.sestavine != null) {
                RepozitorijSeznama.nalozi(this);
                List<PostavkaSeznama> seznam = RepozitorijSeznama.getPostavke();
                for (Recept.Sestavina s : recept.sestavine) {
                    seznam.add(new PostavkaSeznama(s.toString()));
                }
                RepozitorijSeznama.shrani(this);
                Toast.makeText(this, "Dodano na nakupovalni seznam",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatirajCas(int minute) {
        int h = minute / 60;
        int m = minute % 60;
        if (h > 0) return h + " h " + m + " min";
        return m + " min";
    }

    private String seznamSestavinVBesedilo(List<Recept.Sestavina> seznam) {
        if (seznam == null || seznam.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (Recept.Sestavina s : seznam) {
            sb.append("• ").append(s.toString()).append("\n");
        }
        return sb.toString().trim();
    }
}