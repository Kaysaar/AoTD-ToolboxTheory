package data.kaysaar.aotd.tot.recipes;

import data.kaysaar.aotd.tot.conditions.AoTDRecipeCondition;
import ashlib.data.plugins.misc.AshMisc;
import com.fs.starfarer.api.Global;
import data.kaysaar.aotd.vok.scripts.research.AoTDMainResearchManager;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.kaysaar.aotd.tot.scripts.commoditydata.AoTDCollectedProduction;
import data.kaysaar.aotd.tot.scripts.economy.AoTdMainWorkTask2;
import data.kaysaar.aotd.tot.scripts.economy.AoTDIndustryData;
import data.kaysaar.aotd.tot.scripts.economy.AoTDUpdateMarketAgainTask;

import java.util.ArrayList;
import java.util.Collection;
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
        boolean force;
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

    @SuppressWarnings("unchecked")
    private static void clearManual(Industry industry) {
        MemoryAPI memory = industry.getMarket().getMemoryWithoutUpdate();
        Map<String, Industry> owners = (Map<String, Industry>) memory.get(MANUAL_KEY);
        if (owners == null) return;
        owners.remove(industry.getId());
        if (owners.isEmpty()) memory.unset(MANUAL_KEY);
    }

    public static boolean hasLocalOrder(Industry industry) { return localOrders().containsKey(industry); }

    public static boolean isAwaitingAllocation(Industry industry) {
        return industry.getMarket().getMemoryWithoutUpdate().get(AoTDIndustryData.memKey) instanceof AoTDIndustryData data
                && data.isPending(industry.getId());
    }

    private static boolean applyPendingPlan(Industry industry, String recipeId) {
        AoTDIndustryRecipeLocalManager local = AoTDIndustryRecipeManager.getInstance().getLocalManager(industry);
        String previous = local.getActiveRecipeId(industry);
        try {
            if (!local.setActiveRecipe(industry, recipeId)) return false;
            AoTDUpdateMarketAgainTask.applyPendingIndustrySuppression(industry);
        } catch (RuntimeException ex) {
            Global.getLogger(AoTDProductionLineManager.class).warn("Changing a pending production plan", ex);
            if (!Objects.equals(previous, local.getActiveRecipeId(industry))) {
                local.setActiveRecipe(industry, previous);
                AoTDUpdateMarketAgainTask.applyPendingIndustrySuppression(industry);
            }
            return false;
        }
        Map<Industry, LocalOrder> orders = localOrders();
        if (orders.containsKey(industry)) {
            orders.remove(industry);
            if (orders.isEmpty()) playerMemory().unset(LOCAL_KEY);
        }
        return true;
    }

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
        if (isAwaitingAllocation(industry)) {
            if (!applyPendingPlan(industry, recipeId)) return false;
            markManual(industry);
            AoTdMainWorkTask2.refreshMarketCommodityData(industry.getMarket());
            return true;
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
        lines.sort(Comparator.comparing(AoTDProductionLine::getIndustryName, String.CASE_INSENSITIVE_ORDER));
        return lines;
    }

    public static int[] applyToAllMarkets() {
        ApplicationReport report = applyToAllMarketsDetailed();
        return new int[]{report.changedIndustries, report.skippedIndustries};
    }

    public static ApplicationReport applyToAllMarketsDetailed() {
        return queueMarkets(AshMisc.getMarketsUnderPlayer(), false);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, PendingPlans> marketOrders() {
        Object saved = playerMemory().get(PENDING_KEY);
        if (saved instanceof Map) return new LinkedHashMap<>((Map<String, PendingPlans>) saved);
        Map<String, PendingPlans> orders = new LinkedHashMap<>();
        if (saved instanceof PendingPlans legacy) {
            for (MarketAPI market : AshMisc.getMarketsUnderPlayer()) orders.put(market.getId(), legacy);
        }
        return orders;
    }

    public static ApplicationReport queueMarkets(List<MarketAPI> markets, boolean force) {
        PendingPlans order = new PendingPlans();
        order.force = force;
        Set<MarketAPI> targets = new LinkedHashSet<>(markets);
        targets.retainAll(AshMisc.getMarketsUnderPlayer());
        Map<String, PendingPlans> orders = marketOrders();
        for (MarketAPI market : targets) orders.put(market.getId(), order);
        if (!targets.isEmpty()) playerMemory().set(PENDING_KEY, orders);
        ApplicationReport report = inspectPlans(targets, orders, null, null, null, Collections.emptyMap(), false);
        AoTDIndustryRecipeManager manager = AoTDIndustryRecipeManager.getInstance();
        for (MarketAPI market : targets) {
            boolean changed = false;
            for (Industry industry : market.getIndustries()) {
                if (!isAwaitingAllocation(industry) || manager.getRecipesForIndustry(industry.getId()).isEmpty()) continue;
                if (!force && (hasManualPlan(industry) || hasLocalOrder(industry))) continue;
                if (applyPendingPlan(industry, order.recipes.get(industry.getId()))) {
                    if (force) clearManual(industry);
                    changed = true;
                }
            }
            if (changed) AoTdMainWorkTask2.refreshMarketCommodityData(market);
        }
        return report;
    }

    public static ApplicationReport previewMarket(MarketAPI market, boolean force) {
        PendingPlans order = new PendingPlans();
        order.force = force;
        return inspectPlans(List.of(market), Collections.singletonMap(market.getId(), order), null, null, null, Collections.emptyMap(), false);
    }

    public static ApplicationReport previewApplication() {
        return previewApplication(null, null);
    }

    public static ApplicationReport previewApplication(String industryId, String recipeId) {
        List<MarketAPI> markets = AshMisc.getMarketsUnderPlayer();
        PendingPlans order = new PendingPlans();
        Map<String, PendingPlans> orders = new LinkedHashMap<>();
        for (MarketAPI market : markets) orders.put(market.getId(), order);
        return inspectPlans(markets, orders, industryId, recipeId, null, Collections.emptyMap(), false);
    }

    public static String queuedSummary() {
        return queuedReport().getQueueSummary();
    }

    public static ApplicationReport queuedReport() {
        Map<String, PendingPlans> orders = marketOrders();
        Map<Industry, LocalOrder> local = localOrders();
        List<MarketAPI> markets = AshMisc.getMarketsUnderPlayer();
        return inspectPlans(markets, orders, null, null, null, local, true);
    }

    public static void applyDuePlans(AoTDCollectedProduction production) {
        int month = currentMonth();
        Map<String, PendingPlans> orders = marketOrders();
        Map<String, PendingPlans> dueMarkets = new LinkedHashMap<>(orders);
        dueMarkets.values().removeIf(order -> month <= order.month);
        Map<Industry, LocalOrder> pending = localOrders();
        Map<Industry, LocalOrder> due = new LinkedHashMap<>(pending);
        due.values().removeIf(order -> month <= order.month);
        if (dueMarkets.isEmpty() && due.isEmpty()) return;
        ApplicationReport result = inspectPlans(AshMisc.getMarketsUnderPlayer(), dueMarkets, null, null, production, due, true);
        playerMemory().set(RESULT_KEY, result);
        orders.keySet().removeAll(dueMarkets.keySet());
        if (orders.isEmpty()) playerMemory().unset(PENDING_KEY);
        else playerMemory().set(PENDING_KEY, orders);
        if (!due.isEmpty()) {
            pending.keySet().removeAll(due.keySet());
            if (pending.isEmpty()) playerMemory().unset(LOCAL_KEY);
        }
    }

    public static String getUnavailableReason(AoTDIndustryRecipePlugin recipe, Industry industry) {
        if (recipe == null) return null;
        if (industry == null) return "No industry context is available to check this plan's requirements.";
        if (Global.getSector() != null && Global.getSettings().getModManager().isModEnabled("aotd_vok")) {
            String research = AoTDMainResearchManager.getInstance().getMissingRecipeResearch(recipe.getId(), industry.getMarket());
            if (research != null) return "Required technology: " + research;
        }
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

    private static ApplicationReport inspectPlans(Collection<MarketAPI> markets, Map<String, PendingPlans> orders,
            String onlyIndustry, String selectedRecipe, AoTDCollectedProduction production, Map<Industry, LocalOrder> local,
            boolean includeLocal) {
        boolean apply = production != null;
        ApplicationReport report = new ApplicationReport(apply);
        AoTDIndustryRecipeManager manager = AoTDIndustryRecipeManager.getInstance();
        Map<Industry, LocalOrder> protectedOrders = includeLocal ? local : localOrders();
        if (apply) {
            for (MarketAPI market : markets) {
                PendingPlans order = orders.get(market.getId());
                boolean force = order != null && order.force;
                Map<String, String> plans = order == null ? null : order.recipes;
                for (Industry industry : market.getIndustries()) {
                    LocalOrder override = force ? null : local.get(industry);
                    if (override == null && (plans == null || !force && (hasManualPlan(industry) || protectedOrders.containsKey(industry)))) continue;
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
            int queuedBefore = report.queuedIndustries;
            boolean hasIndustry = false;
            int changesHere = 0, matchesHere = 0, skippedHere = 0;
            PendingPlans order = orders.get(market.getId());
            boolean force = order != null && order.force;
            Map<String, String> plans = order == null ? null : order.recipes;
            for (Industry industry : new ArrayList<>(market.getIndustries())) {
                if (onlyIndustry != null && !onlyIndustry.equals(industry.getId())) continue;
                LocalOrder override = force ? null : local.get(industry);
                if (override == null && (plans == null || !force && (hasManualPlan(industry) || protectedOrders.containsKey(industry)))) continue;
                if (override == null && manager.getRecipesForIndustry(industry.getId()).isEmpty()
                        && !plans.containsKey(industry.getId())) continue;
                hasIndustry = true;
                String recipeId = override != null ? override.recipeId
                        : onlyIndustry == null ? plans.get(industry.getId()) : selectedRecipe;
                AoTDIndustryRecipePlugin active = manager.getActiveRecipe(industry);
                String activeId = active == null ? null : active.getId();
                if (Objects.equals(activeId, recipeId)) {
                    if (override != null || force && (protectedOrders.containsKey(industry) || hasManualPlan(industry))) {
                        report.queuedIndustries++;
                        if (!apply && includeLocal) report.addQueueDetail(industry, active, active, force, override != null);
                    }
                    if (apply && force) clearManual(industry);
                    else if (apply && override != null) markManual(industry);
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
                if (apply && force) clearManual(industry);
                else if (apply && override != null) markManual(industry);
                report.queuedIndustries++;
                if (!apply && includeLocal) report.addQueueDetail(industry, active, recipe, force, override != null);
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
                if (report.queuedIndustries > queuedBefore) report.queuedMarkets++;
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
        public int queuedIndustries, queuedMarkets;
        public final boolean applied;
        public int examinedMarkets, changedMarkets, changedIndustries, unchangedIndustries, skippedIndustries, failedIndustries;
        public final List<String> marketDetails = new ArrayList<>();
        public final List<String> details = new ArrayList<>();

        public ApplicationReport(boolean applied) { this.applied = applied; }

        public final transient Map<String, List<String>> queuedByMarket = new LinkedHashMap<>();

        private void addQueueDetail(Industry industry, AoTDIndustryRecipePlugin active, AoTDIndustryRecipePlugin next,
                                    boolean force, boolean local) {
            String from = active == null ? "Normal production" : active.getName();
            String to = next == null ? "Normal production" : next.getName();
            String change = active == next ? "Keep " + to : from + " to " + to;
            String order = force ? "Force Apply" : local ? "Local order" : "Faction plan";
            queuedByMarket.computeIfAbsent(industry.getMarket().getName(), key -> new ArrayList<>())
                    .add(industry.getCurrentName() + ": " + change + " (" + order + ")");
        }

        public String getQueueSummary() {
            return queuedIndustries + (queuedIndustries == 1 ? " recipe queued across " : " recipes queued across ")
                    + queuedMarkets + (queuedMarkets == 1 ? " market" : " markets");
        }

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
                if (!recipe.isVisibleInListOfRecipes(baseline) || getUnavailableReason(recipe, baseline) != null) continue;
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
