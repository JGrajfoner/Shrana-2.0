package si.uni_lj.fe.tnuv.shrana;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class SeznamActivity extends AppCompatActivity {

    private List<PostavkaSeznama> postavke;
    private PostavkaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seznam);

        // Naložimo seznam z diska
        RepozitorijSeznama.nalozi(this);
        postavke = RepozitorijSeznama.getPostavke();

        RecyclerView seznam = findViewById(R.id.seznamPostavk);
        seznam.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PostavkaAdapter(postavke, this);
        seznam.setAdapter(adapter);

        // Ročno dodajanje
        EditText vnos = findViewById(R.id.vnosPostavka);
        Button gumbDodaj = findViewById(R.id.gumbDodaj);
        gumbDodaj.setOnClickListener(v -> {
            String besedilo = vnos.getText().toString().trim();
            if (besedilo.isEmpty()) {
                Toast.makeText(this, "Vpiši postavko", Toast.LENGTH_SHORT).show();
                return;
            }
            postavke.add(new PostavkaSeznama(besedilo));
            adapter.notifyItemInserted(postavke.size() - 1);
            RepozitorijSeznama.shrani(this);
            vnos.setText(""); // počistimo vnosno polje
        });

        // Počisti odkljukane
        Button gumbPocisti = findViewById(R.id.gumbPocisti);
        gumbPocisti.setOnClickListener(v -> {
            // Odstranimo vse postavke, ki so odkljukane
            postavke.removeIf(p -> p.kupljeno);
            adapter.notifyDataSetChanged();
            RepozitorijSeznama.shrani(this);
        });

        nastaviNavigacijo();
    }

    private void nastaviNavigacijo() {
        BottomNavigationView navigacija = findViewById(R.id.spodnjaNavigacija);
        navigacija.setSelectedItemId(R.id.nav_seznam);
        navigacija.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_seznam) {
                return true;
            } else if (id == R.id.nav_recepti) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_koledar) {
                startActivity(new Intent(this, KoledarActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_koledar) {
                startActivity(new Intent(this, KoledarActivity.class));
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

    // ===== Adapter za postavke =====
    static class PostavkaAdapter extends RecyclerView.Adapter<PostavkaAdapter.PostavkaViewHolder> {

        private final List<PostavkaSeznama> postavke;
        private final AppCompatActivity aktivnost; // za shranjevanje ob spremembi

        PostavkaAdapter(List<PostavkaSeznama> postavke, AppCompatActivity aktivnost) {
            this.postavke = postavke;
            this.aktivnost = aktivnost;
        }

        @NonNull
        @Override
        public PostavkaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View pogled = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_postavka, parent, false);
            return new PostavkaViewHolder(pogled);
        }

        @Override
        public void onBindViewHolder(@NonNull PostavkaViewHolder holder, int position) {
            PostavkaSeznama p = postavke.get(position);
            holder.besedilo.setText(p.besedilo);

            // Pomembno: najprej odstranimo poslušalca, da se ne sproži med nastavljanjem
            holder.kljukica.setOnCheckedChangeListener(null);
            holder.kljukica.setChecked(p.kupljeno);
            prikaziPrecrtano(holder.besedilo, p.kupljeno);

            holder.kljukica.setOnCheckedChangeListener((b, jeOznacena) -> {
                p.kupljeno = jeOznacena;
                prikaziPrecrtano(holder.besedilo, jeOznacena);
                RepozitorijSeznama.shrani(aktivnost);
            });
        }

        // Odkljukano postavko prečrtamo
        private void prikaziPrecrtano(TextView pogled, boolean precrtaj) {
            if (precrtaj) {
                pogled.setPaintFlags(pogled.getPaintFlags()
                        | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                pogled.setPaintFlags(pogled.getPaintFlags()
                        & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }

        @Override
        public int getItemCount() {
            return postavke.size();
        }

        static class PostavkaViewHolder extends RecyclerView.ViewHolder {
            CheckBox kljukica;
            TextView besedilo;

            PostavkaViewHolder(@NonNull View itemView) {
                super(itemView);
                kljukica = itemView.findViewById(R.id.kljukica);
                besedilo = itemView.findViewById(R.id.besedilo);
            }
        }
    }
}