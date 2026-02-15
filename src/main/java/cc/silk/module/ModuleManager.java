package cc.silk.module;

import cc.silk.module.modules.client.Client;
import cc.silk.module.modules.client.ClientSettingsModule;
import cc.silk.module.modules.client.Debugger;
import cc.silk.module.modules.client.KeybindsModule;
import cc.silk.module.modules.client.NewClickGUIModule;
import cc.silk.module.modules.client.Secret;
import cc.silk.module.modules.combat.*;
import cc.silk.module.modules.misc.*;
import cc.silk.module.modules.player.*;
import cc.silk.module.modules.render.*;
import cc.silk.module.modules.movement.AutoFirework;
import cc.silk.module.modules.movement.AutoHeadHitter;
import cc.silk.module.modules.movement.KeepSprint;
import cc.silk.module.modules.movement.SnapTap;
import cc.silk.module.modules.movement.Sprint;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
public final class ModuleManager {

    private final List<Module> modules = new java.util.ArrayList<>();

    public ModuleManager() {
        addModules();
    }

    public List<Module> getEnabledModules() {
        return modules.stream()
                .filter(Module::isEnabled)
                .toList();
    }

    public List<Module> getModulesInCategory(Category category) {
        return modules.stream()
                .filter(module -> module.getModuleCategory() == category)
                .toList();
    }

    public List<Module> getModulesByCategory(Category category) {
        return getModulesInCategory(category);
    }

    public <T extends Module> Optional<T> getModule(Class<T> moduleClass) {
        return modules.stream()
                .filter(module -> module.getClass().equals(moduleClass))
                .map(moduleClass::cast)
                .findFirst();
    }

    private void addModules() {
        // Combat
        add(
           new HitCob(), new KeyLava(), new AutoMace(), new Triggerbot(), new AutoCart(),new XbowCart());
        // Movement
        add( 
           new Sprint(), new AutoFirework(), new AutoHeadHitter(), new KeepSprint(), new SnapTap());

        // Player
       add(
                new AutoWeb(), new PingSpoof(), new AutoDrain(), new FastEXP());

        // Render
        add(   
                new ContainerSlots(), 
                new ArrayList());
        // Misc
        add(
                new HoverTotem(), new CartKey());

        // Client
        add(    
                new NewClickGUIModule(), new ClientSettingsModule(), new Client(), new Debugger(),
                new Secret(), new KeybindsModule());
    }

    private void add(Module... mods) {
        modules.addAll(Arrays.asList(mods));
    }
}
