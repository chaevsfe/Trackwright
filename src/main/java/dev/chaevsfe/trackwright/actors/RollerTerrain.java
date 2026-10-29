package dev.chaevsfe.trackwright.actors;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

public enum RollerTerrain implements INamedIconOptions {
    EVERYTHING(AllIcons.I_ROLLER_PAVE, "trackwright.roller.terrain.everything"),
    FILTER_ONLY(AllIcons.I_WHITELIST, "trackwright.roller.terrain.reshape");

    private final AllIcons icon;
    private final String translationKey;

    RollerTerrain(AllIcons icon, String translationKey) {
        this.icon = icon;
        this.translationKey = translationKey;
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }

    @Override
    public String getTranslationKey() {
        return translationKey;
    }
}
