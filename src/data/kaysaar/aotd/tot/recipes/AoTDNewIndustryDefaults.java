package data.kaysaar.aotd.tot.recipes;

import ashlib.data.plugins.misc.AshMisc;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.listeners.PlayerColonizationListener;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.kaysaar.aotd.tot.conditions.AoTDRecipeCondition;

import java.util.LinkedHashMap;
import java.util.Map;

public class AoTDNewIndustryDefaults implements EveryFrameScript, PlayerColonizationListener {
    private static final String COMPLETED = "$aotd_completed_recipe_industries";
    private float elapsed;

    public static void install() {
        AoTDNewIndustryDefaults tracker = new AoTDNewIndustryDefaults();
        for (MarketAPI market : AshMisc.getMarketsUnderPlayer()) update(market);
        Global.getSector().getListenerManager().addListener(tracker, true);
        Global.getSector().addTransientScript(tracker);
    }

    @Override
    public void reportPlayerColonizedPlanet(PlanetAPI planet) {
        MarketAPI market = planet.getMarket();
        if (market == null) return;
        market.getMemoryWithoutUpdate().set(COMPLETED, new LinkedHashMap<String, Industry>());
        update(market);
    }

    @Override
    public void reportPlayerAbandonedColony(MarketAPI colony) {
        colony.getMemoryWithoutUpdate().unset(COMPLETED);
    }

    @Override
    public boolean isDone() {
        return false;
    }
    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        elapsed += amount;
        if (elapsed < 0.5f) return;
        elapsed = 0f;
        for (MarketAPI market : AshMisc.getMarketsUnderPlayer()) update(market);
    }

    @SuppressWarnings("unchecked")
    static void update(MarketAPI market) {
        MemoryAPI memory = market.getMemoryWithoutUpdate();
        Map<String, Industry> completed = (Map<String, Industry>) memory.get(COMPLETED);
        boolean baseline = completed == null;
        if (baseline) {
            completed = new LinkedHashMap<>();
            memory.set(COMPLETED, completed);
        }
        AoTDProductionLineManager.pruneManualPlans(market);
        completed.entrySet().removeIf(e -> market.getIndustry(e.getKey()) != e.getValue());
        AoTDIndustryRecipeManager manager = AoTDIndustryRecipeManager.getInstance();
        for (Industry industry : market.getIndustries()) {
            String id = industry.getId();
            if (completed.get(id) == industry || (industry.isBuilding() && !industry.isUpgrading())) continue;
            completed.put(id, industry);
            if (baseline || AoTDProductionLineManager.hasManualPlan(industry) || manager.getActiveRecipe(industry) != null) continue;
            String recipe = AoTDProductionLineManager.getRecipeId(id);
            if (recipe != null && manager.getLocalManager(industry).setActiveRecipe(industry, recipe)) {
                AoTDRecipeCondition.reapplyIndustry(industry);
            }
        }
    }
}
