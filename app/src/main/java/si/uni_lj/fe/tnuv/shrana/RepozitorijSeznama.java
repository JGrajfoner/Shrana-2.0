package si.uni_lj.fe.tnuv.shrana;

import android.content.Context;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RepozitorijSeznama {

    private static final String IME_DATOTEKE = "shramba_seznam";
    private static final String KLJUC = "postavke";

    private static final List<PostavkaSeznama> postavke = new ArrayList<>();
    private static boolean nalozeno = false;

    public static List<PostavkaSeznama> getPostavke() {
        return postavke;
    }

    public static void nalozi(Context context) {
        if (nalozeno) return;

        Type tip = new TypeToken<ArrayList<PostavkaSeznama>>() {}.getType();
        List<PostavkaSeznama> shranjene =
                JsonShramba.nalozi(context, IME_DATOTEKE, KLJUC, tip);
        if (shranjene != null) {
            postavke.clear();
            postavke.addAll(shranjene);
        }
        nalozeno = true;
    }

    public static void shrani(Context context) {
        JsonShramba.shrani(context, IME_DATOTEKE, KLJUC, postavke);
    }
}