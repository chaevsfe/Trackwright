package dev.chaevsfe.trackwright.client.input;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import net.createmod.catnip.data.Couple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.Map;

public final class CreateClickFirst {
    private CreateClickFirst() {
    }

    public static boolean contraptionHit(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.level == null) {
            return false;
        }
        Map<Integer, WeakReference<AbstractContraptionEntity>> loaded = ContraptionHandler.loadedContraptions.get(mc.level);
        if (loaded == null || loaded.isEmpty()) {
            return false;
        }
        Couple<Vec3> ray = ContraptionHandlerClient.getRayInputs(player);
        Vec3 origin = ray.getFirst();
        Vec3 target = ray.getSecond();
        AABB box = new AABB(origin, target).inflate(16);
        for (WeakReference<AbstractContraptionEntity> reference : loaded.values()) {
            AbstractContraptionEntity entity = reference.get();
            if (entity != null && entity.getBoundingBox().intersects(box)
                && ContraptionHandlerClient.rayTraceContraption(origin, target, entity) != null) {
                return true;
            }
        }
        return false;
    }
}
