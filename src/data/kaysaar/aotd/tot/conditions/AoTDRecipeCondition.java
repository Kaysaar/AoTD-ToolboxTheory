package data.kaysaar.aotd.tot.conditions;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import data.kaysaar.aotd.tot.recipes.AoTDIndustryRecipeLocalManager;
import data.kaysaar.aotd.tot.recipes.AoTDIndustryRecipeManager;
import data.kaysaar.aotd.tot.recipes.AoTDIndustryRecipePlugin;

public class AoTDRecipeCondition extends BaseMarketConditionPlugin {
    public static final String ID = "aotd_recipe_controller";

    public static void ensure(MarketAPI market) {
        if (market.hasCondition(ID)) return;
        AoTDIndustryRecipeLocalManager local = AoTDIndustryRecipeManager.findLocalManager(market);
        if (local != null && local.hasAssignments()) market.addCondition(ID);
    }

    public static void reapplyIndustry(Industry industry) {
        AoTDIndustryRecipeLocalManager local = AoTDIndustryRecipeManager.findLocalManager(industry.getMarket());
        AoTDIndustryRecipePlugin recipe = local == null ? null : local.getActiveRecipe(industry);
        if (recipe == null || AoTDIndustryRecipePlugin.supportsDirectRecipes(industry)) {
            industry.reapply();
            return;
        }
        if (!recipe.doesFullyOverrideIndustry()) industry.reapply();
        local.applyIndirect(industry);
    }

    @Override
    public boolean showIcon() {
        return false;
    }
    @Override
    public void apply(String id) {
        AoTDIndustryRecipeLocalManager local = AoTDIndustryRecipeManager.findLocalManager(market);
        if (local != null) local.reconcile(true);
    }
    @Override
    public void unapply(String id) {
        AoTDIndustryRecipeLocalManager local = AoTDIndustryRecipeManager.findLocalManager(market);
        if (local != null) local.reconcile(false);
    }
}
