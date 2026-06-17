package si.uni_lj.fe.tnuv.shrana;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    // Seznam in adapter shranimo kot polji razreda,
    // da do njiju lahko dostopamo tudi izven metode onCreate.
    private List<Recept> recepti;
    private ReceptAdapter adapter;

    // Launcher, ki odpre obrazec in počaka na rezultat (nov recept)
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

        // 1) Naložimo recepte z diska in vzamemo deljeni seznam
        RepozitorijReceptov.nalozi(this);
        recepti = RepozitorijReceptov.getRecepti();

        // Ob prvem zagonu dodamo testni recept, če je seznam prazen
        if (recepti.isEmpty()) {
            recepti.add(new Recept("Piščanec s paradižnikom", "45 min", "Okusen recept"));
            RepozitorijReceptov.shrani(this);
        }

        // 2) Povežemo RecyclerView z adapterjem
        RecyclerView seznam = findViewById(R.id.seznamReceptov);
        seznam.setLayoutManager(new LinearLayoutManager(this));

        // Poslušalec, ki obravnava klik na recept IN klik na srček
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
                RepozitorijReceptov.shrani(MainActivity.this); // shranimo spremembo
            }
        }

        adapter = new ReceptAdapter(recepti, new ReceptPoslusalec());
        seznam.setAdapter(adapter);

        // 3) Brisanje z drsenjem (swipe) v stran
        nastaviDrsenjeZaBrisanje(seznam);

        // 4) Gumb + DODAJ odpre obrazec za nov recept
        findViewById(R.id.gumbDodaj).setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, DodajReceptActivity.class);
            dodajLauncher.launch(i);
        });

        // 5) Iskalnik: ob vsaki spremembi besedila filtriramo seznam
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

        // 6) Gumb filter priljubljenih: preklaplja med vsemi in samo priljubljenimi
        final boolean[] samoPriljubljeni = {false}; // polje, da ga lahko spreminjamo v lambdi
        findViewById(R.id.gumbPriljubljeni).setOnClickListener(v -> {
            samoPriljubljeni[0] = !samoPriljubljeni[0];
            adapter.prikaziPriljubljene(samoPriljubljeni[0]);
            Toast.makeText(this,
                    samoPriljubljeni[0] ? "Samo priljubljeni" : "Vsi recepti",
                    Toast.LENGTH_SHORT).show();
        });

        // 7) Spodnja navigacija
        nastaviNavigacijo();
    }

    private void nastaviNavigacijo() {
        BottomNavigationView navigacija = findViewById(R.id.spodnjaNavigacija);
        navigacija.setSelectedItemId(R.id.nav_recepti);
        navigacija.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_recepti) {
                return true; // smo že tukaj
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
            // Pomočnik (še ni narejen) samo izpiše sporočilo
            Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    // Nastavi drsenje (swipe) levo/desno za brisanje recepta
    private void nastaviDrsenjeZaBrisanje(RecyclerView seznam) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0,                                              // ne dovolimo premikanja gor/dol
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT    // dovolimo drsenje levo in desno
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // premikanja ne uporabljamo
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

    // ===== Adapter, ki povezuje podatke z vrsticami =====
    static class ReceptAdapter extends RecyclerView.Adapter<ReceptAdapter.ReceptViewHolder> {

        interface OnReceptClick {
            void naReceptKlik(Recept recept);
        }

        // Sporoči, da se je priljubljenost spremenila (za shranjevanje)
        interface OnSrcekClick {
            void naSrcekKlik();
        }

        private final List<Recept> vsiRecepti;     // vsi recepti (original)
        private final List<Recept> prikazani;       // trenutno prikazani (filtrirani)
        private final OnReceptClick poslusalec;

        ReceptAdapter(List<Recept> recepti, OnReceptClick poslusalec) {
            this.vsiRecepti = recepti;
            this.prikazani = new ArrayList<>(recepti); // na začetku prikažemo vse
            this.poslusalec = poslusalec;
        }

        // Doda recept v oba seznama
        public void dodaj(Recept recept) {
            vsiRecepti.add(recept);
            prikazani.add(recept);
            notifyItemInserted(prikazani.size() - 1);
        }

        // Odstrani recept (po poziciji v prikazanem seznamu)
        public void odstrani(int pozicija) {
            if (pozicija >= 0 && pozicija < prikazani.size()) {
                Recept r = prikazani.get(pozicija);
                vsiRecepti.remove(r);       // odstranimo iz originala
                prikazani.remove(pozicija); // in iz prikazanih
                notifyItemRemoved(pozicija);
            }
        }

        // Filtrira recepte glede na iskalni niz
        public void filtriraj(String poizvedba) {
            prikazani.clear();
            String q = poizvedba.toLowerCase().trim();

            if (q.isEmpty()) {
                prikazani.addAll(vsiRecepti); // prazno iskanje = pokaži vse
            } else {
                for (Recept r : vsiRecepti) {
                    if (r.naslov.toLowerCase().contains(q)) {
                        prikazani.add(r);
                    }
                }
            }
            notifyDataSetChanged(); // prerišemo cel seznam
        }

        // Pokaže samo priljubljene (ali spet vse)
        public void prikaziPriljubljene(boolean samoPriljubljeni) {
            prikazani.clear();
            if (samoPriljubljeni) {
                for (Recept r : vsiRecepti) {
                    if (r.priljubljen) {
                        prikazani.add(r);
                    }
                }
            } else {
                prikazani.addAll(vsiRecepti);
            }
            notifyDataSetChanged();
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
            holder.cas.setText("🕐 " + r.cas);
            holder.opis.setText(r.opis);
            holder.itemView.setOnClickListener(v -> poslusalec.naReceptKlik(r));

            // Srček: prikažemo poln/prazen glede na stanje
            holder.srcek.setImageResource(r.priljubljen
                    ? android.R.drawable.btn_star_big_on
                    : android.R.drawable.btn_star_big_off);

            // Klik na srček preklopi priljubljenost
            holder.srcek.setOnClickListener(v -> {
                r.priljubljen = !r.priljubljen;
                notifyItemChanged(position); // osvežimo to vrstico
                if (poslusalec instanceof OnSrcekClick) {
                    ((OnSrcekClick) poslusalec).naSrcekKlik(); // sporočimo, da shranimo
                }
            });
        }

        @Override
        public int getItemCount() {
            return prikazani.size();
        }

        static class ReceptViewHolder extends RecyclerView.ViewHolder {
            TextView naslov, cas, opis;
            ImageView slika, srcek;

            ReceptViewHolder(@NonNull View itemView) {
                super(itemView);
                naslov = itemView.findViewById(R.id.naslovRecepta);
                cas = itemView.findViewById(R.id.casRecepta);
                opis = itemView.findViewById(R.id.opisRecepta);
                slika = itemView.findViewById(R.id.slikaRecepta);
                srcek = itemView.findViewById(R.id.srcek);
            }
        }
    }
}