package data.kaysaar.aotd.tot.industries;

import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import data.kaysaar.aotd.tot.abilities.DeployFrontierKit;

public class AoTDSpaceframeFoundry extends AoTDBaseIndustry {
    @Override
    protected void applyIndustry() {
        int size = market.getSize();
        demandExact(Commodities.METALS,size * 1500);
        demand(Commodities.ORGANICS,size);
        supply("aotd_spaceframe_modules",size - 3);
        supply(DeployFrontierKit.KIT,size);
        float fulfillment = getDemandFulfillment(Commodities.METALS,Commodities.ORGANICS);
        getSupply("aotd_spaceframe_modules").getQuantity().modifyMult("aotd_input_shortage",fulfillment,"Input shortages");
        getSupply(DeployFrontierKit.KIT).getQuantity().modifyMult("aotd_input_shortage",fulfillment,"Input shortages");
    }
}
