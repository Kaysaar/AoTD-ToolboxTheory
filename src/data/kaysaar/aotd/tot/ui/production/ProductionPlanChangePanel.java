package data.kaysaar.aotd.tot.ui.production;

import ashlib.data.plugins.misc.AshMisc;
import data.kaysaar.aotd.tot.ui.production.table.prodplan.ProductionPlanTable;
import ashlib.data.plugins.ui.models.ExtendedUIPanelPlugin;
import ashlib.data.plugins.ui.plugins.UILinesRenderer;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProductionPlanChangePanel implements ExtendedUIPanelPlugin {
    private final CustomPanelAPI mainPanel;
    private CustomPanelAPI contentPanel, detailsPanel;
    private final IndustrySpecAPI spec;
    private final Industry targetIndustry;
    private final String originalRecipeId;
    private final String initialSelection;
    private final List<AoTDProductionLine> plans = new ArrayList<>();
    private AoTDProductionLine selected;
    private float tableWidth, detailsWidth;
    private String status;
    private String unavailableReason;

    public ProductionPlanChangePanel(float width, float height, IndustrySpecAPI spec, String currentRecipeId) {
        this(width, height, spec, currentRecipeId, null);
    }

    public ProductionPlanChangePanel(float width, float height, IndustrySpecAPI spec,
                                     String currentRecipeId, Industry targetIndustry) {
        if (targetIndustry != null && !spec.getId().equals(targetIndustry.getId())) {
            throw new IllegalArgumentException("The selection context must match the industry type.");
        }
        this.spec = spec;
        this.originalRecipeId = currentRecipeId;
        this.targetIndustry = targetIndustry;
        initialSelection = targetIndustry == null ? currentRecipeId : AoTDProductionLineManager.localSelection(targetIndustry);
        mainPanel = Global.getSettings().createCustom(width, height, this);
        plans.add(new AoTDProductionLine(spec, null, targetIndustry));
        AoTDIndustryRecipeManager manager = AoTDIndustryRecipeManager.getInstance();
        Industry context = targetIndustry == null ? plans.get(0).industry : targetIndustry;
        for (AoTDIndustryRecipePlugin recipe : manager.getRecipesForIndustry(spec.getId())) {
            // Explicitly hidden recipes stay hidden. Locked, visible recipes remain inspectable.
            if (Objects.equals(currentRecipeId, recipe.getId()) || isVisible(recipe, context)) {
                plans.add(new AoTDProductionLine(spec, recipe.getId(), targetIndustry));
            }
        }
        selected = plans.get(0);
        for (AoTDProductionLine plan : plans) {
            if (Objects.equals(plan.recipeId, initialSelection)) selected = plan;
        }
        createUI();
    }

    private boolean isVisible(AoTDIndustryRecipePlugin recipe, Industry fallback) {
        if (fallback != null && recipe.isVisibleInListOfRecipes(fallback)) return true;
        if (targetIndustry == null) {
            for (MarketAPI market : AshMisc.getMarketsUnderPlayer()) {
                Industry industry = market.getIndustry(spec.getId());
                if (industry != null && recipe.isVisibleInListOfRecipes(industry)) return true;
            }
        }
        return false;
    }

    public String getSelectedRecipeId() {
        return selected == null ? null : selected.recipeId;
    }
    public boolean hasSelection() {
        return selected != null;
    }
    public Industry getTargetIndustry() {
        return targetIndustry;
    }
    public boolean hasChangedSelection() {
        return selected != null && !Objects.equals(initialSelection, selected.recipeId);
    }
    public boolean canConfirm() { return getUnavailableReason() == null && (hasChangedSelection() || targetIndustry != null
            && !AoTDProductionLineManager.hasLocalOrder(targetIndustry)
            && !AoTDProductionLineManager.hasManualPlan(targetIndustry)); }

    public String getUnavailableReason() {
        return unavailableReason;
    }

    public void refreshAvailability() {
        String previous = unavailableReason;
        unavailableReason = computeUnavailableReason();
        if (!Objects.equals(previous, unavailableReason)) createDetails();
    }

    private String computeUnavailableReason() {
        if (selected == null) return "Select a production plan.";
        if (targetIndustry != null) {
            if (!AoTDProductionLineManager.isLocalTarget(targetIndustry)) return "This industry is no longer under your control.";
            if (Objects.equals(originalRecipeId, selected.recipeId)) return null;
        }
        if (selected.recipeId == null) return null;
        AoTDIndustryRecipePlugin recipe = AoTDIndustryRecipeManager.getInstance().getRecipe(selected.recipeId);
        if (recipe == null) return "This production plan is no longer registered.";
        Industry context = targetIndustry == null ? selected.industry : targetIndustry;
        String reason = AoTDProductionLineManager.getUnavailableReason(recipe, context);
        if (targetIndustry != null || reason == null) return reason;
        // A market-specific restriction on the sample world must not prevent a usable faction default.
        for (MarketAPI market : AshMisc.getMarketsUnderPlayer()) {
            Industry industry = market.getIndustry(spec.getId());
            if (industry != null && AoTDProductionLineManager.getUnavailableReason(recipe, industry) == null) return null;
        }
        return reason;
    }

    public void setStatus(String status) {
        this.status = status; createDetails();
    }
    @Override
    public CustomPanelAPI getMainPanel() {
        return mainPanel;
    }

    @Override
    public void createUI() {
        clearUI();
        float width = mainPanel.getPosition().getWidth();
        float height = mainPanel.getPosition().getHeight();
        detailsWidth = ProductionPlanShowcase.BODY_WIDTH + 36f;
        tableWidth = width - detailsWidth - 16f;
        contentPanel = mainPanel.createCustomPanel(width, height, null);
        ProductionPlanTable table = new ProductionPlanTable(tableWidth, height, plans, selected, this::select);
        table.createSections();
        table.createTable();
        contentPanel.addComponent(table.mainPanel).inTL(0, 0);
        createDetails();
        mainPanel.addComponent(contentPanel).inTL(0, 0);
    }

    private void select(AoTDProductionLine plan) {
        if (selected == plan) return;
        selected = plan;
        status = null;
        createDetails();
    }

    private void createDetails() {
        if (contentPanel == null || selected == null) return;
        if (detailsPanel != null) contentPanel.removeComponent(detailsPanel);
        float height = mainPanel.getPosition().getHeight();
        UILinesRenderer border = new UILinesRenderer(0f);
        detailsPanel = contentPanel.createCustomPanel(detailsWidth, height, border);
        border.setPanel(detailsPanel);
        unavailableReason = computeUnavailableReason();
        TooltipMakerAPI tooltip = detailsPanel.createUIElement(detailsWidth - 24f, height - 16f, true);
        ProductionPlanShowcase.addTo(detailsPanel, tooltip, selected, Objects.equals(originalRecipeId, selected.recipeId));
        if (unavailableReason == null) {
            tooltip.addPara("Criteria met for plan to be applied.", Misc.getPositiveHighlightColor(), 10f);
        } else {
            tooltip.addPara(unavailableReason, Misc.getNegativeHighlightColor(), 10f);
        }
        if (targetIndustry == null) {
            tooltip.addPara("Changing this plan sets the default for newly built industries. To apply it to existing industries, use Apply on markets or change the plan locally at each industry.",
                    Misc.getHighlightColor(), 12f);
        } else {
            if (targetIndustry instanceof BaseIndustry industry) {
                industry.addInstalledItemsSection(Industry.IndustryTooltipMode.NORMAL, tooltip, true);
                industry.addImprovedSection(Industry.IndustryTooltipMode.NORMAL, tooltip, true);
            } else {
                tooltip.addSectionHeading("Installed items", Alignment.MID, 10f);
                for (var item : targetIndustry.getVisibleInstalledItems()) {
                    tooltip.addPara(Global.getSettings().getSpecialItemSpec(item.getId()).getName(), 3f);
                }
                if (targetIndustry.getAICoreId() != null) {
                    targetIndustry.addAICoreSection(tooltip, Industry.AICoreDescriptionMode.INDUSTRY_TOOLTIP);
                }
                tooltip.addSectionHeading("Improvements", Alignment.MID, 10f);
                if (targetIndustry.isImproved()) targetIndustry.addImproveDesc(tooltip, Industry.ImprovementDescriptionMode.INDUSTRY_TOOLTIP);
                else tooltip.addPara("No improvements installed.", 3f);
            }
            if (AoTDProductionLineManager.hasLocalOrder(targetIndustry)) {
                AoTDIndustryRecipePlugin queued = AoTDIndustryRecipeManager.getInstance().getRecipe(initialSelection);
                String name = initialSelection == null ? "Normal production" : queued == null ? initialSelection : queued.getName();
                tooltip.addPara("Queued for next month: " + name, Misc.getHighlightColor(), 12f);
            }
            tooltip.addPara(targetIndustry.getMarket().getName() + " / " + targetIndustry.getCurrentName(),
                    Misc.getHighlightColor(), 12f);
            tooltip.addPara(AoTDProductionLineManager.isAwaitingAllocation(targetIndustry)
                    ? "Confirm updates the resources requested by this industry immediately. Production starts after allocation."
                    : "Confirm schedules the selected plan after this month's production is collected.", 5f);
            tooltip.addPara("Apply on markets preserves manual plans. Force Apply can replace them.", 5f);
        }
        if (status != null) tooltip.addPara(status, Misc.getNegativeHighlightColor(), 12f);
        detailsPanel.addUIElement(tooltip).inTL(12f, 8f);
        contentPanel.addComponent(detailsPanel).inTL(tableWidth + 16f, 0);
    }

    @Override
    public void clearUI() {
        if (contentPanel != null) mainPanel.removeComponent(contentPanel);
        contentPanel = null;
        detailsPanel = null;
    }
    @Override
    public void advance(float amount) { }
    @Override
    public void positionChanged(PositionAPI position) { }
    @Override
    public void renderBelow(float alphaMult) { }
    @Override
    public void render(float alphaMult) { }
    @Override
    public void processInput(List<InputEventAPI> events) { }
    @Override
    public void buttonPressed(Object buttonId) { }
}
