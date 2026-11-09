package dev.chaevsfe.trackwright.track;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import dev.chaevsfe.trackwright.Trackwright;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.fixes.PotDecorationsComponentUnflatteningFix;
import net.minecraft.util.datafix.fixes.References;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class OldWorldData {
    private static final String[] CARRIED_STATES = {"Material", "Bracket"};
    private static final int NAME_TAG_ATTRIBUTE_VERSION = 4888;

    private final DataFixTypes type;
    private final DataFixer fixer;
    private final int from;
    private final int to;
    private int renamed;
    private int upgraded;
    private int contraptions;
    private int refixed;
    private int failures;
    private Throwable firstFailure;

    private OldWorldData(DataFixTypes type, DataFixer fixer, int from, int to) {
        this.type = type;
        this.fixer = fixer;
        this.from = from;
        this.to = to;
    }

    public static Tag unfixed(DataFixTypes type, CompoundTag input, int from, int to) {
        if (from >= to || input == null) {
            return null;
        }
        if (type == DataFixTypes.ENTITY_CHUNK) {
            return input.get("Entities");
        }
        if (type == DataFixTypes.PLAYER && input.get("RootVehicle") instanceof CompoundTag vehicle) {
            return vehicle.get("Entity");
        }
        return null;
    }

    public static void fix(DataFixTypes type, DataFixer fixer, int from, int to, CompoundTag output, Tag unfixed) {
        if (from >= to) {
            return;
        }
        if (output == null || type != DataFixTypes.ENTITY_CHUNK && type != DataFixTypes.CHUNK && type != DataFixTypes.PLAYER) {
            return;
        }
        try {
            new OldWorldData(type, fixer, from, to).run(output, unfixed);
        } catch (RuntimeException | StackOverflowError e) {
            Trackwright.LOGGER.error("Could not upgrade old {} data", type, e);
        }
    }

    private void run(CompoundTag output, Tag unfixed) {
        if (type == DataFixTypes.ENTITY_CHUNK) {
            if (output.get("Entities") instanceof ListTag list) {
                ListTag roots = list == unfixed ? refixRoots(list) : list;
                ListTag fixed = parts(roots, this::entity);
                if (fixed != list) {
                    output.put("Entities", fixed);
                }
            }
        } else if (type == DataFixTypes.CHUNK) {
            if (output.get("block_entities") instanceof ListTag list) {
                ListTag fixed = parts(list, this::blockEntity);
                if (fixed != list) {
                    output.put("block_entities", fixed);
                }
            }
        } else {
            player(output, unfixed);
        }
        report(output);
    }

    private void player(CompoundTag player, Tag unfixed) {
        for (String key : List.copyOf(player.keySet())) {
            if (!key.equals("RootVehicle") && player.get(key) instanceof Tag value) {
                Tag fixed = part(value, this::fixStacks);
                if (fixed != value) {
                    player.put(key, fixed);
                }
            }
        }
        if (player.get("RootVehicle") instanceof CompoundTag vehicle && vehicle.get("Entity") instanceof CompoundTag entity) {
            CompoundTag fixed = entity;
            if (entity == unfixed) {
                ListTag one = new ListTag();
                one.add(entity);
                if (refixRoots(one).get(0) instanceof CompoundTag refixedEntity) {
                    fixed = refixedEntity;
                }
            }
            fixed = part(fixed, this::entity);
            if (fixed != entity) {
                CompoundTag copy = shallowCopy(vehicle);
                copy.put("Entity", fixed);
                player.put("RootVehicle", copy);
            }
        }
    }

    private ListTag refixRoots(ListTag list) {
        Map<String, ?> known;
        try {
            known = knownEntities();
        } catch (RuntimeException | StackOverflowError e) {
            failed(e);
            return list;
        }
        ListTag out = new ListTag();
        for (Tag root : list) {
            out.add(root instanceof CompoundTag entity ? refixRoot(entity, known) : root);
        }
        return out;
    }

    private CompoundTag refixRoot(CompoundTag entity, Map<String, ?> known) {
        try {
            return refixTree(entity, known);
        } catch (RuntimeException | StackOverflowError e) {
            failed(e);
            return entity;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, ?> knownEntities() {
        return (Map<String, ?>) (Map) fixer.getSchema(DataFixUtils.makeKey(from)).findChoiceType(References.ENTITY).types();
    }

    private CompoundTag refixTree(CompoundTag entity, Map<String, ?> known) {
        if (treeKnown(entity, known)) {
            return refixEntity(entity);
        }
        CompoundTag root = shallowCopy(entity);
        Tag passengers = root.remove("Passengers");
        if (isKnown(entity, known)) {
            root = refixEntity(root);
        }
        if (passengers instanceof ListTag list) {
            ListTag fixed = new ListTag();
            for (Tag passenger : list) {
                fixed.add(passenger instanceof CompoundTag p ? refixTree(p, known) : passenger);
            }
            root.put("Passengers", fixed);
        } else if (passengers != null) {
            root.put("Passengers", passengers);
        }
        return root;
    }

    private CompoundTag refixEntity(CompoundTag entity) {
        try {
            if (fixer.update(References.ENTITY_TREE, new Dynamic<Tag>(NbtOps.INSTANCE, entity), from, to).getValue() instanceof CompoundTag fixed) {
                refixed++;
                return fixed;
            }
        } catch (RuntimeException | StackOverflowError e) {
            failed(e);
        }
        return entity;
    }

    private static boolean isKnown(CompoundTag entity, Map<String, ?> known) {
        return entity.get("id") instanceof StringTag(String id) && known.containsKey(id);
    }

    private static boolean treeKnown(CompoundTag entity, Map<String, ?> known) {
        if (!isKnown(entity, known)) {
            return false;
        }
        if (entity.get("Passengers") instanceof ListTag passengers) {
            for (Tag passenger : passengers) {
                if (!(passenger instanceof CompoundTag p) || !treeKnown(p, known)) {
                    return false;
                }
            }
        }
        return true;
    }

    private ListTag parts(ListTag list, Consumer<CompoundTag> fix) {
        ListTag out = null;
        for (int i = 0; i < list.size(); i++) {
            Tag element = list.get(i);
            Tag fixed = element instanceof CompoundTag compound ? part(compound, fix) : element;
            if (fixed != element && out == null) {
                out = new ListTag();
                for (int j = 0; j < i; j++) {
                    out.add(list.get(j));
                }
            }
            if (out != null) {
                out.add(fixed);
            }
        }
        return out == null ? list : out;
    }

    private <T extends Tag> T part(T tag, Consumer<? super T> fix) {
        int renamedBefore = renamed;
        int upgradedBefore = upgraded;
        int contraptionsBefore = contraptions;
        T copy;
        try {
            @SuppressWarnings("unchecked")
            T fresh = (T) tag.copy();
            copy = fresh;
            fix.accept(copy);
        } catch (RuntimeException | StackOverflowError e) {
            renamed = renamedBefore;
            upgraded = upgradedBefore;
            contraptions = contraptionsBefore;
            failed(e);
            return tag;
        }
        return renamed == renamedBefore && upgraded == upgradedBefore ? tag : copy;
    }

    private void entity(CompoundTag entity) {
        for (String key : entity.keySet()) {
            Tag value = entity.get(key);
            if (key.equals("Contraption") && value instanceof CompoundTag contraption) {
                contraption(contraption);
            } else if (key.equals("Passengers") && value instanceof ListTag passengers) {
                for (Tag passenger : passengers) {
                    if (passenger instanceof CompoundTag p) {
                        entity(p);
                    }
                }
            } else {
                fixStacks(value);
            }
        }
    }

    private void blockEntity(CompoundTag blockEntity) {
        fixStacks(blockEntity);
        if (blockEntity.get("FlyingBlocks") instanceof ListTag flying) {
            for (Tag launched : flying) {
                if (launched instanceof CompoundTag l) {
                    if (l.get("BlockState") instanceof CompoundTag state && renameState(state)) {
                        renamed++;
                    }
                    renameCarried(l.get("Data"));
                }
            }
        }
    }

    private void contraption(CompoundTag contraption) {
        int before = renamed + upgraded;
        if (contraption.get("Blocks") instanceof CompoundTag blocks) {
            fixBlocks(blocks);
        }
        fixStacks(contraption.get("Actors"));
        fixStacks(contraption.get("DisabledActors"));
        fixStacks(contraption.get("items"));
        if (renamed + upgraded > before) {
            contraptions++;
        }
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
        if (stack.get("components") instanceof CompoundTag components && hasOldComponent(stack, components)) {
            upgrade(stack);
        }
        if (stack.get("components") instanceof CompoundTag components) {
            if (components.get("create:shaper_block_used") instanceof CompoundTag used && renameState(used)) {
                renamed++;
            }
            renameCarried(components.get("create:shaper_block_data"));
            if (components.get("create:minecart_contraption_data") instanceof CompoundTag contraption) {
                contraption(contraption);
            }
        }
    }

    private boolean hasOldComponent(CompoundTag stack, CompoundTag components) {
        return components.get("minecraft:pot_decorations") instanceof ListTag
            || components.contains("minecraft:map_color")
            || components.contains("minecraft:swing_animation")
            || components.contains("minecraft:block_transformer")
            || hasOldCarriedState(components.get("minecraft:block_entity_data"))
            || isItem(stack, "minecraft:filled_map") && components.contains("minecraft:map_decorations")
            || isItem(stack, "minecraft:map") && components.get("minecraft:item_name") instanceof CompoundTag name
                && name.get("translate") instanceof StringTag(String key) && key.equals("filled_map.buried_treasure")
            || from < NAME_TAG_ATTRIBUTE_VERSION && holds(components.get("minecraft:attribute_modifiers"), "minecraft:nameplate_distance");
    }

    private static boolean isItem(CompoundTag stack, String item) {
        return stack.get("id") instanceof StringTag(String id) && id.equals(item);
    }

    private static boolean holds(Tag tag, String value) {
        if (tag instanceof StringTag(String s)) {
            return s.equals(value);
        }
        if (tag instanceof ListTag list) {
            for (Tag element : list) {
                if (holds(element, value)) {
                    return true;
                }
            }
        } else if (tag instanceof CompoundTag compound) {
            for (String key : compound.keySet()) {
                if (holds(compound.get(key), value)) {
                    return true;
                }
            }
        }
        return false;
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
            if (fixer.update(References.ITEM_STACK, new Dynamic<Tag>(NbtOps.INSTANCE, stack.copy()), from, to).getValue() instanceof CompoundTag result
                && !result.equals(stack)) {
                for (String key : List.copyOf(stack.keySet())) {
                    stack.remove(key);
                }
                stack.merge(result);
                upgraded++;
            }
        } catch (RuntimeException | StackOverflowError e) {
            failed(e);
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

    private static CompoundTag shallowCopy(CompoundTag tag) {
        CompoundTag copy = new CompoundTag();
        for (String key : tag.keySet()) {
            copy.put(key, tag.get(key));
        }
        return copy;
    }

    private void failed(Throwable e) {
        if (failures++ == 0) {
            firstFailure = e;
        }
    }

    private void report(CompoundTag output) {
        if (renamed > 0 || upgraded > 0 || refixed > 0) {
            Trackwright.LOGGER.info("Upgraded old data in {}: {} contraptions, {} block state keys renamed, {} items upgraded, {} entities fixed one by one",
                where(output), contraptions, renamed, upgraded, refixed);
        }
        if (failures > 0) {
            Trackwright.LOGGER.error("Could not upgrade {} parts of the old data in {}, they stay as the game upgraded them", failures, where(output), firstFailure);
        }
    }

    private String where(CompoundTag tag) {
        if (type == DataFixTypes.PLAYER) {
            return tag.getIntArray("UUID").filter(uuid -> uuid.length == 4).map(uuid -> "player " + UUIDUtil.uuidFromIntArray(uuid)).orElse("a player");
        }
        if (type == DataFixTypes.ENTITY_CHUNK) {
            return tag.getIntArray("Position").filter(pos -> pos.length == 2).map(pos -> "entity chunk [" + pos[0] + ", " + pos[1] + "]").orElse("an entity chunk");
        }
        return "chunk [" + tag.getIntOr("xPos", 0) + ", " + tag.getIntOr("zPos", 0) + "]";
    }
}
