package si.uni_lj.fe.tnuv.shrana;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class PodrobnostiActivity extends AppCompatActivity {

    private Recept recept;
    private ImageView slika;
    private TextView naslov, opis, casPriprave, casKuhanja, kalorije, sestavine, priprava;
    private ChipGroup chipGroupOznake;

    private final ActivityResultLauncher<Intent> urediLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), rezultat -> {
                if (rezultat.getResultCode() == RESULT_OK && rezultat.getData() != null) {
                    Recept posodobljen = (Recept) rezultat.getData().getSerializableExtra("recept");
                    if (posodobljen != null) {
                        posodobiVRepozitoriju(posodobljen);
                        this.recept = posodobljen;
                        prikaziPodatke();
                        RepozitorijReceptov.shrani(this);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_podrobnosti);

        recept = (Recept) getIntent().getSerializableExtra("recept");

        slika = findViewById(R.id.slika);
        naslov = findViewById(R.id.naslov);
        opis = findViewById(R.id.opis);
        casPriprave = findViewById(R.id.casPriprave);
        casKuhanja = findViewById(R.id.casKuhanja);
        kalorije = findViewById(R.id.kalorijePrikaz);
        sestavine = findViewById(R.id.sestavine);
        priprava = findViewById(R.id.priprava);
        chipGroupOznake = findViewById(R.id.chipGroupOznakePrikaz);

        findViewById(R.id.gumbNazaj).setOnClickListener(v -> finish());
        findViewById(R.id.gumbUredi).setOnClickListener(v -> {
            Intent i = new Intent(this, UrejanjeReceptaActivity.class);
            i.putExtra("recept", recept);
            urediLauncher.launch(i);
        });

        prikaziPodatke();

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

    private void prikaziPodatke() {
        if (recept == null) return;

        if (recept.slikaUri != null && !recept.slikaUri.isEmpty()) {
            slika.setImageURI(Uri.parse(recept.slikaUri));
        } else {
            slika.setImageResource(R.drawable.ic_image);
        }

        naslov.setText(recept.naslov);
        opis.setText(recept.opis);
        // Odstranjeni emojiji, saj so zdaj ikonice v XML-ju
        casPriprave.setText("Priprava: " + formatirajCas(recept.casPriprave));
        casKuhanja.setText("Kuhanje: " + formatirajCas(recept.casKuhanja));
        kalorije.setText(recept.kalorije + " kcal");
        sestavine.setText(seznamSestavinVBesedilo(recept.sestavine));
        priprava.setText(recept.priprava);

        chipGroupOznake.removeAllViews();
        if (recept.oznake != null) {
            for (String oznaka : recept.oznake) {
                Chip chip = new Chip(this);
                chip.setText(oznaka);
                chipGroupOznake.addView(chip);
            }
        }
    }

    private void posodobiVRepozitoriju(Recept novi) {
        List<Recept> vsi = RepozitorijReceptov.getRecepti();
        for (int i = 0; i < vsi.size(); i++) {
            Recept r = vsi.get(i);
            if (r.naslov.equals(recept.naslov) && (r.opis != null && r.opis.equals(recept.opis))) {
                vsi.set(i, novi);
                break;
            }
        }
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
