package data.kaysaar.aotd.tot.ui.production.table;

import ashlib.data.plugins.ui.models.CustomButton;
import ashlib.data.plugins.ui.models.DropDownButton;
import ashlib.data.plugins.ui.plugins.UITableImpl;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLine;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProductionLineTable extends UITableImpl {
    public final LinkedHashMap<String, Float> widthMap = new LinkedHashMap<>();
    public final List<AoTDProductionLine> lines;
    public AoTDProductionLine currentlyChosen;
    ButtonAPI buttonName, buttonRecipe;
    float scroll;

    public ProductionLineTable(float width, float height, List<AoTDProductionLine> lines) {
        super(width, height, true, 0, 0);
        this.lines = lines;
        resizeColumns(width);
        for (AoTDProductionLine line : lines) {
            dropDownButtons.add(new ProductionLineDropDownButton(this, width - 20f, line));
        }
        if (!lines.isEmpty()) currentlyChosen = lines.get(0);
    }

    public void resizeColumns(float width) {
        // Name and recipe stop growing early; all remaining width is split equally.
        // Recompute from scratch, so reopening/resizing cannot accumulate width.
        float available = width - 23f; // left inset, scrollbar allowance, three dividers
        float name = Math.min(285f, available * 0.42f);
        float recipe = Math.min(155f, available * 0.22f);
        float commodities = (available - name - recipe) / 2f;
        widthMap.put("name", name);
        widthMap.put("recipe", recipe);
        widthMap.put("production", commodities);
        widthMap.put("demand", commodities);
    }

    public float getStartingX(String id) {
        float x = 0;
        for (Map.Entry<String, Float> entry : widthMap.entrySet()) {
            if (id.equals(entry.getKey())) break;
            x += entry.getValue() + 1f;
        }
        return x;
    }

    @Override
    public void createSections() {
        buttonName = addHeader("Name", "name");
        buttonRecipe = addHeader("Current recipe", "recipe");
        addHeader("Avg. Prod.", "production").setClickable(false);
        addHeader("Avg. Dem.", "demand").setClickable(false);
        mainPanel.addUIElement(tooltipOfButtons).inTL(0, 0);
    }

    private ButtonAPI addHeader(String text, String column) {
        ButtonAPI button = tooltipOfButtons.addAreaCheckbox(text, SortingState.NON_INITIALIZED,
                Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
                widthMap.get(column), 20, 0f);
        button.getPosition().inTL(10f + getStartingX(column), 0);
        return button;
    }

    @Override
    public void createTable() {
        // UITableImpl's default body uses the full height before its 22px header offset.
        panelToWorkWith = mainPanel.createCustomPanel(width, height - 22f, null);
        tooltipOfImpl = panelToWorkWith.createUIElement(width, height - 22f, true);
        for (DropDownButton button : dropDownButtons) {
            button.resetUI();
            button.createUI();
            tooltipOfImpl.addCustom(button.getPanelOfImpl(), 2f);
        }
        panelToWorkWith.addUIElement(tooltipOfImpl).inTL(0, 0);
        if (tooltipOfImpl.getExternalScroller() != null) {
            scroll = Math.max(0f, Math.min(scroll, tooltipOfImpl.getHeightSoFar() - (height - 22f) + 2f));
            tooltipOfImpl.getExternalScroller().setYOffset(scroll);
        }
        updateHighlight();
        mainPanel.addComponent(panelToWorkWith).inTL(0, 22f);
    }

    public float getScroll() {
        if (tooltipOfImpl != null && tooltipOfImpl.getExternalScroller() != null) {
            scroll = tooltipOfImpl.getExternalScroller().getYOffset();
        }
        return scroll;
    }

    public void setScroll(float scroll) { this.scroll = scroll; }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        getScroll();
        sort(buttonName, Comparator.comparing(button -> ((ProductionLineDropDownButton) button).line.spec.getName(),
                String.CASE_INSENSITIVE_ORDER));
        sort(buttonRecipe, Comparator.comparing(button -> ((ProductionLineDropDownButton) button).line.getRecipeName(),
                String.CASE_INSENSITIVE_ORDER));
    }

    private void sort(ButtonAPI button, Comparator<DropDownButton> comparator) {
        if (button == null || !button.isChecked()) return;
        button.setChecked(false);
        SortingState state = switchState((SortingState) button.getCustomData());
        button.setCustomData(state);
        dropDownButtons.sort(state == SortingState.ASCENDING ? comparator : comparator.reversed());
        recreateTable();
    }

    private void updateHighlight() {
        for (DropDownButton button : dropDownButtons) {
            if (button.mainButton == null) continue;
            if (((ProductionLineDropDownButton) button).line == currentlyChosen) button.mainButton.mainButton.highlight();
            else button.mainButton.mainButton.unhighlight();
        }
    }

    @Override
    public void reportButtonPressed(CustomButton button) {
        if (button instanceof ProductionLineButton lineButton) {
            currentlyChosen = lineButton.line;
            updateHighlight();
        }
    }
}
