package data.kaysaar.aotd.tot.recipes;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;

import java.util.LinkedHashMap;
import data.kaysaar.aotd.tot.conditions.AoTDRecipeCondition;
import java.util.Map;
import java.util.ArrayList;

public class AoTDIndustryRecipeLocalManager {

    private MarketAPI market;
    private Map<String, Industry> owners = new LinkedHashMap<>();

    private Map<String, Industry> owners() {
        if (owners == null) owners = new LinkedHashMap<>();
        return owners;
    }

    public boolean hasAssignments() { return !industryRecipes.isEmpty(); }

    private void clearEffects(String id, Industry owner) {
        AoTDIndustryRecipePlugin recipe = getActiveRecipe(id);
        if (recipe == null) return;
        if (owner instanceof BaseIndustry base) recipe.unapply(base);
        recipe.unapplyMarketEffects(market, id);
    }

    private void removeEffects(String id, Industry owner) {
        clearEffects(id, owner);
        AoTDIndustryRecipePlugin recipe = getActiveRecipe(id);
        if (recipe != null && owner instanceof BaseIndustry base) recipe.onRecipeRemoved(base);
    }

    public void reconcile(boolean applying) {
        for (String id : new ArrayList<>(industryRecipes.keySet())) {
            Industry current = market.getIndustry(id);
            Industry owner = owners().get(id);
            if (current == null || (owner != null && current != owner)) {
                removeEffects(id, owner);
                owners().remove(id);
                industryRecipes.remove(id);
                recipePlugins.remove(id);
                continue;
            }
            owners().put(id, current);
            if (AoTDIndustryRecipePlugin.supportsDirectRecipes(current)) continue;
            if (applying) applyIndirect(current);
            else clearEffects(id, current);
        }
    }

    public void applyIndirect(Industry industry) {
        if (market.getIndustry(industry.getId()) != industry || AoTDIndustryRecipePlugin.supportsDirectRecipes(industry)) return;
        AoTDIndustryRecipePlugin recipe = getActiveRecipe(industry);
        if (recipe == null) return;
        if (recipe.doesFullyOverrideIndustry()) recipe.cancelIndustryEffects(industry);
        if (!industry.isFunctional()) {
            clearEffects(industry.getId(), industry);
            return;
        }
        if (industry instanceof BaseIndustry base) recipe.applyRecipeToIndustry(base);
        recipe.applyMarketEffects(market, industry.getId());
    }

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
    public AoTDIndustryRecipePlugin getActiveRecipe(Industry industry) {
        Industry owner = owners().get(industry.getId());
        return owner == null || owner == industry ? getActiveRecipe(industry.getId()) : null;
    }

    public boolean setActiveRecipe(Industry industry, String recipeId) {
        if (industry.getMarket() != market) return false;
        if (recipePlugins == null) recipePlugins = new LinkedHashMap<>();
        String industryId = industry.getId();
        Industry previousOwner = owners().get(industryId);
        if (previousOwner != null && previousOwner != industry) {
            if (market.getIndustry(industryId) != industry) return false;
            removeEffects(industryId, previousOwner);
            owners().remove(industryId);
            industryRecipes.remove(industryId);
            recipePlugins.remove(industryId);
        }
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

        removeEffects(industryId, owners().getOrDefault(industryId, industry));

        if (next == null) {
            owners().remove(industryId);
            industryRecipes.remove(industryId);
            recipePlugins.remove(industryId);
        } else {
            industryRecipes.put(industryId, recipeId);
            recipePlugins.put(industryId, next);
            owners().put(industryId, industry);
            AoTDRecipeCondition.ensure(market);
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
