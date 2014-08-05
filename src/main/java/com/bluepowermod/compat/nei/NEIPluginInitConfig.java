package com.bluepowermod.compat.nei;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;

import com.bluepowermod.client.gui.GuiProjectTable;
import com.bluepowermod.references.Refs;

/**
 * 
 * @author MineMaarten
 */

public class NEIPluginInitConfig implements IConfigureNEI {
    
    @Override
    public void loadConfig() {
    
        API.registerUsageHandler(new AlloyFurnaceHandler());
        API.registerRecipeHandler(new AlloyFurnaceHandler());
        API.registerGuiOverlayHandler(GuiProjectTable.class, new ProjectTableOverlayHandler(), "crafting");
    }
    
    @Override
    public String getName() {
    
        return "BPNEIHandler";
    }
    
    @Override
    public String getVersion() {
    
        return Refs.fullVersionString();
    }
    
}
