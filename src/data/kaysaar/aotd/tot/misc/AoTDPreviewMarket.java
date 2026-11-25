package data.kaysaar.aotd.tot.misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.campaign.StarSystem;
import data.kaysaar.aotd.tot.conditions.AoTDRecipeCondition;
import data.kaysaar.aotd.tot.plugins.ReflectionUtilis;
import data.kaysaar.aotd.tot.scripts.commoditydata.AoTDCommodityOnMarket;
import data.kaysaar.aotd.tot.scripts.commoditydata.AoTDIndustryAmounts;
import data.kaysaar.aotd.tot.scripts.commoditydata.AoTDSupplyDemandData;
import data.kaysaar.aotd.tot.scripts.economy.AoTDIndustryData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class AoTDPreviewMarket {

    public static MarketAPI getMarket() {
        Global.getSector().getMemoryWithoutUpdate().unset("$aotd_checkout_market");
        return createMarket();
    }

    public static MarketAPI createMarket(MarketAPI source) {
        MarketAPI market = Global.getFactory().createMarket("aotd_plan_preview", source.getName(), source.getSize());
        market.setFactionId(source.getFactionId());
        market.setPrimaryEntity(source.getPrimaryEntity());
        market.setPlayerOwned(source.isPlayerOwned());
        market.setFreePort(source.isFreePort());
        for (MarketConditionAPI condition : source.getConditions()) {
            String id = condition.getId();
            if (AoTDRecipeCondition.ID.equals(id) || source.isConditionSuppressed(id) || market.hasCondition(id)) continue;
            market.addCondition(id);
            market.getFirstCondition(id).setSurveyed(condition.isSurveyed());
        }
        return market;
    }

    public static void setLocalAvailability(MarketAPI preview, Industry target) {
        MarketAPI source = target.getMarket();
        List<Industry> industries = new ArrayList<>(source.getIndustries());
        if (!industries.contains(target)) industries.add(target);
        industries.sort(Comparator.comparingInt(industry -> industry.getSpec().getOrder()));
        List<Industry> earlier = industries.subList(0, industries.indexOf(target));
        Object state = source.getMemoryWithoutUpdate().get(AoTDIndustryData.memKey);
        if (state instanceof AoTDIndustryData data) earlier.removeIf(industry -> data.isPending(industry.getId()));

        for (CommodityOnMarketAPI commodity : source.getAllCommodities()) {
            int available = commodity.getAvailable();
            if (commodity instanceof AoTDCommodityOnMarket local) {
                AoTDSupplyDemandData data = local.getSupplyDemandData();
                float raw = Math.max(data.getTotalRawUnitsFromSupply(), data.getTotalRawUnitsFromDemand());
                raw += local.getExcessQuantity() - local.getDeficitQuantity();
                for (String id : data.getAdditionalDemand().getSources(true)) {
                    raw -= data.getRawAdditionalDemand(id, source);
                }
                for (Industry industry : earlier) raw -= AoTDIndustryAmounts.getDemand(industry, commodity.getId());
                available = data.getEconSpec().getCalculationScript().convertRawUnitsToDemand(
                        Math.nextUp(Math.max(0f, raw)), source, commodity.getId());
            } else {
                for (Industry industry : earlier) {
                    available -= Math.max(0, industry.getDemand(commodity.getId()).getQuantity().getModifiedInt());
                }
            }
            preview.getCommodityData(commodity.getId()).getAvailableStat().setBaseValue(Math.max(0, available));
        }
    }

    public static void withFullAvailability(MarketAPI market, Runnable action) {
        Map<CommodityOnMarketAPI, Float> previous = new LinkedHashMap<>();
        try {
            for (CommodityOnMarketAPI commodity : market.getAllCommodities()) {
                previous.put(commodity, commodity.getAvailableStat().getBaseValue());
                commodity.getAvailableStat().setBaseValue(100000f);
            }
            action.run();
        } finally {
            previous.forEach((commodity, value) -> commodity.getAvailableStat().setBaseValue(value));
        }
    }

    public static MarketAPI createMarket() {
        return createMarket(true);
    }

    public static MarketAPI createMarket(boolean addTemporarySupply) {
        MarketAPI market = Global.getFactory().createMarket("to_delete", "TEst", 6);
        StarSystemAPI system = new StarSystem("Example");
        SectorEntityToken entity = system.initNonStarCenter();
        market.setPrimaryEntity(entity);
        entity.setMarket(market);
        ReflectionUtilis.setPrivateVariableFromSuperclass("accessibilityMod", market, new AoTDPreviewAccessibility());
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

        if (addTemporarySupply) {
            for (CommodityOnMarketAPI commodity : market.getAllCommodities()) {
                commodity.getAvailableStat().addTemporaryModFlat(10000, "src", 30);
            }
        }
        market.getConditions().forEach(x->x.setSurveyed(true));
        market.setUseStockpilesForShortages(true);
        return market;
    }
}
