package dev.chaevsfe.trackwright.track;

import com.zurrtum.create.AllBlockTags;
import com.zurrtum.create.AllItemTags;
import com.zurrtum.create.content.trains.track.TrackBlockItem;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class TrackItems {
    private TrackItems() {
    }

    public static boolean isTrack(ItemStack stack) {
        return stack.is(AllItemTags.TRACKS) || byBlockTag(stack);
    }

    public static boolean is(ItemStack stack, TagKey<Item> tag, boolean original) {
        return original || tag == AllItemTags.TRACKS && byBlockTag(stack);
    }

    private static boolean byBlockTag(ItemStack stack) {
        return stack.getItem() instanceof TrackBlockItem item && item.getBlock().defaultBlockState().is(AllBlockTags.TRACKS);
    }
}
