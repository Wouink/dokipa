package io.github.wouink.dokipa.server;

import net.minecraft.nbt.CompoundTag;

public class MemorizedLocation {
    private String description;
    private LocalizedBlockPos localizedBlockPos;

    public MemorizedLocation(CompoundTag tag) {
        if(tag.contains("Description")) {
            this.description = tag.getString("Description");
        }
        if(tag.contains("Pos")) {
            this.localizedBlockPos = new LocalizedBlockPos(tag.getCompound("Pos"));
        }
    }

    public MemorizedLocation(String description, LocalizedBlockPos localizedBlockPos) {
        this.description = description;
        this.localizedBlockPos = localizedBlockPos;
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putString("Description", this.description);
        CompoundTag posTag = new CompoundTag();
        this.localizedBlockPos.save(posTag);
        tag.put("Pos", posTag);
        return tag;
    }
}
