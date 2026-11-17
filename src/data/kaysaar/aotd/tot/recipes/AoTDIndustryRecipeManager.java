package data.kaysaar.aotd.tot.recipes;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AoTDIndustryRecipeManager {

    public static String CSV_PATH = "data/campaign/aotd_industry_recipes.csv";
    private static String LOCAL_MEMORY_KEY = "$aotd_recipe_manager";
    private static AoTDIndustryRecipeManager instance = new AoTDIndustryRecipeManager();

    private Map<String, AoTDIndustryRecipePlugin> recipes = new LinkedHashMap<>();
    private Map<String, List<AoTDIndustryRecipePlugin>> recipesByIndustry = new HashMap<>();

    public static AoTDIndustryRecipeManager getInstance() { return instance; }

    public void init() {
        reload();
    }

    public void reload() {
        recipes.clear();
        recipesByIndustry.clear();
        try {
            JSONArray rows = Global.getSettings().getMergedSpreadsheetData("id", CSV_PATH);
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String id = row.getString("id").trim();
                String industryId = row.getString("industryTiedTo").trim();
                String name = row.optString("name", id).trim();
                String className = row.getString("pluginClass").trim();
                if (id.isEmpty() || industryId.isEmpty() || className.isEmpty() || recipes.containsKey(id)) {
                    throw new IllegalArgumentException("Invalid recipe at row " + (i + 2) + ": " + id);
                }

                AoTDIndustryRecipePlugin plugin = (AoTDIndustryRecipePlugin) Global.getSettings()
                        .getScriptClassLoader().loadClass(className).newInstance();
                plugin.setMetadata(id, name.isEmpty() ? id : name, industryId);
                plugin.onInit();
                recipes.put(id, plugin);
                recipesByIndustry.computeIfAbsent(industryId, key -> new ArrayList<>()).add(plugin);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed loading " + CSV_PATH, e);
        }
    }

    public AoTDIndustryRecipePlugin getRecipe(String id) {
        return recipes.get(id);
    }

    public List<AoTDIndustryRecipePlugin> getRecipesForIndustry(String industryId) {
        return recipesByIndustry.getOrDefault(industryId, Collections.emptyList());
    }

    public AoTDIndustryRecipePlugin createLocalPlugin(String id) {
        AoTDIndustryRecipePlugin original = getRecipe(id);
        if (original == null) return null;
        try {
            AoTDIndustryRecipePlugin plugin = original.getClass().newInstance();
            plugin.setMetadata(original.getId(), original.getName(), original.getIndustryTiedTo());
            plugin.onInit();
            return plugin;
        } catch (Exception e) {
            throw new RuntimeException("Failed creating recipe " + id, e);
        }
    }

    public AoTDIndustryRecipeLocalManager getLocalManager(MarketAPI market) {
        MemoryAPI memory = market.getMemory();
        Object saved = memory.get(LOCAL_MEMORY_KEY);
        if (saved instanceof AoTDIndustryRecipeLocalManager) {
            return (AoTDIndustryRecipeLocalManager) saved;
        }
        AoTDIndustryRecipeLocalManager local = new AoTDIndustryRecipeLocalManager(market);
        memory.set(LOCAL_MEMORY_KEY, local);
        return local;
    }

    public AoTDIndustryRecipeLocalManager getLocalManager(Industry industry) {
        return getLocalManager(industry.getMarket());
    }

    public AoTDIndustryRecipePlugin getActiveRecipe(Industry industry) {
        Object saved = industry.getMarket().getMemory().get(LOCAL_MEMORY_KEY);
        if (!(saved instanceof AoTDIndustryRecipeLocalManager)) return null;
        return ((AoTDIndustryRecipeLocalManager) saved).getActiveRecipe(industry);
    }

    public void forgetIndustry(Industry industry) {
        getLocalManager(industry).forgetIndustry(industry);
    }

}
