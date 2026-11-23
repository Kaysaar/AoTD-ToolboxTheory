package data.kaysaar.aotd.tot.industries;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import data.kaysaar.aotd.tot.abilities.DeployFrontierKit;
import data.kaysaar.aotd.vok.Ids.AoTDTechIds;
import data.kaysaar.aotd.vok.scripts.research.AoTDMainResearchManager;

public class AoTDSpaceframeFoundry extends AoTDBaseIndustry {
    private boolean hasRequiredResearch() {
        return !Global.getSettings().getModManager().isModEnabled("aotd_vok")
                || AoTDMainResearchManager.getInstance().isAvailableForThisMarket(AoTDTechIds.ORBITAL_ASSEMBLY, market);
    }

    @Override
    public boolean isAvailableToBuild() {
        return super.isAvailableToBuild() && hasRequiredResearch();
    }

    @Override
    public String getUnavailableReason() {
        if (!hasRequiredResearch()) {
            return AoTDMainResearchManager.getInstance().getNameForResearchBd(AoTDTechIds.ORBITAL_ASSEMBLY);
        }
        return super.getUnavailableReason();
    }

    @Override
    protected void applyIndustry() {
        int size = market.getSize();
        demandExact(Commodities.METALS,size * 1500);
        demand(Commodities.ORGANICS,size);
        supply("aotd_spaceframe_modules",size - 2);
        supply(DeployFrontierKit.KIT,size);
        float fulfillment = getDemandFulfillment(Commodities.METALS,Commodities.ORGANICS);
        getSupply("aotd_spaceframe_modules").getQuantity().modifyMult("aotd_input_shortage",fulfillment,"Input shortages");
        getSupply(DeployFrontierKit.KIT).getQuantity().modifyMult("aotd_input_shortage",fulfillment,"Input shortages");
    }
}
