package data.kaysaar.aotd.tot.ui.production;

import ashlib.data.plugins.misc.AshMisc;
import data.kaysaar.aotd.tot.ui.production.table.ProductionPlanTable;
import ashlib.data.plugins.ui.models.ExtendedUIPanelPlugin;
import ashlib.data.plugins.ui.plugins.UILinesRenderer;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Browse-only content: the enclosing dialog decides where to commit the selection. */
public class ProductionPlanChangePanel implements ExtendedUIPanelPlugin {
    private final CustomPanelAPI mainPanel;
    private CustomPanelAPI contentPanel, detailsPanel;
    private final IndustrySpecAPI spec;
    private final Industry targetIndustry;
    private final String originalRecipeId;
    private final List<AoTDProductionLine> plans = new ArrayList<>();
    private AoTDProductionLine selected;
    private float tableWidth, detailsWidth;
    private String status;
    private String unavailableReason;

    public ProductionPlanChangePanel(float width, float height, IndustrySpecAPI spec, String currentRecipeId) {
        this(width, height, spec, currentRecipeId, null);
    }

    /** A non-null industry checks requirements for that individual industry; previews remain size six. */
    public ProductionPlanChangePanel(float width, float height, IndustrySpecAPI spec,
                                     String currentRecipeId, Industry targetIndustry) {
        if (targetIndustry != null && !spec.getId().equals(targetIndustry.getId())) {
            throw new IllegalArgumentException("The selection context must match the industry type.");
        }
        this.spec = spec;
        this.originalRecipeId = currentRecipeId;
        this.targetIndustry = targetIndustry;
        mainPanel = Global.getSettings().createCustom(width, height, this);
        plans.add(new AoTDProductionLine(spec, null));
        AoTDIndustryRecipeManager manager = AoTDIndustryRecipeManager.getInstance();
        Industry context = targetIndustry == null ? plans.get(0).industry : targetIndustry;
        for (AoTDIndustryRecipePlugin recipe : manager.getRecipesForIndustry(spec.getId())) {
            // Explicitly hidden recipes stay hidden. Locked, visible recipes remain inspectable.
            if (Objects.equals(currentRecipeId, recipe.getId()) || isVisible(recipe, context)) {
                plans.add(new AoTDProductionLine(spec, recipe.getId()));
            }
        }
        selected = plans.get(0);
        for (AoTDProductionLine plan : plans) {
            if (Objects.equals(plan.recipeId, currentRecipeId)) selected = plan;
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

    public String getSelectedRecipeId() { return selected == null ? null : selected.recipeId; }
    public boolean hasSelection() { return selected != null; }
    public Industry getTargetIndustry() { return targetIndustry; }
    public boolean hasChangedSelection() { return selected != null && !Objects.equals(originalRecipeId, selected.recipeId); }
    public boolean canConfirm() { return hasChangedSelection() && getUnavailableReason() == null; }

    public String getUnavailableReason() { return unavailableReason; }

    public void refreshAvailability() {
        String previous = unavailableReason;
        unavailableReason = computeUnavailableReason();
        if (!Objects.equals(previous, unavailableReason)) createDetails();
    }

    private String computeUnavailableReason() {
        if (selected == null) return "Select a production plan.";
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

    public void setStatus(String status) { this.status = status; createDetails(); }
    @Override public CustomPanelAPI getMainPanel() { return mainPanel; }

    @Override public void createUI() {
        clearUI();
        float width = mainPanel.getPosition().getWidth();
        float height = mainPanel.getPosition().getHeight();
        detailsWidth = Math.min(370f, width * 0.38f);
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
        tooltip.setParaFont(Fonts.ORBITRON_24AABOLD);
        tooltip.addPara(selected.getRecipeName(), selected.getRecipeColor(), 0f).setAlignment(Alignment.MID);
        tooltip.setParaFontDefault();
        if (Objects.equals(originalRecipeId, selected.recipeId)) {
            tooltip.addPara(targetIndustry == null ? "Current default plan" : "Current industry plan",
                    Misc.getHighlightColor(), 8f).setAlignment(Alignment.MID);
        }
        Industry context = targetIndustry == null ? selected.industry : targetIndustry;
        tooltip.addSectionHeading("Description", Alignment.MID, 16f);
        if (selected.recipe == null) {
            tooltip.addPara("Uses the industry's normal production and demand without a recipe override.", 8f);
        } else if (context == null) {
            tooltip.addPara("The preview context is unavailable, so this plan's description could not be loaded.",
                    Misc.getGrayColor(), 8f);
        } else {
            String description = selected.recipe.getDescription(context);
            if (description != null && !description.trim().isEmpty()) tooltip.addPara(description, 8f);
            float before = tooltip.getHeightSoFar();
            selected.recipe.addOnHoverTooltip(tooltip, context, 6);
            if ((description == null || description.trim().isEmpty()) && tooltip.getHeightSoFar() == before) {
                tooltip.addPara("No description has been provided for this plan.", Misc.getGrayColor(), 8f);
            }
        }
        tooltip.addSectionHeading("Availability", Alignment.MID, 16f);
        String reason = getUnavailableReason();
        tooltip.addPara(reason == null ? "Available" : reason,
                reason == null ? Misc.getPositiveHighlightColor() : Misc.getNegativeHighlightColor(), 8f);
        if (selected.error != null) tooltip.addPara(selected.error, Misc.getNegativeHighlightColor(), 8f);
        tooltip.addPara("Average supply and demand use a size 6 world, without additional bonuses and with all inputs supplied.",
                Misc.getGrayColor(), 12f);
        if (targetIndustry == null) {
            tooltip.addSectionHeading("Application preview", Alignment.MID, 16f);
            AoTDProductionLineManager.ApplicationReport impact = AoTDProductionLineManager.previewApplication(spec.getId(), selected.recipeId);
            tooltip.addPara(impact.getSummary(), 8f);
            for (String detail : impact.details) tooltip.addPara(detail, Misc.getNegativeHighlightColor(), 8f);
            tooltip.addPara("Confirm changes the default plan only. Use Apply on all markets to update existing industries.",
                    Misc.getHighlightColor(), 12f);
        } else {
            tooltip.addPara("Requirements are checked for " + targetIndustry.getMarket().getName()
                    + ". The enclosing dialog controls when this choice is applied.", Misc.getGrayColor(), 12f);
        }
        if (status != null) tooltip.addPara(status, Misc.getNegativeHighlightColor(), 12f);
        detailsPanel.addUIElement(tooltip).inTL(12f, 8f);
        contentPanel.addComponent(detailsPanel).inTL(tableWidth + 16f, 0);
    }

    @Override public void clearUI() {
        if (contentPanel != null) mainPanel.removeComponent(contentPanel);
        contentPanel = null;
        detailsPanel = null;
    }
    @Override public void advance(float amount) { }
    @Override public void positionChanged(PositionAPI position) { }
    @Override public void renderBelow(float alphaMult) { }
    @Override public void render(float alphaMult) { }
    @Override public void processInput(List<InputEventAPI> events) { }
    @Override public void buttonPressed(Object buttonId) { }
}
