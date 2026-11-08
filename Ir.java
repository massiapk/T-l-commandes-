package dz.telecommandes;

import android.content.Context;
import android.hardware.ConsumerIrManager;

/** Émetteur infrarouge : encodeurs NEC, NEC étendu, Samsung32 et signaux bruts. */
final class Ir {
    private final ConsumerIrManager mgr;

    Ir(Context c) {
        mgr = (ConsumerIrManager) c.getSystemService(Context.CONSUMER_IR_SERVICE);
    }

    boolean available() {
        return mgr != null && mgr.hasIrEmitter();
    }

    private void tx(int hz, int[] pattern) {
        if (!available()) return;
        try {
            mgr.transmit(hz, pattern);
        } catch (Exception ignored) {
        }
    }

    /** Trame de 4 octets, bits envoyés poids faible en premier. */
    private static int[] frame(int headerMark, int headerSpace, int[] b) {
        int[] p = new int[2 + 64 + 1];
        int i = 0;
        p[i++] = headerMark;
        p[i++] = headerSpace;
        for (int k = 0; k < 4; k++) {
            for (int bit = 0; bit < 8; bit++) {
                p[i++] = 560;
                p[i++] = ((b[k] >> bit) & 1) == 1 ? 1690 : 560;
            }
        }
        p[i] = 560;
        return p;
    }

    /** NEC standard : adresse et commande de 8 bits. */
    void nec(int addr, int cmd) {
        tx(38000, frame(9000, 4500, new int[]{addr & 255, (~addr) & 255, cmd & 255, (~cmd) & 255}));
    }

    /** NEC étendu : adresse de 16 bits, commande de 16 bits (octets dans l'ordre d'envoi). */
    void necExt(int a0, int a1, int c0, int c1) {
        tx(38000, frame(9000, 4500, new int[]{a0 & 255, a1 & 255, c0 & 255, c1 & 255}));
    }

    /** Samsung32 : adresse de 8 bits répétée, commande et son complément. */
    void samsung(int addr, int cmd) {
        tx(38000, frame(4500, 4500, new int[]{addr & 255, addr & 255, cmd & 255, (~cmd) & 255}));
    }

    void raw(int[] pattern) {
        tx(38000, pattern);
    }
}
