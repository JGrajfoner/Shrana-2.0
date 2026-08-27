package si.uni_lj.fe.tnuv.shrana;

import android.content.Context;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

// skupna shramba receptov + trajno shranjevanje preko JsonShramba.
public class RepozitorijReceptov {

    private static final String IME_DATOTEKE = "shramba_receptov";
    private static final String KLJUC = "recepti";

    private static final List<Recept> recepti = new ArrayList<>();
    private static boolean nalozeno = false;

    // Vrne deljeni seznam (oba zaslona dobita isti objekt)
    public static List<Recept> getRecepti() {
        return recepti;
    }

    // prebere recepte z diska v seznam, kliče se ob zagonu.
    public static void nalozi(Context context) {
        if (nalozeno) return; // dovolj je enkrat

        Type tip = new TypeToken<ArrayList<Recept>>() {}.getType();
        List<Recept> shranjeni =
                JsonShramba.nalozi(context, IME_DATOTEKE, KLJUC, tip);
        if (shranjeni != null) {
            recepti.clear();
            recepti.addAll(shranjeni);
        }
        nalozeno = true;
    }

    // zapiše trenutni seznam na disk kot JSON, kliče se po vsaki spremembi.
    public static void shrani(Context context) {
        JsonShramba.shrani(context, IME_DATOTEKE, KLJUC, recepti);
    }
}