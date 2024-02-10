package io.github.wouink.dokipa.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.function.BiConsumer;

public class LocalizedBlockPos {
    private BlockPos pos;
    private ResourceLocation dimension;

    public LocalizedBlockPos(BlockPos pos, ResourceLocation dimension) {
        this.pos = pos;
        this.dimension = dimension;
    }

    public LocalizedBlockPos(BlockPos pos, Level level) {
        this.pos = pos;
        this.dimension = level.dimension().location();
    }

    public LocalizedBlockPos(CompoundTag tag) {
        String dim = tag.getString("L");
        this.pos = Util.loadBlockPos(tag);
        this.dimension = new ResourceLocation(dim);
    }

    public CompoundTag save(CompoundTag tag) {
        Util.saveBlockPos(pos, tag);
        tag.putString("L", dimension.toString());
        return tag;
    }

    public BlockPos getPos() {
        return pos;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    public ResourceLocation getDimension() {
        return dimension;
    }

    public Level getDimension(MinecraftServer server) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
    }

    public void setDimension(ResourceLocation dim) {
        this.dimension = dim;
    }

    public void setDimension(Level level) {
        this.dimension = level.dimension().location();
    }

    public void executeAt(MinecraftServer server, BiConsumer<Level, BlockPos> function) {
        Level level = getDimension(server);
        BlockPos pos = getPos();
        if(level != null && pos != null) function.accept(level, pos);
    }
}
