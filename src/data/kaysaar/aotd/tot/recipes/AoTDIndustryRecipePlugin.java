package data.kaysaar.aotd.tot.recipes;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.ui.Alignment;
import data.kaysaar.aotd.tot.ids.AoTDTags;
import data.kaysaar.aotd.tot.ui.commoditypanel.AoTDCommodityShortPanelCombined;

import data.kaysaar.aotd.tot.industries.AoTDBaseIndustry;

import data.kaysaar.aotd.tot.scripts.commoditydata.AoTDIndustryAmounts;
import data.kaysaar.aotd.tot.scripts.commoditydata.AoTDSupplyDemandData;
import java.util.List;
import java.awt.Color;
import java.util.Collections;
import java.util.Map;

public class AoTDIndustryRecipePlugin {

    private String id;
    private String name;
    private String industryTiedTo;

    void setMetadata(String id, String name, String industryTiedTo) {
        this.id = id;
        this.name = name;
        this.industryTiedTo = industryTiedTo;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getIndustryTiedTo() { return industryTiedTo; }

    public void onInit() { }
    public boolean canBeUsed(Industry industry) { return true; }
    public boolean isVisibleInListOfRecipes(Industry industry) { return true; }

    public String getIndustryNameOverride(BaseIndustry industry) { return null; }
    public String getIndustryImageOverride(BaseIndustry industry) { return null; }

    public String getDescription(Industry industry) { return ""; }

    public String getUnavailableReason(Industry industry) {
        return "This industry does not meet this production plan's requirements.";
    }
    public Color getNameColor() { return Misc.getTextColor(); }
    public void addOnHoverTooltip(TooltipMakerAPI tooltip, Industry industry, int marketSize) { }

    public void createAdditionalEffectsSection(TooltipMakerAPI tooltip) { }

    public Map<String, Integer> getVanillaProdUnits(Industry industry, int marketSize) {
        return Collections.emptyMap();
    }

    public Map<String, Integer> getVanillaDemandUnits(Industry industry, int marketSize) {
        return Collections.emptyMap();
    }

    public Map<String, Integer> getAbsoluteProdUnits(Industry industry, int marketSize) {
        return Collections.emptyMap();
    }

    public Map<String, Integer> getAbsoluteDemandUnits(Industry industry, int marketSize) {
        return Collections.emptyMap();
    }

    public static boolean supportsDirectRecipes(Industry industry) {
        return industry.getSpec() != null && industry.getSpec().hasTag(AoTDTags.AOTD_SUPPORT_RECIPE_DIRECTLY);
    }

    public boolean doesFullyOverrideIndustry() { return false; }

    public void applyMarketEffects(MarketAPI market, String industryId) { }
    public void unapplyMarketEffects(MarketAPI market, String industryId) { }

    public void createTooltipForIndustry(Industry industry, Industry.IndustryTooltipMode mode,
                                         TooltipMakerAPI tooltip, boolean expanded, float width, boolean ignoreDeficits) {
        if (!doesFullyOverrideIndustry()) {
            createAdditionalEffectsSection(tooltip);
            return;
        }
        MarketAPI market = industry.getMarket();
        tooltip.addTitle(industry.getCurrentName(), market.getFaction().getBaseUIColor());
        tooltip.addPara(getName(), getNameColor(), 3f);
        createIndustryTooltipBody(industry, mode, tooltip, expanded, width, ignoreDeficits);
    }

    public void createIndustryTooltipBody(Industry industry, Industry.IndustryTooltipMode mode,
                                          TooltipMakerAPI tooltip, boolean expanded, float width, boolean ignoreDeficits) {
        createIndustryTooltipBody(industry, mode, tooltip, expanded, width, ignoreDeficits,
                AoTDIndustryAmounts.getCommodityIds(industry, false).stream()
                        .filter(id -> AoTDIndustryAmounts.getProduction(industry, id) > 0).toList(),
                AoTDIndustryAmounts.getCommodityIds(industry, true).stream()
                        .filter(id -> AoTDIndustryAmounts.getDemand(industry, id) > 0).toList());
    }

    public void createIndustryTooltipBody(Industry industry, Industry.IndustryTooltipMode mode, TooltipMakerAPI tooltip,
                                          boolean expanded, float width, boolean ignoreDeficits,
                                          List<String> production, List<String> demand) {
        MarketAPI market = industry.getMarket();
        String industryDescription = industry.getSpec().getDesc();
        String recipeDescription = getDescription(industry);
        if (industryDescription != null && !industryDescription.isBlank()) {
            tooltip.addPara(Global.getSector().getRules().performTokenReplacement(
                    null, industryDescription, market.getPrimaryEntity(), null), 10f);
        }
        if (recipeDescription != null && !recipeDescription.isBlank() && !recipeDescription.equals(industryDescription)) {
            tooltip.addPara(recipeDescription, 8f);
        }
        addOnHoverTooltip(tooltip, industry, market.getSize());
        if (industry.isDisrupted()) {
            tooltip.addPara("Operations disrupted for %s more days.", 10f, Misc.getNegativeHighlightColor(),
                    String.valueOf((int) Math.ceil(industry.getDisruptedDays())));
        } else if (industry.isBuilding()) {
            tooltip.addPara("Under construction", Misc.getHighlightColor(), 10f);
        }
        if (mode == Industry.IndustryTooltipMode.ADD_INDUSTRY || mode == Industry.IndustryTooltipMode.UPGRADE) {
            tooltip.addPara("Construction: %s credits, %s days.", 10f, Misc.getHighlightColor(),
                    Misc.getWithDGS((int) industry.getBuildCost()), String.valueOf((int) industry.getBuildTime()));
        }
        if (mode == Industry.IndustryTooltipMode.DOWNGRADE && industry.getSpec().getUpgrade() != null) {
            float refund = Global.getSettings().getFloat("industryRefundFraction")
                    * Global.getSettings().getIndustrySpec(industry.getSpec().getUpgrade()).getCost();
            tooltip.addPara("Downgrade refund: %s", 10f, Misc.getHighlightColor(), Misc.getDGSCredits(refund));
        }
        if (mode == Industry.IndustryTooltipMode.QUEUED) tooltip.addPara("Queued for construction.", 10f);
        if (mode != Industry.IndustryTooltipMode.NORMAL && !industry.isAvailableToBuild()
                && industry.getUnavailableReason() != null) tooltip.addPara(industry.getUnavailableReason(), Misc.getNegativeHighlightColor(), 10f);
        addMonthlyAmount(tooltip, "Monthly income: %s", industry.getIncome());
        addMonthlyAmount(tooltip, "Monthly upkeep: %s", industry.getUpkeep());
        if (!production.isEmpty()) {
            tooltip.addSectionHeading("Production", Alignment.MID, 10f);
            tooltip.addCustom(new AoTDCommodityShortPanelCombined(width, 3, industry, false, false, production).getMainPanel(), 5f);
        }
        if (!demand.isEmpty()) {
            tooltip.addSectionHeading("Demand & effects", Alignment.MID, 10f);
            tooltip.addCustom(new AoTDCommodityShortPanelCombined(width, 3, industry, true, ignoreDeficits, demand).getMainPanel(), 5f);
        }
        if (market.getMemoryWithoutUpdate().getBoolean(AoTDProductionLine.PREVIEW_KEY)) {
            tooltip.addPara("Preview uses a size 6 market with no resource bonuses.", Misc.getGrayColor(), 5f)
                    .setAlignment(Alignment.MID);
        }
        if (doesFullyOverrideIndustry()) createAdditionalEffectsSection(tooltip);
        else createTooltipForIndustry(industry, mode, tooltip, expanded, width, ignoreDeficits);
        ListenerUtil.addToIndustryTooltip(industry, mode, tooltip, width, expanded);
    }

    private void addMonthlyAmount(TooltipMakerAPI tooltip, String label, MutableStat amount) {
        if (amount.isUnmodified()) return;
        tooltip.addPara(label, 10f, Misc.getHighlightColor(), Misc.getDGSCredits(amount.getModifiedInt()));
        tooltip.addStatModGrid(300f, 65f, 10f, 3f, amount, true, new TooltipMakerAPI.StatModValueGetter() {
            public String getPercentValue(MutableStat.StatMod mod) { return null; }
            public String getMultValue(MutableStat.StatMod mod) { return null; }
            public Color getModColor(MutableStat.StatMod mod) { return null; }
            public String getFlatValue(MutableStat.StatMod mod) { return Misc.getWithDGS(mod.value) + "c"; }
        });
    }

    public void cancelIndustryEffects(Industry industry) {
        industry.unapply();
        if (industry instanceof AoTDBaseIndustry exact) {
            exact.getExactProduction().clear();
            exact.getExactDemand().clear();
        }
        industry.getIncome().unmodify("ind_base");
        industry.getIncome().unmodify("ind_stability");
        industry.getUpkeep().unmodify("ind_base");
        industry.getUpkeep().unmodify("ind_hazard");
        String source = "ind_" + industry.getId();
        clearNativeAmounts(industry.getAllSupply(), source, "ind_sb");
        clearNativeAmounts(AoTDSupplyDemandData.getDemandEntries(industry, AoTDProductionLineManager.isAwaitingAllocation(industry)), source, "ind_dr");
    }

    private static void clearNativeAmounts(Iterable<MutableCommodityQuantity> entries, String source, String bonus) {
        for (MutableCommodityQuantity entry : entries) {
            MutableStat stat = entry.getQuantity();
            stat.unmodify(source);
            for (int index = 0; index < 10; index++) stat.unmodify(source + "_" + index);
            stat.unmodify(bonus);
        }
    }

    public static void applyRecipeFromIndustry(BaseIndustry industry, Runnable nativeEffects) {
        AoTDIndustryRecipePlugin recipe = supportsDirectRecipes(industry)
                ? AoTDIndustryRecipeManager.getInstance().getActiveRecipe(industry) : null;
        if (recipe == null || !recipe.doesFullyOverrideIndustry()) nativeEffects.run();
        if (recipe != null) recipe.applyDirectEffects(industry);
    }

    private void applyDirectEffects(BaseIndustry industry) {
        if (doesFullyOverrideIndustry()) cancelIndustryEffects(industry);
        if (!industry.isFunctional()) return;
        applyRecipeToIndustry(industry);
        if (!AoTDProductionLineManager.isAwaitingAllocation(industry)) applyMarketEffects(industry.getMarket(), industry.getId());
    }

    public static boolean applyRecipeFromIndustry(BaseIndustry industry) {
        if (!supportsDirectRecipes(industry)) return false;
        AoTDIndustryRecipePlugin recipe = AoTDIndustryRecipeManager.getInstance().getActiveRecipe(industry);
        if (recipe == null) return false;
        recipe.applyDirectEffects(industry);
        return recipe.doesFullyOverrideIndustry();
    }

    public static void unapplyRecipeFromIndustry(BaseIndustry industry) {
        if (!supportsDirectRecipes(industry)) return;
        AoTDIndustryRecipePlugin recipe = AoTDIndustryRecipeManager.getInstance().getActiveRecipe(industry);
        if (recipe != null) {
            recipe.unapply(industry);
            recipe.unapplyMarketEffects(industry.getMarket(), industry.getId());
        }
    }

    public void applyRecipeToIndustry(BaseIndustry industry) { }
    public void unapply(BaseIndustry industry) { }

    public void onRecipeRemoved(BaseIndustry industry) { }

    protected void applyDeficitToProduction(BaseIndustry industry, String[] inputs, String... outputs) {
        if (inputs.length == 0) return;
        applyDeficitToProduction(industry, industry.getMaxDeficit(inputs), outputs);
    }

    protected void applyDeficitToProduction(BaseIndustry industry, Pair<String, Integer> deficit, String... outputs) {
        for (String output : outputs) {
            if (industry.getSupply(output).getQuantity().isUnmodified()) continue;
            industry.supply(2, output, -deficit.two, BaseIndustry.getDeficitText(deficit.one));
        }
    }

    protected void applyInputFulfillment(BaseIndustry industry, float fulfillment, String... outputs) {
        applyInputFulfillment(industry, "main", fulfillment, outputs);
    }

    protected void applyInputFulfillment(BaseIndustry industry, String group, float fulfillment, String... outputs) {
        float mult = Float.isNaN(fulfillment) ? 1f : Math.max(0f, Math.min(1f, fulfillment));
        for (String output : outputs) {
            String modId = "aotd_recipe_fulfillment_" + id + "_" + group + "_" + output;
            if (mult == 1f) industry.getSupply(output).getQuantity().unmodifyMult(modId);
            else industry.getSupply(output).getQuantity().modifyMult(modId, mult, "Input shortages");
        }
    }
}

