package data.kaysaar.aotd.tot.entities;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.impl.campaign.BaseCampaignObjectivePlugin;
import com.fs.starfarer.api.impl.campaign.NavBuoyEntityPlugin;
import com.fs.starfarer.api.impl.campaign.SensorArrayEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class FrontierDevice extends BaseCampaignObjectivePlugin {
    private static float getFleetBonus(boolean sensorArray) {
        return (sensorArray ? SensorArrayEntityPlugin.SENSOR_BONUS_MAKESHIFT : NavBuoyEntityPlugin.NAV_BONUS_MAKESHIFT) * 0.5f;
    }

    @Override
    public void advance(float amount) {
        if (entity.getContainingLocation() == null || entity.isInHyperspace()
                || entity.getMemoryWithoutUpdate().getBoolean(MemFlags.OBJECTIVE_NON_FUNCTIONAL)) return;
        boolean sensorArray = entity.hasTag(Tags.SENSOR_ARRAY);
        if (!sensorArray && isReset()) return;
        String id = sensorArray ? Tags.SENSOR_ARRAY : Tags.NAV_BUOY;
        float bonus = getFleetBonus(sensorArray);
        for (CampaignFleetAPI fleet : entity.getContainingLocation().getFleets()) {
            if (fleet.isInHyperspaceTransition()) continue;
            if (fleet.getFaction() != entity.getFaction() && !(isHacked() && fleet.getFaction().isPlayerFaction())) continue;
            var stats = fleet.getStats();
            var stat = sensorArray ? stats.getSensorRangeMod() : stats.getFleetwideMaxBurnMod();
            var current = stat.getFlatBonus(id);
            if (current == null || current.value <= bonus) {
                stats.addTemporaryModFlat(0.1f, id, entity.getName(), bonus, stat);
            }
        }
    }

    @Override
    public boolean hasCustomMapTooltip() {
        return true;
    }

    @Override
    public void createMapTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        tooltip.addTitle(entity.getName(), entity.getFaction().getBaseUIColor());
        FrontierDevice.addDetails(entity, tooltip, VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS);
    }

    @Override
    public void appendToCampaignTooltip(TooltipMakerAPI tooltip, VisibilityLevel level) {
        FrontierDevice.addDetails(entity, tooltip, level);
    }

    static void addDetails(SectorEntityToken entity, TooltipMakerAPI tooltip, VisibilityLevel level) {
        if (level != VisibilityLevel.COMPOSITION_DETAILS && level != VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS) return;
        if (entity.hasTag(Tags.COMM_RELAY)) {
            tooltip.addPara("Your fleet receives %s while in relay range. You can also install a comm sniffer or request fleet support here.", 5f, Misc.getHighlightColor(), "queued intel");
            if (entity.getMemoryWithoutUpdate().getBoolean(HACKED)) tooltip.addPara("Comm sniffer installed", Misc.getHighlightColor(), 5f);
        } else if (entity.hasTag(Tags.SENSOR_ARRAY)) {
            tooltip.addPara("%s sensor range for same-faction fleets in this system. The array can also emit a neutrino burst to reveal nearby slipstreams.",
                    5f, Misc.getHighlightColor(), "+" + Misc.getRoundedValue(getFleetBonus(true)));
        } else {
            tooltip.addPara("%s maximum burn level for same-faction fleets in this system.",
                    5f, Misc.getHighlightColor(), "+" + Misc.getRoundedValue(getFleetBonus(false)));
        }
        int days = FrontierDeviceExpiry.getDaysRemaining(entity);
        if (days > 0) tooltip.addPara("Operational for another %s.", 5f, Misc.getHighlightColor(), days == 1 ? "1 day" : days + " days");
    }
}
