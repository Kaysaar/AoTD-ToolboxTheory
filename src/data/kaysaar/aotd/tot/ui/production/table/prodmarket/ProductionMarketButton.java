package data.kaysaar.aotd.tot.ui.production.table.prodmarket;

import ashlib.data.plugins.ui.EntityWithNameComponent;
import ashlib.data.plugins.ui.models.CustomButton;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import data.kaysaar.aotd.tot.ui.production.ProductionPlanCommodities;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

class ProductionMarketButton extends CustomButton {
    private final int count;

    ProductionMarketButton(float width, float height, MarketAPI market, int count) {
        super(width, height, market, 0f, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor());
        this.count = count;
    }

    @Override
    public void createButtonContent(TooltipMakerAPI tooltip) {
        MarketAPI market = (MarketAPI) buttonData;
        CustomPanelAPI row = Global.getSettings().createCustom(width, height, null);
        float nameWidth = width * ProductionMarketTable.MARKET_COLUMN;
        float entityWidth = Math.min(nameWidth - 8f, 180f);
        if (market.getPrimaryEntity() != null) {
            EntityWithNameComponent entity = new EntityWithNameComponent(market.getPrimaryEntity(), entityWidth, 50f, true);
            entity.createUI();
            row.addComponent(entity.getMainPanel()).inTL(4f, 8f);
        } else {
            ProductionPlanCommodities.addText(row, market.getName() + " (size " + market.getSize() + ")",
                    Misc.getTextColor(), 4f, 0, entityWidth, height);
        }
        ProductionPlanCommodities.addText(row, String.valueOf(count), Misc.getHighlightColor(),
                nameWidth, 0, width - nameWidth, height);
        tooltip.addCustom(row, 0f).getPosition().inTL(5f, 0);
    }
}
