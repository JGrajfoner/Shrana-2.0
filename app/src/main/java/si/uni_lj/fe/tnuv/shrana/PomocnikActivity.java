package si.uni_lj.fe.tnuv.shrana;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Locale;

public class PomocnikActivity extends AppCompatActivity {

    private static final int ZAHTEVA_GOVOR = 100;

    private TextView besediloPogovora;
    private EditText vnosSporocila;
    private Button gumbPoslji;
    private ImageButton gumbMikrofon;
    private BottomNavigationView spodnjaNavigacija;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomocnik);

        besediloPogovora = findViewById(R.id.besediloPogovora);
        vnosSporocila = findViewById(R.id.vnosSporocila);
        gumbPoslji = findViewById(R.id.gumbPoslji);
        gumbMikrofon = findViewById(R.id.gumbMikrofon);
        spodnjaNavigacija = findViewById(R.id.spodnjaNavigacija);

        nastaviZacetnoBesedilo();
        nastaviGumbe();
        nastaviSpodnjoNavigacijo();
    }

    private void nastaviZacetnoBesedilo() {
        besediloPogovora.setText("Pomočnik: Živjo! Kako ti lahko pomagam pri kuhanju?");
    }

    private void nastaviGumbe() {
        gumbPoslji.setOnClickListener(v -> posljiSporocilo());

        gumbMikrofon.setOnClickListener(v -> zacniGovorniVnos());
    }

    private void posljiSporocilo() {
        String sporocilo = vnosSporocila.getText().toString().trim();

        if (sporocilo.isEmpty()) {
            Toast.makeText(this, "Najprej vpiši ali povej vprašanje.", Toast.LENGTH_SHORT).show();
            return;
        }

        dodajVPogovor("Ti: " + sporocilo);

        vnosSporocila.setText("");

        // To je samo začasni testni odgovor.
        // Kasneje bomo tukaj poklicali Gemini / Firebase AI.
        String odgovor = ustvariTestniOdgovor(sporocilo);
        dodajVPogovor("Pomočnik: " + odgovor);
    }

    private String ustvariTestniOdgovor(String vprasanje) {
        return "Zaenkrat sem testni pomočnik. Kasneje bom znal odgovoriti na vprašanje: \"" + vprasanje + "\".";
    }

    private void dodajVPogovor(String novoBesedilo) {
        String trenutnoBesedilo = besediloPogovora.getText().toString();

        besediloPogovora.setText(trenutnoBesedilo + "\n\n" + novoBesedilo);
    }

    private void zacniGovorniVnos() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sl-SI");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Povej vprašanje ...");

        try {
            startActivityForResult(intent, ZAHTEVA_GOVOR);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Govorni vnos na tej napravi ni podprt.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ZAHTEVA_GOVOR && resultCode == RESULT_OK && data != null) {
            ArrayList<String> rezultati = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (rezultati != null && !rezultati.isEmpty()) {
                String prepoznanoBesedilo = rezultati.get(0);
                vnosSporocila.setText(prepoznanoBesedilo);
                vnosSporocila.setSelection(vnosSporocila.getText().length());
            }
        }
    }

    private void nastaviSpodnjoNavigacijo() {
        spodnjaNavigacija.setSelectedItemId(R.id.nav_pomocnik);

        spodnjaNavigacija.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_recepti) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_koledar) {
                startActivity(new Intent(this, KoledarActivity.class));
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
            } else if (id == R.id.nav_pomocnik) {
                return true;
            }

            return false;
        });
    }
}