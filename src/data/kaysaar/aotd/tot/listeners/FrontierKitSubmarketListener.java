package data.kaysaar.aotd.tot.listeners;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.SubmarketUpdateListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import data.kaysaar.aotd.tot.abilities.DeployFrontierKit;
import data.kaysaar.aotd.tot.industries.AoTDSpaceframeFoundry;

public class FrontierKitSubmarketListener implements SubmarketUpdateListener {
    private static final String RESTOCK = "$aotd_frontier_kit_restock";

    @Override
    public void reportSubmarketCargoAndShipsUpdated(SubmarketAPI submarket) {
        boolean blackMarket = Submarkets.SUBMARKET_BLACK.equals(submarket.getSpecId());
        if (!blackMarket && !Submarkets.SUBMARKET_OPEN.equals(submarket.getSpecId())) return;
        MarketAPI market = submarket.getMarket();
        if (!(market.getIndustry("aotd_spaceframe_foundry") instanceof AoTDSpaceframeFoundry foundry)
                || !foundry.isFunctional() || foundry.isPending()) return;
        MemoryAPI memory = market.getMemoryWithoutUpdate();
        String restockKey = blackMarket ? RESTOCK + "_black" : RESTOCK;
        if (memory.contains(restockKey)) return;

        float standing = market.getFaction().getRelationship(Factions.PLAYER);
        int target = 4 + Math.round((standing + 1f) * 10.5f);
        if (market.hasSubmarket(blackMarket ? Submarkets.SUBMARKET_OPEN : Submarkets.SUBMARKET_BLACK)) {
            target = blackMarket ? target / 2 : (target + 1) / 2;
        }
        CargoAPI cargo = submarket.getCargo();
        float missing = target - cargo.getCommodityQuantity(DeployFrontierKit.KIT);
        if (missing > 0) {
            cargo.addCommodity(DeployFrontierKit.KIT, missing);
            cargo.sort();
        }
        memory.set(restockKey, true, 30f);
    }
}
