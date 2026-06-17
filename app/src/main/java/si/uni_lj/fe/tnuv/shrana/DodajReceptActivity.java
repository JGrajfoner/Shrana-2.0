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

public class DodajReceptActivity extends AppCompatActivity {

    private int casPripraveMin = 0;
    private int casKuhanjaMin = 0;
    private String izbranaSlikaUri = null;
    private final List<Recept.Sestavina> seznamSestavin = new ArrayList<>();
    private final List<String> seznamOznak = new ArrayList<>();

    private final ActivityResultLauncher<String> slikaLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    izbranaSlikaUri = uri.toString();
                    ImageView preview = findViewById(R.id.slikaPredogled);
                    preview.setImageURI(uri);
                    
                    // Pridobimo trajno dovoljenje za dostop do URI-ja, če je mogoče
                    try {
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {}
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dodaj_recept);

        EditText vnosNaslov = findViewById(R.id.vnosNaslov);
        Button gumbCasPriprave = findViewById(R.id.gumbCasPriprave);
        Button gumbCasKuhanja = findViewById(R.id.gumbCasKuhanja);
        EditText vnosKalorije = findViewById(R.id.vnosKalorije);
        EditText vnosOpis = findViewById(R.id.vnosOpis);
        EditText vnosPriprava = findViewById(R.id.vnosPriprava);
        
        // Slika
        findViewById(R.id.gumbIzberiSliko).setOnClickListener(v -> slikaLauncher.launch("image/*"));

        // Sestavine vnos
        EditText vnosImeSestavine = findViewById(R.id.vnosImeSestavine);
        EditText vnosKolicinaSestavine = findViewById(R.id.vnosKolicinaSestavine);
        AutoCompleteTextView autoCompleteEnota = findViewById(R.id.autoCompleteEnota);
        Button gumbDodajSestavino = findViewById(R.id.gumbDodajSestavino);
        LinearLayout kontejnerSestavin = findViewById(R.id.seznamSestavinKontejner);

        // Oznake vnos
        EditText vnosOznaka = findViewById(R.id.vnosOznaka);
        Button gumbDodajOznako = findViewById(R.id.gumbDodajOznako);
        ChipGroup chipGroupOznake = findViewById(R.id.chipGroupOznake);

        // Nastavitev dropdown-a za enote
        String[] enote = {"kg", "g", "ml", "l", "tbsp", "tsp", "kos", "strok"};
        ArrayAdapter<String> adapterEnote = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, enote);
        autoCompleteEnota.setAdapter(adapterEnote);
        autoCompleteEnota.setText(enote[0], false);

        // Time pickers
        gumbCasPriprave.setOnClickListener(v -> prikaziTimePicker(true, gumbCasPriprave));
        gumbCasKuhanja.setOnClickListener(v -> prikaziTimePicker(false, gumbCasKuhanja));

        // Dodajanje sestavine
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

        // Dodajanje oznak
        gumbDodajOznako.setOnClickListener(v -> {
            String oznaka = vnosOznaka.getText().toString().trim();
            if (!oznaka.isEmpty() && !seznamOznak.contains(oznaka)) {
                dodajOznako(oznaka, chipGroupOznake);
                vnosOznaka.setText("");
            }
        });

        vnosOznaka.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                gumbDodajOznako.performClick();
                return true;
            }
            return false;
        });

        Button gumbShrani = findViewById(R.id.gumbShrani);
        Button gumbPreklici = findViewById(R.id.gumbPreklici);

        gumbPreklici.setOnClickListener(v -> finish());

        gumbShrani.setOnClickListener(v -> {
            String naslov = vnosNaslov.getText().toString().trim();
            String opis = vnosOpis.getText().toString().trim();
            String priprava = vnosPriprava.getText().toString().trim();
            String kalStr = vnosKalorije.getText().toString().trim();
            int kalorije = kalStr.isEmpty() ? 0 : Integer.parseInt(kalStr);

            if (naslov.isEmpty()) {
                Toast.makeText(this, "Vnesi vsaj naslov recepta", Toast.LENGTH_SHORT).show();
                return;
            }

            Recept novi = new Recept(naslov, casPripraveMin, casKuhanjaMin, kalorije, opis, 
                                     new ArrayList<>(seznamSestavin), priprava, new ArrayList<>(seznamOznak), izbranaSlikaUri);

            Intent rezultat = new Intent();
            rezultat.putExtra("recept", novi);
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
            
            String tekst = "";
            if (hourOfDay > 0) tekst += hourOfDay + " h ";
            tekst += minuteOfHour + " min";
            gumb.setText(tekst);
        }, ure, minute, true);
        timePickerDialog.setTitle(isPriprava ? "Čas priprave" : "Čas kuhanja");
        timePickerDialog.show();
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