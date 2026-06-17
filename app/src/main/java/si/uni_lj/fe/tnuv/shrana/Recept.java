package si.uni_lj.fe.tnuv.shrana;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Serializable omogoča, da cel objekt Recept pošljemo med zasloni preko Intenta.
public class Recept implements Serializable {
    String naslov;
    String cas;
    String opis;
    List<String> sestavine;
    List<String> koraki;
    boolean priljubljen;

    // Prazen konstruktor, ki ga potrebuje Gson pri branju iz JSON
    public Recept() {
        this.sestavine = new ArrayList<>();
        this.koraki = new ArrayList<>();
    }

    public Recept(String naslov, String cas, String opis,
                  List<String> sestavine, List<String> koraki) {
        this.naslov = naslov;
        this.cas = cas;
        this.opis = opis;
        this.sestavine = sestavine;
        this.koraki = koraki;
        this.priljubljen = false;
    }

    // Pomožni konstruktor, ko sestavin/korakov (še) nimamo — ustvari prazna seznama.
    public Recept(String naslov, String cas, String opis) {
        this(naslov, cas, opis, new ArrayList<>(), new ArrayList<>());
    }
}