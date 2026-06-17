package si.uni_lj.fe.tnuv.shrana;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class UrejanjeReceptaActivity extends AppCompatActivity {

    private int casPripraveMin = 0;
    private int casKuhanjaMin = 0;
    private String izbranaSlikaUri = null;
    private final List<Recept.Sestavina> seznamSestavin = new ArrayList<>();
    private final List<String> seznamOznak = new ArrayList<>();
    private Recept originalniRecept;

    private final ActivityResultLauncher<String> slikaLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    izbranaSlikaUri = uri.toString();
                    ImageView preview = findViewById(R.id.slikaPredogled);
                    preview.setImageURI(uri);
                    try {
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {}
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_urejanje_recepta);

        originalniRecept = (Recept) getIntent().getSerializableExtra("recept");
        if (originalniRecept == null) {
            finish();
            return;
        }

        EditText vnosNaslov = findViewById(R.id.vnosNaslov);
        Button gumbCasPriprave = findViewById(R.id.gumbCasPriprave);
        Button gumbCasKuhanja = findViewById(R.id.gumbCasKuhanja);
        EditText vnosKalorije = findViewById(R.id.vnosKalorije);
        EditText vnosOpis = findViewById(R.id.vnosOpis);
        EditText vnosPriprava = findViewById(R.id.vnosPriprava);
        ImageView slikaPredogled = findViewById(R.id.slikaPredogled);
        
        // Predizpolnjevanje
        vnosNaslov.setText(originalniRecept.naslov);
        casPripraveMin = originalniRecept.casPriprave;
        casKuhanjaMin = originalniRecept.casKuhanja;
        gumbCasPriprave.setText(formatirajCas(casPripraveMin));
        gumbCasKuhanja.setText(formatirajCas(casKuhanjaMin));
        vnosKalorije.setText(String.valueOf(originalniRecept.kalorije));
        vnosOpis.setText(originalniRecept.opis);
        vnosPriprava.setText(originalniRecept.priprava);
        izbranaSlikaUri = originalniRecept.slikaUri;
        if (izbranaSlikaUri != null) {
            slikaPredogled.setImageURI(Uri.parse(izbranaSlikaUri));
        }

        findViewById(R.id.gumbIzberiSliko).setOnClickListener(v -> slikaLauncher.launch("image/*"));

        EditText vnosImeSestavine = findViewById(R.id.vnosImeSestavine);
        EditText vnosKolicinaSestavine = findViewById(R.id.vnosKolicinaSestavine);
        AutoCompleteTextView autoCompleteEnota = findViewById(R.id.autoCompleteEnota);
        Button gumbDodajSestavino = findViewById(R.id.gumbDodajSestavino);
        LinearLayout kontejnerSestavin = findViewById(R.id.seznamSestavinKontejner);

        // Naloži obstoječe sestavine
        if (originalniRecept.sestavine != null) {
            for (Recept.Sestavina s : originalniRecept.sestavine) {
                seznamSestavin.add(s);
                dodajSestavinoVLayout(s, kontejnerSestavin);
            }
        }

        String[] enote = {"kg", "g", "ml", "l", "tbsp", "tsp", "kos", "strok"};
        ArrayAdapter<String> adapterEnote = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, enote);
        autoCompleteEnota.setAdapter(adapterEnote);
        autoCompleteEnota.setText(enote[0], false);

        gumbDodajSestavino.setOnClickListener(v -> {
            String ime = vnosImeSestavine.getText().toString().trim();
            String kolStr = vnosKolicinaSestavine.getText().toString().trim();
            String enota = autoCompleteEnota.getText().toString();
            if (!ime.isEmpty() && !kolStr.isEmpty()) {
                try {
                    double kolicina = Double.parseDouble(kolStr);
                    Recept.Sestavina s = new Recept.Sestavina(ime, kolicina, enota);
                    seznamSestavin.add(s);
                    dodajSestavinoVLayout(s, kontejnerSestavin);
                    vnosImeSestavine.setText("");
                    vnosKolicinaSestavine.setText("");
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Neveljavna količina", Toast.LENGTH_SHORT).show();
                }
            }
        });

        EditText vnosOznaka = findViewById(R.id.vnosOznaka);
        Button gumbDodajOznako = findViewById(R.id.gumbDodajOznako);
        ChipGroup chipGroupOznake = findViewById(R.id.chipGroupOznake);

        // Naloži obstoječe oznake
        if (originalniRecept.oznake != null) {
            for (String oznaka : originalniRecept.oznake) {
                dodajOznako(oznaka, chipGroupOznake);
            }
        }

        gumbDodajOznako.setOnClickListener(v -> {
            String oznaka = vnosOznaka.getText().toString().trim();
            if (!oznaka.isEmpty() && !seznamOznak.contains(oznaka)) {
                dodajOznako(oznaka, chipGroupOznake);
                vnosOznaka.setText("");
            }
        });

        gumbCasPriprave.setOnClickListener(v -> prikaziTimePicker(true, gumbCasPriprave));
        gumbCasKuhanja.setOnClickListener(v -> prikaziTimePicker(false, gumbCasKuhanja));

        findViewById(R.id.gumbPreklici).setOnClickListener(v -> finish());
        findViewById(R.id.gumbPosodobi).setOnClickListener(v -> {
            String naslov = vnosNaslov.getText().toString().trim();
            if (naslov.isEmpty()) {
                Toast.makeText(this, "Vnesi vsaj naslov recepta", Toast.LENGTH_SHORT).show();
                return;
            }
            
            originalniRecept.naslov = naslov;
            originalniRecept.casPriprave = casPripraveMin;
            originalniRecept.casKuhanja = casKuhanjaMin;
            String kalStr = vnosKalorije.getText().toString().trim();
            originalniRecept.kalorije = kalStr.isEmpty() ? 0 : Integer.parseInt(kalStr);
            originalniRecept.opis = vnosOpis.getText().toString().trim();
            originalniRecept.priprava = vnosPriprava.getText().toString().trim();
            originalniRecept.sestavine = new ArrayList<>(seznamSestavin);
            originalniRecept.oznake = new ArrayList<>(seznamOznak);
            originalniRecept.slikaUri = izbranaSlikaUri;

            Intent rezultat = new Intent();
            rezultat.putExtra("recept", originalniRecept);
            setResult(RESULT_OK, rezultat);
            finish();
        });
    }

    private void prikaziTimePicker(boolean isPriprava, Button gumb) {
        int ure = (isPriprava ? casPripraveMin : casKuhanjaMin) / 60;
        int minute = (isPriprava ? casPripraveMin : casKuhanjaMin) % 60;
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            int skupajMin = hourOfDay * 60 + minuteOfHour;
            if (isPriprava) casPripraveMin = skupajMin;
            else casKuhanjaMin = skupajMin;
            gumb.setText(formatirajCas(skupajMin));
        }, ure, minute, true);
        timePickerDialog.show();
    }

    private String formatirajCas(int skupajMin) {
        int ure = skupajMin / 60;
        int min = skupajMin % 60;
        String tekst = "";
        if (ure > 0) tekst += ure + " h ";
        tekst += min + " min";
        return tekst;
    }

    private void dodajSestavinoVLayout(Recept.Sestavina s, LinearLayout kontejner) {
        TextView tv = new TextView(this);
        tv.setText(s.toString());
        tv.setPadding(0, 8, 0, 8);
        tv.setTextSize(16);
        tv.setOnClickListener(v -> {
            seznamSestavin.remove(s);
            kontejner.removeView(tv);
        });
        kontejner.addView(tv);
    }

    private void dodajOznako(String besedilo, ChipGroup group) {
        Chip chip = new Chip(this);
        chip.setText(besedilo);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            group.removeView(chip);
            seznamOznak.remove(besedilo);
        });
        group.addView(chip);
        seznamOznak.add(besedilo);
    }
}