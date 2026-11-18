package data.kaysaar.aotd.tot.ui.production;

import ashlib.data.plugins.ui.models.ExtendedUIPanelPlugin;
import ashlib.data.plugins.misc.AshMisc;
import ashlib.data.plugins.ui.models.ImagePanel;
import ashlib.data.plugins.ui.plugins.UILinesRenderer;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLine;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLineManager;
import data.kaysaar.aotd.tot.ui.commoditypanel.AoTDCommodityShortPanel;

import java.awt.Color;
import java.util.List;
import java.util.Map;

public class IndustryPlanDetails implements ExtendedUIPanelPlugin {
    public static final float WIDTH = 410f;
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

        TooltipMakerAPI title = contentPanel.createUIElement(width, 70f, false);
        title.setParaFont(Fonts.ORBITRON_20AA);
        title.addPara(line.spec.getName(), Misc.getTooltipTitleAndLightHighlightColor(),2f).setAlignment(Alignment.MID);
        contentPanel.addUIElement(title).inTL(0, 0);
        float y = title.getHeightSoFar() + 12f;
        UILinesRenderer renderer = new UILinesRenderer(0f);
        ImagePanel panel = new ImagePanel();
        CustomPanelAPI panelHolder = Global.getSettings().createCustom(190,95,renderer);
        CustomPanelAPI panelImage  = panelHolder.createCustomPanel(190,95,panel);
        renderer.setPanel(panelImage);
        panel.init(panelImage,Global.getSettings().getSprite(line.spec.getImageName()));
        panelHolder.addComponent(panelImage).inTL(0, 0);
        TooltipMakerAPI image = contentPanel.createUIElement(190f, 95f, false);
        image.addCustom(panelHolder,0f).getPosition().inTL(0,0);
        contentPanel.addUIElement(image).inTL((WIDTH - 190f) / 2f, y);
        y += 107f;

        TooltipMakerAPI description = contentPanel.createUIElement(width, height, false);
        String desc = line.spec.getDesc();
        if (desc != null && !desc.isEmpty()) {
            description.addPara(desc, 0f);
        }
        description.addPara("Values shown for a size 6 world, without additional bonuses and with all inputs supplied.",
                Misc.getGrayColor(), 10f);
        description.addSectionHeading("Current Production Plan", Alignment.MID, 12f);
        description.addPara(line.getRecipeName(), line.getRecipeColor(), 6f).setAlignment(Alignment.MID);
        contentPanel.addUIElement(description).inTL(0, y);
        y += description.getHeightSoFar() + 10f;

        // Only these two sections share a scroller. Everything above and the button
        // below are separate, fixed components.
        TooltipMakerAPI amounts = contentPanel.createUIElement(width,
                Math.max(1f, height - 40f - y), true);
        if (line.error != null) amounts.addPara(line.error, Misc.getNegativeHighlightColor(), 0f);
        addAmounts(amounts, "Expected production", line.production, false, width - 10f);
        addAmounts(amounts, "Expected demand", line.demand, true, width - 10f);
        contentPanel.addUIElement(amounts).inTL(0f, y);
        mainPanel.addComponent(contentPanel).inTL(0, 0);
    }

    private void addAmounts(TooltipMakerAPI tooltip, String heading, Map<String, Integer> amounts,
                            boolean demand, float width) {
        tooltip.addSectionHeading(heading, Alignment.MID, demand ? 10f : 0f);
        if (amounts.isEmpty()) {
            tooltip.addPara(line.error == null ? "None" : "Unavailable", Misc.getGrayColor(), 5f);
            return;
        }
        for (Map.Entry<String, Integer> entry : amounts.entrySet()) {
            CustomPanelAPI row = contentPanel.createCustomPanel(width, 40f, null);
            TooltipMakerAPI name = row.createUIElement(width - 155f, 40f, false);
            name.addPara(Global.getSettings().getCommoditySpec(entry.getKey()).getName(), 12f);
            row.addUIElement(name).inTL(0, 0);
            AoTDCommodityShortPanel commodity = new AoTDCommodityShortPanel(entry.getKey(), entry.getValue(),
                    demand ? Color.ORANGE : Misc.getPositiveHighlightColor(), 150f, 40f, demand, false);
            row.addComponent(commodity.getMainPanel()).inTL(width - 150f, 0);
            tooltip.addCustom(row, 3f);
        }
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
