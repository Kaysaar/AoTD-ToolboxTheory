package data.kaysaar.aotd.tot.ui.production.table.prodplan;

import ashlib.data.plugins.ui.models.CustomButton;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLine;
import data.kaysaar.aotd.tot.ui.production.ProductionPlanCommodities;

public class ProductionPlanLineButton extends CustomButton {
    final AoTDProductionLine plan;
    private final ProductionPlanTable table;

    public ProductionPlanLineButton(ProductionPlanTable table, float width, float height,
                                    AoTDProductionLine plan) {
        super(width, height, plan, 0f, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor());
        this.table = table;
        this.plan = plan;
    }

    @Override
    public void createButtonContent(TooltipMakerAPI tooltip) {
        CustomPanelAPI content = mainPanel.createCustomPanel(width, height, null);
        ProductionPlanCommodities.addSingleLineText(content, plan.getRecipeName(), plan.getRecipeColor(),
                0f, table.nameWidth, height, Alignment.MID);
        ProductionPlanCommodities.add(content, plan.production, false, plan.error,
                table.nameWidth, 0, table.commodityWidth, height);
        ProductionPlanCommodities.add(content, plan.demand, true, plan.error,
                table.nameWidth + table.commodityWidth, 0, table.commodityWidth, height);
        // Match the checkbox inset so each cell starts under its column header.
        tooltip.addCustom(content, 0f).getPosition().inTL(5f, 0);
    }
}
