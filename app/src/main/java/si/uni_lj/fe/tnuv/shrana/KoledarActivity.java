package si.uni_lj.fe.tnuv.shrana;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class KoledarActivity extends AppCompatActivity {

    // Trije obroki, ki se pokažejo za vsak dan
    private final String[] OBROKI = {"Zajtrk", "Kosilo", "Večerja"};

    // Shramba načrtovanih obrokov. Ključ = "datum|obrok", vrednost = ime recepta.
    private Map<String, String> nacrt = new HashMap<>();

    // Trenutno izbrani datum (npr. "16.4.2026")
    private String trenutniDatum;

    private LinearLayout vsebinaObrokov;

    private static final String IME_DATOTEKE = "shramba_koledar";
    private static final String KLJUC = "nacrt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_koledar);

        naloziNacrt();

        CalendarView koledar = findViewById(R.id.koledar);
        TextView izbraniDatum = findViewById(R.id.izbraniDatum);
        vsebinaObrokov = findViewById(R.id.vsebinaObrokov);

        // Ob kliku na dan zapomnimo datum in osvežimo tri vrstice obrokov
        koledar.setOnDateChangeListener((view, leto, mesec, dan) -> {
            trenutniDatum = dan + "." + (mesec + 1) + "." + leto;
            izbraniDatum.setText("Obroki za " + trenutniDatum);
            prikaziObroke();
        });

        nastaviNavigacijo();
    }

    // Zgradi tri vrstice (Zajtrk, Kosilo, Večerja) za trenutni datum
    private void prikaziObroke() {
        vsebinaObrokov.removeAllViews(); // počistimo prejšnje vrstice

        for (String obrok : OBROKI) {
            // Ustvarimo vrstico iz postavitve item_obrok.xml
            View vrstica = LayoutInflater.from(this)
                    .inflate(R.layout.item_obrok, vsebinaObrokov, false);

            TextView imeObroka = vrstica.findViewById(R.id.imeObroka);
            TextView imeRecepta = vrstica.findViewById(R.id.imeRecepta);
            ImageButton gumbDodaj = vrstica.findViewById(R.id.gumbDodaj);
            ImageButton gumbOdstrani = vrstica.findViewById(R.id.gumbOdstrani);

            imeObroka.setText(obrok);

            String kljuc = trenutniDatum + "|" + obrok;
            String recept = nacrt.get(kljuc);

            if (recept != null) {
                // Obrok je določen: pokažemo ime in gumb ×, skrijemo +
                imeRecepta.setText(recept);
                imeRecepta.setVisibility(View.VISIBLE);
                gumbOdstrani.setVisibility(View.VISIBLE);
                gumbDodaj.setVisibility(View.GONE);

                // Klik na × odstrani recept za ta obrok
                gumbOdstrani.setOnClickListener(v -> {
                    nacrt.remove(kljuc);
                    shraniNacrt();
                    prikaziObroke();
                });
            } else {
                // Obrok ni določen: pokažemo +, skrijemo ime in ×
                imeRecepta.setVisibility(View.GONE);
                gumbOdstrani.setVisibility(View.GONE);
                gumbDodaj.setVisibility(View.VISIBLE);
                gumbDodaj.setOnClickListener(v -> izberiRecept(obrok));
            }

            vsebinaObrokov.addView(vrstica);
        }
    }

    // Odpre seznam receptov za izbiro pod določen obrok
    // Odpre seznam vnešenih receptov za izbiro pod določen obrok
    private void izberiRecept(String obrok) {
        List<Recept> recepti = RepozitorijReceptov.getRecepti();

        // Če ni nobenega recepta, uporabnika opozorimo
        if (recepti.isEmpty()) {
            Toast.makeText(this,
                    "Najprej dodaj recept v zavihku Recepti",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Iz receptov naredimo polje imen za prikaz v meniju
        String[] imena = new String[recepti.size()];
        for (int i = 0; i < recepti.size(); i++) {
            imena[i] = recepti.get(i).naslov;
        }

        new AlertDialog.Builder(this)
                .setTitle(obrok + " - izberi recept")
                .setItems(imena, (dialog, kateri) -> {
                    String izbran = imena[kateri];
                    nacrt.put(trenutniDatum + "|" + obrok, izbran);
                    shraniNacrt();
                    prikaziObroke();
                })
                .setNegativeButton("Prekliči", null)
                .show();
    }

    private void nastaviNavigacijo() {
        BottomNavigationView navigacija = findViewById(R.id.spodnjaNavigacija);
        navigacija.setSelectedItemId(R.id.nav_koledar);
        navigacija.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_koledar) {
                return true; // smo že tukaj
            } else if (id == R.id.nav_recepti) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_seznam) {
                startActivity(new Intent(this, SeznamActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_casovnik) {
                startActivity(new Intent(this, CasovnikiActivity.class));
                finish();
                return true;
            }
            Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    // Prebere načrt obrokov z diska
    private void naloziNacrt() {
        Type tip = new TypeToken<HashMap<String, String>>() {}.getType();
        Map<String, String> shranjen =
                JsonShramba.nalozi(this, IME_DATOTEKE, KLJUC, tip);
        if (shranjen != null) {
            nacrt = shranjen;
        }
    }

    // Zapiše načrt obrokov na disk kot JSON
    private void shraniNacrt() {
        JsonShramba.shrani(this, IME_DATOTEKE, KLJUC, nacrt);
    }
}