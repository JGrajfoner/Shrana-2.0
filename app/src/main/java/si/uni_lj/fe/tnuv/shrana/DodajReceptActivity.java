package si.uni_lj.fe.tnuv.shrana;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class DodajReceptActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dodaj_recept);

        EditText vnosNaslov = findViewById(R.id.vnosNaslov);
        EditText vnosCas = findViewById(R.id.vnosCas);
        EditText vnosOpis = findViewById(R.id.vnosOpis);
        Button gumbShrani = findViewById(R.id.gumbShrani);
        Button gumbPreklici = findViewById(R.id.gumbPreklici);
        // PREKLIČI: zapremo obrazec brez shranjevanja
        gumbPreklici.setOnClickListener(v -> finish());

        // SHRANI: preberemo vnose in jih pošljemo nazaj domači strani
        gumbShrani.setOnClickListener(v -> {
            String naslov = vnosNaslov.getText().toString().trim();
            String cas = vnosCas.getText().toString().trim();
            String opis = vnosOpis.getText().toString().trim();
            // PREKLIČI: zapremo obrazec brez shranjevanja


            // Preprosta preverba: naslov ne sme biti prazen
            if (naslov.isEmpty()) {
                Toast.makeText(this, "Vnesi vsaj naslov recepta", Toast.LENGTH_SHORT).show();
                return;
            }

            // Sestavine in korake razbijemo po vrsticah v seznama
            EditText vnosSestavine = findViewById(R.id.vnosSestavine);
            EditText vnosPriprava = findViewById(R.id.vnosPriprava);

            List<String> sestavine = vNizPoVrsticah(vnosSestavine.getText().toString());
            List<String> koraki = vNizPoVrsticah(vnosPriprava.getText().toString());

            // Sestavimo cel recept in ga pošljemo nazaj
            Recept novi = new Recept(naslov, cas, opis, sestavine, koraki);

            Intent rezultat = new Intent();
            rezultat.putExtra("recept", novi);
            setResult(RESULT_OK, rezultat);
            finish();
        });
    }

    // Besedilo z več vrsticami razbije v seznam; prazne vrstice preskoči
    private List<String> vNizPoVrsticah(String besedilo) {
        List<String> seznam = new ArrayList<>();
        for (String vrstica : besedilo.split("\n")) {
            String t = vrstica.trim();
            if (!t.isEmpty()) {
                seznam.add(t);
            }
        }
        return seznam;
    }
}