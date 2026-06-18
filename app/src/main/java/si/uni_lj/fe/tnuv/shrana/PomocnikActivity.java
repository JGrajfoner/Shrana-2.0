package si.uni_lj.fe.tnuv.shrana;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.Locale;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// Importi za Firebase AI
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;

import java.util.concurrent.Executor;

public class PomocnikActivity extends AppCompatActivity {

    private static final int ZAHTEVA_GOVOR = 100;

    private ScrollView drsnikPogovora;
    private LinearLayout vsebnikPogovora;
    private TextView oblacekRazmisljanja;
    private EditText vnosSporocila;
    private Button gumbPoslji;
    private ImageButton gumbMikrofon;
    private BottomNavigationView spodnjaNavigacija;
    private GenerativeModelFutures model;
    private Executor executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomocnik);

        drsnikPogovora = findViewById(R.id.drsnikPogovora);
        vsebnikPogovora = findViewById(R.id.vsebnikPogovora);
        vnosSporocila = findViewById(R.id.vnosSporocila);
        gumbPoslji = findViewById(R.id.gumbPoslji);
        gumbMikrofon = findViewById(R.id.gumbMikrofon);
        spodnjaNavigacija = findViewById(R.id.spodnjaNavigacija);

        nastaviGemini();
        nastaviZacetnoBesedilo();
        nastaviGumbe();
        nastaviSpodnjoNavigacijo();

    }

    private void nastaviZacetnoBesedilo() {
        vsebnikPogovora.removeAllViews();
        dodajSporocilo("Živjo! Kako ti lahko pomagam pri kuhanju?", false);
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

        dodajSporocilo(sporocilo, true);

        vnosSporocila.setText("");

        oblacekRazmisljanja = dodajSporocilo("Pomočnik razmišlja ...", false);

        posljiGemini(sporocilo);
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

    private void scrollNaDno() {
        drsnikPogovora.post(() -> drsnikPogovora.fullScroll(View.FOCUS_DOWN));
    }

    private TextView dodajSporocilo(String besedilo, boolean jeUporabnik) {
        TextView oblacek = new TextView(this);

        oblacek.setText(pretvoriMarkdownVBesedilo(besedilo));
        oblacek.setTextSize(16);
        oblacek.setLineSpacing(4, 1);

        int notranjiOdmikHorizontalno = dp(14);
        int notranjiOdmikVertikalno = dp(10);
        oblacek.setPadding(
                notranjiOdmikHorizontalno,
                notranjiOdmikVertikalno,
                notranjiOdmikHorizontalno,
                notranjiOdmikVertikalno
        );

        GradientDrawable ozadje = new GradientDrawable();
        ozadje.setCornerRadius(dp(18));

        if (jeUporabnik) {
            ozadje.setColor(Color.parseColor("#6B8377"));
            oblacek.setTextColor(Color.WHITE);
        } else {
            ozadje.setColor(Color.parseColor("#EEF4F1"));
            oblacek.setTextColor(Color.parseColor("#1F2A26"));
        }

        oblacek.setBackground(ozadje);

        LinearLayout.LayoutParams parametri = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        parametri.setMargins(
                jeUporabnik ? dp(56) : 0,
                dp(6),
                jeUporabnik ? 0 : dp(56),
                dp(6)
        );

        parametri.gravity = jeUporabnik ? Gravity.END : Gravity.START;

        vsebnikPogovora.addView(oblacek, parametri);

        scrollNaDno();

        return oblacek;
    }

    private CharSequence pretvoriMarkdownVBesedilo(String besedilo) {
        SpannableStringBuilder rezultat = new SpannableStringBuilder();

        Pattern vzorec = Pattern.compile("\\*\\*(.*?)\\*\\*");
        Matcher matcher = vzorec.matcher(besedilo);

        int zadnjiKonec = 0;

        while (matcher.find()) {
            rezultat.append(besedilo.substring(zadnjiKonec, matcher.start()));

            int zacetekBold = rezultat.length();
            rezultat.append(matcher.group(1));
            int konecBold = rezultat.length();

            rezultat.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    zacetekBold,
                    konecBold,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            zadnjiKonec = matcher.end();
        }

        rezultat.append(besedilo.substring(zadnjiKonec));

        return rezultat;
    }

    private int dp(int vrednost) {
        return (int) (vrednost * getResources().getDisplayMetrics().density);
    }

    private void nastaviGemini() {
        executor = ContextCompat.getMainExecutor(this);

        GenerativeModel ai = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel("gemini-2.5-flash");

        model = GenerativeModelFutures.from(ai);
    }

    private void posljiGemini(String vprasanje) {
        String prompt = "Si kuharski pomočnik v aplikaciji sHrana. "
                + "Odgovarjaj v slovenščini, kratko, prijazno in praktično. "
                + "Pomagaj pri receptih, sestavinah, zamenjavah sestavin, "
                + "načrtovanju obrokov, kuhanju in nakupovalnem seznamu. "
                + "Če uporabnik sprašuje o pokvarjeni hrani, alergijah ali varnosti hrane, "
                + "ga opozori na previdnost. "
                + "Vprašanje uporabnika: " + vprasanje;

        Content vsebina = new Content.Builder()
                .addText(prompt)
                .build();

        ListenableFuture<GenerateContentResponse> odgovor = model.generateContent(vsebina);

        Futures.addCallback(odgovor, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String besediloOdgovora = result.getText();

                if (besediloOdgovora == null || besediloOdgovora.trim().isEmpty()) {
                    besediloOdgovora = "Žal nisem dobil odgovora.";
                }

                if (oblacekRazmisljanja != null) {
                    oblacekRazmisljanja.setText(pretvoriMarkdownVBesedilo(besediloOdgovora.trim()));
                    oblacekRazmisljanja = null;
                    scrollNaDno();
                } else {
                    dodajSporocilo(besediloOdgovora.trim(), false);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                android.util.Log.e("PomocnikActivity", "Napaka pri Gemini klicu", t);

                String napaka = "Prišlo je do napake pri povezavi z Gemini.";

                if (oblacekRazmisljanja != null) {
                    oblacekRazmisljanja.setText(napaka);
                    oblacekRazmisljanja = null;
                    scrollNaDno();
                } else {
                    dodajSporocilo(napaka, false);
                }

                Toast.makeText(PomocnikActivity.this, "Napaka: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, executor);
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