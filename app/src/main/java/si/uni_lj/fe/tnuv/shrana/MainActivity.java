package si.uni_lj.fe.tnuv.shrana;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<Recept> recepti;
    private ReceptAdapter adapter;

    private final androidx.activity.result.ActivityResultLauncher<Intent> dodajLauncher =
            registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                    rezultat -> {
                        if (rezultat.getResultCode() == RESULT_OK && rezultat.getData() != null) {
                            Recept novi = (Recept) rezultat.getData()
                                    .getSerializableExtra("recept");
                            if (novi != null) {
                                adapter.dodaj(novi);
                                RepozitorijReceptov.shrani(MainActivity.this);
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RepozitorijReceptov.nalozi(this);
        recepti = RepozitorijReceptov.getRecepti();

        if (recepti.isEmpty()) {
            recepti.add(new Recept("Piščanec s paradižnikom", 15, 30, 450, "Okusen recept",
                    new ArrayList<>(), "Priprava...", new ArrayList<>()));
            RepozitorijReceptov.shrani(this);
        }

        RecyclerView seznam = findViewById(R.id.seznamReceptov);
        seznam.setLayoutManager(new LinearLayoutManager(this));

        class ReceptPoslusalec implements ReceptAdapter.OnReceptClick,
                ReceptAdapter.OnSrcekClick {
            @Override
            public void naReceptKlik(Recept recept) {
                Intent i = new Intent(MainActivity.this, PodrobnostiActivity.class);
                i.putExtra("recept", recept);
                startActivity(i);
            }

            @Override
            public void naSrcekKlik() {
                RepozitorijReceptov.shrani(MainActivity.this);
            }
        }

        adapter = new ReceptAdapter(recepti, new ReceptPoslusalec());
        seznam.setAdapter(adapter);

        nastaviDrsenjeZaBrisanje(seznam);

        findViewById(R.id.gumbDodaj).setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, DodajReceptActivity.class);
            dodajLauncher.launch(i);
        });

        EditText iskalnik = findViewById(R.id.iskalnik);
        iskalnik.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                adapter.filtriraj(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });

        final boolean[] samoPriljubljeni = {false};
        findViewById(R.id.gumbPriljubljeni).setOnClickListener(v -> {
            samoPriljubljeni[0] = !samoPriljubljeni[0];
            adapter.prikaziPriljubljene(samoPriljubljeni[0]);
            Toast.makeText(this,
                    samoPriljubljeni[0] ? "Samo priljubljeni" : "Vsi recepti",
                    Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.gumbFilter).setOnClickListener(v -> odpriFilterDialog());

        osveziBarvoFilterGumba();
        nastaviNavigacijo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ob vrnitvi iz podrobnosti/urejanja osvežimo seznam,
        // saj se je morda spremenil naslov, slika ali priljubljenost recepta.
        if (adapter != null) {
            adapter.osvezi();
        }
    }

    private void nastaviNavigacijo() {
        BottomNavigationView navigacija = findViewById(R.id.spodnjaNavigacija);
        navigacija.setSelectedItemId(R.id.nav_recepti);
        navigacija.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_recepti) {
                return true;
            } else if (id == R.id.nav_koledar) {
                startActivity(new Intent(MainActivity.this, KoledarActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_seznam) {
                startActivity(new Intent(MainActivity.this, SeznamActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_casovnik) {
                startActivity(new Intent(MainActivity.this, CasovnikiActivity.class));
                finish();
                return true;
            }
            Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void nastaviDrsenjeZaBrisanje(RecyclerView seznam) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pozicija = viewHolder.getAdapterPosition();
                adapter.odstrani(pozicija);
                RepozitorijReceptov.shrani(MainActivity.this);
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(seznam);
    }

    // ===== Dialog za napredno filtriranje (oznake, čas, kalorije) =====
    private void odpriFilterDialog() {
        View pogled = getLayoutInflater().inflate(R.layout.dialog_filter, null);

        com.google.android.material.chip.ChipGroup chipi =
                pogled.findViewById(R.id.chipGroupFilter);
        TextView oznakePrazno = pogled.findViewById(R.id.oznakePrazno);
        com.google.android.material.slider.Slider sliderCas =
                pogled.findViewById(R.id.sliderCas);
        com.google.android.material.slider.Slider sliderKalorije =
                pogled.findViewById(R.id.sliderKalorije);
        TextView oznakaCas = pogled.findViewById(R.id.oznakaCas);
        TextView oznakaKalorije = pogled.findViewById(R.id.oznakaKalorije);

        // --- Zberi vse oznake in najvišje vrednosti iz obstoječih receptov ---
        java.util.TreeSet<String> vseOznake = new java.util.TreeSet<>();
        int najvecCas = 0;
        int najvecKalorij = 0;
        for (Recept r : recepti) {
            if (r.oznake != null) vseOznake.addAll(r.oznake);
            najvecCas = Math.max(najvecCas, r.getSkupniCas());
            najvecKalorij = Math.max(najvecKalorij, r.kalorije);
        }
        // Zaokroži navzgor in poskrbi za smiselne minimalne razpone
        final int casZgornja = Math.max(60, ((najvecCas / 30) + 1) * 30);
        final int kalorijeZgornja = Math.max(500, ((najvecKalorij / 100) + 1) * 100);

        // --- Oznake kot izbirni chipi ---
        if (vseOznake.isEmpty()) {
            oznakePrazno.setVisibility(View.VISIBLE);
        }
        java.util.Set<String> ze = adapter.getIzbraneOznake();
        for (String oznaka : vseOznake) {
            com.google.android.material.chip.Chip chip =
                    new com.google.android.material.chip.Chip(this);
            chip.setText(oznaka);
            chip.setCheckable(true);
            chip.setChecked(ze.contains(oznaka));
            chipi.addView(chip);
        }

        // --- Slider: čas ---
        sliderCas.setValueFrom(0);
        sliderCas.setValueTo(casZgornja);
        int trenutniMaxCas = adapter.getMaxCas();
        float zacetniCas = (trenutniMaxCas == Integer.MAX_VALUE)
                ? casZgornja : Math.min(trenutniMaxCas, casZgornja);
        sliderCas.setValue(zacetniCas);
        oznakaCas.setText(formatirajCasFilter((int) zacetniCas, casZgornja));
        sliderCas.addOnChangeListener((s, value, fromUser) ->
                oznakaCas.setText(formatirajCasFilter((int) value, casZgornja)));

        // --- Slider: kalorije ---
        sliderKalorije.setValueFrom(0);
        sliderKalorije.setValueTo(kalorijeZgornja);
        int trenutniMaxKal = adapter.getMaxKalorije();
        float zacetneKal = (trenutniMaxKal == Integer.MAX_VALUE)
                ? kalorijeZgornja : Math.min(trenutniMaxKal, kalorijeZgornja);
        sliderKalorije.setValue(zacetneKal);
        oznakaKalorije.setText(formatirajKalorijeFilter((int) zacetneKal, kalorijeZgornja));
        sliderKalorije.addOnChangeListener((s, value, fromUser) ->
                oznakaKalorije.setText(formatirajKalorijeFilter((int) value, kalorijeZgornja)));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Filtriraj recepte")
                .setView(pogled)
                .setPositiveButton("Uporabi", (dialog, kateri) -> {
                    java.util.Set<String> izbrane = new java.util.HashSet<>();
                    for (int i = 0; i < chipi.getChildCount(); i++) {
                        com.google.android.material.chip.Chip c =
                                (com.google.android.material.chip.Chip) chipi.getChildAt(i);
                        if (c.isChecked()) izbrane.add(c.getText().toString());
                    }
                    // Če je slider na vrhu, pomeni "brez omejitve"
                    int cas = (int) sliderCas.getValue();
                    int kal = (int) sliderKalorije.getValue();
                    int maxCas = (cas >= casZgornja) ? Integer.MAX_VALUE : cas;
                    int maxKal = (kal >= kalorijeZgornja) ? Integer.MAX_VALUE : kal;

                    adapter.nastaviFiltre(izbrane, maxCas, maxKal);
                    osveziBarvoFilterGumba();
                })
                .setNeutralButton("Počisti", (dialog, kateri) -> {
                    adapter.nastaviFiltre(null, Integer.MAX_VALUE, Integer.MAX_VALUE);
                    osveziBarvoFilterGumba();
                })
                .setNegativeButton("Prekliči", null)
                .show();
    }

    private String formatirajCasFilter(int minute, int zgornja) {
        if (minute >= zgornja) return "Brez omejitve";
        int h = minute / 60;
        int m = minute % 60;
        if (h > 0) return "do " + h + " h " + m + " min";
        return "do " + m + " min";
    }

    private String formatirajKalorijeFilter(int kal, int zgornja) {
        if (kal >= zgornja) return "Brez omejitve";
        return "do " + kal + " kcal";
    }

    private void osveziBarvoFilterGumba() {
        ImageView gumbFilter = findViewById(R.id.gumbFilter);
        if (gumbFilter == null) return;
        int barva = adapter.jeFilterAktiven()
                ? androidx.core.content.ContextCompat.getColor(this, R.color.primarna)
                : androidx.core.content.ContextCompat.getColor(this, R.color.besedilo_sekundarno);
        gumbFilter.setColorFilter(barva);
    }

    static class ReceptAdapter extends RecyclerView.Adapter<ReceptAdapter.ReceptViewHolder> {

        interface OnReceptClick {
            void naReceptKlik(Recept recept);
        }

        interface OnSrcekClick {
            void naSrcekKlik();
        }

        private final List<Recept> vsiRecepti;
        private final List<Recept> prikazani;
        private final OnReceptClick poslusalec;

        // ===== Stanje filtrov (vsa merila se uveljavijo skupaj) =====
        private String iskalniNiz = "";
        private boolean samoPriljubljeni = false;
        private final java.util.Set<String> izbraneOznake = new java.util.HashSet<>();
        private int maxCas = Integer.MAX_VALUE;       // skupni čas (priprava + kuhanje)
        private int maxKalorije = Integer.MAX_VALUE;

        ReceptAdapter(List<Recept> recepti, OnReceptClick poslusalec) {
            this.vsiRecepti = recepti;
            this.prikazani = new ArrayList<>(recepti);
            this.poslusalec = poslusalec;
        }

        public void dodaj(Recept recept) {
            vsiRecepti.add(recept);
            uveljavi();
        }

        // Ponovno uskladi prikazani seznam z glavnim in osveži celoten prikaz.
        public void osvezi() {
            uveljavi();
        }

        public void odstrani(int pozicija) {
            if (pozicija >= 0 && pozicija < prikazani.size()) {
                Recept r = prikazani.get(pozicija);
                vsiRecepti.remove(r);
                prikazani.remove(pozicija);
                notifyItemRemoved(pozicija);
            }
        }

        // ===== Vmesniki za posamezna merila (vsak le nastavi svoj del) =====
        public void filtriraj(String poizvedba) {
            iskalniNiz = poizvedba == null ? "" : poizvedba.toLowerCase().trim();
            uveljavi();
        }

        public void prikaziPriljubljene(boolean samo) {
            this.samoPriljubljeni = samo;
            uveljavi();
        }

        public void nastaviFiltre(java.util.Set<String> oznake, int maxCas, int maxKalorije) {
            izbraneOznake.clear();
            if (oznake != null) izbraneOznake.addAll(oznake);
            this.maxCas = maxCas;
            this.maxKalorije = maxKalorije;
            uveljavi();
        }

        // Trenutno aktivno stanje filtrov (za predizpolnitev dialoga)
        public java.util.Set<String> getIzbraneOznake() { return izbraneOznake; }
        public int getMaxCas() { return maxCas; }
        public int getMaxKalorije() { return maxKalorije; }

        // Ali je kak filter sploh aktiven (za npr. obarvanje gumba)
        public boolean jeFilterAktiven() {
            return !izbraneOznake.isEmpty()
                    || maxCas != Integer.MAX_VALUE
                    || maxKalorije != Integer.MAX_VALUE;
        }

        // ===== Osrednja filtrirna logika: vsa merila hkrati =====
        private void uveljavi() {
            prikazani.clear();
            for (Recept r : vsiRecepti) {
                if (ustreza(r)) {
                    prikazani.add(r);
                }
            }
            notifyDataSetChanged();
        }

        private boolean ustreza(Recept r) {
            // Iskalni niz (po naslovu)
            if (!iskalniNiz.isEmpty()) {
                if (r.naslov == null || !r.naslov.toLowerCase().contains(iskalniNiz)) {
                    return false;
                }
            }
            // Samo priljubljeni
            if (samoPriljubljeni && !r.priljubljen) {
                return false;
            }
            // Največji skupni čas
            if (r.getSkupniCas() > maxCas) {
                return false;
            }
            // Največ kalorij
            if (r.kalorije > maxKalorije) {
                return false;
            }
            // Oznake: recept mora vsebovati VSE izbrane oznake
            if (!izbraneOznake.isEmpty()) {
                if (r.oznake == null || !r.oznake.containsAll(izbraneOznake)) {
                    return false;
                }
            }
            return true;
        }

        @NonNull
        @Override
        public ReceptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View pogled = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recept, parent, false);
            return new ReceptViewHolder(pogled);
        }

        @Override
        public void onBindViewHolder(@NonNull ReceptViewHolder holder, int position) {
            Recept r = prikazani.get(position);
            holder.naslov.setText(r.naslov);

            int skupniMin = r.getSkupniCas();
            int ure = skupniMin / 60;
            int minute = skupniMin % 60;
            String casTekst = (ure > 0 ? ure + "h " : "") + minute + "m";

            holder.cas.setText(casTekst);
            holder.kalorije.setText(r.kalorije + " kcal");
            holder.opis.setText(r.opis);
            holder.itemView.setOnClickListener(v -> poslusalec.naReceptKlik(r));

            if (r.slikaUri != null && !r.slikaUri.isEmpty()) {
                holder.slika.setImageURI(Uri.parse(r.slikaUri));
            } else {
                holder.slika.setImageResource(R.drawable.ic_image);
            }

            holder.srcek.setImageResource(r.priljubljen
                    ? R.drawable.ic_star_filled
                    : R.drawable.ic_star_outline);

            holder.srcek.setOnClickListener(v -> {
                r.priljubljen = !r.priljubljen;
                notifyItemChanged(position);
                if (poslusalec instanceof OnSrcekClick) {
                    ((OnSrcekClick) poslusalec).naSrcekKlik();
                }
            });
        }

        @Override
        public int getItemCount() {
            return prikazani.size();
        }

        static class ReceptViewHolder extends RecyclerView.ViewHolder {
            TextView naslov, cas, kalorije, opis;
            ImageView slika, srcek;

            ReceptViewHolder(@NonNull View itemView) {
                super(itemView);
                naslov = itemView.findViewById(R.id.naslovRecepta);
                cas = itemView.findViewById(R.id.casRecepta);
                kalorije = itemView.findViewById(R.id.kalorijeRecepta);
                opis = itemView.findViewById(R.id.opisRecepta);
                slika = itemView.findViewById(R.id.slikaRecepta);
                srcek = itemView.findViewById(R.id.srcek);
            }
        }
    }
}