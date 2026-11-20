package data.kaysaar.aotd.tot.ui.production.table.prodplan;

import ashlib.data.plugins.ui.models.CustomButton;
import ashlib.data.plugins.ui.models.DropDownButton;
import data.kaysaar.aotd.tot.ui.production.table.ProductionTable;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLine;
import data.kaysaar.aotd.tot.ui.production.ProductionPlanCommodities;

import java.util.List;
import java.util.function.Consumer;

public class ProductionPlanTable extends ProductionTable {
    final float nameWidth;
    final float commodityWidth;
    private AoTDProductionLine selected;
    private final Consumer<AoTDProductionLine> onSelected;

    public ProductionPlanTable(float width, float height, List<AoTDProductionLine> plans,
                               AoTDProductionLine selected, Consumer<AoTDProductionLine> onSelected) {
        super(width, height);
        this.selected = selected;
        this.onSelected = onSelected;
        float rowWidth = width - 20f;
        nameWidth = rowWidth * 0.36f;
        commodityWidth = (rowWidth - nameWidth) / 2f;
        for (AoTDProductionLine plan : plans) {
            float rowHeight = ProductionPlanCommodities.requiredButtonHeight(commodityWidth,
                    plan.production.size(), plan.demand.size());
            dropDownButtons.add(new ProductionPlanDropDownButton(this, rowWidth, rowHeight, plan));
        }
    }

    @Override
    public void createSections() {
        addHeader("Name of plan", 0, nameWidth, false);
        addHeader(selected != null && selected.local ? "Supply" : "Avg. Supply", nameWidth, commodityWidth, false);
        addHeader(selected != null && selected.local ? "Demand" : "Avg. Demand", nameWidth + commodityWidth, commodityWidth, false);
        mainPanel.addUIElement(tooltipOfButtons).inTL(0, 0);
    }

    @Override
    public void createTable() {
        super.createTable();
        highlightSelection();
    }

    @Override
    public void reportButtonPressed(CustomButton button) {
        if (!(button instanceof ProductionPlanLineButton row) || row.plan == selected) return;
        selected = row.plan;
        highlightSelection();
        onSelected.accept(selected);
    }

    private void highlightSelection() {
        for (DropDownButton row : dropDownButtons) {
            if (row.mainButton.buttonData == selected) row.mainButton.mainButton.highlight();
            else row.mainButton.mainButton.unhighlight();
        }
    }
}
