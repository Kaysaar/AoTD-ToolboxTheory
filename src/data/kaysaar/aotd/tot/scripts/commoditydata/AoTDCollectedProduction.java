package data.kaysaar.aotd.tot.scripts.commoditydata;

import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import data.kaysaar.aotd.tot.scripts.trade.manager.AoTDTradeManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AoTDCollectedProduction implements AutoCloseable {
    private final Set<MarketAPI> markets = new HashSet<>();
    private final List<AoTDSupplyDemandData> captured = new ArrayList<>();

    public boolean includes(MarketAPI market) {
        return markets.contains(market);
    }

    public void collect(MarketAPI market) {
        if (!markets.add(market)) return;
        AoTDSupplyDemandData.updateSupplyDemandDataAcrossMarket(market);
        for (CommodityOnMarketAPI commodity : market.getAllCommodities()) {
            if (!(commodity instanceof AoTDCommodityOnMarket com)) continue;
            AoTDSupplyDemandData data = com.getSupplyDemandData();
            captured.add(data);
            data.collectedSupply = data.getTotalRawUnitsFromSupply();
            com.getExcDefData().collectTradeSurplus(com);
        }
    }

    public void refreshDemand(MarketAPI market) {
        AoTDSupplyDemandData.updateSupplyDemandDataAcrossMarket(market);
        for (CommodityOnMarketAPI commodity : market.getAllCommodities()) {
            if (!(commodity instanceof AoTDCommodityOnMarket com)) continue;
            AoTDSupplyDemandData data = com.getSupplyDemandData();
            if (data.collectedSupply == null) {
                captured.add(data);
                data.collectedSupply = 0;
            }
        }
        AoTDTradeManager.getInstance().addMarket(market);
    }

    @Override
    public void close() {
        for (AoTDSupplyDemandData data : captured) data.collectedSupply = null;
    }
}
