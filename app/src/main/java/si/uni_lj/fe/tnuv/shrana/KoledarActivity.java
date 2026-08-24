package si.uni_lj.fe.tnuv.shrana;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.GridLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class KoledarActivity extends AppCompatActivity {

    // Trije obroki, ki se pokažejo za vsak dan
    private String[] OBROKI;

    // Shramba načrtovanih obrokov. Ključ = "datum|obrok", vrednost = ime recepta.
    private Map<String, String> nacrt = new HashMap<>();

    // Trenutno izbrani datum (npr. "16.4.2026")
    private String trenutniDatum;

    private LinearLayout vsebinaObrokov;
    private TextView izbraniDatum;
    private TextView mesecLeto;
    private GridLayout koledarMreza;

    private final Calendar prikazanMesec = Calendar.getInstance();
    private final Calendar izbranDan = Calendar.getInstance();

    private final SimpleDateFormat zapisMeseca =
            new SimpleDateFormat("MMMM yyyy", new Locale("sl", "SI"));

    private static final String IME_DATOTEKE = "shramba_koledar";
    private static final String KLJUC = "nacrt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_koledar);

        OBROKI = getResources().getStringArray(R.array.obroki);

        naloziNacrt();

        izbraniDatum = findViewById(R.id.izbraniDatum);
        vsebinaObrokov = findViewById(R.id.vsebinaObrokov);
        mesecLeto = findViewById(R.id.mesecLeto);
        koledarMreza = findViewById(R.id.koledarMreza);

        findViewById(R.id.gumbPrejsnjiMesec).setOnClickListener(v -> {
            prikazanMesec.add(Calendar.MONTH, -1);
            prikaziKoledar();
        });

        findViewById(R.id.gumbNaslednjiMesec).setOnClickListener(v -> {
            prikazanMesec.add(Calendar.MONTH, 1);
            prikaziKoledar();
        });

        Calendar danes = Calendar.getInstance();

        izbranDan.setTimeInMillis(danes.getTimeInMillis());
        prikazanMesec.setTimeInMillis(danes.getTimeInMillis());
        prikazanMesec.set(Calendar.DAY_OF_MONTH, 1);

        trenutniDatum = formatirajDatum(izbranDan);
        izbraniDatum.setText("Obroki za " + trenutniDatum);

        prikaziKoledar();
        prikaziObroke();

        nastaviNavigacijo();
    }

    private void prikaziKoledar() {
        koledarMreza.removeAllViews();

        String naslov = zapisMeseca.format(prikazanMesec.getTime());
        if (!naslov.isEmpty()) {
            naslov = naslov.substring(0, 1).toUpperCase(Locale.ROOT) + naslov.substring(1);
        }
        mesecLeto.setText(naslov);

        String[] dnevi = getResources().getStringArray(R.array.kratice_dni);

        for (String dan : dnevi) {
            TextView oznaka = new TextView(this);
            oznaka.setText(dan);
            oznaka.setGravity(Gravity.CENTER);
            oznaka.setTypeface(null, Typeface.BOLD);
            oznaka.setTextSize(11);
            oznaka.setTextColor(getColorCompat(R.color.besedilo_sekundarno));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(24);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            oznaka.setLayoutParams(params);

            koledarMreza.addView(oznaka);
        }

        Calendar prviDan = (Calendar) prikazanMesec.clone();
        prviDan.set(Calendar.DAY_OF_MONTH, 1);

        int zamik = (prviDan.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int steviloDni = prviDan.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < zamik; i++) {
            koledarMreza.addView(praznaCelica());
        }

        int leto = prikazanMesec.get(Calendar.YEAR);
        int mesec = prikazanMesec.get(Calendar.MONTH);

        for (int dan = 1; dan <= steviloDni; dan++) {
            koledarMreza.addView(celicaDneva(leto, mesec, dan));
        }
    }

    private View praznaCelica() {
        TextView prazno = new TextView(this);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(44);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        prazno.setLayoutParams(params);

        return prazno;
    }

    private View celicaDneva(int leto, int mesec, int dan) {
        LinearLayout celica = new LinearLayout(this);
        celica.setOrientation(LinearLayout.VERTICAL);
        celica.setGravity(Gravity.CENTER);
        celica.setPadding(0, dp(2), 0, dp(2));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(44);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(1), dp(1), dp(1), dp(1));
        celica.setLayoutParams(params);

        boolean izbran = jeIzbraniDan(leto, mesec, dan);
        boolean danes = jeDanes(leto, mesec, dan);

        celica.setBackground(ozadjeDneva(izbran, danes));

        TextView stevilka = new TextView(this);
        stevilka.setText(String.valueOf(dan));
        stevilka.setGravity(Gravity.CENTER);
        stevilka.setTextSize(14);
        stevilka.setTextColor(getColorCompat(R.color.besedilo_primarno));

        if (izbran) {
            stevilka.setTypeface(null, Typeface.BOLD);
        }

        celica.addView(stevilka);

        LinearLayout pikice = new LinearLayout(this);
        pikice.setOrientation(LinearLayout.HORIZONTAL);
        pikice.setGravity(Gravity.CENTER);
        pikice.setPadding(0, dp(2), 0, 0);

        String datum = dan + "." + (mesec + 1) + "." + leto;
        int steviloObrokov = steviloObrokovZaDatum(datum);

        for (int i = 0; i < steviloObrokov; i++) {
            pikice.addView(pikica());
        }

        celica.addView(pikice);

        celica.setOnClickListener(v -> {
            izbranDan.set(Calendar.YEAR, leto);
            izbranDan.set(Calendar.MONTH, mesec);
            izbranDan.set(Calendar.DAY_OF_MONTH, dan);

            trenutniDatum = formatirajDatum(izbranDan);
            izbraniDatum.setText("Obroki za " + trenutniDatum);

            prikaziObroke();
            prikaziKoledar();
        });

        return celica;
    }

    private View pikica() {
        View p = new View(this);

        GradientDrawable oblika = new GradientDrawable();
        oblika.setShape(GradientDrawable.OVAL);
        oblika.setColor(getColorCompat(R.color.primarna));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(5), dp(5));
        params.setMargins(dp(1), 0, dp(1), 0);

        p.setLayoutParams(params);
        p.setBackground(oblika);

        return p;
    }

    private GradientDrawable ozadjeDneva(boolean izbran, boolean danes) {
        GradientDrawable ozadje = new GradientDrawable();
        ozadje.setCornerRadius(dp(12));

        if (izbran) {
            ozadje.setColor(getColorCompat(R.color.primarna_svetla));
            ozadje.setStroke(dp(1), getColorCompat(R.color.primarna));
        } else {
            ozadje.setColor(Color.TRANSPARENT);

            if (danes) {
                ozadje.setStroke(dp(1), getColorCompat(R.color.locnica));
            }
        }

        return ozadje;
    }

    private boolean jeIzbraniDan(int leto, int mesec, int dan) {
        return izbranDan.get(Calendar.YEAR) == leto
                && izbranDan.get(Calendar.MONTH) == mesec
                && izbranDan.get(Calendar.DAY_OF_MONTH) == dan;
    }

    private boolean jeDanes(int leto, int mesec, int dan) {
        Calendar danes = Calendar.getInstance();

        return danes.get(Calendar.YEAR) == leto
                && danes.get(Calendar.MONTH) == mesec
                && danes.get(Calendar.DAY_OF_MONTH) == dan;
    }

    private int steviloObrokovZaDatum(String datum) {
        int stevilo = 0;

        for (String obrok : OBROKI) {
            if (nacrt.containsKey(datum + "|" + obrok)) {
                stevilo++;
            }
        }

        return stevilo;
    }

    private String formatirajDatum(Calendar datum) {
        return datum.get(Calendar.DAY_OF_MONTH)
                + "."
                + (datum.get(Calendar.MONTH) + 1)
                + "."
                + datum.get(Calendar.YEAR);
    }

    private int dp(int vrednost) {
        return (int) (vrednost * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int getColorCompat(int id) {
        return getResources().getColor(id, getTheme());
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

                gumbOdstrani.setOnClickListener(v -> {
                    nacrt.remove(kljuc);
                    shraniNacrt();
                    prikaziObroke();
                    prikaziKoledar();
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
                    prikaziKoledar();
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
            else if (id == R.id.nav_pomocnik) {
                startActivity(new Intent(this, PomocnikActivity.class));
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