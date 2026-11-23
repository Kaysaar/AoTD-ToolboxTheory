package data.kaysaar.aotd.tot.plugins;

import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.plugins.BuildObjectiveTypePicker;
import data.kaysaar.aotd.tot.ids.AoTDTags;

public class AoTDBuildObjectiveTypePicker implements BuildObjectiveTypePicker, GenericPluginManagerAPI.GenericPlugin {
    @Override
    public int getHandlingPriority(Object params) {
        if (params instanceof BuildObjectiveParams) {
            BuildObjectiveParams build = (BuildObjectiveParams) params;
            if (build.stableLoc != null && build.stableLoc.hasTag(AoTDTags.AOTD_STABLE_LOCATION)) {
                return GenericPluginManagerAPI.MOD_SUBSET;
            }
        }
        return -1;
    }

    @Override
    public String pickObjectiveToBuild(BuildObjectiveParams params) {
        return null;
    }
}
