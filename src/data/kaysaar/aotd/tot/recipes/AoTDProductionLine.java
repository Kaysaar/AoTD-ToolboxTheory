package data.kaysaar.aotd.tot.recipes;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.misc.AoTDPreviewMarket;
import data.kaysaar.aotd.tot.scripts.commoditydata.AoTDIndustryAmounts;

import data.kaysaar.aotd.tot.scripts.economy.AoTDIndustryData;

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
    public final boolean local;
    public final boolean pending;

    public AoTDProductionLine(IndustrySpecAPI spec, String recipeId) {
        this(spec, recipeId, null);
    }

    public AoTDProductionLine(IndustrySpecAPI spec, String recipeId, Industry target) {
        local = target != null;
        pending = local && target.getMarket().getMemoryWithoutUpdate().get(AoTDIndustryData.memKey) instanceof AoTDIndustryData data
                && data.isPending(spec.getId());
        this.spec = spec;
        this.recipeId = recipeId;
        recipe = AoTDIndustryRecipeManager.getInstance().getRecipe(recipeId);
        try {
            MarketAPI market = local ? AoTDPreviewMarket.createMarket(target.getMarket()) : AoTDPreviewMarket.createMarket(false);
            market.getMemoryWithoutUpdate().set(PREVIEW_KEY, !local);
            AoTDPreviewMarket.withFullAvailability(market, () -> buildPreview(market, target));
        } catch (RuntimeException ex) {
            error = "Unable to preview this production line.";
            production.clear();
            demand.clear();
            Global.getLogger(AoTDProductionLine.class).warn("Production line preview: " + spec.getId(), ex);
        }
    }

    private void buildPreview(MarketAPI market, Industry target) {
        market.addIndustry(spec.getId());
        industry = market.getIndustry(spec.getId());
        if (local) {
            if (target instanceof BaseIndustry source && industry instanceof BaseIndustry preview) {
                AoTDIndustryRecipeLocalManager.copyBaseAppearance(source, preview);
            }
            if (!pending && target.isBuilding() && !target.isUpgrading()) industry.startBuilding();
            industry.setSpecialItem(target.getSpecialItem());
            industry.setAICoreId(target.getAICoreId());
            industry.setImproved(target.isImproved());
            if (target.isDisrupted()) industry.setDisrupted(target.getDisruptedDays(), false);
            industry.getSupplyBonusFromOther().applyMods(target.getSupplyBonusFromOther());
            industry.getDemandReductionFromOther().applyMods(target.getDemandReductionFromOther());
            if (pending) {
                industry.getSupplyBonusFromOther().unmodifyFlat(AoTDIndustryData.source);
                industry.getDemandReductionFromOther().unmodifyFlat(AoTDIndustryData.source);
            }
        }
        market.reapplyConditions();
        if (recipeId != null) {
            AoTDIndustryRecipeManager manager = AoTDIndustryRecipeManager.getInstance();
            recipe = manager.createLocalPlugin(recipeId);
            if (recipe == null || !spec.getId().equals(recipe.getIndustryTiedTo())) {
                throw new IllegalArgumentException("Unknown production plan: " + recipeId);
            }
            if (recipe.doesFullyOverrideIndustry()) recipe.cancelIndustryEffects(industry);
            else industry.reapply();
            if (!local || (pending ? industry.isFunctional() : target.isFunctional())) {
                if (industry instanceof BaseIndustry base) recipe.applyRecipeToIndustry(base);
                recipe.applyMarketEffects(market, industry.getId());
            }
            manager.getLocalManager(market).updateAppearance(industry, recipe);
        } else {
            industry.reapply();
        }
        if (spec.hasTag(Industries.FARMING) && (recipe == null || !recipe.doesFullyOverrideIndustry())) {
            MarketConditionAPI condition = market.getFirstCondition("aotd_toolbox_food_corrector");
            if (condition != null && condition.isSurveyed()) {
                market.reapplyCondition(condition.getIdForPluginModifications());
            }
        }
        collect(production, false, target);
        collect(demand, true, target);
    }

    private void collect(Map<String, Integer> target, boolean isDemand, Industry source) {
        for (String id : AoTDIndustryAmounts.getPreviewCommodityIds(industry, source, isDemand)) {
            int amount = isDemand ? AoTDIndustryAmounts.getDemand(industry, id)
                    : AoTDIndustryAmounts.getProduction(industry, id);
            if (amount > 0) target.put(id, amount);
        }
    }

    public String getIndustryName() {
        if (industry == null || error != null) return spec.getName();
        if (industry instanceof BaseIndustry base && base.getNameOverride() != null) return base.getNameOverride();
        return industry.getCurrentName();
    }

    public String getIndustryImage() {
        if (industry == null || error != null) return spec.getImageName();
        if (industry instanceof BaseIndustry base && base.getImageOverride() != null) return base.getImageOverride();
        return industry.getCurrentImage();
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
