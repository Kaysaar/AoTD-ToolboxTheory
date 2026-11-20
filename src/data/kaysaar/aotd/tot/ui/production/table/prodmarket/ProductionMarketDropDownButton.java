package data.kaysaar.aotd.tot.ui.production.table.prodmarket;

import ashlib.data.plugins.ui.models.DropDownButton;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

class ProductionMarketDropDownButton extends DropDownButton {
    private final MarketAPI market;
    private final int count;

    ProductionMarketDropDownButton(ProductionMarketTable table, float width, MarketAPI market, int count) {
        super(table, width, 76f, 0, 0, false);
        this.market = market;
        this.count = count;
    }

    @Override
    public void createUIContent() {
        super.createUIContent();
        mainButton = new ProductionMarketButton(width, height, market, count);
        mainButton.createUI();
        tooltipOfImpl.addCustom(mainButton.getPanel(), 0f).getPosition().inTL(0, 0);
    }
}
