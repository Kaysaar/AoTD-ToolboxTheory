package data.kaysaar.aotd.tot.ui.production.table;

import data.kaysaar.aotd.tot.ui.production.ProductionPlanCommodities;
import ashlib.data.plugins.ui.models.DropDownButton;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLine;

public class ProductionLineDropDownButton extends DropDownButton {
    public final AoTDProductionLine line;

    public ProductionLineDropDownButton(ProductionLineTable table, float width, AoTDProductionLine line) {
        super(table, width, Math.max(70f, ProductionPlanCommodities.requiredButtonHeight(
                table.widthMap.get("production") - 6f, line.production.size(), line.demand.size())), 0, 0, false);
        this.line = line;
    }

    @Override
    public void createUIContent() {
        super.createUIContent();
        mainButton = new ProductionLineButton(width, height, line, (ProductionLineTable) tableOfReference);
        mainButton.createUI();
        tooltipOfImpl.addCustom(mainButton.getPanel(), 0f).getPosition().inTL(0, 0);
    }
}
