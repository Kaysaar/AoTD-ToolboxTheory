package data.kaysaar.aotd.tot.recipes;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.misc.AoTDPreviewMarket;
import data.kaysaar.aotd.tot.scripts.commoditydata.AoTDIndustryAmounts;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

public class AoTDProductionLine {
    static final String PREVIEW_KEY = "$aotd_production_plan_preview";
    public final IndustrySpecAPI spec;
    public final LinkedHashMap<String, Integer> production = new LinkedHashMap<>();
    public final LinkedHashMap<String, Integer> demand = new LinkedHashMap<>();
    public Industry industry;
    public final String recipeId;
    public AoTDIndustryRecipePlugin recipe;
    public String error;

    public AoTDProductionLine(IndustrySpecAPI spec, String recipeId) {
        this.spec = spec;
        this.recipeId = recipeId;
        recipe = AoTDIndustryRecipeManager.getInstance().getRecipe(recipeId);
        try {
            // A fresh preview prevents recipes/industry effects leaking into other rows
            // or into the cached market used by VoK research tooltips.
            MarketAPI market = AoTDPreviewMarket.createMarket();
            market.getMemoryWithoutUpdate().set(PREVIEW_KEY, true);
            market.addIndustry(spec.getId());
            industry = market.getIndustry(spec.getId());
            market.reapplyConditions();
            for (CommodityOnMarketAPI commodity : market.getAllCommodities()) {
                commodity.getAvailableStat().setBaseValue(10000000f);
            }

            if (recipeId != null) {
                AoTDIndustryRecipeManager manager = AoTDIndustryRecipeManager.getInstance();
                recipe = manager.createLocalPlugin(recipeId);
                if (recipe == null || !spec.getId().equals(recipe.getIndustryTiedTo())) {
                    throw new IllegalArgumentException("Unknown production plan: " + recipeId);
                }
                // This is an isolated size-six preview. Availability is checked against
                // the selection context separately, so locked plans remain inspectable.
                if (recipe.doesFullyOverrideIndustry()) recipe.cancelIndustryEffects(industry);
                else industry.reapply();
                if (industry instanceof BaseIndustry base) recipe.applyRecipeToIndustry(base);
                recipe.applyMarketEffects(market, industry.getId());
            } else {
                industry.reapply();
            }
            collect(production, false);
            collect(demand, true);
        } catch (RuntimeException ex) {
            error = "Unable to preview this production line.";
            production.clear();
            demand.clear();
            Global.getLogger(AoTDProductionLine.class).warn("Production line preview: " + spec.getId(), ex);
        }
    }

    private void collect(Map<String, Integer> target, boolean isDemand) {
        for (String id : AoTDIndustryAmounts.getCommodityIds(industry, isDemand)) {
            int amount = isDemand ? AoTDIndustryAmounts.getDemand(industry, id)
                    : AoTDIndustryAmounts.getProduction(industry, id);
            if (amount > 0) target.put(id, amount);
        }
    }

    public String getRecipeName() {
        return recipe == null ? (recipeId == null ? "Default" : recipeId) : recipe.getName();
    }

    public Color getRecipeColor() {
        return recipe == null ? Misc.getGrayColor() : recipe.getNameColor();
    }

    public double getValue(boolean isDemand) {
        double value = 0;
        for (Map.Entry<String, Integer> entry : (isDemand ? demand : production).entrySet()) {
            value += (double) entry.getValue() * Global.getSettings().getCommoditySpec(entry.getKey()).getBasePrice();
        }
        return value;
    }

    public double getEfficiency() {
        double output = getValue(false);
        double input = getValue(true);
        if (output <= 0) return 0;
        return input <= 0 ? Double.POSITIVE_INFINITY : output / input;
    }
}
