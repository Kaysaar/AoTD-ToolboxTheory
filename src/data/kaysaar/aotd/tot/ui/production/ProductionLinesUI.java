package data.kaysaar.aotd.tot.ui.production;

import ashlib.data.plugins.ui.models.ExtendedUIPanelPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLine;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLineManager;
import data.kaysaar.aotd.tot.ui.production.table.ProductionLineTable;

import java.util.List;

public class ProductionLinesUI implements ExtendedUIPanelPlugin {
    CustomPanelAPI mainPanel, contentPanel;
    public ProductionLineTable table;
    public IndustryPlanDetails details;
    public ProductionLineActions actions;
    String selectedIndustry;
    String lastActionResult;
    AoTDProductionLineManager.ApplicationReport lastApplicationReport;
    boolean refreshRequested;

    public void requestRefresh() { refreshRequested = true; }

    public ProductionLinesUI(float width, float height) {
        mainPanel = Global.getSettings().createCustom(width, height, this);
        createUI();
    }

    @Override
    public CustomPanelAPI getMainPanel() { return mainPanel; }

    @Override
    public void createUI() {
        float scroll = table == null ? 0f : table.getScroll();
        if (table != null && table.currentlyChosen != null) selectedIndustry = table.currentlyChosen.spec.getId();
        clearUI();
        float width = mainPanel.getPosition().getWidth();
        float height = mainPanel.getPosition().getHeight();
        float bodyHeight = height - 40f;
        float tableWidth = width - IndustryPlanDetails.WIDTH - 5f;
        contentPanel = mainPanel.createCustomPanel(width, height, null);

        List<AoTDProductionLine> lines = AoTDProductionLineManager.getLines();
        table = new ProductionLineTable(tableWidth, bodyHeight, lines);
        for (AoTDProductionLine line : lines) {
            if (line.spec.getId().equals(selectedIndustry)) table.currentlyChosen = line;
        }
        table.setScroll(scroll);
        table.createSections();
        table.createTable();
        details = new IndustryPlanDetails(bodyHeight, table.currentlyChosen, this::requestRefresh);
        actions = new ProductionLineActions(width, this);

        contentPanel.addComponent(table.mainPanel).inTL(-10, 0);
        contentPanel.addComponent(details.getMainPanel()).inTL(tableWidth + 5f, 1);
        contentPanel.addComponent(actions.getMainPanel()).inTL(0, height - 30f);
        mainPanel.addComponent(contentPanel).inTL(0, 0);
    }

    @Override
    public void clearUI() {
        if (contentPanel != null) mainPanel.removeComponent(contentPanel);
        contentPanel = null;
    }

    @Override
    public void advance(float amount) {
        if (refreshRequested) {
            refreshRequested = false;
            createUI();
        }
        if (table != null && details != null) details.setLine(table.currentlyChosen);
    }

    @Override public void positionChanged(PositionAPI position) { }
    @Override public void renderBelow(float alphaMult) { }
    @Override public void render(float alphaMult) { }
    @Override public void processInput(List<InputEventAPI> events) { }
    @Override public void buttonPressed(Object buttonId) { }
}
