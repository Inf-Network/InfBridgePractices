/*
 * Decompiled with CFR 0.152.
 */
package net.infnetwork.snowball.bridgingskin.data;

import java.util.LinkedHashSet;
import net.infnetwork.snowball.bridgingskin.data.SkinSet;
import com.google.gson.annotations.SerializedName;

public class PlayerSkin {
    @SerializedName(value="uuid")
    public final String uuid;
    public String player;
    @SerializedName(value="currentSelected")
    public SkinSet currentSkin;
    @SerializedName(value="allSkins")
    public final LinkedHashSet<SkinSet> allSkin;

    public PlayerSkin(String player, String uuid, SkinSet current, LinkedHashSet<SkinSet> all) {
        this.uuid = uuid;
        this.player = player;
        this.currentSkin = current;
        this.allSkin = all;
    }

    public PlayerSkin(String player, String uuid) {
        this.uuid = uuid;
        this.player = player;
        this.currentSkin = new SkinSet();
        this.allSkin = new LinkedHashSet();
        this.allSkin.add(this.currentSkin);
    }

    public PlayerSkin() {
        this.uuid = "#NULL";
        this.player = "#NULL";
        this.currentSkin = new SkinSet();
        this.allSkin = new LinkedHashSet();
        this.allSkin.add(this.currentSkin);
    }
}

