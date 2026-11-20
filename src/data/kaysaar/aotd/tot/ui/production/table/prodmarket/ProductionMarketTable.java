package data.kaysaar.aotd.tot.ui.production.table.prodmarket;

import ashlib.data.plugins.ui.models.CustomButton;
import ashlib.data.plugins.ui.models.DropDownButton;
import data.kaysaar.aotd.tot.ui.production.table.ProductionTable;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ProductionMarketTable extends ProductionTable {
    static final float MARKET_COLUMN = 0.65f;
    public final Set<MarketAPI> selected = new LinkedHashSet<>();
    private final Runnable onSelection;

    public ProductionMarketTable(float width, float height, Map<MarketAPI, Integer> counts, Runnable onSelection) {
        super(width, height);
        this.onSelection = onSelection;
        for (var entry : counts.entrySet()) {
            dropDownButtons.add(new ProductionMarketDropDownButton(this, width - 20f, entry.getKey(), entry.getValue()));
        }
    }

    @Override
    public void createSections() {
        float rowWidth = width - 20f;
        float marketWidth = rowWidth * MARKET_COLUMN;
        addHeader("Market", 0, marketWidth, false);
        addHeader("Industries affected", marketWidth, rowWidth - marketWidth, false);
        mainPanel.addUIElement(tooltipOfButtons).inTL(0, 0);
    }

    @Override
    public void createTable() {
        super.createTable();
        if (dropDownButtons.isEmpty()) tooltipOfImpl.addPara("No player-controlled markets.", 5f);
        highlightSelection();
    }

    @Override
    public void reportButtonPressed(CustomButton button) {
        MarketAPI market = (MarketAPI) button.buttonData;
        if (!selected.remove(market)) selected.add(market);
        highlightSelection();
        onSelection.run();
    }

    public void toggleAll() {
        boolean clear = selected.size() == dropDownButtons.size();
        selected.clear();
        if (!clear) {
            for (DropDownButton row : dropDownButtons) selected.add((MarketAPI) row.mainButton.buttonData);
        }
        highlightSelection();
        onSelection.run();
    }

    private void highlightSelection() {
        for (DropDownButton row : dropDownButtons) {
            if (selected.contains(row.mainButton.buttonData)) row.mainButton.mainButton.highlight();
            else row.mainButton.mainButton.unhighlight();
        }
    }
}
