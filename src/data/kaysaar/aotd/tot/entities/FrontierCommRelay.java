package data.kaysaar.aotd.tot.entities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.CommRelayEntityPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class FrontierCommRelay extends CommRelayEntityPlugin {
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

    @Override
    public void advance(float amount) {
        if (!isHacked()) return;
        boolean inRange = Global.getSector().getIntelManager().isPlayerInRangeOfCommRelay();
        for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getCommQueue()) {
            if (intel instanceof CommSnifferReadableIntel readable && readable.canMakeVisibleToCommSniffer(inRange, entity)) {
                intel.setForceAddNextFrame(true);
            }
        }
    }

    @Override
    public void printEffect(TooltipMakerAPI tooltip, float pad) {
    }

    @Override
    public void addHackStatusToTooltip(TooltipMakerAPI tooltip, float pad) {
        if (isHacked()) tooltip.addPara("Comm sniffer installed", pad);
    }
}
