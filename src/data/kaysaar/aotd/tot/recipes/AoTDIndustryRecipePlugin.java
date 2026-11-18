package data.kaysaar.aotd.tot.recipes;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

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
    /** Plain description used by the reusable production-plan browser. */
    public String getDescription(Industry industry) { return ""; }
    /** Override with the actual missing research, condition, item, or other requirement. */
    public String getUnavailableReason(Industry industry) {
        return "This industry does not meet this production plan's requirements.";
    }
    public Color getNameColor() { return Misc.getTextColor(); }
    public void addOnHoverTooltip(TooltipMakerAPI tooltip, Industry industry, int marketSize) { }

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

    public static boolean applyRecipeFromIndustry(BaseIndustry industry) {
        AoTDIndustryRecipePlugin recipe = AoTDIndustryRecipeManager.getInstance().getActiveRecipe(industry);
        if (recipe == null) return false;
        recipe.applyRecipeToIndustry(industry);
        return true;
    }

    public static void unapplyRecipeFromIndustry(BaseIndustry industry) {
        AoTDIndustryRecipePlugin recipe = AoTDIndustryRecipeManager.getInstance().getActiveRecipe(industry);
        if (recipe != null) recipe.unapply(industry);
    }

    public void applyRecipeToIndustry(BaseIndustry industry) { }
    public void unapply(BaseIndustry industry) { }

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

