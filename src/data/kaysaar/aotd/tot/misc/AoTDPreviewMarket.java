package data.kaysaar.aotd.tot.misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;

public class AoTDPreviewMarket {

    public static MarketAPI getMarket() {
        MarketAPI market = (MarketAPI) Global.getSector().getMemoryWithoutUpdate().get("$aotd_checkout_market");
        if (market == null) {
            market = createMarket();
            Global.getSector().getMemoryWithoutUpdate().set("$aotd_checkout_market", market);
        }
        return market;
    }

    public static MarketAPI createMarket() {
        MarketAPI market = Global.getFactory().createMarket("to_delete", "TEst", 6);
        market.addCondition("aotd_toolbox_food_corrector_start");
        market.addCondition(Conditions.FARMLAND_ADEQUATE);
        market.addCondition(Conditions.ORE_MODERATE);
        market.addCondition(Conditions.RARE_ORE_MODERATE);
        market.addCondition(Conditions.ORGANICS_COMMON);
        market.addCondition(Conditions.VOLATILES_DIFFUSE);

        if (Global.getSettings().getModManager().isModEnabled("aotd_vok")) {
            market.addCondition("AoDFoodDemand");
        }



        if (Global.getSettings().getModManager().isModEnabled("aotd_vok")) {
            market.addIndustry("dummy_industry");
        }

        market.setFactionId(Global.getSector().getPlayerFaction().getId());
        market.addCondition("aotd_toolbox_food_corrector");
        market.reapplyConditions();
        market.setFreePort(true);

        for (CommodityOnMarketAPI commodity : market.getAllCommodities()) {
            commodity.getAvailableStat().addTemporaryModFlat(10000, "src", 30);
        }
        market.getConditions().forEach(x->x.setSurveyed(true));
        market.setUseStockpilesForShortages(true);
        return market;
    }
}
