package data.kaysaar.aotd.tot.ui.production.table;

import ashlib.data.plugins.ui.models.CustomButton;
import ashlib.data.plugins.ui.models.DropDownButton;
import ashlib.data.plugins.ui.plugins.UITableImpl;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLine;
import data.kaysaar.aotd.tot.ui.production.ProductionPlanCommodities;

import java.util.List;
import java.util.function.Consumer;

public class ProductionPlanTable extends UITableImpl {
    final float nameWidth;
    final float commodityWidth;
    private AoTDProductionLine selected;
    private final Consumer<AoTDProductionLine> onSelected;

    public ProductionPlanTable(float width, float height, List<AoTDProductionLine> plans,
                               AoTDProductionLine selected, Consumer<AoTDProductionLine> onSelected) {
        super(width, height, true, 0, 0);
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
        addHeader("Name of plan", 0, nameWidth);
        addHeader("Avg. Supply", nameWidth, commodityWidth);
        addHeader("Avg. Demand", nameWidth + commodityWidth, commodityWidth);
        mainPanel.addUIElement(tooltipOfButtons).inTL(0, 0);
    }

    private void addHeader(String title, float x, float width) {
        ButtonAPI header = tooltipOfButtons.addAreaCheckbox(title, null, Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), width, 20f, 0f);
        header.setClickable(false);
        // Match the scroller inset plus the row checkbox's own tooltip inset.
        header.getPosition().inTL(x + 10f, 0);
    }

    @Override
    public void createTable() {
        float bodyHeight = height - 22f;
        panelToWorkWith = mainPanel.createCustomPanel(width, bodyHeight, null);
        tooltipOfImpl = panelToWorkWith.createUIElement(width, bodyHeight, true);
        for (DropDownButton button : dropDownButtons) {
            button.resetUI();
            button.createUI();
            tooltipOfImpl.addCustom(button.getPanelOfImpl(), 2f);
        }
        panelToWorkWith.addUIElement(tooltipOfImpl).inTL(0, 0);
        mainPanel.addComponent(panelToWorkWith).inTL(0, 22f);
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
