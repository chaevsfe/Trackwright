package dev.chaevsfe.trackwright.track;

import com.mojang.serialization.Dynamic;
import dev.chaevsfe.trackwright.Trackwright;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.PotDecorationsComponentUnflatteningFix;
import net.minecraft.util.datafix.fixes.References;

import java.nio.file.Path;
import java.util.List;

public final class OldTrainData {
    private static final String[] CARRIED_STATES = {"Material", "Bracket"};
    private static final int NEWEST_OLD_ITEM_VERSION = 4903;

    private final int oldVersion;
    private final int version;
    private int renamed;
    private int upgraded;

    private OldTrainData(int oldVersion, int version) {
        this.oldVersion = oldVersion;
        this.version = version;
    }

    public static void fix(Path path, CompoundTag root, int version) {
        if (root == null || !path.endsWith("data/create/tracks.dat")) {
            return;
        }
        try {
            if (!hasOldPalette(root)) {
                return;
            }
            OldTrainData pass = new OldTrainData(Math.min(NbtUtils.getDataVersion(root, NEWEST_OLD_ITEM_VERSION), NEWEST_OLD_ITEM_VERSION), version);
            pass.fixTrains(root);
            if (pass.renamed > 0 || pass.upgraded > 0) {
                Trackwright.LOGGER.info("Renamed {} old block state keys and upgraded {} old items on saved trains in {}", pass.renamed, pass.upgraded, path);
            }
        } catch (RuntimeException e) {
            Trackwright.LOGGER.error("Could not check saved trains in {}", path, e);
        }
    }

    private static boolean hasOldPalette(CompoundTag root) {
        if (root.get("data") instanceof CompoundTag data && data.get("Trains") instanceof ListTag trains) {
            for (Tag train : trains) {
                if (train instanceof CompoundTag t && t.get("Carriages") instanceof ListTag carriages) {
                    for (Tag carriage : carriages) {
                        if (carriage instanceof CompoundTag c && carriageHasOldPalette(c)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean carriageHasOldPalette(CompoundTag carriage) {
        if (carriage.get("Entity") instanceof CompoundTag entity && entity.get("Contraption") instanceof CompoundTag contraption
            && contraption.get("Blocks") instanceof CompoundTag blocks && blocks.get("Palette") instanceof ListTag palette) {
            for (Tag state : palette) {
                if (state instanceof CompoundTag s && isOldState(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void fixTrains(CompoundTag root) {
        if (root.get("data") instanceof CompoundTag data && data.get("Trains") instanceof ListTag trains) {
            for (Tag train : trains) {
                if (train instanceof CompoundTag t && t.get("Carriages") instanceof ListTag carriages) {
                    for (Tag carriage : carriages) {
                        if (carriage instanceof CompoundTag c) {
                            fixCarriage(c);
                        }
                    }
                }
            }
        }
    }

    private void fixCarriage(CompoundTag carriage) {
        if (carriage.get("Entity") instanceof CompoundTag entity && entity.get("Contraption") instanceof CompoundTag contraption) {
            if (contraption.get("Blocks") instanceof CompoundTag blocks) {
                fixBlocks(blocks);
            }
            fixStacks(contraption.get("Actors"));
            fixStacks(contraption.get("DisabledActors"));
        }
        if (carriage.get("EntityPositioning") instanceof ListTag positions) {
            for (Tag position : positions) {
                if (position instanceof CompoundTag p && p.get("items") instanceof ListTag items) {
                    for (Tag item : items) {
                        if (item instanceof CompoundTag i && i.get("storage") instanceof CompoundTag storage) {
                            fixStacks(storage.get("value"));
                        }
                    }
                }
            }
        }
        fixStacks(carriage.get("Passengers"));
    }

    private void fixBlocks(CompoundTag blocks) {
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
                    fixBlockEntity(b.get("Data"));
                    fixBlockEntity(b.get("UpdateTag"));
                    if (b.get("Data") instanceof CompoundTag data && data.get("id") instanceof StringTag(String id) && id.equals("minecraft:decorated_pot")) {
                        unpackSherds(data);
                        unpackSherds(b.get("UpdateTag"));
                    }
                }
            }
        }
    }

    private void fixBlockEntity(Tag blockEntity) {
        renameCarried(blockEntity);
        fixStacks(blockEntity);
    }

    private void renameCarried(Tag blockEntity) {
        if (blockEntity instanceof CompoundTag be) {
            for (String key : CARRIED_STATES) {
                if (be.get(key) instanceof CompoundTag s && renameState(s)) {
                    renamed++;
                }
            }
        }
    }

    private void unpackSherds(Tag blockEntity) {
        if (blockEntity instanceof CompoundTag be && be.get("sherds") instanceof ListTag sherds) {
            be.put("sherds", PotDecorationsComponentUnflatteningFix.unpackList(new Dynamic<Tag>(NbtOps.INSTANCE, sherds)).getValue());
            upgraded++;
        }
    }

    private void fixStacks(Tag tag) {
        if (tag instanceof ListTag list) {
            for (Tag element : list) {
                fixStacks(element);
            }
        } else if (tag instanceof CompoundTag compound) {
            for (String key : compound.keySet()) {
                if (!key.equals("minecraft:custom_data")) {
                    fixStacks(compound.get(key));
                }
            }
            if (compound.get("id") instanceof StringTag && compound.get("components") instanceof CompoundTag) {
                fixStack(compound);
            }
        }
    }

    private void fixStack(CompoundTag stack) {
        if (stack.get("components") instanceof CompoundTag components && hasOldComponent(components)) {
            upgrade(stack);
        }
        if (stack.get("components") instanceof CompoundTag components) {
            if (components.get("create:shaper_block_used") instanceof CompoundTag used && renameState(used)) {
                renamed++;
            }
            renameCarried(components.get("create:shaper_block_data"));
        }
    }

    private static boolean hasOldComponent(CompoundTag components) {
        return components.get("minecraft:pot_decorations") instanceof ListTag
            || components.contains("minecraft:map_color")
            || components.contains("minecraft:swing_animation")
            || hasOldCarriedState(components.get("minecraft:block_entity_data"));
    }

    private static boolean hasOldCarriedState(Tag blockEntity) {
        if (blockEntity instanceof CompoundTag be) {
            for (String key : CARRIED_STATES) {
                if (be.get(key) instanceof CompoundTag s && isOldState(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void upgrade(CompoundTag stack) {
        try {
            if (DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<Tag>(NbtOps.INSTANCE, stack.copy()), oldVersion, version).getValue() instanceof CompoundTag result
                && !result.equals(stack)) {
                for (String key : List.copyOf(stack.keySet())) {
                    stack.remove(key);
                }
                stack.merge(result);
                upgraded++;
            }
        } catch (RuntimeException e) {
            Trackwright.LOGGER.error("Could not upgrade the old item {} on a saved train", stack, e);
        }
    }

    private static boolean isOldState(CompoundTag state) {
        Tag properties = state.get("Properties");
        return state.get("Name") instanceof StringTag(String name) && name.indexOf(':') >= 0
            && state.size() == (properties == null ? 1 : 2) && (properties == null || properties instanceof CompoundTag);
    }

    private static boolean renameState(CompoundTag state) {
        if (!isOldState(state)) {
            return false;
        }
        state.put("id", state.remove("Name"));
        if (state.contains("Properties")) {
            state.put("properties", state.remove("Properties"));
        }
        return true;
    }
}
