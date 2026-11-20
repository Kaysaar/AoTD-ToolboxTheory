package data.kaysaar.aotd.tot.ui.production.table;

import ashlib.data.plugins.ui.models.DropDownButton;
import ashlib.data.plugins.ui.plugins.UITableImpl;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;

public abstract class ProductionTable extends UITableImpl {
    protected ProductionTable(float width, float height) {
        super(width, height, true, 0, 0);
    }

    protected ButtonAPI addHeader(String title, float left, float columnWidth, boolean sortable) {
        ButtonAPI header = tooltipOfButtons.addAreaCheckbox(title, sortable ? SortingState.NON_INITIALIZED : null,
                Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), columnWidth, 20f, 0f);
        header.setClickable(sortable);
        header.getPosition().inTL(left + 10f, 0);
        return header;
    }

    @Override
    public void createTable() {
        float bodyHeight = height - 22f;
        panelToWorkWith = mainPanel.createCustomPanel(width, bodyHeight, null);
        tooltipOfImpl = panelToWorkWith.createUIElement(width, bodyHeight, true);
        for (DropDownButton row : dropDownButtons) {
            row.resetUI();
            row.createUI();
            tooltipOfImpl.addCustom(row.getPanelOfImpl(), 2f);
        }
        panelToWorkWith.addUIElement(tooltipOfImpl).inTL(0, 0);
        mainPanel.addComponent(panelToWorkWith).inTL(0, 22f);
    }
}
