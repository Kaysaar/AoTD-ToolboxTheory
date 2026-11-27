package data.kaysaar.aotd.tot.ui.starsystems.stuctures.map;

import ashlib.data.plugins.ui.models.resizable.ResizableComponent;
import ashlib.data.plugins.ui.models.resizable.map.MapEntityComponent;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.PositionAPI;

public class StablePointConstructionComponent extends ResizableComponent {
    private final ChooseStablePointForBuildingMap map;
    private final StructurePlacementHologram hologram;

    public StablePointConstructionComponent(ChooseStablePointForBuildingMap map, String customEntitySpecId) {
        this.map = map;
        hologram = new StructurePlacementHologram(customEntitySpecId);
        componentPanel = Global.getSettings().createCustom(1,1,this);
    }

    @Override
    public void advance(float amount) {
        hologram.advance(amount);
    }

    @Override
    public void render(float alphaMult) {
        MapEntityComponent point = map.getSelectedPoint();
        if (point == null) return;
        float spriteScale = Math.max(point.scale, 24f / Math.max(1f, Math.max(hologram.spriteWidth, hologram.spriteHeight)));
        PositionAPI position = point.getComponentPanel().getPosition();
        hologram.render(position.getCenterX(), position.getCenterY(), spriteScale, point.getToken().getFacing() - 90f, alphaMult);
    }
}
