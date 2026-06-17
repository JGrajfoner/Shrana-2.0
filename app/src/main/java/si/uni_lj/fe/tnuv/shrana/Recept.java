package si.uni_lj.fe.tnuv.shrana;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Recept implements Serializable {
    public String id;            // unikatni identifikator recepta
    public String naslov;
    public int casPriprave; // v minutah
    public int casKuhanja;  // v minutah
    public int kalorije;
    public String opis;
    public List<Sestavina> sestavine;
    public String priprava;
    public List<String> oznake;
    public boolean priljubljen;
    public String slikaUri; // URI slike kot niz

    public static class Sestavina implements Serializable {
        public String ime;
        public double kolicina;
        public String enota;

        public Sestavina(String ime, double kolicina, String enota) {
            this.ime = ime;
            this.kolicina = kolicina;
            this.enota = enota;
        }

        @Override
        public String toString() {
            return kolicina + " " + enota + " " + ime;
        }
    }

    public Recept() {
        this.id = UUID.randomUUID().toString();
        this.sestavine = new ArrayList<>();
        this.oznake = new ArrayList<>();
    }

    public Recept(String naslov, int casPriprave, int casKuhanja, int kalorije, String opis,
                  List<Sestavina> sestavine, String priprava, List<String> oznake) {
        this(naslov, casPriprave, casKuhanja, kalorije, opis, sestavine, priprava, oznake, null);
    }

    public Recept(String naslov, int casPriprave, int casKuhanja, int kalorije, String opis,
                  List<Sestavina> sestavine, String priprava, List<String> oznake, String slikaUri) {
        this.id = UUID.randomUUID().toString();
        this.naslov = naslov;
        this.casPriprave = casPriprave;
        this.casKuhanja = casKuhanja;
        this.kalorije = kalorije;
        this.opis = opis;
        this.sestavine = sestavine;
        this.priprava = priprava;
        this.oznake = oznake;
        this.slikaUri = slikaUri;
        this.priljubljen = false;
    }

    public int getSkupniCas() {
        return casPriprave + casKuhanja;
    }

    // Kopira vsa polja iz drugega recepta, vendar OHRANI lasten id.
    // Tako lahko posodobimo obstoječi objekt na mestu, ne da bi ga zamenjali.
    public void prepisiIz(Recept drugi) {
        this.naslov = drugi.naslov;
        this.casPriprave = drugi.casPriprave;
        this.casKuhanja = drugi.casKuhanja;
        this.kalorije = drugi.kalorije;
        this.opis = drugi.opis;
        this.sestavine = drugi.sestavine;
        this.priprava = drugi.priprava;
        this.oznake = drugi.oznake;
        this.slikaUri = drugi.slikaUri;
        this.priljubljen = drugi.priljubljen;
    }
}