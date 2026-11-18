package data.kaysaar.aotd.tot.ui.production.table;

import ashlib.data.plugins.ui.models.DropDownButton;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLine;

public class ProductionLineDropDownButton extends DropDownButton {
    public final AoTDProductionLine line;

    public ProductionLineDropDownButton(ProductionLineTable table, float width, AoTDProductionLine line) {
        super(table, width, 70f, 0, 0, false);
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
