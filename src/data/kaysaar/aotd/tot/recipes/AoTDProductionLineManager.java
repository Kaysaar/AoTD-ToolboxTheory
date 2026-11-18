package data.kaysaar.aotd.tot.recipes;

import data.kaysaar.aotd.tot.conditions.AoTDRecipeCondition;
import ashlib.data.plugins.misc.AshMisc;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.kaysaar.aotd.tot.scripts.commoditydata.AoTDCollectedProduction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AoTDProductionLineManager {
    private static final String MEMORY_KEY = "$aotd_production_line_recipes";

    private static final String PENDING_KEY = "$aotd_pending_production_plans";
    private static final String RESULT_KEY = "$aotd_production_plan_result";
    private static final String MANUAL_KEY = "$aotd_manual_production_plans";
    private static final String LOCAL_KEY = "$aotd_pending_local_production_plans";

    private static class PendingPlans {
        final Map<String, String> recipes = new LinkedHashMap<>(getPlans());
        final int month = currentMonth();
    }

    private static class LocalOrder {
        final String recipeId;
        final int month = currentMonth();

        LocalOrder(String recipeId) { this.recipeId = recipeId; }
    }

    @SuppressWarnings("unchecked")
    private static Map<Industry, LocalOrder> localOrders() {
        Map<Industry, LocalOrder> orders = (Map<Industry, LocalOrder>) playerMemory().get(LOCAL_KEY);
        return orders == null ? Collections.emptyMap() : orders;
    }

    public static boolean hasSettlementOrders() {
        return hasPendingApplication() || !localOrders().isEmpty();
    }

    public static boolean isLocalTarget(Industry industry) {
        return industry != null && industry.getMarket() != null
                && industry.getMarket().getIndustry(industry.getId()) == industry
                && AshMisc.getMarketsUnderPlayer().contains(industry.getMarket());
    }

    @SuppressWarnings("unchecked")
    public static boolean hasManualPlan(Industry industry) {
        Map<String, Industry> owners = (Map<String, Industry>) industry.getMarket().getMemoryWithoutUpdate().get(MANUAL_KEY);
        return owners != null && owners.get(industry.getId()) == industry;
    }

    @SuppressWarnings("unchecked")
    static void pruneManualPlans(MarketAPI market) {
        MemoryAPI memory = market.getMemoryWithoutUpdate();
        Map<String, Industry> owners = (Map<String, Industry>) memory.get(MANUAL_KEY);
        if (owners == null) return;
        owners.entrySet().removeIf(entry -> market.getIndustry(entry.getKey()) != entry.getValue());
        if (owners.isEmpty()) memory.unset(MANUAL_KEY);
    }

    @SuppressWarnings("unchecked")
    private static void markManual(Industry industry) {
        MemoryAPI memory = industry.getMarket().getMemoryWithoutUpdate();
        Map<String, Industry> owners = (Map<String, Industry>) memory.get(MANUAL_KEY);
        if (owners == null) {
            owners = new LinkedHashMap<>();
            memory.set(MANUAL_KEY, owners);
        }
        owners.put(industry.getId(), industry);
    }

    public static boolean hasLocalOrder(Industry industry) { return localOrders().containsKey(industry); }

    public static String localSelection(Industry industry) {
        LocalOrder order = localOrders().get(industry);
        if (order != null) return order.recipeId;
        AoTDIndustryRecipePlugin active = AoTDIndustryRecipeManager.getInstance().getActiveRecipe(industry);
        return active == null ? null : active.getId();
    }

    public static boolean queueLocalPlan(Industry industry, String recipeId) {
        if (!isLocalTarget(industry)) return false;
        AoTDIndustryRecipeManager manager = AoTDIndustryRecipeManager.getInstance();
        AoTDIndustryRecipePlugin active = manager.getActiveRecipe(industry);
        String activeId = active == null ? null : active.getId();
        if (recipeId != null && !recipeId.equals(activeId)) {
            AoTDIndustryRecipePlugin recipe = manager.getRecipe(recipeId);
            if (recipe == null || !industry.getId().equals(recipe.getIndustryTiedTo())
                    || !recipe.isVisibleInListOfRecipes(industry) || getUnavailableReason(recipe, industry) != null) {
                return false;
            }
        }
        Map<Industry, LocalOrder> orders = new LinkedHashMap<>(localOrders());
        orders.put(industry, new LocalOrder(recipeId));
        playerMemory().set(LOCAL_KEY, orders);
        return true;
    }

    private static int currentMonth() {
        return Global.getSector().getClock().getCycle() * 12 + Global.getSector().getClock().getMonth();
    }

    private static MemoryAPI playerMemory() {
        return Global.getSector().getPlayerMemoryWithoutUpdate();
    }

    public static boolean hasPendingApplication() {
        return playerMemory().contains(PENDING_KEY);
    }

    public static ApplicationReport getLastApplication() {
        return (ApplicationReport) playerMemory().get(RESULT_KEY);
    }

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

    public static int[] applyToAllMarkets() {
        ApplicationReport report = applyToAllMarketsDetailed();
        return new int[]{report.changedIndustries, report.skippedIndustries};
    }

    public static ApplicationReport applyToAllMarketsDetailed() {
        PendingPlans order = new PendingPlans();
        ApplicationReport estimate = inspectPlans(order.recipes, null, null, null, localOrders());
        playerMemory().set(PENDING_KEY, order);
        return estimate;
    }

    public static ApplicationReport previewApplication() {
        return inspectPlans(getPlans(), null, null, null, localOrders());
    }

    public static ApplicationReport previewApplication(String industryId, String recipeId) {
        return inspectPlans(getPlans(), industryId, recipeId, null);
    }

    public static void applyDuePlans(AoTDCollectedProduction production) {
        PendingPlans order = (PendingPlans) playerMemory().get(PENDING_KEY);
        int month = currentMonth();
        boolean bulkDue = order != null && month > order.month;
        Map<Industry, LocalOrder> pending = localOrders();
        Map<Industry, LocalOrder> due = new LinkedHashMap<>();
        for (Map.Entry<Industry, LocalOrder> entry : pending.entrySet()) {
            if (month > entry.getValue().month) due.put(entry.getKey(), entry.getValue());
        }
        if (!bulkDue && due.isEmpty()) return;
        ApplicationReport result = inspectPlans(bulkDue ? order.recipes : null, null, null, production, due);
        playerMemory().set(RESULT_KEY, result);
        if (bulkDue) playerMemory().unset(PENDING_KEY);
        if (!due.isEmpty()) {
            pending.keySet().removeAll(due.keySet());
            if (pending.isEmpty()) playerMemory().unset(LOCAL_KEY);
        }
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

    private static ApplicationReport inspectPlans(Map<String, String> plans, String onlyIndustry,
                                                  String selectedRecipe, AoTDCollectedProduction production) {
        return inspectPlans(plans, onlyIndustry, selectedRecipe, production, Collections.emptyMap());
    }

    private static ApplicationReport inspectPlans(Map<String, String> plans, String onlyIndustry,
            String selectedRecipe, AoTDCollectedProduction production, Map<Industry, LocalOrder> local) {
        boolean apply = production != null;
        ApplicationReport report = new ApplicationReport(apply);
        AoTDIndustryRecipeManager manager = AoTDIndustryRecipeManager.getInstance();
        Set<MarketAPI> markets = new LinkedHashSet<>(AshMisc.getMarketsUnderPlayer());
        if (apply) {
            for (MarketAPI market : markets) {
                for (Industry industry : market.getIndustries()) {
                    LocalOrder override = local.get(industry);
                    if (override == null && (plans == null || hasManualPlan(industry))) continue;
                    if (override == null && manager.getRecipesForIndustry(industry.getId()).isEmpty()) continue;
                    AoTDIndustryRecipePlugin active = manager.getActiveRecipe(industry);
                    if (!Objects.equals(active == null ? null : active.getId(),
                            override != null ? override.recipeId : plans.get(industry.getId()))) {
                        production.collect(market);
                        break;
                    }
                }
            }
        }
        Set<MarketAPI> changed = new LinkedHashSet<>();
        for (MarketAPI market : markets) {
            boolean hasIndustry = false;
            int changesHere = 0, matchesHere = 0, skippedHere = 0;
            for (Industry industry : new ArrayList<>(market.getIndustries())) {
                if (onlyIndustry != null && !onlyIndustry.equals(industry.getId())) continue;
                LocalOrder override = local.get(industry);
                if (override == null && (plans == null || hasManualPlan(industry))) continue;
                if (override == null && manager.getRecipesForIndustry(industry.getId()).isEmpty()
                        && !plans.containsKey(industry.getId())) continue;
                hasIndustry = true;
                String recipeId = override != null ? override.recipeId
                        : onlyIndustry == null ? plans.get(industry.getId()) : selectedRecipe;
                AoTDIndustryRecipePlugin active = manager.getActiveRecipe(industry);
                String activeId = active == null ? null : active.getId();
                if (Objects.equals(activeId, recipeId)) {
                    if (apply && override != null) markManual(industry);
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
                if (apply && override != null) markManual(industry);
                report.changedIndustries++;
                changesHere++;
                changed.add(market);
                if (apply) {
                    try {
                        AoTDRecipeCondition.reapplyIndustry(industry);
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
        if (apply) {
            for (MarketAPI market : changed) production.refreshDemand(market);
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
