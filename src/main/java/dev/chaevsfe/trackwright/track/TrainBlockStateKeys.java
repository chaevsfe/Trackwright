package dev.chaevsfe.trackwright.track;

import dev.chaevsfe.trackwright.Trackwright;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.nio.file.Path;

public final class TrainBlockStateKeys {
    private static final String[] CARRIED_STATES = {"Material", "Bracket"};

    private TrainBlockStateKeys() {
    }

    public static void fix(Path path, CompoundTag root) {
        if (root == null || !path.endsWith("data/create/tracks.dat")) {
            return;
        }
        try {
            int renamed = renameTrains(root);
            if (renamed > 0) {
                Trackwright.LOGGER.info("Renamed {} old block state keys on saved trains in {}", renamed, path);
            }
        } catch (RuntimeException e) {
            Trackwright.LOGGER.error("Could not check saved trains in {}", path, e);
        }
    }

    private static int renameTrains(CompoundTag root) {
        int renamed = 0;
        if (root.get("data") instanceof CompoundTag data && data.get("Trains") instanceof ListTag trains) {
            for (Tag train : trains) {
                if (train instanceof CompoundTag t && t.get("Carriages") instanceof ListTag carriages) {
                    for (Tag carriage : carriages) {
                        if (carriage instanceof CompoundTag c && c.get("Entity") instanceof CompoundTag entity
                            && entity.get("Contraption") instanceof CompoundTag contraption
                            && contraption.get("Blocks") instanceof CompoundTag blocks) {
                            renamed += renameBlocks(blocks);
                        }
                    }
                }
            }
        }
        return renamed;
    }

    private static int renameBlocks(CompoundTag blocks) {
        int renamed = 0;
        if (blocks.get("Palette") instanceof ListTag palette) {
            for (Tag state : palette) {
                if (state instanceof CompoundTag s && renameState(s)) {
                    renamed++;
                }
            }
        }
        if (blocks.get("BlockList") instanceof ListTag blockList) {
            for (Tag block : blockList) {
                if (block instanceof CompoundTag b) {
                    renamed += renameCarried(b.get("Data")) + renameCarried(b.get("UpdateTag"));
                }
            }
        }
        return renamed;
    }

    private static int renameCarried(Tag blockEntity) {
        int renamed = 0;
        if (blockEntity instanceof CompoundTag be) {
            for (String key : CARRIED_STATES) {
                if (be.get(key) instanceof CompoundTag s && renameState(s)) {
                    renamed++;
                }
            }
        }
        return renamed;
    }

    private static boolean renameState(CompoundTag state) {
        Tag properties = state.get("Properties");
        if (!(state.get("Name") instanceof StringTag(String name)) || name.indexOf(':') < 0
            || state.size() != (properties == null ? 1 : 2) || properties != null && !(properties instanceof CompoundTag)) {
            return false;
        }
        state.put("id", state.remove("Name"));
        if (properties != null) {
            state.put("properties", state.remove("Properties"));
        }
        return true;
    }
}
