package data.kaysaar.aotd.tot.recipes;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;

import java.util.LinkedHashMap;
import java.util.Map;

public class AoTDIndustryRecipeLocalManager {

    private MarketAPI market;
    private LinkedHashMap<String, String> industryRecipes = new LinkedHashMap<>();
    private transient LinkedHashMap<String, AoTDIndustryRecipePlugin> recipePlugins = new LinkedHashMap<>();

    AoTDIndustryRecipeLocalManager(MarketAPI market) {
        this.market = market;
    }

    public MarketAPI getMarket() { return market; }
    public String getActiveRecipeId(String industryId) { return industryRecipes.get(industryId); }
    public String getActiveRecipeId(Industry industry) { return getActiveRecipeId(industry.getId()); }
    public AoTDIndustryRecipePlugin getActiveRecipe(String industryId) {
        if (recipePlugins == null) recipePlugins = new LinkedHashMap<>();
        AoTDIndustryRecipePlugin recipe = recipePlugins.get(industryId);
        if (recipe != null) return recipe;
        String recipeId = industryRecipes.get(industryId);
        if (recipeId == null) return null;
        recipe = AoTDIndustryRecipeManager.getInstance().createLocalPlugin(recipeId);
        if (recipe == null || !industryId.equals(recipe.getIndustryTiedTo())) return null;
        recipePlugins.put(industryId, recipe);
        return recipe;
    }
    public AoTDIndustryRecipePlugin getActiveRecipe(Industry industry) { return getActiveRecipe(industry.getId()); }

    public boolean setActiveRecipe(Industry industry, String recipeId) {
        if (industry.getMarket() != market) return false;
        if (recipePlugins == null) recipePlugins = new LinkedHashMap<>();
        String industryId = industry.getId();
        if (recipeId != null) {
            recipeId = recipeId.trim();
            if (recipeId.isEmpty()) recipeId = null;
        }
        if (recipeId == null && !industryRecipes.containsKey(industryId)) return true;
        if (recipeId != null && recipeId.equals(industryRecipes.get(industryId)) && recipePlugins.containsKey(industryId)) return true;

        AoTDIndustryRecipePlugin next = null;
        if (recipeId != null) {
            next = AoTDIndustryRecipeManager.getInstance().createLocalPlugin(recipeId);
            if (next == null || !industryId.equals(next.getIndustryTiedTo()) || !next.canBeUsed(industry)) return false;
        }

        AoTDIndustryRecipePlugin previous = getActiveRecipe(industryId);
        if (previous != null && industry instanceof BaseIndustry) previous.unapply((BaseIndustry) industry);

        if (next == null) {
            industryRecipes.remove(industryId);
            recipePlugins.remove(industryId);
        } else {
            industryRecipes.put(industryId, recipeId);
            recipePlugins.put(industryId, next);
        }
        return true;
    }

    public void validateActiveRecipe(Industry industry) {
        AoTDIndustryRecipePlugin recipe = getActiveRecipe(industry);
        if (industryRecipes.containsKey(industry.getId()) && (recipe == null || !recipe.canBeUsed(industry))) {
            setActiveRecipe(industry, null);
        }
    }

    public void forgetIndustry(Industry industry) {
        setActiveRecipe(industry, null);
    }

    public Map<String, String> getAssignedRecipes() {
        return new LinkedHashMap<>(industryRecipes);
    }
}
