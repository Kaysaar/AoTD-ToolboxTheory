package data.kaysaar.aotd.tot.ui.production.table.prodline;

import ashlib.data.plugins.ui.models.CustomButton;
import ashlib.data.plugins.ui.models.resizable.ImageViewer;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLine;
import data.kaysaar.aotd.tot.ui.production.ProductionPlanCommodities;

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
        ImageViewer viewer = new ImageViewer(imageHeight * 2f, imageHeight, line.getIndustryImage());
        content.addComponent(viewer.getComponentPanel()).inTL(2f, (height - imageHeight) / 2f);
        float nameX = imageHeight * 2f + 8f;
        ProductionPlanCommodities.addSingleLineText(content, line.getIndustryName(), Misc.getTooltipTitleAndLightHighlightColor(),
                nameX, nameWidth - nameX - 4f, height, Alignment.LMID);
        ProductionPlanCommodities.addSingleLineText(content, line.getRecipeName(), line.getRecipeColor(),
                table.getStartingX("recipe") + 5f, table.widthMap.get("recipe") - 10f, height, Alignment.MID);
        addCommodities(content, line.production, "production", false);
        addCommodities(content, line.demand, "demand", true);
        tooltip.addCustom(content, 0f).getPosition().inTL(5, 0);
    }

    private void addCommodities(CustomPanelAPI content, Map<String, Integer> amounts, String column, boolean demand) {
        ProductionPlanCommodities.add(content, amounts, demand, line.error,
                table.getStartingX(column) + 3f, 0, table.widthMap.get(column) - 6f, height);
    }
}
