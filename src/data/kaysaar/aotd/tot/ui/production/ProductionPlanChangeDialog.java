package data.kaysaar.aotd.tot.ui.production;

import ashlib.data.plugins.ui.models.BasePopUpDialog;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.ui.*;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLineManager;

/** Thin default-plan dialog. Reuse ProductionPlanChangePanel for individual industries. */
public class ProductionPlanChangeDialog extends BasePopUpDialog {
    public static final float WIDTH = 1150f;
    private final IndustrySpecAPI spec;
    private final Runnable onChanged;
    private ProductionPlanChangePanel content;
    private float availabilityTimer;

    public ProductionPlanChangeDialog(IndustrySpecAPI spec, Runnable onChanged) {
        super("Change Production Plan");
        this.spec = spec;
        this.onChanged = onChanged;
    }

    public ProductionPlanChangePanel getContent() { return content; }

    @Override public void createUI(CustomPanelAPI panel) {
        createHeaader(panel);
        float width = panel.getPosition().getWidth() - 30f;
        // AshLib anchors its footer 40 pixels from the bottom. Leave room above it.
        content = new ProductionPlanChangePanel(width, panel.getPosition().getHeight() - y - 50f,
                spec, AoTDProductionLineManager.getRecipeId(spec.getId()));
        panel.addComponent(content.getMainPanel()).inTL(x, y);
        addPanel(content.getMainPanel());
        createConfirmAndCancelSection(panel);
        confirmButton.setEnabled(content.canConfirm());
    }

    @Override public void advance(float amount) {
        if (content != null && confirmButton != null) {
            availabilityTimer += amount;
            if (availabilityTimer >= 0.5f || confirmButton.isChecked()) {
                availabilityTimer = 0f;
                content.refreshAvailability();
            }
            boolean allowed = content.canConfirm();
            confirmButton.setEnabled(allowed);
            // PopUpUI closes after a checked Confirm, so clear stale/invalid clicks before it handles them.
            if (!allowed && confirmButton.isChecked()) {
                confirmButton.setChecked(false);
                content.setStatus(content.getUnavailableReason());
            }
        }
        super.advance(amount);
    }

    @Override public void applyConfirmScript() {
        if (content == null || !content.canConfirm()) return;
        AoTDProductionLineManager.setRecipeId(spec.getId(), content.getSelectedRecipeId());
        if (onChanged != null) onChanged.run();
    }
}
