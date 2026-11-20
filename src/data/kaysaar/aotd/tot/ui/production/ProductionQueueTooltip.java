package data.kaysaar.aotd.tot.ui.production;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLineManager;

class ProductionQueueTooltip implements TooltipMakerAPI.TooltipCreator {
    private final AoTDProductionLineManager.ApplicationReport queue;

    ProductionQueueTooltip(AoTDProductionLineManager.ApplicationReport queue) {
        this.queue = queue;
    }

    @Override
    public boolean isTooltipExpandable(Object param) {
        return AoTDProductionLineManager.getLastApplication() != null;
    }

    @Override
    public float getTooltipWidth(Object param) {
        return 420f;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object param) {
        tooltip.addTitle("Recipes queued for month end");
        if (queue.queuedByMarket.isEmpty()) tooltip.addPara("No recipe changes queued.", 5f);
        for (var entry : queue.queuedByMarket.entrySet()) {
            tooltip.addSectionHeading(entry.getKey(), Alignment.MID, 8f);
            for (String detail : entry.getValue()) tooltip.addPara(detail, 4f);
        }
        var last = AoTDProductionLineManager.getLastApplication();
        if (expanded && last != null) {
            tooltip.addSectionHeading("Last application", Alignment.MID, 10f);
            tooltip.addPara(last.getSummary(), 5f);
            for (String market : last.marketDetails) tooltip.addPara(market, 4f);
            for (String detail : last.details) tooltip.addPara(detail, Misc.getNegativeHighlightColor(), 4f);
        }
    }
}
