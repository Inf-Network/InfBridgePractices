package net.infnetwork.snowball.blocklv.holographicdisplay;

public class Lv {
    public int lv;
    public String name;
    private long preciseLevel;
    private boolean preciseLevelSet;
    private int legacyMirror;

    public Lv(int lv, String name) {
        this.lv = lv;
        this.legacyMirror = lv;
        this.name = name;
    }

    public Lv(long level, String name) {
        this.preciseLevel = level;
        this.preciseLevelSet = true;
        this.lv = level > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : level < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) level;
        this.legacyMirror = this.lv;
        this.name = name;
    }

    public Lv() {
    }

    public long level() {
        if (!preciseLevelSet || lv != legacyMirror) {
            return lv;
        }
        return preciseLevel;
    }
}
