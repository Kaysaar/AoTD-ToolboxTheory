package data.kaysaar.aotd.tot.ui.production;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.ui.commoditypanel.AoTDCommodityShortPanel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ProductionPlanCommodities {
    private static final float CELL_WIDTH = 95f;
    private static final float CELL_HEIGHT = 30f;

    private ProductionPlanCommodities() { }

    private static int columns(float width) {
        return Math.max(1, (int) (width / CELL_WIDTH));
    }

    public static float requiredButtonHeight(float columnWidth, int supplyCount, int demandCount) {
        int items = Math.max(1, Math.max(supplyCount, demandCount));
        int columns = columns(columnWidth);
        int rows = 1 + (items - 1) / columns;
        return 8f + rows * CELL_HEIGHT;
    }

    public static void add(CustomPanelAPI parent, Map<String, Integer> amounts, boolean demand,
                           String error, float x, float y, float width, float height) {
        if (error != null || amounts.isEmpty()) {
            addText(parent, error == null ? "None" : "Unavailable", Misc.getGrayColor(), x, y, width, height);
            return;
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(amounts.entrySet());
        int columns = columns(width);
        int capacity = columns * Math.max(1, (int) (height / CELL_HEIGHT));
        boolean overflow = entries.size() > capacity;
        int shown = overflow ? capacity - 1 : entries.size();
        int cells = shown + (overflow ? 1 : 0);
        int rows = (cells + columns - 1) / columns;
        float top = y + (height - rows * CELL_HEIGHT) / 2f;
        for (int row = 0, index = 0; row < rows; row++) {
            int count = Math.min(columns, cells - index);
            // Every occupied row gets equal slots. The final partial row is centered too.
            float slot = width / count;
            float cell = Math.min(CELL_WIDTH, slot);
            for (int col = 0; col < count; col++, index++) {
                float cellX = x + slot * col + (slot - cell) / 2f;
                if (index < shown) {
                    Map.Entry<String, Integer> entry = entries.get(index);
                    AoTDCommodityShortPanel commodity = new AoTDCommodityShortPanel(entry.getKey(), entry.getValue(),
                            demand ? Color.ORANGE : Misc.getPositiveHighlightColor(), cell, CELL_HEIGHT, demand, true);
                    parent.addComponent(commodity.getMainPanel()).inTL(cellX, top + row * CELL_HEIGHT);
                } else {
                    TooltipMakerAPI more = parent.createUIElement(cell, CELL_HEIGHT, false);
                    more.addPara("+" + (entries.size() - shown) + " more", Misc.getGrayColor(), 7f).setAlignment(Alignment.MID);
                    more.addTooltipToPrevious(new TooltipMakerAPI.TooltipCreator() {
                        @Override public boolean isTooltipExpandable(Object param) { return false; }
                        @Override public float getTooltipWidth(Object param) { return 280f; }
                        @Override public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object param) {
                            for (Map.Entry<String, Integer> entry : entries) {
                                tooltip.addPara(Global.getSettings().getCommoditySpec(entry.getKey()).getName()
                                        + ": " + Misc.getWithDGS(entry.getValue()), 3f);
                            }
                        }
                    }, TooltipMakerAPI.TooltipLocation.ABOVE, false);
                    parent.addUIElement(more).inTL(cellX, top + row * CELL_HEIGHT);
                }
            }
        }
    }

    public static void addSingleLineText(CustomPanelAPI parent, String text, Color color,
                                         float x, float width, float height, Alignment alignment) {
        if (width <= 0f) return;
        LabelAPI label = Global.getSettings().createLabel(text, Fonts.DEFAULT_SMALL);
        float available = Math.max(0f, width - 4f);
        boolean overflow = label.computeTextWidth(text) > available;
        String displayed = text;
        if (overflow) {
            String ellipsis = "\u2026";
            int end = text.length();
            while (end > 0 && label.computeTextWidth(text.substring(0, end).stripTrailing() + ellipsis) > available) {
                end = text.offsetByCodePoints(end, -1);
            }
            displayed = end == 0 && label.computeTextWidth(ellipsis) > available
                    ? "" : text.substring(0, end).stripTrailing() + ellipsis;
        }
        float lineHeight = label.computeTextHeight("Ag");
        float top = Math.max(0f, (height - lineHeight) / 2f);
        label.setText(displayed);
        label.setColor(color);
        label.getPosition().setSize(width, lineHeight);
        label.setAlignment(alignment);
        parent.addComponent(label).inTL(x, top);
        if (overflow) {
            TooltipMakerAPI tooltip = parent.createUIElement(width, lineHeight, false);
            tooltip.addTooltipTo(new TooltipMakerAPI.TooltipCreator() {
                @Override public boolean isTooltipExpandable(Object param) { return false; }
                @Override public float getTooltipWidth(Object param) { return 300f; }
                @Override public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object param) {
                    tooltip.addPara(text, color, 0f);
                }
            }, label, TooltipMakerAPI.TooltipLocation.ABOVE, false);
            parent.addUIElement(tooltip).inTL(x, top);
        }
    }

    public static void addText(CustomPanelAPI parent, String text, Color color,
                               float x, float y, float width, float height) {
        addText(parent, text, color, x, y, width, height, Alignment.MID);
    }

    public static void addText(CustomPanelAPI parent, String text, Color color, float x, float y,
                               float width, float height, Alignment alignment) {
        TooltipMakerAPI tooltip = parent.createUIElement(Math.max(1f, width), height, false);
        LabelAPI label = tooltip.addPara(text, color, 0f);
        label.setAlignment(alignment);
        label.getPosition().inTL(0, Math.max(0f, (height - label.computeTextHeight(text)) / 2f));
        parent.addUIElement(tooltip).inTL(x, y);
    }
}
