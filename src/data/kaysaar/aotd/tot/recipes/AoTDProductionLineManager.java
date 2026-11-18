package data.kaysaar.aotd.tot.recipes;

import ashlib.data.plugins.misc.AshMisc;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;

/** Faction-wide defaults; local managers still own each colony's actual assignment. */
public class AoTDProductionLineManager {
    private static final String MEMORY_KEY = "$aotd_production_line_recipes";

    @SuppressWarnings("unchecked")
    public static Map<String, String> getPlans() {
        Object saved = Global.getSector().getPlayerMemoryWithoutUpdate().get(MEMORY_KEY);
        if (saved instanceof Map) return (Map<String, String>) saved;
        Map<String, String> plans = new LinkedHashMap<>();
        Global.getSector().getPlayerMemoryWithoutUpdate().set(MEMORY_KEY, plans);
        return plans;
    }

    public static String getRecipeId(String industryId) {
        String id = getPlans().get(industryId);
        AoTDIndustryRecipePlugin recipe = AoTDIndustryRecipeManager.getInstance().getRecipe(id);
        return recipe != null && industryId.equals(recipe.getIndustryTiedTo()) ? id : null;
    }

    // Stores only the faction default. Null means normal industry production.
    public static void setRecipeId(String industryId, String recipeId) {
        if (recipeId == null) {
            getPlans().remove(industryId);
            return;
        }
        AoTDIndustryRecipePlugin recipe = AoTDIndustryRecipeManager.getInstance().getRecipe(recipeId);
        if (recipe == null || !industryId.equals(recipe.getIndustryTiedTo())) {
            throw new IllegalArgumentException("Recipe does not belong to industry: " + industryId);
        }
        getPlans().put(industryId, recipeId);
    }

    public static List<AoTDProductionLine> getLines() {
        List<AoTDProductionLine> lines = new ArrayList<>();
        AoTDIndustryRecipeManager manager = AoTDIndustryRecipeManager.getInstance();
        for (IndustrySpecAPI spec : Global.getSettings().getAllIndustrySpecs()) {
            if (manager.getRecipesForIndustry(spec.getId()).isEmpty()) continue;
            lines.add(new AoTDProductionLine(spec, getRecipeId(spec.getId())));
        }
        lines.sort(Comparator.comparing(line -> line.spec.getName(), String.CASE_INSENSITIVE_ORDER));
        return lines;
    }

    /** Compatibility entry point for existing callers. */
    public static int[] applyToAllMarkets() {
        ApplicationReport report = applyToAllMarketsDetailed();
        return new int[]{report.changedIndustries, report.skippedIndustries};
    }

    public static ApplicationReport applyToAllMarketsDetailed() {
        return inspectPlans(null, null, true);
    }

    public static ApplicationReport previewApplication() {
        return inspectPlans(null, null, false);
    }

    public static ApplicationReport previewApplication(String industryId, String recipeId) {
        return inspectPlans(industryId, recipeId, false);
    }

    public static String getUnavailableReason(AoTDIndustryRecipePlugin recipe, Industry industry) {
        if (recipe == null) return null;
        if (industry == null) return "No industry context is available to check this plan's requirements.";
        try {
            if (recipe.canBeUsed(industry)) return null;
            String reason = recipe.getUnavailableReason(industry);
            return reason == null || reason.trim().isEmpty()
                    ? "This industry does not meet this production plan's requirements." : reason;
        } catch (RuntimeException ex) {
            Global.getLogger(AoTDProductionLineManager.class).warn("Checking production plan " + recipe.getId(), ex);
            return "Unable to check this plan's requirements. See the game log for details.";
        }
    }

    /** No recipe is assigned or reapplied by the preview path. */
    private static ApplicationReport inspectPlans(String onlyIndustry, String selectedRecipe, boolean apply) {
        ApplicationReport report = new ApplicationReport(apply);
        AoTDIndustryRecipeManager manager = AoTDIndustryRecipeManager.getInstance();
        Set<MarketAPI> checked = new LinkedHashSet<>();
        Set<MarketAPI> changed = new LinkedHashSet<>();
        for (MarketAPI market : AshMisc.getMarketsUnderPlayer()) {
            if (!checked.add(market)) continue;
            boolean hasIndustry = false;
            int changesHere = 0, matchesHere = 0, skippedHere = 0;
            for (Industry industry : new ArrayList<>(market.getIndustries())) {
                if (onlyIndustry != null && !onlyIndustry.equals(industry.getId())) continue;
                if (manager.getRecipesForIndustry(industry.getId()).isEmpty()) continue;
                hasIndustry = true;
                String recipeId = onlyIndustry == null ? getPlans().get(industry.getId()) : selectedRecipe;
                AoTDIndustryRecipePlugin active = manager.getActiveRecipe(industry);
                String activeId = active == null ? null : active.getId();
                if (Objects.equals(activeId, recipeId)) {
                    report.unchangedIndustries++;
                    matchesHere++;
                    continue;
                }
                AoTDIndustryRecipePlugin recipe = manager.getRecipe(recipeId);
                String reason = recipeId != null && (recipe == null || !industry.getId().equals(recipe.getIndustryTiedTo()))
                        ? "The selected plan is no longer registered for this industry."
                        : getUnavailableReason(recipe, industry);
                if (reason != null) {
                    report.skippedIndustries++;
                    skippedHere++;
                    report.details.add(market.getName() + " / " + industry.getCurrentName() + ": " + reason);
                    continue;
                }
                if (apply) {
                    try {
                        if (!manager.getLocalManager(industry).setActiveRecipe(industry, recipeId)) {
                            report.skippedIndustries++;
                            skippedHere++;
                            report.details.add(market.getName() + " / " + industry.getCurrentName()
                                    + ": requirements changed; assignment was skipped.");
                            continue;
                        }
                    } catch (RuntimeException ex) {
                        report.failedIndustries++;
                        report.details.add(market.getName() + " / " + industry.getCurrentName()
                                + ": assignment failed; check the game log.");
                        Global.getLogger(AoTDProductionLineManager.class).warn("Applying production plan", ex);
                        continue;
                    }
                }
                report.changedIndustries++;
                changesHere++;
                changed.add(market);
                if (apply) {
                    try {
                        industry.reapply();
                    } catch (RuntimeException ex) {
                        report.failedIndustries++;
                        report.details.add(market.getName() + " / " + industry.getCurrentName()
                                + ": plan assigned, but refreshing the industry failed; check the game log.");
                        Global.getLogger(AoTDProductionLineManager.class).warn("Refreshing production plan", ex);
                    }
                }
            }
            if (hasIndustry) {
                report.examinedMarkets++;
                report.marketDetails.add(market.getName() + ": " + changesHere
                        + (apply ? " changed, " : " to change, ") + matchesHere + " already matching, "
                        + skippedHere + " skipped.");
            }
        }
        report.changedMarkets = changed.size();
        return report;
    }

    public static class ApplicationReport {
        public final boolean applied;
        public int examinedMarkets, changedMarkets, changedIndustries, unchangedIndustries, skippedIndustries, failedIndustries;
        public final List<String> marketDetails = new ArrayList<>();
        public final List<String> details = new ArrayList<>();

        public ApplicationReport(boolean applied) { this.applied = applied; }

        public String getSummary() {
            return (applied ? "Changed " : "Will change ") + changedIndustries + " industries across "
                    + changedMarkets + " markets. " + unchangedIndustries + " already matching; "
                    + skippedIndustries + " skipped."
                    + (failedIndustries == 0 ? "" : " " + failedIndustries + " errors; see details.");
        }
    }

    public static void chooseMostEfficient(List<AoTDProductionLine> lines) {
        AoTDIndustryRecipeManager manager = AoTDIndustryRecipeManager.getInstance();
        for (AoTDProductionLine line : lines) {
            AoTDProductionLine best = new AoTDProductionLine(line.spec, null);
            if (best.error != null) continue;
            Industry baseline = best.industry;
            String bestId = null;
            for (AoTDIndustryRecipePlugin recipe : manager.getRecipesForIndustry(line.spec.getId())) {
                if (!recipe.isVisibleInListOfRecipes(baseline) || !recipe.canBeUsed(baseline)) continue;
                AoTDProductionLine candidate = new AoTDProductionLine(line.spec, recipe.getId());
                if (candidate.error != null) continue;
                int comparison = Double.compare(candidate.getEfficiency(), best.getEfficiency());
                if (comparison > 0 || (comparison == 0
                        && candidate.getValue(false) - candidate.getValue(true)
                        > best.getValue(false) - best.getValue(true))) {
                    best = candidate;
                    bestId = recipe.getId();
                }
            }
            setRecipeId(line.spec.getId(), bestId);
        }
    }
}
