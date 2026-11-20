package data.kaysaar.aotd.tot.ui.production;

import ashlib.data.plugins.ui.models.ExtendedUIPanelPlugin;
import ashlib.data.plugins.misc.AshMisc;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLineManager;


import java.util.List;
import data.kaysaar.aotd.tot.ui.onhover.ButtonOnHoverInfo;

public class ProductionLineActions implements ExtendedUIPanelPlugin {
    final CustomPanelAPI mainPanel;
    final ProductionLinesUI owner;
    TooltipMakerAPI tooltip;
    ButtonAPI applyAll, forceApply;

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
        var queue = AoTDProductionLineManager.queuedReport();
        LabelAPI queuedLabel = tooltip.addPara(queue.getQueueSummary(), Misc.getHighlightColor(), 4f);
        queuedLabel.autoSizeToWidth((float) Math.ceil(queuedLabel.computeTextWidth(queuedLabel.getText())) + 1f);
        queuedLabel.setHighlightOnMouseover(true);
        tooltip.addTooltipTo(new ProductionQueueTooltip(queue), queuedLabel, TooltipMakerAPI.TooltipLocation.ABOVE, false);
        applyAll = tooltip.addButton("Apply on markets", null, Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.BOTTOM, buttonWidth, 25f, 0f);
        applyAll.getPosition().inTL(width - 2f * buttonWidth - 10f, 0);
        addHelp(applyAll, "Apply on markets", "Waiting industries update demand immediately. Operating industries switch at month end. Industries with manual plans or queued local changes keep their local choices.");
        forceApply = tooltip.addButton("Force Apply on markets", null, Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.BOTTOM, buttonWidth, 25f, 0f);
        forceApply.getPosition().inTL(width - buttonWidth - 5f, 0);
        addHelp(forceApply, "Force Apply on markets", "Overrides manual plans and queued local changes. Waiting industries update demand immediately. Operating industries switch at month end. Industries that cannot use the faction recipe are skipped.");
        boolean enabled = owner.table != null && !owner.table.lines.isEmpty();
        applyAll.setEnabled(enabled);
        forceApply.setEnabled(enabled);
        mainPanel.addUIElement(tooltip).inTL(0, 0);
    }

    private void addHelp(ButtonAPI button, String title, String description) {
        tooltip.addTooltipTo(new ButtonOnHoverInfo(380f, false, null, description, null, null, null, title), button,
                TooltipMakerAPI.TooltipLocation.ABOVE, false);
    }

    @Override
    public void advance(float amount) {
        if (applyAll == null || !applyAll.isChecked() && !forceApply.isChecked()) return;
        boolean force = forceApply.isChecked();
        applyAll.setChecked(false);
        forceApply.setChecked(false);
        AshMisc.initPopUpDialog(new ProductionMarketPicker(force, this::createUI),
                Math.min(720f, Global.getSettings().getScreenWidth() - 40f),
                Math.min(550f, Global.getSettings().getScreenHeight() - 40f));
    }

    @Override
    public void clearUI() {
        if (tooltip != null) mainPanel.removeComponent(tooltip);
        tooltip = null;
        applyAll = null;
        forceApply = null;
    }

    @Override public void positionChanged(PositionAPI position) { }
    @Override public void renderBelow(float alphaMult) { }
    @Override public void render(float alphaMult) { }
    @Override public void processInput(List<InputEventAPI> events) { }
    @Override public void buttonPressed(Object buttonId) { }
}
