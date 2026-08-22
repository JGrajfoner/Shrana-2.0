package si.uni_lj.fe.tnuv.shrana;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

public class RepozitorijCasovnikov {

    public interface Poslusalec {
        void osveziPrikaz();
    }

    public interface CasovnikiObvestilo {
        void prikaziObvestilo(String opis);
    }

    private static final List<Casovnik> casovniki = new ArrayList<>();
    private static Poslusalec poslusalec;
    private static CasovnikiObvestilo obvestiloPoslusalec;
    private static Context appContext;
    private static Ringtone trenutniRingtone;
    
    // Globalno sledenje aktivni dejavnosti
    private static Activity aktivnaAktivnost = null;
    
    public static final String CHANNEL_ID = "TimerChannel_v2";
    public static final int NOTIFICATION_ID = 1001;
    public static NotificationManager notificationManager;

    public static List<Casovnik> getCasovniki() {
        return casovniki;
    }

    public static void nastaviPoslusalca(Poslusalec p, Context context) {
        poslusalec = p;
        if (context != null) {
            appContext = context.getApplicationContext();
            ustvariNotificationChannel();
        }
    }

    public static void odjaviPoslusalca() {
        poslusalec = null;
    }

    public static void nastaviCasovnikiVOspredju(boolean ospredje, CasovnikiObvestilo p) {
        // Ta metoda se zdaj uporablja predvsem za specifično obnašanje CasovnikiActivity
        obvestiloPoslusalec = p;
    }

    public static void inicializirajObvestila(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
            ustvariNotificationChannel();
            
            // Registracija sledenja dejavnostim za celotno aplikacijo
            if (context.getApplicationContext() instanceof Application) {
                ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                    @Override public void onActivityResumed(@NonNull Activity activity) { aktivnaAktivnost = activity; }
                    @Override public void onActivityPaused(@NonNull Activity activity) { if (aktivnaAktivnost == activity) aktivnaAktivnost = null; }
                    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
                    @Override public void onActivityStarted(@NonNull Activity activity) {}
                    @Override public void onActivityStopped(@NonNull Activity activity) {}
                    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
                    @Override public void onActivityDestroyed(@NonNull Activity activity) {}
                });
            }
        }
    }

    public static void dodaj(long milis, String opis) {
        Casovnik c = new Casovnik(milis, opis);
        casovniki.add(c);
        zazeni(c);
    }

    public static void odstrani(int pozicija) {
        if (pozicija >= 0 && pozicija < casovniki.size()) {
            casovniki.get(pozicija).ustavi();
            casovniki.remove(pozicija);
        }
    }

    public static void preklopiPavzo(Casovnik c) {
        if (c.tece) {
            c.ustavi();
        } else if (c.preostaloMilis > 0) {
            zazeni(c);
        }
        if (poslusalec != null) {
            poslusalec.osveziPrikaz();
        }
    }

    private static void zazeni(Casovnik c) {
        c.tece = true;
        c.timer = new CountDownTimer(c.preostaloMilis, 1000) {
            @Override
            public void onTick(long preostalo) {
                c.preostaloMilis = preostalo;
                if (poslusalec != null) {
                    poslusalec.osveziPrikaz();
                }
            }

            @Override
            public void onFinish() {
                c.preostaloMilis = 0;
                c.tece = false;
                sproziAlarm(c.opis);
                if (poslusalec != null) {
                    poslusalec.osveziPrikaz();
                }
            }
        }.start();
    }

    private static void sproziAlarm(String opis) {
        predvajajZvok();

        // Če je katera koli dejavnost v ospredju, prikažemo dialog neposredno v njej
        if (aktivnaAktivnost != null) {
            prikaziDialogVTopAktivnosti(opis);
        } else {
            // Le če je aplikacija v ozadju, pošljemo notifikacijo
            prikaziNotification(opis);
        }
    }

    private static void prikaziDialogVTopAktivnosti(String opis) {
        if (aktivnaAktivnost == null) return;
        
        aktivnaAktivnost.runOnUiThread(() -> {
            new AlertDialog.Builder(aktivnaAktivnost)
                    .setTitle("⏰ Časovnik je potekel!")
                    .setMessage("Časovnik: " + opis)
                    .setPositiveButton("Prekini", (dialog, which) -> {
                        prekiniAlarm();
                    })
                    .setCancelable(false)
                    .show();
        });
    }

    private static void predvajajZvok() {
        if (appContext == null) return;
        try {
            Uri zvok = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (zvok == null) {
                zvok = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            trenutniRingtone = RingtoneManager.getRingtone(appContext, zvok);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                trenutniRingtone.setLooping(true);
            }
            trenutniRingtone.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void prekiniAlarm() {
        if (trenutniRingtone != null && trenutniRingtone.isPlaying()) {
            trenutniRingtone.stop();
        }
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private static void ustvariNotificationChannel() {
        if (notificationManager == null && appContext != null) {
            notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alarm za časovnik",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Obvestila za iztekajoče časovnike");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 500, 500});
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            
            notificationManager.createNotificationChannel(channel);
        }
    }

    private static void prikaziNotification(String opis) {
        if (appContext == null) return;
        
        Intent intent = new Intent(appContext, CasovnikiActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("show_popup", true);
        intent.putExtra("popup_text", opis);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(appContext, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(appContext, AlarmReceiver.class);
        stopIntent.setAction("STOP_ALARM");
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(appContext, 0, stopIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.timer_24px)
                .setContentTitle("Časovnik je potekel!")
                .setContentText("Časovnik [" + opis + "] je potekel.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.timer_24px, "Prekini", stopPendingIntent)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

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