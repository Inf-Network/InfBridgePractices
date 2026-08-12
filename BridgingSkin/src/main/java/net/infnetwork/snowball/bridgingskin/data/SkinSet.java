package net.infnetwork.snowball.bridgingskin.data;

import com.google.gson.annotations.SerializedName;

public class SkinSet {
    @SerializedName(value="Material")
    public String material;

    public SkinSet(String material) {
        this.material = material;
    }

    /** 默认皮肤:1.8 的「平滑砂岩」(SANDSTONE:2),扁平化后即 CUT_SANDSTONE。 */
    public SkinSet() {
        this("CUT_SANDSTONE");
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SkinSet)) {
            return super.equals(obj);
        }
        return this.material.equals(((SkinSet)obj).material);
    }

    public int hashCode() {
        return this.material.hashCode();
    }
}
