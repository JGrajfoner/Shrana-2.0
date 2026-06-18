package si.uni_lj.fe.tnuv.shrana;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class CasovnikiActivity extends AppCompatActivity
        implements RepozitorijCasovnikov.Poslusalec {

    private List<RepozitorijCasovnikov.Casovnik> casovniki;
    private CasovnikAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_casovniki);

        casovniki = RepozitorijCasovnikov.getCasovniki();

        RecyclerView seznam = findViewById(R.id.seznamCasovnikov);
        seznam.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CasovnikAdapter(casovniki);
        seznam.setAdapter(adapter);

        findViewById(R.id.gumbDodaj).setOnClickListener(v -> odpriDialogZaNovCasovnik());

        nastaviNavigacijo();
    }

    // Ko je zaslon viden, se naročimo na posodobitve in osvežimo prikaz
    @Override
    protected void onResume() {
        super.onResume();
        RepozitorijCasovnikov.nastaviPoslusalca(this, this);
        adapter.notifyDataSetChanged(); // pokažemo trenutno stanje
        posodobiPraznoStanje();
    }

    // Ko zaslon ni več viden, se odjavimo (a časovniki tečejo naprej v repozitoriju)
    @Override
    protected void onPause() {
        super.onPause();
        RepozitorijCasovnikov.odjaviPoslusalca();
    }

    // Pokaže navodilo, če ni časovnikov; sicer ga skrije
    private void posodobiPraznoStanje() {
        View navodilo = findViewById(R.id.praznoNavodilo);
        if (casovniki.isEmpty()) {
            navodilo.setVisibility(View.VISIBLE);
        } else {
            navodilo.setVisibility(View.GONE);
        }
    }

    // Repozitorij nas kliče ob vsakem tiku; osvežimo cel seznam
    @Override
    public void osveziPrikaz() {
        runOnUiThread(() -> {
            adapter.notifyDataSetChanged();
            posodobiPraznoStanje();
        });
    }

    private void odpriDialogZaNovCasovnik() {
        // Najprej izbira ure in minut prek urnega izbirnika (24-urni način).
        // Sekunde se ne vnašajo, a odštevanje kasneje teče sekundo za sekundo.
        TimePickerDialog izbirnikCasa = new TimePickerDialog(
                this,
                (view, ure, minute) -> {
                    long milis = (ure * 3600L + minute * 60L) * 1000L;
                    if (milis <= 0) {
                        Toast.makeText(this, "Nastavi čas, večji od 0",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Ko je čas izbran, vprašamo še za opis.
                    vprasajZaOpisInDodaj(milis);
                },
                0, 0, true // začetni čas 00:00, true = 24-urni način
        );
        izbirnikCasa.setTitle("Nastavi trajanje");
        izbirnikCasa.show();
    }

    // Kratek dialog z enim poljem za opis časovnika.
    private void vprasajZaOpisInDodaj(long milis) {
        final EditText vnosOpis = new EditText(this);
        vnosOpis.setHint("Opis (npr. Krompir)");
        vnosOpis.setSingleLine(true);

        // Malce odmika, da polje ni nalepljeno na rob dialoga.
        int rob = (int) (24 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout ovoj = new android.widget.FrameLayout(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = rob;
        lp.rightMargin = rob;
        ovoj.addView(vnosOpis, lp);

        new AlertDialog.Builder(this)
                .setTitle("Opis časovnika")
                .setView(ovoj)
                .setPositiveButton("Dodaj", (dialog, kateri) -> {
                    String opis = vnosOpis.getText().toString().trim();
                    RepozitorijCasovnikov.dodaj(milis, opis);
                    adapter.notifyItemInserted(casovniki.size() - 1);
                    posodobiPraznoStanje();
                })
                .setNegativeButton("Prekliči", null)
                .show();
    }

    private void nastaviNavigacijo() {
        BottomNavigationView navigacija = findViewById(R.id.spodnjaNavigacija);
        navigacija.setSelectedItemId(R.id.nav_casovnik);
        navigacija.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_casovnik) {
                return true;
            } else if (id == R.id.nav_recepti) {
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

    // ===== Adapter za časovnike =====
    static class CasovnikAdapter extends RecyclerView.Adapter<CasovnikAdapter.CasovnikViewHolder> {

        private final List<RepozitorijCasovnikov.Casovnik> casovniki;

        CasovnikAdapter(List<RepozitorijCasovnikov.Casovnik> casovniki) {
            this.casovniki = casovniki;
        }

        @NonNull
        @Override
        public CasovnikViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View pogled = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_casovnik, parent, false);
            return new CasovnikViewHolder(pogled);
        }

        @Override
        public void onBindViewHolder(@NonNull CasovnikViewHolder holder, int position) {
            RepozitorijCasovnikov.Casovnik c = casovniki.get(position);

            holder.opis.setText(c.opis);
            holder.prikazCasa.setText(formatCas(c.preostaloMilis));

            // Ikona pavze/predvajaj glede na stanje
            holder.gumbPavza.setImageResource(c.tece
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play);

            holder.gumbPavza.setOnClickListener(v ->
                    RepozitorijCasovnikov.preklopiPavzo(c));

            holder.gumbOdstrani.setOnClickListener(v -> {
                int poz = holder.getAdapterPosition();
                RepozitorijCasovnikov.odstrani(poz);
                notifyItemRemoved(poz);
            });
        }

        private String formatCas(long milis) {
            long sekunde = milis / 1000;
            long ure = sekunde / 3600;
            long minute = (sekunde % 3600) / 60;
            long sek = sekunde % 60;
            return String.format("%02d:%02d:%02d", ure, minute, sek);
        }

        @Override
        public int getItemCount() {
            return casovniki.size();
        }

        static class CasovnikViewHolder extends RecyclerView.ViewHolder {
            TextView prikazCasa, opis;
            ImageButton gumbPavza, gumbOdstrani;

            CasovnikViewHolder(@NonNull View itemView) {
                super(itemView);
                prikazCasa = itemView.findViewById(R.id.prikazCasa);
                opis = itemView.findViewById(R.id.opisCasovnika);
                gumbPavza = itemView.findViewById(R.id.gumbPavza);
                gumbOdstrani = itemView.findViewById(R.id.gumbOdstrani);
            }
        }
    }
}