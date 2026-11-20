package data.kaysaar.aotd.tot.stablestructures.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.scripts.commoditydata.AoTDCommodityOnMarket;
import data.kaysaar.aotd.tot.scripts.trade.ScavengerHarvestModel;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureImpl;
import data.kaysaar.aotd.tot.ui.starsystems.stuctures.map.ChooseOrbitForBuildingMap;
import data.kaysaar.aotd.tot.ui.starsystems.stuctures.map.StructureLocationPicker;

public class AoTDScavGuild extends AoTDStableStructureImpl {
    private MarketAPI salvageMarket;

    public static int getAmountOfDebrisFieldFromSystem(StarSystemAPI systemAPI) {
        int fields = 0;
        for (CampaignTerrainAPI terrain : systemAPI.getTerrainCopy()) {
            if (!terrain.isExpired() && terrain.getPlugin() instanceof DebrisFieldTerrainPlugin) fields++;
        }
        return fields;
    }

    @Override
    public StructureLocationPicker createLocationPicker(float width, float height, StarSystemAPI systemAPI) {
        return new ChooseOrbitForBuildingMap(width,height,systemAPI,id);
    }

    @Override
    public boolean isAvailableToBuild(StarSystemAPI systemAPI, FactionAPI factionAttemptingToBuildIt) {
        return getAmountOfDebrisFieldFromSystem(systemAPI)>0;
    }

    @Override
    public String getUnavailableReason(StarSystemAPI systemAPI, FactionAPI factionAttemptingToBuildIt) {
        if(getAmountOfDebrisFieldFromSystem(systemAPI)<=0){
            return "There is no Debris field to be scavenged.";
        }
        return super.getUnavailableReason(systemAPI, factionAttemptingToBuildIt);
    }

    @Override
    public int getEffectOrder() {
        return 1;
    }

    @Override
    public void apply() {
        unapply();
        if (!isFunctional()) return;
        StarSystemAPI system = currentlyAssignedToken.getStarSystem();
        int fields = getAmountOfDebrisFieldFromSystem(system);
        if (fields == 0) return;

        float best = Float.NEGATIVE_INFINITY;
        for (MarketAPI market : Global.getSector().getEconomy().getMarkets(system)) {
            if (!(market.getPrimaryEntity() instanceof PlanetAPI)
                    || !market.getFactionId().equals(currentlyAssignedToken.getFaction().getId())) continue;
            float accessibility = market.getAccessibilityMod().computeEffective(0f);
            if (salvageMarket == null || accessibility > best || accessibility == best && market.getId().compareTo(salvageMarket.getId()) < 0) {
                best = accessibility;
                salvageMarket = market;
            }
        }
        if (salvageMarket == null) return;

        long seed = ScavengerHarvestModel.monthlySeed(Global.getSector().getSeedString(),
                Global.getSector().getClock().getCycle(), Global.getSector().getClock().getMonth(), getModId());
        for (ScavengerSalvage salvage : ScavengerSalvage.values()) {
            int amount = salvage.getMonthlyAmount(fields, seed);
            if (amount > 0) {
                AoTDCommodityOnMarket.getComMarketInstanceSave(salvageMarket, salvage.commodity)
                        .getSupplyDemandData().getAdditionalProduction().modifyFlatExact(getModId(), amount, getName());
            }
        }
    }

    @Override
    public void unapply() {
        if (salvageMarket == null) return;
        for (ScavengerSalvage salvage : ScavengerSalvage.values()) {
            if (salvageMarket.getCommodityData(salvage.commodity) instanceof AoTDCommodityOnMarket commodity) {
                commodity.getSupplyDemandData().getAdditionalProduction().unmodify(getModId());
            }
        }
        salvageMarket = null;
    }

    @Override
    protected void notifyDisrupted() {
        unapply();
    }

    @Override
    public void createTooltipForEffectSection(TooltipMakerAPI tooltip, AoTDStableStructureTooltipMode mode) {
        tooltip.addPara("While operational, the enclave sends salvage to the most accessible planet its faction controls in this system. Each debris field rolls separately for each commodity every month.", 5f);
        int tier = 0;
        for (ScavengerSalvage salvage : ScavengerSalvage.values()) {
            if (tier != salvage.fieldsRequired) {
                tier = salvage.fieldsRequired;
                tooltip.addPara("%s or more debris fields", 8f, Misc.getHighlightColor(), String.valueOf(tier));
            }
            tooltip.addPara("%s: %s chance of %s units per field", 2f, Misc.getHighlightColor(),
                    Global.getSettings().getCommoditySpec(salvage.commodity).getName(),
                    salvage.chance + "%", salvage.min + "-" + salvage.max);
        }
    }
}