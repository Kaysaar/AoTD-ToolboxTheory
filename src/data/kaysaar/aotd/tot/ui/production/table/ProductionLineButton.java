package data.kaysaar.aotd.tot.ui.production.table;

import ashlib.data.plugins.ui.models.CustomButton;
import ashlib.data.plugins.ui.models.resizable.ImageViewer;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLine;
import data.kaysaar.aotd.tot.ui.production.ProductionPlanCommodities;

import java.awt.Color;
import java.util.Map;

public class ProductionLineButton extends CustomButton {
    public final AoTDProductionLine line;
    final ProductionLineTable table;

    public ProductionLineButton(float width, float height, AoTDProductionLine line, ProductionLineTable table) {
        super(width, height, line, 0f, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor());
        this.line = line;
        this.table = table;
    }

    @Override
    public void createButtonContent(TooltipMakerAPI tooltip) {
        CustomPanelAPI content = Global.getSettings().createCustom(width, height, null);
        float nameWidth = table.widthMap.get("name");
        float imageHeight = Math.min(66f, (nameWidth - 4f) / 4f);
        ImageViewer viewer = new ImageViewer(imageHeight * 2f, imageHeight, line.spec.getImageName());
        content.addComponent(viewer.getComponentPanel()).inTL(2f, (height - imageHeight) / 2f);
        addText(content, line.spec.getName(), Misc.getTooltipTitleAndLightHighlightColor(), imageHeight * 2f + 8f, nameWidth - imageHeight * 2f - 12f, null);
        addText(content, line.getRecipeName(), line.getRecipeColor(), table.getStartingX("recipe") + 5f,
                table.widthMap.get("recipe") - 10f,Alignment.MID);
        addCommodities(content, line.production, "production", false);
        addCommodities(content, line.demand, "demand", true);
        tooltip.addCustom(content, 0f).getPosition().inTL(5, 0);
    }

    private void addText(CustomPanelAPI content, String text, Color color, float x, float width,Alignment alignment) {
        TooltipMakerAPI tooltip = content.createUIElement(Math.max(1f, width), height, false);
        LabelAPI label = tooltip.addPara(text, color, 0f);
        if(alignment!=null) {
            label.setAlignment(alignment);
        }
        label.getPosition().inTL(0, Math.max(0f, (height - label.computeTextHeight(text)) / 2f));
        content.addUIElement(tooltip).inTL(x, 0);
    }

    private void addCommodities(CustomPanelAPI content, Map<String, Integer> amounts, String column, boolean demand) {
        ProductionPlanCommodities.add(content, amounts, demand, line.error,
                table.getStartingX(column) + 3f, 0, table.widthMap.get(column) - 6f, height);
    }
}
