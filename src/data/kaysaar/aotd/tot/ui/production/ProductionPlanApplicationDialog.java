package data.kaysaar.aotd.tot.ui.production;

import ashlib.data.plugins.ui.models.BasePopUpDialog;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLineManager;



public class ProductionPlanApplicationDialog extends BasePopUpDialog {
    private final AoTDProductionLineManager.ApplicationReport report;

    public ProductionPlanApplicationDialog(AoTDProductionLineManager.ApplicationReport report) {
        super(report.applied ? "Production Plans Applied" : "Production Plans Queued");
        this.report = report;
    }

    @Override public void createUI(CustomPanelAPI panel) {
        createHeaader(panel);
        TooltipMakerAPI tooltip = panel.createUIElement(panel.getPosition().getWidth() - 30f,
                panel.getPosition().getHeight() - y - 40f, true);
        if (!report.applied) {
            tooltip.addPara("At month end, colonies collect output from their current plans, then consume inputs for the new plans. Market counts below are estimates.", 0f);
        }
        tooltip.addPara(report.getSummary(), Misc.getHighlightColor(), report.applied ? 0f : 10f);
        tooltip.addPara("Markets with matching industry types: " + report.examinedMarkets, 10f);
        tooltip.addSectionHeading("Markets", Alignment.MID, 16f);
        for (String detail : report.marketDetails) tooltip.addPara(detail, 8f);
        if (!report.details.isEmpty()) {
            tooltip.addSectionHeading("Skipped industries and errors", Alignment.MID, 16f);
            for (String detail : report.details) tooltip.addPara(detail, Misc.getNegativeHighlightColor(), 8f);
        }
        panel.addUIElement(tooltip).inTL(x, y);
        addTooltip(tooltip);
        TooltipMakerAPI buttons = panel.createUIElement(buttonConfirmWidth, 25f, false);
        buttons.setButtonFontOrbitron20();
        setCancelText("Close");
        generateCancelButton(buttons);
        panel.addUIElement(buttons).inTL(panel.getPosition().getWidth() - buttonConfirmWidth - 15f, goalSizeY - 30f);
    }
}
