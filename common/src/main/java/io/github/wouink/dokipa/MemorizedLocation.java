package io.github.wouink.dokipa;

import io.github.wouink.dokipa.server.LocalizedBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

public class MemorizedLocation {
    private String description;
    private LocalizedBlockPos localizedBlockPos;
    private Direction facing;

    public MemorizedLocation(CompoundTag tag) {
        if(tag.contains("Description")) {
            this.description = tag.getString("Description");
        }
        if(tag.contains("Pos")) {
            this.localizedBlockPos = new LocalizedBlockPos(tag.getCompound("Pos"));
        }
        if(tag.contains("Facing")) {
            this.facing = Direction.values()[tag.getInt("Facing")];
        }
    }

    public MemorizedLocation(String description, LocalizedBlockPos localizedBlockPos, Direction facing) {
        this.description = description;
        this.localizedBlockPos = localizedBlockPos;
        this.facing = facing;
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putString("Description", this.description);
        CompoundTag posTag = new CompoundTag();
        this.localizedBlockPos.save(posTag);
        tag.put("Pos", posTag);
        tag.putInt("Facing", this.facing.ordinal());
        return tag;
    }

    public LocalizedBlockPos getLoc() {
        return this.localizedBlockPos;
    }

    public String getDescription() {
        return this.description;
    }

    public Direction getFacing() {
        return this.facing;
    }

    @Override
    public String toString() {
        return "MemorizedLocation \"" + description + "\" " + localizedBlockPos;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof MemorizedLocation memLoc) {
            // todo compare facing ?
            return memLoc.localizedBlockPos.equals(localizedBlockPos) && memLoc.description.equals(description);
        }
        return false;
    }
}
