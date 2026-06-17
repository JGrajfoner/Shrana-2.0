package si.uni_lj.fe.tnuv.shrana;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;

// Skupna pomožni razred za branje/pisanje objektov v SharedPreferences kot JSON.
public class JsonShramba {

    private static final Gson GSON = new Gson();

    // Prebere objekt tipa "tip" iz datoteke pod ključem.
    // Vrne null, če zapisa ni (oz. ga Gson ne uspe prebrati).
    public static <T> T nalozi(Context context, String imeDatoteke,
                               String kljuc, Type tip) {
        SharedPreferences prefs =
                context.getSharedPreferences(imeDatoteke, Context.MODE_PRIVATE);
        String json = prefs.getString(kljuc, null);
        if (json == null) return null;
        try {
            return GSON.fromJson(json, tip);
        } catch (JsonSyntaxException e) {
            Log.e("JsonShramba", "Napaka pri branju JSON: " + e.getMessage());
            return null;
        }
    }

    // Zapiše objekt v datoteko pod ključem kot JSON.
    public static void shrani(Context context, String imeDatoteke,
                              String kljuc, Object objekt) {
        SharedPreferences prefs =
                context.getSharedPreferences(imeDatoteke, Context.MODE_PRIVATE);
        String json = GSON.toJson(objekt);
        prefs.edit().putString(kljuc, json).apply();
    }
}