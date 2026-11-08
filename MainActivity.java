package dz.telecommandes;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    // Palette AMOLED : noir pur, touches gris très sombre, accent ambre.
    static final int BG = Color.BLACK;
    static final int KEY = 0xFF17171B;
    static final int LINE = 0xFF2A2A31;
    static final int FG = 0xFFEDEDED;
    static final int MUTED = 0xFF8A8A93;
    static final int ACCENT = 0xFFF2A20C;
    static final int DANGER = 0xFFE5493D;

    static final String[] TAB_NAMES = {"Télé", "Décodeur", "Clim", "Lumière"};

    Ir ir;
    Vibrator vib;
    SharedPreferences prefs;
    LinearLayout content;
    Button[] tabs = new Button[4];
    int tab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ir = new Ir(this);
        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        prefs = getSharedPreferences("telecommandes", MODE_PRIVATE);
        tab = prefs.getInt("tab", 0);
        if (tab < 0 || tab > 3) tab = 0;
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(12), dp(12), dp(12), dp(8));

        TextView title = new TextView(this);
        title.setText("Mes télécommandes");
        title.setTextColor(FG);
        title.setTextSize(22);
        title.setPadding(dp(4), dp(4), dp(4), dp(8));
        root.addView(title);

        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            Button b = new Button(this);
            b.setText(TAB_NAMES[i]);
            b.setAllCaps(false);
            b.setTextSize(13);
            b.setStateListAnimator(null);
            b.setMinHeight(0);
            b.setMinimumHeight(0);
            b.setOnClickListener(v -> {
                tab = idx;
                prefs.edit().putInt("tab", tab).apply();
                render();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(44), 1f);
            p.setMargins(dp(3), 0, dp(3), 0);
            tabRow.addView(b, p);
            tabs[i] = b;
        }
        root.addView(tabRow);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(8), 0, dp(24));
        scroll.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        render();
    }

    // ---------- Outils d'interface ----------

    int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    void feedback() {
        try {
            if (vib == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                vib.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vib.vibrate(15);
            }
        } catch (Exception ignored) {
        }
    }

    Button key(String label, int fill, int textColor, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(textColor);
        b.setTextSize(15);
        b.setStateListAnimator(null);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(16));
        d.setStroke(dp(1), LINE);
        b.setBackground(d);
        b.setOnClickListener(v -> {
            feedback();
            action.run();
        });
        return b;
    }

    Button key(String label, Runnable action) {
        return key(label, KEY, FG, action);
    }

    View spacer() {
        return new View(this);
    }

    LinearLayout row(View... views) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        for (View v : views) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(52), 1f);
            p.setMargins(dp(4), dp(4), dp(4), dp(4));
            l.addView(v, p);
        }
        content.addView(l, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return l;
    }

    void label(String text) {
        TextView t = new TextView(this);
        t.setText(text.toUpperCase());
        t.setTextColor(MUTED);
        t.setTextSize(11);
        t.setLetterSpacing(0.1f);
        t.setPadding(dp(6), dp(14), dp(6), dp(2));
        content.addView(t);
    }

    void note(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(MUTED);
        t.setTextSize(12);
        t.setPadding(dp(6), dp(6), dp(6), dp(6));
        content.addView(t);
    }

    void render() {
        for (int i = 0; i < 4; i++) {
            GradientDrawable d = new GradientDrawable();
            d.setCornerRadius(dp(14));
            d.setStroke(dp(1), LINE);
            boolean on = i == tab;
            d.setColor(on ? ACCENT : KEY);
            tabs[i].setBackground(d);
            tabs[i].setTextColor(on ? Color.BLACK : MUTED);
        }
        content.removeAllViews();
        if (!ir.available()) {
            note("Aucun émetteur infrarouge détecté sur ce téléphone. Les boutons ne feront rien.");
        }
        switch (tab) {
            case 0: viewTv(); break;
            case 1: viewDecoder(); break;
            case 2: viewAc(); break;
            default: viewLed(); break;
        }
    }

    void digits(java.util.function.IntConsumer send) {
        label("Chiffres");
        int[] order = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int r = 0; r < 3; r++) {
            View[] v = new View[3];
            for (int c = 0; c < 3; c++) {
                final int n = order[r * 3 + c];
                v[c] = key(String.valueOf(n), () -> send.accept(n));
            }
            row(v);
        }
        row(spacer(), key("0", () -> send.accept(0)), spacer());
    }

    // ---------- Télé : Samsung32, adresse 0x07 (codes de la base publique) ----------

    Runnable sam(int cmd) {
        return () -> ir.samsung(0x07, cmd);
    }

    void viewTv() {
        row(key("Marche", DANGER, Color.WHITE, sam(0x02)), key("Source", sam(0x01)), key("Muet", sam(0x0F)));
        label("Navigation");
        row(spacer(), key("▲", sam(0x60)), spacer());
        row(key("◀", sam(0x65)), key("OK", ACCENT, Color.BLACK, sam(0x68)), key("▶", sam(0x62)));
        row(spacer(), key("▼", sam(0x61)), spacer());
        row(key("Retour", sam(0x58)), key("Accueil", sam(0x79)), key("Menu", sam(0x1A)));
        row(key("Info", sam(0x1F)), key("Quitter", sam(0x2D)));
        label("Volume");
        row(key("Vol −", sam(0x0B)), key("Vol +", sam(0x07)));
        label("Chaîne");
        row(key("Chaîne −", sam(0x10)), key("Chaîne +", sam(0x12)));
        final int[] samDigit = {0x11, 0x04, 0x05, 0x06, 0x08, 0x09, 0x0A, 0x0C, 0x0D, 0x0E};
        digits(n -> ir.samsung(0x07, samDigit[n]));
    }

    // ---------- Décodeur : profil « Géant SAT » (NEC étendu DE 20), à calibrer ----------

    Runnable dec(int cmd) {
        return () -> ir.necExt(0xDE, 0x20, cmd, ~cmd);
    }

    void viewDecoder() {
        note("Profil Géant SAT. Les codes exacts du Christor TH500 ne sont dans aucune base publique : si des touches ne répondent pas, il faudra les corriger.");
        row(key("Marche", DANGER, Color.WHITE, dec(0xAF)), key("Menu", dec(0x97)), key("Quitter", dec(0x07)));
        row(key("OK", ACCENT, Color.BLACK, dec(0x4F)), key("Guide", dec(0x17)), key("Fav", dec(0xD7)));
        row(key("Rappel", dec(0x47)), key("Info", dec(0xFD)), key("Texte", dec(0xDD)));
        row(key("USB", dec(0x2D)), key("Heure", dec(0xBD)), key("Mode", dec(0x7D)));
        label("Touches de couleur");
        row(key("Jaune", 0xFF3A3000, 0xFFFFD84D, dec(0x55)),
                key("Vert", 0xFF0B2A12, 0xFF6FE08A, dec(0xB5)),
                key("Bleu", 0xFF0B1A3A, 0xFF7FA8FF, dec(0x95)));
        final int[] decDigit = {0xFF, 0x7F, 0xBF, 0x3F, 0xDF, 0x5F, 0x9F, 0x1F, 0xEF, 0x6F};
        digits(n -> ir.necExt(0xDE, 0x20, decDigit[n], ~decDigit[n]));
    }

    // ---------- Clim : captures brutes Gree KFR-70G/A1, à calibrer ----------

    void viewAc() {
        note("Profil Gree (8 commandes enregistrées). Les codes de la Maxwell GL-MX-9CF ne sont dans aucune base publique : si la clim ne répond pas, il faudra changer de profil.");
        label("Commandes");
        String[] names = AcData.NAMES;
        for (int i = 0; i < names.length; i += 2) {
            final int a = i;
            Button k1 = key(frName(names[a]), a == 0 ? DANGER : KEY, a == 0 ? Color.WHITE : FG,
                    () -> ir.raw(AcData.CODES[a]));
            if (a + 1 < names.length) {
                final int b = a + 1;
                row(k1, key(frName(names[b]), () -> ir.raw(AcData.CODES[b])));
            } else {
                row(k1, spacer());
            }
        }
    }

    String frName(String n) {
        return n.replace("Off", "Arrêt")
                .replace("Cool", "Froid")
                .replace("Heat", "Chaud")
                .replace("Dry", "Déshum.")
                .replace("Fan", "Ventilation");
    }

    // ---------- Lumière RGB : télécommande 44 touches (NEC, adresse 0x00) ----------

    Runnable led(int cmd) {
        return () -> ir.nec(0x00, cmd);
    }

    void viewLed() {
        note("Pour une télécommande infrarouge à 44 touches. Si ta lumière fonctionne par radio (RF), le téléphone ne peut pas la commander. Les teintes affichées sont approximatives.");
        row(key("Marche / Arrêt", DANGER, Color.WHITE, led(0x40)), key("Lecture", led(0x41)));
        label("Luminosité");
        row(key("Moins", led(0x5D)), key("Plus", led(0x5C)));
        label("Couleurs");
        int[][] c = {
                {0x58, 0xFFFF2D2D}, {0x59, 0xFF2ECC40}, {0x45, 0xFF2F6FFF}, {0x44, 0xFFFFFFFF},
                {0x54, 0xFFFF7A00}, {0x55, 0xFFA6E22E}, {0x49, 0xFF8A2BE2}, {0x48, 0xFFFF69B4},
                {0x50, 0xFFFF9F1C}, {0x51, 0xFF2ECCC0}, {0x4D, 0xFFA020F0}, {0x4C, 0xFFFF8DA1},
                {0x1C, 0xFFFFB347}, {0x1D, 0xFF1EA7E1}, {0x1E, 0xFFB57EDC}, {0x1F, 0xFF20B2AA},
                {0x18, 0xFFFFE600}, {0x19, 0xFF008B8B}, {0x1A, 0xFFFF4FA3}, {0x1B, 0xFF89CFF0}};
        for (int r = 0; r < c.length / 4; r++) {
            View[] v = new View[4];
            for (int k = 0; k < 4; k++) {
                final int code = c[r * 4 + k][0];
                int col = c[r * 4 + k][1];
                boolean light = (Color.red(col) + Color.green(col) + Color.blue(col)) > 480;
                v[k] = key("", col, light ? Color.BLACK : Color.WHITE, led(code));
            }
            row(v);
        }
        label("Effets");
        row(key("Flash", led(0x0B)), key("Fondu", led(0x07)), key("Saut", led(0x04)), key("Auto", led(0x0F)));
        row(key("Plus vite", led(0x17)), key("Plus lent", led(0x13)));
    }
}
