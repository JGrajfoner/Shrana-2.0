package si.uni_lj.fe.tnuv.shrana;

import android.content.Context;
import android.media.RingtoneManager;
import android.os.CountDownTimer;

import java.util.ArrayList;
import java.util.List;

// Skupna shramba časovnikov. Časovniki tečejo tukaj, zato preživijo preklop zavihka.
public class RepozitorijCasovnikov {

    // Poslušalec, preko katerega repozitorij obvesti Activity, da naj osveži prikaz
    public interface Poslusalec {
        void osveziPrikaz();
    }

    private static final List<Casovnik> casovniki = new ArrayList<>();
    private static Poslusalec poslusalec; // trenutno prikazana Activity (ali null)
    private static Context appContext;     // za predvajanje zvoka

    public static List<Casovnik> getCasovniki() {
        return casovniki;
    }

    // Activity se ob prihodu naroči na posodobitve
    public static void nastaviPoslusalca(Poslusalec p, Context context) {
        poslusalec = p;
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    // Activity se ob odhodu odjavi (a časovniki tečejo naprej)
    public static void odjaviPoslusalca() {
        poslusalec = null;
    }

    // Doda nov časovnik in ga takoj zažene
    public static void dodaj(long milis, String opis) {
        Casovnik c = new Casovnik(milis, opis);
        casovniki.add(c);
        zazeni(c);
    }

    // Odstrani časovnik
    public static void odstrani(int pozicija) {
        if (pozicija >= 0 && pozicija < casovniki.size()) {
            casovniki.get(pozicija).ustavi();
            casovniki.remove(pozicija);
        }
    }

    // Pavza / nadaljuj za en časovnik
    public static void preklopiPavzo(Casovnik c) {
        if (c.tece) {
            c.ustavi();
        } else if (c.preostaloMilis > 0) {
            zazeni(c);
        }
        // Po preklopu osvežimo prikaz takoj, da se ikona spremeni
        // pavza <-> nadaljuj. (Ob pavzi se timer ustavi in onTick več ne
        // sproži osvežitve, zato jo moramo sprožiti tukaj.)
        if (poslusalec != null) {
            poslusalec.osveziPrikaz();
        }
    }

    // Zažene odštevanje
    private static void zazeni(Casovnik c) {
        c.tece = true;
        c.timer = new CountDownTimer(c.preostaloMilis, 1000) {
            @Override
            public void onTick(long preostalo) {
                c.preostaloMilis = preostalo;
                // če je kakšna Activity trenutno prikazana, jo obvestimo
                if (poslusalec != null) {
                    poslusalec.osveziPrikaz();
                }
            }

            @Override
            public void onFinish() {
                c.preostaloMilis = 0;
                c.tece = false;
                predvajajZvok();
                if (poslusalec != null) {
                    poslusalec.osveziPrikaz();
                }
            }
        }.start();
    }

    private static void predvajajZvok() {
        if (appContext == null) return;
        try {
            android.net.Uri zvok = RingtoneManager
                    .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            RingtoneManager.getRingtone(appContext, zvok).play();
        } catch (Exception e) {
            // ignoriramo, če zvok ne uspe
        }
    }

    // ===== Razred za en časovnik =====
    public static class Casovnik {
        public long preostaloMilis;
        public String opis;
        public boolean tece;
        CountDownTimer timer;

        Casovnik(long preostaloMilis, String opis) {
            this.preostaloMilis = preostaloMilis;
            this.opis = opis;
            this.tece = false;
        }

        void ustavi() {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            tece = false;
        }
    }
}