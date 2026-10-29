package dev.chaevsfe.trackwright.config;

import com.google.gson.JsonObject;
import dev.chaevsfe.trackwright.Trackwright;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class ServerConfig {
    public static final String FILE_NAME = "trackwright-server.json";
    public static final String STRAIGHT_THROUGH_BLOCKS = "straightThroughBlocks";
    public static final String MAX_CONNECTION_LENGTH = "maxConnectionLength";
    public static final String PROTECT_TRAIN_PASSENGERS = "protectTrainPassengers";
    public static final String BORE_THROUGH_BLOCKS = "boreThroughBlocks";
    public static final String BORE_HEADROOM = "boreHeadroom";
    public static final String BORE_LAVA_CHECK = "boreLavaCheck";
    public static final String BORE_MAX_HARDNESS = "boreMaxHardness";
    public static final String REFUSE_BLOCKED_TRACK_ENDS = "refuseBlockedTrackEnds";
    public static final String PROTECT_TRACK_EQUIPMENT = "protectTrackEquipment";
    public static final String ROLLERS_SKIP_AIR_PAVING = "rollersSkipAirPaving";
    public static final String ROLLERS_CLEAR_POWDER_SNOW = "rollersClearPowderSnow";
    public static final String ROLLERS_IGNORE_FLUIDS = "rollersIgnoreFluids";
    public static final String CONTRAPTION_CONTROLS_SCHEDULE = "contraptionControlsSchedule";
    public static final String TRAIN_DRILLS_FULL_FACE = "trainDrillsFullFace";
    public static final String ROLLERS_NEED_GROUND = "rollersNeedGround";
    public static final String ROLLER_RESHAPE = "rollerReshape";

    private static volatile TrackwrightSettings current;

    private ServerConfig() {
    }

    public static TrackwrightSettings get() {
        TrackwrightSettings settings = current;
        if (settings == null) {
            synchronized (ServerConfig.class) {
                settings = current;
                if (settings == null) {
                    settings = load(path());
                    current = settings;
                }
            }
        }
        return settings;
    }

    public static TrackwrightSettings reload() {
        TrackwrightSettings settings = load(path());
        synchronized (ServerConfig.class) {
            current = settings;
        }
        Trackwright.LOGGER.info("Trackwright server settings: straightThroughBlocks={}, maxConnectionLength={}, protectTrainPassengers={}, boreThroughBlocks={}, boreHeadroom={}, boreLavaCheck={}, boreMaxHardness={}, refuseBlockedTrackEnds={}, protectTrackEquipment={}, rollersSkipAirPaving={}, rollersClearPowderSnow={}, rollersIgnoreFluids={}, contraptionControlsSchedule={}, trainDrillsFullFace={}, rollersNeedGround={}, rollerReshape={}",
            settings.straightThroughBlocks(), settings.maxConnectionLength(), settings.protectTrainPassengers(), settings.boreThroughBlocks(),
            settings.boreHeadroom(), settings.boreLavaCheck(), settings.boreMaxHardness(), settings.refuseBlockedTrackEnds(), settings.protectTrackEquipment(),
            settings.rollersSkipAirPaving(), settings.rollersClearPowderSnow(), settings.rollersIgnoreFluids(), settings.contraptionControlsSchedule(),
            settings.trainDrillsFullFace(), settings.rollersNeedGround(), settings.rollerReshape());
        return settings;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    private static TrackwrightSettings load(Path path) {
        JsonConfigFile.Contents contents = JsonConfigFile.read(path);
        JsonObject json = contents.json();
        TrackwrightSettings defaults = TrackwrightSettings.DEFAULTS;
        boolean complete = true;
        Boolean straight = JsonConfigFile.readBoolean(json, STRAIGHT_THROUGH_BLOCKS);
        Integer length = JsonConfigFile.readInt(json, MAX_CONNECTION_LENGTH);
        Boolean protect = JsonConfigFile.readBoolean(json, PROTECT_TRAIN_PASSENGERS);
        String boreText = JsonConfigFile.readString(json, BORE_THROUGH_BLOCKS);
        Integer headroom = JsonConfigFile.readInt(json, BORE_HEADROOM);
        Boolean lava = JsonConfigFile.readBoolean(json, BORE_LAVA_CHECK);
        Double hardness = JsonConfigFile.readDouble(json, BORE_MAX_HARDNESS);
        Boolean refuseEnds = JsonConfigFile.readBoolean(json, REFUSE_BLOCKED_TRACK_ENDS);
        Boolean equipment = JsonConfigFile.readBoolean(json, PROTECT_TRACK_EQUIPMENT);
        Boolean airPaving = JsonConfigFile.readBoolean(json, ROLLERS_SKIP_AIR_PAVING);
        Boolean powderSnow = JsonConfigFile.readBoolean(json, ROLLERS_CLEAR_POWDER_SNOW);
        Boolean fluids = JsonConfigFile.readBoolean(json, ROLLERS_IGNORE_FLUIDS);
        Boolean controls = JsonConfigFile.readBoolean(json, CONTRAPTION_CONTROLS_SCHEDULE);
        Boolean drillFace = JsonConfigFile.readBoolean(json, TRAIN_DRILLS_FULL_FACE);
        Boolean needGround = JsonConfigFile.readBoolean(json, ROLLERS_NEED_GROUND);
        Boolean reshape = JsonConfigFile.readBoolean(json, ROLLER_RESHAPE);
        if (straight == null) {
            straight = defaults.straightThroughBlocks();
            complete = false;
        }
        if (length == null) {
            length = defaults.maxConnectionLength();
            complete = false;
        } else if (length != TrackwrightSettings.clampLength(length)) {
            Trackwright.LOGGER.warn("{} {} is outside {}..{}, using {}", MAX_CONNECTION_LENGTH, length,
                TrackwrightSettings.MIN_CONNECTION_LENGTH, TrackwrightSettings.MAX_CONNECTION_LENGTH, TrackwrightSettings.clampLength(length));
        }
        if (protect == null) {
            protect = defaults.protectTrainPassengers();
            complete = false;
        }
        BoreMode bore;
        if (boreText == null) {
            bore = defaults.boreThroughBlocks();
            complete = false;
        } else {
            bore = BoreMode.byId(boreText);
            if (bore == null) {
                Trackwright.LOGGER.warn("{} \"{}\" is not one of off, survival, creative-only, using off", BORE_THROUGH_BLOCKS, boreText);
                bore = BoreMode.OFF;
            }
        }
        if (headroom == null) {
            headroom = defaults.boreHeadroom();
            complete = false;
        } else if (headroom != TrackwrightSettings.clampHeadroom(headroom)) {
            Trackwright.LOGGER.warn("{} {} is outside 0..{}, using {}", BORE_HEADROOM, headroom, TrackwrightSettings.MAX_BORE_HEADROOM,
                TrackwrightSettings.clampHeadroom(headroom));
        }
        if (lava == null) {
            lava = defaults.boreLavaCheck();
            complete = false;
        }
        if (hardness == null) {
            hardness = defaults.boreMaxHardness();
            complete = false;
        } else if (hardness < 0) {
            Trackwright.LOGGER.warn("{} {} is below 0, using 0 (no limit)", BORE_MAX_HARDNESS, hardness);
        }
        if (refuseEnds == null) {
            refuseEnds = defaults.refuseBlockedTrackEnds();
            complete = false;
        }
        if (equipment == null) {
            equipment = defaults.protectTrackEquipment();
            complete = false;
        }
        if (airPaving == null) {
            airPaving = defaults.rollersSkipAirPaving();
            complete = false;
        }
        if (powderSnow == null) {
            powderSnow = defaults.rollersClearPowderSnow();
            complete = false;
        }
        if (fluids == null) {
            fluids = defaults.rollersIgnoreFluids();
            complete = false;
        }
        if (controls == null) {
            controls = defaults.contraptionControlsSchedule();
            complete = false;
        }
        if (drillFace == null) {
            drillFace = defaults.trainDrillsFullFace();
            complete = false;
        }
        if (needGround == null) {
            needGround = defaults.rollersNeedGround();
            complete = false;
        }
        if (reshape == null) {
            reshape = defaults.rollerReshape();
            complete = false;
        }
        TrackwrightSettings settings = new TrackwrightSettings(straight, length, protect, bore, headroom, lava, hardness, refuseEnds,
            equipment, airPaving, powderSnow, fluids, controls, drillFace, needGround, reshape);
        if (contents.writable() && !complete) {
            JsonObject known = new JsonObject();
            known.addProperty(STRAIGHT_THROUGH_BLOCKS, settings.straightThroughBlocks());
            known.addProperty(MAX_CONNECTION_LENGTH, length);
            known.addProperty(PROTECT_TRAIN_PASSENGERS, settings.protectTrainPassengers());
            known.addProperty(BORE_THROUGH_BLOCKS, boreText != null ? boreText : settings.boreThroughBlocks().id());
            known.addProperty(BORE_HEADROOM, headroom);
            known.addProperty(BORE_LAVA_CHECK, settings.boreLavaCheck());
            known.addProperty(BORE_MAX_HARDNESS, hardness);
            known.addProperty(REFUSE_BLOCKED_TRACK_ENDS, settings.refuseBlockedTrackEnds());
            known.addProperty(PROTECT_TRACK_EQUIPMENT, settings.protectTrackEquipment());
            known.addProperty(ROLLERS_SKIP_AIR_PAVING, settings.rollersSkipAirPaving());
            known.addProperty(ROLLERS_CLEAR_POWDER_SNOW, settings.rollersClearPowderSnow());
            known.addProperty(ROLLERS_IGNORE_FLUIDS, settings.rollersIgnoreFluids());
            known.addProperty(CONTRAPTION_CONTROLS_SCHEDULE, settings.contraptionControlsSchedule());
            known.addProperty(TRAIN_DRILLS_FULL_FACE, settings.trainDrillsFullFace());
            known.addProperty(ROLLERS_NEED_GROUND, settings.rollersNeedGround());
            known.addProperty(ROLLER_RESHAPE, settings.rollerReshape());
            JsonConfigFile.write(path, known, json);
        }
        return settings;
    }
}
