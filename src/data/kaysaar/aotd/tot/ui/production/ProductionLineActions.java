package data.kaysaar.aotd.tot.ui.production;

import ashlib.data.plugins.ui.models.ExtendedUIPanelPlugin;
import ashlib.data.plugins.misc.AshMisc;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLineManager;


import java.util.List;

public class ProductionLineActions implements ExtendedUIPanelPlugin {
    final CustomPanelAPI mainPanel;
    final ProductionLinesUI owner;
    TooltipMakerAPI tooltip;
    ButtonAPI applyAll, viewResult;

    public ProductionLineActions(float width, ProductionLinesUI owner) {
        this.owner = owner;
        mainPanel = Global.getSettings().createCustom(width, 25f, this);
        createUI();
    }

    @Override public CustomPanelAPI getMainPanel() { return mainPanel; }

    @Override
    public void createUI() {
        clearUI();
        float width = mainPanel.getPosition().getWidth();
        float buttonWidth = 200f;
        tooltip = mainPanel.createUIElement(width, 25f, false);
        applyAll = tooltip.addButton("Apply on all markets", null, Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.BOTTOM, buttonWidth, 25f, 0f);
        applyAll.getPosition().inTL(width-buttonWidth-5, 0);
        addHelp("Queues these defaults for all player-controlled markets at month end. Applying again replaces the queued order. "
                + "Recipes that cannot be used on a particular market are skipped.");
        boolean enabled = owner.table != null && !owner.table.lines.isEmpty();
        applyAll.setEnabled(enabled);
        if (AoTDProductionLineManager.getLastApplication() != null) {
            viewResult = tooltip.addButton("View last result", null, Misc.getBasePlayerColor(),
                    Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.BOTTOM, 150f, 25f, 0f);
            viewResult.getPosition().inTL(width - buttonWidth - 165f, 0);
        }
        mainPanel.addUIElement(tooltip).inTL(0, 0);
    }

    private void addHelp(final String text) {
        tooltip.addTooltipToPrevious(new TooltipMakerAPI.TooltipCreator() {
            @Override public boolean isTooltipExpandable(Object param) { return false; }
            @Override public float getTooltipWidth(Object param) { return 400f; }
            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object param) {
                tooltip.addPara(text, 0f);
                tooltip.addPara(AoTDProductionLineManager.previewApplication().getSummary(), Misc.getHighlightColor(), 12f);
                if (AoTDProductionLineManager.hasPendingApplication()) {
                    tooltip.addPara("A plan change is queued for month end.", Misc.getHighlightColor(), 10f);
                }
            }
        }, TooltipMakerAPI.TooltipLocation.ABOVE, false);
    }

    @Override
    public void advance(float amount) {
        if (applyAll == null) return;
        if(applyAll.isChecked()){
            applyAll.setChecked(false);
            showResult(AoTDProductionLineManager.applyToAllMarketsDetailed());
            createUI();
        }
        if (viewResult != null && viewResult.isChecked()) {
            viewResult.setChecked(false);
            showResult(AoTDProductionLineManager.getLastApplication());
        }
    }

    private void showResult(AoTDProductionLineManager.ApplicationReport report) {
        if (report == null) return;
        AshMisc.initPopUpDialog(new ProductionPlanApplicationDialog(report),
                Math.min(720f, Global.getSettings().getScreenWidth() - 40f),
                Math.min(550f, Global.getSettings().getScreenHeight() - 40f));
    }

    @Override
    public void clearUI() {
        if (tooltip != null) mainPanel.removeComponent(tooltip);
        tooltip = null;
        applyAll = null;
        viewResult = null;
    }

    @Override public void positionChanged(PositionAPI position) { }
    @Override public void renderBelow(float alphaMult) { }
    @Override public void render(float alphaMult) { }
    @Override public void processInput(List<InputEventAPI> events) { }
    @Override public void buttonPressed(Object buttonId) { }
}
