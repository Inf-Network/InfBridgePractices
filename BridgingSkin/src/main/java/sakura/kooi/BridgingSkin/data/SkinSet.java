/*
 * Decompiled with CFR 0.152.
 *
 * 1.21.11 移植:删除了原版的 data 字节字段。
 * 1.13 扁平化后方块变体各自是独立的 Material —— 1.8 的 SANDSTONE:2 现在叫 CUT_SANDSTONE,
 * data 不再承载任何信息。存量皮肤数据需在部署前一次性转换。
 *
 * 老 json 里残留的 "Data" 键会被 gson 忽略,读取不会报错。
 */
package sakura.kooi.BridgingSkin.data;

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
