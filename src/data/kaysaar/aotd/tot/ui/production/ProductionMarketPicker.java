package data.kaysaar.aotd.tot.ui.production;

import ashlib.data.plugins.misc.AshMisc;
import data.kaysaar.aotd.tot.ui.production.table.prodmarket.ProductionMarketTable;
import ashlib.data.plugins.ui.models.BasePopUpDialog;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLineManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProductionMarketPicker extends BasePopUpDialog {
    private final boolean force;
    private final Runnable onQueued;
    private ProductionMarketTable table;
    private final Map<MarketAPI, Integer> counts = new LinkedHashMap<>();
    private ButtonAPI selectAll;
    private LabelAPI summary;

    public ProductionMarketPicker(boolean force, Runnable onQueued) {
        super(force ? "Force Apply on markets" : "Apply on markets");
        this.force = force;
        this.onQueued = onQueued;
    }

    @Override
    public void createUI(CustomPanelAPI panel) {
        createHeaader(panel);
        float width = panel.getPosition().getWidth() - 30f;
        float infoTop = goalSizeY - 100f;
        counts.clear();
        List<MarketAPI> markets = new ArrayList<>(AshMisc.getMarketsUnderPlayer());
        markets.sort(Comparator.comparing(MarketAPI::getName, String.CASE_INSENSITIVE_ORDER));
        for (MarketAPI market : markets) {
            int count = AoTDProductionLineManager.previewMarket(market, force).queuedIndustries;
            counts.put(market, count);
        }
        table = new ProductionMarketTable(width, infoTop - y - 8f, counts, this::updateSelection);
        table.createSections();
        table.createTable();
        panel.addComponent(table.mainPanel).inTL(x, y);
        addPanel(table.mainPanel);
        TooltipMakerAPI info = panel.createUIElement(width, 50f, false);
        info.addPara(force ? "Replace local orders and manual plans. Waiting industries update demand immediately. Operating industries switch at month end."
                : "Preserve local orders and manual plans. Waiting industries update demand immediately. Operating industries switch at month end.", 0f);
        summary = info.addPara("", Misc.getHighlightColor(), 6f);
        panel.addUIElement(info).inTL(x, infoTop);
        createConfirmAndCancelSection(panel);
        TooltipMakerAPI footer = panel.createUIElement(buttonConfirmWidth, 25f, false);
        footer.setButtonFontOrbitron20();
        selectAll = footer.addButton("Select all", null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                Alignment.MID, CutStyle.TL_BR, buttonConfirmWidth, 25f, 0f);
        selectAll.getPosition().inTL(0, 0);
        panel.addUIElement(footer).inTL(panel.getPosition().getWidth() - 3f * buttonConfirmWidth - 25f, goalSizeY - 40f);
        selectAll.setEnabled(!counts.isEmpty());
        updateSelection();
    }

    private void updateSelection() {
        int markets = table.selected.size();
        int industries = 0;
        for (MarketAPI market : table.selected) industries += counts.get(market);
        summary.setText(markets + " markets selected   |   " + industries + " industries affected");
        selectAll.setText(markets == counts.size() && markets > 0 ? "Clear selection" : "Select all");
        confirmButton.setEnabled(markets > 0);
    }

    @Override
    public void advance(float amount) {
        if (selectAll != null && selectAll.isChecked()) {
            selectAll.setChecked(false);
            table.toggleAll();
        }
        super.advance(amount);
    }

    @Override
    public void applyConfirmScript() {
        if (table.selected.isEmpty()) return;
        AoTDProductionLineManager.queueMarkets(new ArrayList<>(table.selected), force);
        onQueued.run();
    }
}

