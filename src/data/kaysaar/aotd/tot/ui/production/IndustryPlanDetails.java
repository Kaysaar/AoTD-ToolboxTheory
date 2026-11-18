package data.kaysaar.aotd.tot.ui.production;

import ashlib.data.plugins.ui.models.ExtendedUIPanelPlugin;
import ashlib.data.plugins.misc.AshMisc;
import ashlib.data.plugins.ui.plugins.UILinesRenderer;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLine;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLineManager;


import java.util.List;

public class IndustryPlanDetails implements ExtendedUIPanelPlugin {
    public static final float WIDTH = ProductionPlanShowcase.BODY_WIDTH + 22f;
    CustomPanelAPI mainPanel, contentPanel;
    AoTDProductionLine line;
    ButtonAPI changeProductionPlan;
    final Runnable onPlanChanged;
    UILinesRenderer renderer ;
    public IndustryPlanDetails(float height, AoTDProductionLine line) {
        this(height, line, null);
    }

    public IndustryPlanDetails(float height, AoTDProductionLine line, Runnable onPlanChanged) {
        this.onPlanChanged = onPlanChanged;
        this.line = line;
        mainPanel = Global.getSettings().createCustom(WIDTH, height, this);
        renderer = new UILinesRenderer(0f);
        renderer.setPanel(mainPanel);
        createUI();
    }


    public void setLine(AoTDProductionLine line) {
        if (this.line == line) return;
        this.line = line;
        createUI();
    }

    @Override public CustomPanelAPI getMainPanel() { return mainPanel; }

    @Override
    public void createUI() {
        clearUI();
        float height = mainPanel.getPosition().getHeight();
        float width = WIDTH - 10f;
        contentPanel = mainPanel.createCustomPanel(WIDTH, height, null);

        TooltipMakerAPI buttons = contentPanel.createUIElement(width, 25f, false);
        changeProductionPlan = buttons.addButton("Change Production Plan", null, Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.BL_TR, width, 25F, 0f);
        changeProductionPlan.setEnabled(line != null);
        contentPanel.addUIElement(buttons).inTL(0f, height - 30F);

        if (line == null) {
            mainPanel.addComponent(contentPanel).inTL(0, 0);
            return;
        }

        TooltipMakerAPI showcase = contentPanel.createUIElement(width, Math.max(1f, height - 35f), true);
        ProductionPlanShowcase.addTo(contentPanel, showcase, line, true);
        contentPanel.addUIElement(showcase).inTL(7f, 0f);
        mainPanel.addComponent(contentPanel).inTL(0, 0);
    }

    @Override
    public void clearUI() {
        if (contentPanel != null) mainPanel.removeComponent(contentPanel);
        contentPanel = null;
        changeProductionPlan = null;
    }

    @Override
    public void advance(float amount) {
        if (changeProductionPlan != null && changeProductionPlan.isChecked()) {
            changeProductionPlan.setChecked(false);
            if (line == null) return;
            ProductionPlanChangeDialog dialog = new ProductionPlanChangeDialog(line.spec, () -> {
                if (onPlanChanged != null) onPlanChanged.run();
                else setLine(new AoTDProductionLine(line.spec, AoTDProductionLineManager.getRecipeId(line.spec.getId())));
            });
            AshMisc.initPopUpDialog(dialog, ProductionPlanChangeDialog.WIDTH,
                    Math.min(650f, Global.getSettings().getScreenHeight() - 40f));
        }
    }

    @Override public void positionChanged(PositionAPI position) { }
    @Override public void renderBelow(float alphaMult) {renderer.render(alphaMult); }
    @Override public void render(float alphaMult) { }
    @Override public void processInput(List<InputEventAPI> events) { }
    @Override public void buttonPressed(Object buttonId) { }
}
