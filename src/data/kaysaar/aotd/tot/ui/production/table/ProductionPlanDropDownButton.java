package data.kaysaar.aotd.tot.ui.production.table;

import ashlib.data.plugins.ui.models.DropDownButton;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLine;

public class ProductionPlanDropDownButton extends DropDownButton {
    private final AoTDProductionLine plan;

    public ProductionPlanDropDownButton(ProductionPlanTable table, float width, float height,
                                        AoTDProductionLine plan) {
        super(table, width, height, 0, 0, false);
        this.plan = plan;
    }

    @Override
    public void createUIContent() {
        super.createUIContent();
        mainButton = new ProductionPlanLineButton((ProductionPlanTable) tableOfReference, width, height, plan);
        mainButton.createUI();
        tooltipOfImpl.addCustom(mainButton.getPanel(), 0f).getPosition().inTL(0, 0);
    }
}
